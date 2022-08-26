package zstreamer.http.service.file;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.concurrent.FastThreadLocal;
import org.apache.tika.Tika;
import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.http.ContextHandler;
import zstreamer.http.service.AbstractHttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * @author 张贝易
 * 处理文件上传下载的handler
 */
@RequestPath(value = "/file/{fileName}")
public class FileHandler extends AbstractHttpHandler {
    private static final Tika TIKA = new Tika();
    /**
     * uploader是有状态的，所以需要对不同的channel保存不同的uploader
     */
    private static final FastThreadLocal<HashMap<ChannelId, FileUploader>> UP_LOADERS = new FastThreadLocal<>();
    private final FileDownLoader downLoader = new FileDownLoader();

    /**
     * 请求开始时，检测当前线程是否有upLoader的threadLocal值
     * 没有就注入进去
     */
    @Override
    protected void requestStart(ChannelHandlerContext ctx) throws Exception {
        if (UP_LOADERS.get() == null) {
            UP_LOADERS.set(new HashMap<>());
        }
        super.requestStart(ctx);
    }

    /**
     * 请求结束时，关闭当前线程的uploader
     *
     * @param ctx 上下文
     */
    @Override
    protected void requestEnd(ChannelHandlerContext ctx) throws Exception {
        closeAndRemoveUploader(ctx);
        super.requestEnd(ctx);
    }

    @Override
    protected boolean handleGet(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        downLoader.handleHeader(ctx, (DefaultHttpRequest) msg);
        return true;
    }

    @Override
    protected boolean handlePost(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        FileUploader uploader = null;
        boolean finished = false;
        HashMap<ChannelId, FileUploader> upLoaders = UP_LOADERS.get();
        if (upLoaders.containsKey(ctx.channel().id())) {
            uploader = upLoaders.get(ctx.channel().id());
        } else {
            uploader = new FileUploader();
            upLoaders.put(ctx.channel().id(), uploader);
        }
        if (msg instanceof DefaultHttpRequest) {
            uploader.handleHeader(ctx, (DefaultHttpRequest) msg);
        } else {
            if (uploader.handleContent(ctx, (DefaultHttpContent) msg)) {
                finished = true;
                uploader.close();
            }
        }
        return finished;
    }

    @Override
    protected void onException(ChannelHandlerContext ctx) throws Exception {
        closeAndRemoveUploader(ctx);
    }

    /**
     * 文件下载工具
     */
    private static class FileDownLoader {
        protected void handleHeader(ChannelHandlerContext ctx, DefaultHttpRequest request) throws IOException {
            String fileName = ContextHandler.getMessageInfo(ctx).getRestfulUrl().getParam("fileName");
            String range = request.headers().get(HttpHeaderNames.RANGE);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName;
            File file = new File(filePath);
            if (range == null) {
                responseFile(file, ctx);
            } else {
                long[] ranges = handleRange(range, file);
                responseRageFile(file, ctx, ranges[0], ranges[1]);
            }
        }

        /**
         * 用fileRegion发送文件，为了使用其中的transferTo来加速
         * 响应头和content是分开发送的
         *
         * @param file 要发送的文件
         * @param ctx  上下文
         */
        private void responseFile(File file, ChannelHandlerContext ctx) throws IOException {
            String mimeType = TIKA.detect(file);
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            ctx.writeAndFlush(response);
            if (Config.SSL_ENABLED) {
                ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(file)));
            } else {
                ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length()));
            }
        }


        /**
         * 用fileRegion发送文件，为了使用其中的transferTo来加速
         * 这是对于Range的特殊处理，发送文件的某一部分
         * 响应头和content是分开发送的
         *
         * @param file  要发送的文件
         * @param ctx   上下文
         * @param start 文件起始位置
         * @param end   文件结束位置
         */
        private void responseRageFile(File file, ChannelHandlerContext ctx, long start, long end) throws IOException {
            long contentSize = end - start + 1;
            String mimeType = TIKA.detect(file);
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentSize);
            response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + file.length());
            if (Config.SSL_ENABLED) {
                ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(new RandomAccessFile(file, "r"), start, contentSize, Config.FILE_CHUNK_SIZE)));
            } else {
                ctx.writeAndFlush(new DefaultFileRegion(file, start, contentSize));
            }
        }

        private long[] handleRange(String rangeStr, File file) {
            if (rangeStr == null) {
                return new long[]{0, file.length() - 1};
            }
            rangeStr = rangeStr.substring(6);
            String[] ranges = rangeStr.split("-");
            return new long[]{Long.parseLong(ranges[0]), ranges.length > 1 ? Long.parseLong(ranges[1]) : file.length() - 1};
        }
    }

    /**
     * 文件上传工具
     */
    private static class FileUploader {
        private FileChannel fileChannel;
        private long fileSize = 0;
        private long writtenSize = 0;

        protected void handleHeader(ChannelHandlerContext ctx, DefaultHttpRequest request) throws Exception {
            fileSize = Long.parseLong(request.headers().get(HttpHeaderNames.CONTENT_LENGTH));
            String fileName = ContextHandler.getMessageInfo(ctx).getRestfulUrl().getParam("fileName");
            String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
            String fileType = contentType.substring(contentType.lastIndexOf('/') + 1);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName + "." + fileType;
            fileChannel = new FileOutputStream(filePath).getChannel();
        }

        protected boolean handleContent(ChannelHandlerContext ctx, DefaultHttpContent request) throws Exception {
            writtenSize += request.content().readableBytes();
            fileChannel.write(request.content().nioBuffer());
            return writtenSize == fileSize;
        }

        protected void close() throws Exception {
            fileSize = 0;
            writtenSize = 0;
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close();
            }
        }
    }

    /**
     * 关闭并将uploader移除
     *
     * @param ctx 上下文
     */
    private void closeAndRemoveUploader(ChannelHandlerContext ctx) throws Exception {
        HashMap<ChannelId, FileUploader> upLoaders = UP_LOADERS.get();
        FileUploader uploader = upLoaders.get(ctx.channel().id());
        if (uploader != null) {
            uploader.close();
            upLoaders.remove(ctx.channel().id());
        }
    }
}

package zstreamer.http.file;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import org.apache.tika.Tika;
import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.http.AbstractHttpHandler;
import zstreamer.http.WrappedHttpRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 处理文件上传下载的handler
 */
@ChannelHandler.Sharable
@RequestPath(value = "/file/{fileName}")
public class FileHandler extends AbstractHttpHandler {
    private static final Tika TIKA = new Tika();
    /**
     * uploader是有状态的，所以需要对不同的channel保存不同的uploader
     */
    private static final ConcurrentHashMap<ChannelId, FileUploader> UP_LOADERS = new ConcurrentHashMap<>();
    private final FileDownLoader downLoader = new FileDownLoader();

    @Override
    protected boolean handleGet(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        downLoader.handleHeader(ctx, (WrappedHttpRequest) msg);
        return true;
    }

    @Override
    protected boolean handlePost(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        FileUploader uploader = null;
        boolean finished = false;
        if (UP_LOADERS.containsKey(ctx.channel().id())) {
            uploader = UP_LOADERS.get(ctx.channel().id());
        } else {
            uploader = new FileUploader();
            UP_LOADERS.put(ctx.channel().id(), uploader);
        }
        if (msg instanceof WrappedHttpRequest) {
            uploader.handleHeader(ctx, (WrappedHttpRequest) msg);
        } else {
            if (uploader.handleContent(ctx, (DefaultHttpContent) msg)) {
                finished = true;
                uploader.close();
                UP_LOADERS.remove(ctx.channel().id());
            }
        }
        return finished;
    }

    @Override
    protected void onException(ChannelHandlerContext ctx) throws Exception {
        FileUploader uploader = UP_LOADERS.get(ctx.channel().id());
        if (uploader != null) {
            uploader.close();
            UP_LOADERS.remove(ctx.channel().id());
        }
    }

    private static class FileDownLoader {
        protected void handleHeader(ChannelHandlerContext context, WrappedHttpRequest msg) throws IOException {
            String fileName = msg.getParam("fileName");
            String range = msg.headers().get(HttpHeaderNames.RANGE);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName;
            File file = new File(filePath);
            if (range == null) {
                responseFile(file, context);
            } else {
                long[] ranges = handleRange(range, file);
                responseRageFile(file, context, ranges[0], ranges[1]);
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
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentSize);
            response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + file.length());
            ctx.writeAndFlush(response);
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

    private static class FileUploader {
        private FileChannel fileChannel;
        private long fileSize = 0;
        private long writtenSize = 0;

        protected void handleHeader(ChannelHandlerContext ctx, WrappedHttpRequest msg) throws Exception {
            fileSize = Long.parseLong(msg.headers().get(HttpHeaderNames.CONTENT_LENGTH));
            String fileName = msg.getParam("fileName");
            String contentType = msg.headers().get(HttpHeaderNames.CONTENT_TYPE);
            String fileType = contentType.substring(contentType.lastIndexOf('/') + 1);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName + "." + fileType;
            fileChannel = new FileOutputStream(filePath).getChannel();
        }

        protected boolean handleContent(ChannelHandlerContext ctx, DefaultHttpContent msg) throws Exception {
            writtenSize += msg.content().readableBytes();
            fileChannel.write(msg.content().nioBuffer());
            if (writtenSize == fileSize) {
                DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                res.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
                ctx.channel().writeAndFlush(res);
                return true;
            }
            return false;
        }

        protected void close() throws Exception {
            fileSize = 0;
            writtenSize = 0;
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        FileUploader uploader = UP_LOADERS.get(ctx.channel().id());
        if (uploader != null) {
            uploader.close();
            UP_LOADERS.remove(ctx.channel().id());
        }
        super.channelInactive(ctx);
    }
}

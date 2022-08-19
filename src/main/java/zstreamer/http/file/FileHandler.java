package zstreamer.http.file;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.commons.util.WrappedHttpRequest;
import zstreamer.http.AbstractHttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 处理文件上传下载的handler
 */
@ChannelHandler.Sharable
@RequestPath(value = "/file/{fileName}")
public class FileHandler extends AbstractHttpHandler {
    /**
     * uploader是有状态的，所以需要对不同的channel保存不同的uploader
     */
    private static final ConcurrentHashMap<ChannelId, FileUploader> UP_LOADERS = new ConcurrentHashMap<>();
    private final FileDownLoader downLoader = new FileDownLoader();

    @Override
    protected void handleGet(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        downLoader.handleHeader(ctx, (WrappedHttpRequest) msg);
    }

    @Override
    protected void handlePost(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        FileUploader uploader = null;
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
                uploader.close();
                UP_LOADERS.remove(ctx.channel().id());
            }
        }
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
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName;
            responseFile(filePath, context);
        }

        /**
         * 用fileRegion发送文件，为了使用其中的transferTo来加速
         * 响应头和content是分开发送的
         *
         * @param filePath 文件路径
         * @param ctx      上下文
         */
        private void responseFile(String filePath, ChannelHandlerContext ctx) throws IOException {
            File file = new File(filePath);
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set("content-length", file.length());
            ctx.writeAndFlush(response);
            if (Config.SSL_ENABLED) {
                ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(file)));
            } else {
                ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length()));
            }
        }
    }

    private static class FileUploader {
        private FileChannel fileChannel;
        private long fileSize = 0;
        private long writtenSize = 0;

        protected void handleHeader(ChannelHandlerContext ctx, WrappedHttpRequest msg) throws Exception {
            fileSize = Long.parseLong(msg.headers().get("content-length"));
            String fileName = msg.getParam("fileName");
            String contentType = msg.headers().get("content-type");
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
                res.headers().set("content-length", "0");
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
        UP_LOADERS.remove(ctx.channel().id());
        super.channelInactive(ctx);
    }

}

package zstreamer.http.service.file;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedContent;
import zstreamer.http.entity.request.WrappedHead;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.WrappedResponse;
import zstreamer.http.entity.response.file.FileResponse;
import zstreamer.http.service.AbstractHttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author 张贝易
 * 处理文件上传下载的handler
 */
@RequestPath(value = "/file/{fileName}")
public class FileHandler extends AbstractHttpHandler {
    private static final String UPLOADER_NAME = "upLoader";
    private final FileDownLoader downLoader = new FileDownLoader();

    @Override
    protected WrappedResponse handleGet(WrappedRequest msg) throws Exception {
        return downLoader.handleHeader((WrappedHead) msg);
    }

    @Override
    protected WrappedResponse handlePost(WrappedRequest msg) throws Exception {
        Object param = msg.getParam(UPLOADER_NAME);
        if (param == null) {
            param = new FileUploader();
            msg.setParam(UPLOADER_NAME, param);
        }
        FileUploader uploader = (FileUploader) param;

        if (msg instanceof WrappedHead) {
            uploader.handleHeader((WrappedHead) msg);
        } else {
            try {
                if (uploader.handleContent((WrappedContent) msg)) {
                    uploader.close();
                    return InstanceTool.getEmptyOkResponse(msg);
                }
            } catch (Exception e) {
                uploader.close();
                return InstanceTool.getExceptionResponse(msg);
            }
        }
        return null;
    }

    /**
     * 文件下载工具
     */
    private static class FileDownLoader {
        protected FileResponse handleHeader(WrappedHead request) throws IOException {
            String fileName = (String) request.getParam("fileName");
            String range = request.headers().get(HttpHeaderNames.RANGE);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName;
            File file = new File(filePath);
            if (range == null) {
                return responseFile(file, request);
            } else {
                long[] ranges = handleRange(range, file);
                return responseRageFile(file, request, ranges[0], ranges[1]);
            }
        }

        /**
         * 用fileRegion发送文件，为了使用其中的transferTo来加速
         * 响应头和content是分开发送的
         *
         * @param file 要发送的文件
         */
        private FileResponse responseFile(File file, WrappedRequest request) throws IOException {
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            return new FileResponse(response, request, file, 0, file.length());
        }


        /**
         * 用fileRegion发送文件，为了使用其中的transferTo来加速
         * 这是对于Range的特殊处理，发送文件的某一部分
         * 响应头和content是分开发送的
         *
         * @param file  要发送的文件
         * @param start 文件起始位置
         * @param end   文件结束位置
         */
        private FileResponse responseRageFile(File file, WrappedRequest request, long start, long end) throws IOException {
            long contentSize = end - start + 1;
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
            return new FileResponse(response, request, file, start, contentSize);
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

        protected void handleHeader(WrappedHead request) throws Exception {
            fileSize = Long.parseLong(request.headers().get(HttpHeaderNames.CONTENT_LENGTH));
            String fileName = (String) request.getParam("fileName");
            String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
            String fileType = contentType.substring(contentType.lastIndexOf('/') + 1);
            if (fileName == null) {
                throw new NullPointerException();
            }
            String filePath = Config.BASE_URL + fileName + "." + fileType;
            fileChannel = new FileOutputStream(filePath).getChannel();
        }

        protected boolean handleContent(WrappedContent request) throws Exception {
            ByteBuf content = request.getContent();
            writtenSize += content.readableBytes();
            fileChannel.write(content.nioBuffer());
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
}

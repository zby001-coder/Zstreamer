package zstreamer.http.entity.response.file;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.tika.Tika;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.WrappedResponse;

import java.io.File;
import java.io.IOException;

/**
 * @author 张贝易
 * 为传输文件类型的响应设计
 */
public class FileResponse extends WrappedResponse {
    private static final Tika TIKA = new Tika();
    private final File file;
    private final long offSet;
    private final long size;

    public FileResponse(DefaultHttpResponse header, WrappedRequest request, File file, long offSet, long size) {
        super(header, request);
        if (!file.exists()) {
            throw new NullPointerException();
        }
        //解析文件类型
        try {
            String mimeType = TIKA.detect(file);
            header.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //解析长度和范围
        header.headers().set(HttpHeaderNames.CONTENT_LENGTH, size);
        header.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + offSet + "-" + (offSet + size - 1) + "/" + file.length());
        this.file = file;
        this.offSet = offSet;
        this.size = size;
    }

    public File getFile() {
        return file;
    }

    public long getOffSet() {
        return offSet;
    }

    public long getSize() {
        return size;
    }
}

package zstreamer.http.entity.response.file;

import io.netty.handler.codec.http.DefaultHttpResponse;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

import java.io.File;

public class FileResponse extends AbstractWrappedResponse {
    private final File file;
    private final long offSet;
    private final long size;

    public FileResponse(DefaultHttpResponse header, WrappedRequest request, File file, long offSet, long size) {
        super(header, request);
        if (!file.exists()) {
            throw new NullPointerException();
        }
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

package zstreamer.http.entity.response;

import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.DefaultHttpResponse;
import zstreamer.http.entity.request.RequestInfo;
import zstreamer.http.entity.request.WrappedRequest;

public abstract class AbstractWrappedResponse extends DefaultHttpObject {
    private final DefaultHttpResponse delegate;
    private final WrappedRequest request;

    public AbstractWrappedResponse(DefaultHttpResponse delegate, WrappedRequest request) {
        this.delegate = delegate;
        this.request = request;
    }

    public RequestInfo getRequestInfo() {
        return request.getRequestInfo();
    }

    public DefaultHttpResponse getDelegate() {
        return delegate;
    }

    public void finish() {
        request.finish();
    }
}

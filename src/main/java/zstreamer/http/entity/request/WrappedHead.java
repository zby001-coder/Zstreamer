package zstreamer.http.entity.request;

import io.netty.handler.codec.http.HttpRequest;

public class WrappedHead extends WrappedRequest {
    public WrappedHead(HttpRequest delegate,RequestInfo requestInfo) {
        super(delegate,requestInfo);
    }

    @Override
    public boolean isEnd() {
        return false;
    }
}

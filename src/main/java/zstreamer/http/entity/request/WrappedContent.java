package zstreamer.http.entity.request;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCounted;

public class WrappedContent extends WrappedRequest implements ReferenceCounted {
    private final HttpContent convenientDelegate;

    public WrappedContent(HttpContent delegate,RequestInfo requestInfo) {
        super(delegate,requestInfo);
        this.convenientDelegate = delegate;
    }

    public boolean isLast() {
        return convenientDelegate instanceof LastHttpContent;
    }

    public ByteBuf getContent() {
        return convenientDelegate.content();
    }

    @Override
    public int refCnt() {
        return convenientDelegate.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        convenientDelegate.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        convenientDelegate.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        convenientDelegate.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        convenientDelegate.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return convenientDelegate.release();
    }

    @Override
    public boolean release(int decrement) {
        return convenientDelegate.release(decrement);
    }

    @Override
    public boolean isEnd() {
        return delegate instanceof LastHttpContent;
    }
}

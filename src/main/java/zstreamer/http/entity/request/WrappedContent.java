package zstreamer.http.entity.request;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCounted;

public class WrappedContent extends WrappedRequest implements ReferenceCounted {
    private final HttpContent convenientDelegate;

    public WrappedContent(ChannelId id, HttpContent delegate) {
        super(id, delegate);
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
        return convenientDelegate.retain();
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return convenientDelegate.retain(increment);
    }

    @Override
    public ReferenceCounted touch() {
        return convenientDelegate.touch();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return convenientDelegate.touch(hint);
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

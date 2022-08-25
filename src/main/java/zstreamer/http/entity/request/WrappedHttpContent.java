package zstreamer.http.entity.request;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;

/**
 * @author 张贝易
 * 将解析后的DefaultHttpContent包装起来
 */
public class WrappedHttpContent extends WrappedHttpObject {
    public WrappedHttpContent(RequestState state, DefaultHttpContent delegate) {
        super(state, delegate);
    }

    public ByteBuf content() {
        return ((DefaultHttpContent) delegate).content();
    }
}

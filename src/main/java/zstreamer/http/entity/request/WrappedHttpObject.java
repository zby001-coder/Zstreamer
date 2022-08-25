package zstreamer.http.entity.request;

import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.HttpMethod;

/**
 * @author 张贝易
 * 将netty的HttpObject包装成自己的
 * 让content也保存method和url
 */
public abstract class WrappedHttpObject extends DefaultHttpObject {
    protected final RequestState state;
    protected final DefaultHttpObject delegate;

    public WrappedHttpObject(RequestState state, DefaultHttpObject delegate) {
        this.state = state;
        this.delegate = delegate;
    }

    public HttpMethod getMethod() {
        return state.getCurrentMethod();
    }

    public RequestState getState() {
        return state;
    }

    public DefaultHttpObject getDelegate() {
        return delegate;
    }
}

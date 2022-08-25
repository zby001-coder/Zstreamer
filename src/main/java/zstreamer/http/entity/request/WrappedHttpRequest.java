package zstreamer.http.entity.request;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.service.AbstractHttpHandler;

/**
 * @author 张贝易
 * 将解析后的DefaultHttpRequest包装起来
 */
public class WrappedHttpRequest extends WrappedHttpObject {
    protected final UrlResolver.RestfulUrl restfulUrl;
    protected final Class<? extends AbstractHttpHandler> handlerClz;

    public WrappedHttpRequest(UrlResolver.RestfulUrl restfulUrl, RequestState state, DefaultHttpRequest delegate, Class<? extends AbstractHttpHandler> handlerClz) {
        super(state, delegate);
        this.handlerClz = handlerClz;
        this.restfulUrl = restfulUrl;
    }

    public Class<? extends AbstractHttpHandler> getHandlerClz() {
        return handlerClz;
    }

    public String getParam(String key) {
        return restfulUrl.getParam(key);
    }

    public HttpHeaders headers() {
        return ((DefaultHttpRequest) delegate).headers();
    }
}

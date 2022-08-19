package zstreamer.http;

import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import zstreamer.commons.util.UrlResolver;

/**
 * @author 张贝易
 * 将解析后的DefaultHttpRequest包装起来
 */
public class WrappedHttpRequest extends DefaultHttpObject {
    private final DefaultHttpRequest request;
    private final UrlResolver.RestfulUrl restfulUrl;

    public WrappedHttpRequest(DefaultHttpRequest request, UrlResolver.RestfulUrl restfulUrl) {
        this.request = request;
        this.restfulUrl = restfulUrl;
    }

    public String getParam(String key) {
        return restfulUrl.getParam(key);
    }

    public String getUrl() {
        return restfulUrl.getUrl();
    }

    public HttpHeaders headers(){
        return request.headers();
    }

    public HttpMethod method(){
        return request.method();
    }

    public DefaultHttpRequest getRequest() {
        return request;
    }
}

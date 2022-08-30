package zstreamer.http.entity.request;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
import java.util.List;

/**
 * @author 张贝易
 * 封装后的请求的抽象类
 */
public abstract class WrappedRequest {
    /**
     * 保存请求信息
     */
    protected final RequestInfo requestInfo;
    protected final HttpObject delegate;

    public WrappedRequest(HttpObject delegate,RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
        this.delegate = delegate;
    }

    public Object getParam(String key) {
        return requestInfo.getParam(key);
    }

    public void setParam(String key, Object value) {
        requestInfo.setParam(key, value);
    }

    public String url() {
        return requestInfo.getUrl();
    }

    public HashMap<String, Object> getParams() {
        return requestInfo.getParams();
    }

    public HttpHeaders headers() {
        return requestInfo.headers();
    }

    public HttpMethod method() {
        return requestInfo.getMethod();
    }

    public UrlClassTier.ClassInfo<AbstractHttpHandler> getHandlerInfo() {
        return requestInfo.getHandlerInfo();
    }

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> getFilterInfo() {
        return requestInfo.getFilterInfo();
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    /**
     * 确定当前请求是否为整个请求的最后一个分段
     *
     * @return 是否为最后一个分段
     */
    public abstract boolean isEnd();
}

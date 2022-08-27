package zstreamer.http.entity.request;

import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
import java.util.List;

public class WrappedRequest {
    private static final String LENGTH_NAME = "length";
    private static final FastThreadLocal<HashMap<ChannelId, RequestInfo>> REQUEST_INFO = new FastThreadLocal<>();
    private final ChannelId id;
    private final HttpObject delegate;

    public WrappedRequest(ChannelId id, HttpContent delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    /**
     * 在异常情况下可以创建一个内容都为空的WrappedRequest
     *
     * @param id       channel的id
     * @param delegate 请求头
     */
    public WrappedRequest(ChannelId id, HttpRequest delegate) {
        this.id = id;
        this.delegate = delegate;
        //检测threadLocal初始化情况
        HashMap<ChannelId, RequestInfo> allInfo = REQUEST_INFO.get();
        if (allInfo == null) {
            allInfo = new HashMap<>();
            REQUEST_INFO.set(allInfo);
        }
        RequestInfo requestInfo = new RequestInfo(delegate.headers(), delegate.uri(), delegate.method(), null, null);
        allInfo.put(id, requestInfo);
    }

    public WrappedRequest(ChannelId id, HttpRequest delegate, UrlResolver.RestfulUrl urlInfo,
                          UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo,
                          List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo) {
        this.id = id;
        this.delegate = delegate;
        //检测threadLocal初始化情况
        HashMap<ChannelId, RequestInfo> allInfo = REQUEST_INFO.get();
        if (allInfo == null) {
            allInfo = new HashMap<>();
            REQUEST_INFO.set(allInfo);
        }
        //设置参数
        RequestInfo requestInfo = new RequestInfo(delegate.headers(), urlInfo.getUrl(), delegate.method(), handlerInfo, filterInfo);
        requestInfo.setParams(urlInfo.getParams());
        allInfo.put(id, requestInfo);
    }

    public ChannelId getId() {
        return id;
    }

    public HttpObject getDelegate() {
        return delegate;
    }

    public Object getParam(String key) {
        return REQUEST_INFO.get().get(id).getParam(key);
    }

    public void setParam(String key, Object value) {
        REQUEST_INFO.get().get(id).setParam(key, value);
    }

    public String url() {
        return REQUEST_INFO.get().get(id).getUrl();
    }

    public HashMap<String, Object> getParams() {
        return REQUEST_INFO.get().get(id).getParams();
    }

    public HttpHeaders headers() {
        return REQUEST_INFO.get().get(id).headers();
    }

    public HttpMethod method() {
        return REQUEST_INFO.get().get(id).getMethod();
    }

    public UrlClassTier.ClassInfo<AbstractHttpHandler> getHandlerInfo() {
        return REQUEST_INFO.get().get(id).getHandlerInfo();
    }

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> getFilterInfo() {
        return REQUEST_INFO.get().get(id).getFilterInfo();
    }

    public RequestInfo getRequestInfo() {
        return REQUEST_INFO.get().get(id);
    }

    public void finish() {
        REQUEST_INFO.get().remove(id);
    }
}

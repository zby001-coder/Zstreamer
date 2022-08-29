package zstreamer.http.entity.request;

import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.concurrent.FastThreadLocal;
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
     * 保存请求信息的哈希表
     */
    protected static final FastThreadLocal<HashMap<ChannelId, RequestInfo>> REQUEST_INFO = new FastThreadLocal<>();
    /**
     * 一个channel对应一个请求
     */
    protected final ChannelId id;
    protected final HttpObject delegate;

    public WrappedRequest(ChannelId id, HttpObject delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    public ChannelId getId() {
        return id;
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

    /**
     * 确定当前请求是否为整个请求的最后一个分段
     *
     * @return 是否为最后一个分段
     */
    public abstract boolean isEnd();

    /**
     * 在整个请求处理完后调用，清除该请求的信息
     */
    public void finish() {
        REQUEST_INFO.get().remove(id);
    }
}

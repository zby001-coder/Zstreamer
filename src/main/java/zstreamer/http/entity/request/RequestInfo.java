package zstreamer.http.entity.request;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RequestInfo {
    private final HashMap<String, Object> params = new HashMap<>();
    private final HttpHeaders headers;
    private final String url;
    private final HttpMethod method;
    private final UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo;
    private final List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo;

    public RequestInfo(HttpHeaders headers, String url, HttpMethod method,
                       UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo,
                       List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo) {
        this.headers = headers;
        this.url = url;
        this.method = method;
        this.handlerInfo = handlerInfo;
        this.filterInfo = filterInfo;
    }

    public HashMap<String, Object> getParams() {
        return params;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public void setParam(String key, Object value) {
        params.put(key, value);
    }

    public void setParams(HashMap<String, ?> params) {
        this.params.putAll(params);
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public UrlClassTier.ClassInfo<AbstractHttpHandler> getHandlerInfo() {
        return handlerInfo;
    }

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> getFilterInfo() {
        if (filterInfo!=null){
            return new ArrayList<>(filterInfo);
        }
        return new ArrayList<>();
    }
}

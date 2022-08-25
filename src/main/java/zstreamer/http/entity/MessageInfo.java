package zstreamer.http.entity;

import io.netty.handler.codec.http.HttpMethod;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.List;

public class MessageInfo {
    public static final int DISABLED = 0;
    public static final int RECEIVED_HEADER = 1;
    public static final int RESPOND_HEADER = 2;

    private final HttpMethod currentMethod;
    private final UrlResolver.RestfulUrl restfulUrl;
    private final UrlClassTier.ClassInfo<AbstractHttpHandler> classInfo;
    private final List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo;
    private int state;

    public MessageInfo(HttpMethod currentMethod, UrlResolver.RestfulUrl restfulUrl,
                       UrlClassTier.ClassInfo<AbstractHttpHandler> classInfo,
                       List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo) {
        this.state = DISABLED;
        this.currentMethod = currentMethod;
        this.restfulUrl = restfulUrl;
        this.classInfo = classInfo;
        this.filterInfo = filterInfo;
    }

    public HttpMethod getCurrentMethod() {
        return currentMethod;
    }

    public UrlResolver.RestfulUrl getRestfulUrl() {
        return restfulUrl;
    }

    public UrlClassTier.ClassInfo<AbstractHttpHandler> getClassInfo() {
        return classInfo;
    }

    public int getState() {
        return state;
    }

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> getFilterInfo() {
        return filterInfo;
    }

    public void setState(int state) {
        this.state = state;
    }
}

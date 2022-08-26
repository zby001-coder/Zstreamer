package zstreamer.http.entity;

import io.netty.handler.codec.http.HttpMethod;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.List;

public class MessageInfo {
    private final HttpMethod currentMethod;
    private final UrlResolver.RestfulUrl restfulUrl;
    private final UrlClassTier.ClassInfo<AbstractHttpHandler> classInfo;
    private final List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo;

    public MessageInfo(HttpMethod currentMethod, UrlResolver.RestfulUrl restfulUrl,
                       UrlClassTier.ClassInfo<AbstractHttpHandler> classInfo,
                       List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo) {
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

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> getFilterInfo() {
        return filterInfo;
    }
}

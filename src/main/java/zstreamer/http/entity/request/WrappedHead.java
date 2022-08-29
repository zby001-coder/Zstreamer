package zstreamer.http.entity.request;

import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpRequest;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
import java.util.List;

public class WrappedHead extends WrappedRequest {
    public WrappedHead(ChannelId id, HttpRequest delegate, UrlResolver.RestfulUrl urlInfo,
                       UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo,
                       List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo) {
        super(id, delegate);
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

    /**
     * 在异常情况下可以创建一个信息为空的WrappedRequest
     *
     * @param id       channel的id
     * @param delegate 请求头
     */
    public WrappedHead(ChannelId id, HttpRequest delegate) {
        super(id, delegate);
        //检测threadLocal初始化情况
        HashMap<ChannelId, RequestInfo> allInfo = REQUEST_INFO.get();
        if (allInfo == null) {
            allInfo = new HashMap<>();
            REQUEST_INFO.set(allInfo);
        }
        RequestInfo requestInfo = new RequestInfo(delegate.headers(), delegate.uri(), delegate.method(), null, null);
        allInfo.put(id, requestInfo);
    }

    @Override
    public boolean isEnd() {
        return false;
    }
}

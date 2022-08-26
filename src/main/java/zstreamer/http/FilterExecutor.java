package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.util.ReferenceCountUtil;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.HttpEvent;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.filter.AbstractHttpFilter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class FilterExecutor extends ChannelDuplexHandler {
    private static final FilterExecutor INSTANCE = new FilterExecutor();
    /**
     * 已经实例化过的单例Filter的缓存
     */
    private static final ConcurrentHashMap<Class<AbstractHttpFilter>, AbstractHttpFilter> INSTANCED_FILTERS = new ConcurrentHashMap<>();

    public static FilterExecutor getInstance() {
        return INSTANCE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof DefaultHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ContextHandler.getMessageInfo(ctx).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            if (!filter.handleIn(ctx, (DefaultHttpRequest) msg)) {
                ReferenceCountUtil.release(msg);
                ctx.fireUserEventTriggered(HttpEvent.FAIL_FILTER);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof DefaultHttpResponse)) {
            ctx.write(msg);
            return;
        }
        MessageInfo messageInfo = ContextHandler.getMessageInfo(ctx);
        if (messageInfo == null) {
            ctx.write(msg);
            return;
        }

        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ContextHandler.getMessageInfo(ctx).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            filter.handleOut((DefaultHttpResponse) msg);
        }
        ctx.write(msg);
    }

    private AbstractHttpFilter instanceFilter(Class<AbstractHttpFilter> clz) throws InstantiationException, IllegalAccessException {
        return InstanceTool.instanceSingleton(INSTANCED_FILTERS, clz);
    }
}

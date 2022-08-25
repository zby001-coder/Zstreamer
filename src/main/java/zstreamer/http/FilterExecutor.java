package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.util.ReferenceCountUtil;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.filter.AbstractHttpFilter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class FilterExecutor extends ChannelDuplexHandler {
    private static final FilterExecutor INSTANCE = new FilterExecutor();

    public static FilterExecutor getInstance() {
        return INSTANCE;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof DefaultHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = getCurrentMessage(ctx).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            if (!filter.handleIn(ctx, (DefaultHttpRequest) msg)) {
                ReferenceCountUtil.release(msg);
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
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = getCurrentMessage(ctx).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            filter.handleOut((DefaultHttpResponse) msg);
        }
        ctx.write(msg);
    }

    private MessageInfo getCurrentMessage(ChannelHandlerContext ctx) {
        return ctx.pipeline().get(RequestResolver.class).getMessageInfo(ctx);
    }

    private AbstractHttpFilter instanceFilter(Class<AbstractHttpFilter> clz) throws InstantiationException, IllegalAccessException {
        ConcurrentHashMap<Class<AbstractHttpFilter>, AbstractHttpFilter> instancedFilters = ContextHandler.INSTANCED_FILTERS;
        AbstractHttpFilter filter = null;
        if (!instancedFilters.containsKey(clz)) {
            //没有实例化过，进行实例化
            synchronized (this) {
                if (!instancedFilters.containsKey(clz)) {
                    filter = clz.newInstance();
                    instancedFilters.put(clz, filter);
                } else {
                    filter = instancedFilters.get(clz);
                }
            }
        } else {
            //从实例化过的handler中获取
            filter = instancedFilters.get(clz);
        }
        return filter;
    }
}

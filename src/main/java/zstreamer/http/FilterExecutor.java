package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;
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
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ((WrappedRequest) msg).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            AbstractWrappedResponse response = filter.handleIn((WrappedRequest) msg);
            if (response != null) {
                ReferenceCountUtil.release(msg);
                ctx.channel().writeAndFlush(response);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ((AbstractWrappedResponse) msg).getRequestInfo().getFilterInfo();
        if (filterInfo != null) {
            for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
                AbstractHttpFilter filter = instanceFilter(info.getClz());
                filter.handleOut((AbstractWrappedResponse) msg);
            }
        }
        ctx.write(msg, promise);
    }

    private AbstractHttpFilter instanceFilter(Class<AbstractHttpFilter> clz) throws InstantiationException, IllegalAccessException {
        return InstanceTool.instanceSingleton(INSTANCED_FILTERS, clz);
    }
}

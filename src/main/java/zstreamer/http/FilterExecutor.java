package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedHead;
import zstreamer.http.entity.response.WrappedResponse;
import zstreamer.http.filter.AbstractHttpFilter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 过滤器执行工具
 */
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
        //获取所有的filter并执行它们的handleIn
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ((WrappedHead) msg).getFilterInfo();
        for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
            AbstractHttpFilter filter = instanceFilter(info.getClz());
            WrappedResponse response = filter.handleIn((WrappedHead) msg);
            //如果filter生成了response，那么可以直接响应了
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
        //获取所有的filter并执行它们的handleOut
        List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = ((WrappedResponse) msg).getRequestInfo().getFilterInfo();
        if (filterInfo != null) {
            for (UrlClassTier.ClassInfo<AbstractHttpFilter> info : filterInfo) {
                AbstractHttpFilter filter = instanceFilter(info.getClz());
                filter.handleOut((WrappedResponse) msg);
            }
        }
        ctx.write(msg, promise);
    }

    private AbstractHttpFilter instanceFilter(Class<AbstractHttpFilter> clz) throws InstantiationException, IllegalAccessException {
        return InstanceTool.instanceSingleton(INSTANCED_FILTERS, clz);
    }
}

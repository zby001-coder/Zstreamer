package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 上下文信息
 */
@ChannelHandler.Sharable
public class ContextHandler extends ChannelDuplexHandler {
    private static final ContextHandler INSTANCE = new ContextHandler();
    /**
     * 已经实例化过的单例Filter的缓存
     */
    public static final ConcurrentHashMap<Class<AbstractHttpFilter>, AbstractHttpFilter> INSTANCED_FILTERS = new ConcurrentHashMap<>();
    /**
     * 已经实例化过的单例Handler的缓存
     */
    public static final ConcurrentHashMap<Class<AbstractHttpHandler>, AbstractHttpHandler> INSTANCED_HANDLERS = new ConcurrentHashMap<>();

    /**
     * 某一个channel当前请求的状态
     */
    public static final FastThreadLocal<HashMap<ChannelId, MessageInfo>> INFO_MAP = new FastThreadLocal<>();

    private ContextHandler() {
    }

    public static ContextHandler getInstance() {
        return INSTANCE;
    }

    public static AbstractHttpHandler instanceHandler(Class<AbstractHttpHandler> clz) throws InstantiationException, IllegalAccessException {
        return instanceSingleton(INSTANCED_HANDLERS, clz);
    }

    public static AbstractHttpFilter instanceFilter(Class<AbstractHttpFilter> clz) throws InstantiationException, IllegalAccessException {
        return instanceSingleton(INSTANCED_FILTERS, clz);
    }

    private static <T> T instanceSingleton(ConcurrentHashMap<Class<T>, T> map, Class<T> clz) throws InstantiationException, IllegalAccessException {
        if (clz == null) {
            return null;
        }
        T singleton = null;
        if (!map.containsKey(clz)) {
            //没有实例化过，进行实例化
            synchronized (ContextHandler.class) {
                if (!map.containsKey(clz)) {
                    singleton = clz.newInstance();
                    map.put(clz, singleton);
                } else {
                    singleton = map.get(clz);
                }
            }
        } else {
            //从实例化过的中获取
            singleton = map.get(clz);
        }
        return singleton;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除当前HTTP请求信息
        ContextHandler.INFO_MAP.get().remove(ctx.channel().id());
        ctx.fireChannelInactive();
    }
}

package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.http.entity.request.WrappedHttpContent;
import zstreamer.http.entity.request.WrappedHttpObject;
import zstreamer.http.entity.request.WrappedHttpRequest;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 根据请求路径解析url，获取参数，分发消息给handler
 */
@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<WrappedHttpObject> {
    /**
     * 已经实例化过的单例Handler的缓存
     */
    private static final ConcurrentHashMap<Class<? extends AbstractHttpHandler>, AbstractHttpHandler> INSTANCED_HANDLERS = new ConcurrentHashMap<>();
    /**
     * 某一个channel当前使用的Handler
     */
    private static final FastThreadLocal<HashMap<ChannelId, AbstractHttpHandler>> CURRENT_HANDLER = new FastThreadLocal<>();

    private static final RequestDispatcher INSTANCE = new RequestDispatcher();

    private RequestDispatcher() {

    }

    public static RequestDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * channel启动时初始化ThreadLocal的信息
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (CURRENT_HANDLER.get() == null) {
            CURRENT_HANDLER.set(new HashMap<>());
        }
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        if (msg instanceof WrappedHttpRequest) {
            handleHeader(ctx, (WrappedHttpRequest) msg);
        } else {
            handleContent(ctx, (WrappedHttpContent) msg);
        }
    }

    /**
     * 处理请求行和请求体
     *
     * @param ctx 上下文
     * @param msg 请求行和请求头
     */
    private void handleHeader(ChannelHandlerContext ctx, WrappedHttpRequest msg) throws Exception {
        //获取对应的class信息
        Class<? extends AbstractHttpHandler> clz = msg.getHandlerClz();

        //获取url对应的handler，修改当前channel使用的handler
        AbstractHttpHandler handler = instanceHandler(clz);
        CURRENT_HANDLER.get().put(ctx.channel().id(), handler);

        //将信息传递下去
        handler.channelRead(ctx, msg);
    }

    /**
     * 处理请求体
     *
     * @param ctx 上下文
     * @param msg 请求体
     */
    private void handleContent(ChannelHandlerContext ctx, WrappedHttpContent msg) throws Exception {
        //根据当前请求使用的handler分发请求体
        AbstractHttpHandler next = CURRENT_HANDLER.get().get(ctx.channel().id());

        //分发content
        if (next != null) {
            next.channelRead(ctx, msg);
        }
    }

    /**
     * 实例化单例Handler
     *
     * @param clz handler的class信息
     * @return 实例化后的handler
     */
    private AbstractHttpHandler instanceHandler(Class<? extends AbstractHttpHandler> clz) throws InstantiationException, IllegalAccessException {
        AbstractHttpHandler handler = null;
        if (!INSTANCED_HANDLERS.containsKey(clz)) {
            //没有实例化过，进行实例化
            synchronized (this) {
                if (!INSTANCED_HANDLERS.containsKey(clz)) {
                    handler = clz.newInstance();
                    INSTANCED_HANDLERS.put(clz, handler);
                } else {
                    handler = INSTANCED_HANDLERS.get(clz);
                }
            }
        } else {
            //从实例化过的handler中获取
            handler = INSTANCED_HANDLERS.get(clz);
        }
        return handler;
    }

    /**
     * channel关闭后需要删除一些信息，防止内存泄漏
     * 由于业务handler不会注入到pipeline中，所以需要手动触发事件
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除当前业务handler信息
        HashMap<ChannelId, AbstractHttpHandler> currentHandlers = CURRENT_HANDLER.get();
        AbstractHttpHandler handler = currentHandlers.get(ctx.channel().id());
        if (handler != null) {
            currentHandlers.remove(ctx.channel().id());
            //触发当前业务handler的关闭事件
            handler.channelInactive(ctx);
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //触发业务handler的异常事件
        CURRENT_HANDLER.get().get(ctx.channel().id()).exceptionCaught(ctx, cause);
        //将异常传递下去
        ctx.fireExceptionCaught(cause);
    }
}

package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpObject;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 根据请求路径解析url，获取参数，分发消息给handler
 */
@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<DefaultHttpObject> {
    /**
     * 已经实例化过的单例Handler的缓存
     */
    public static final ConcurrentHashMap<Class<AbstractHttpHandler>, AbstractHttpHandler> INSTANCED_HANDLERS = new ConcurrentHashMap<>();

    private static final RequestDispatcher INSTANCE = new RequestDispatcher();

    private RequestDispatcher() {

    }

    public static RequestDispatcher getInstance() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        AbstractHttpHandler handler = getServiceHandler(ctx);
        handler.channelRead(ctx, msg);
    }

    /**
     * channel关闭后需要删除一些信息，防止内存泄漏
     * 由于业务handler不会注入到pipeline中，所以需要手动触发事件
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //触发当前业务handler的关闭事件
        getServiceHandler(ctx).channelInactive(ctx);
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //触发业务handler的异常处理流程
        getServiceHandler(ctx).exceptionCaught(ctx, cause);
    }

    private AbstractHttpHandler getServiceHandler(ChannelHandlerContext ctx) throws Exception {
        MessageInfo info = ContextHandler.getMessageInfo(ctx);
        //获取对应的class信息
        Class<AbstractHttpHandler> clz = info.getClassInfo().getClz();
        //获取url对应的handler
        return instanceHandler(clz);
    }

    private AbstractHttpHandler instanceHandler(Class<AbstractHttpHandler> clz) throws InstantiationException, IllegalAccessException {
        return InstanceTool.instanceSingleton(INSTANCED_HANDLERS, clz);
    }
}

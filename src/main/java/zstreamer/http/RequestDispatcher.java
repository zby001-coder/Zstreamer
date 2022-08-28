package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 分发消息给handler
 */
@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<WrappedRequest> {
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
    protected void channelRead0(ChannelHandlerContext ctx, WrappedRequest msg) throws Exception {
        AbstractHttpHandler handler = getServiceHandler(msg);
        handler.channelRead(ctx, msg);
    }

    private AbstractHttpHandler getServiceHandler(WrappedRequest request) throws Exception {
        //获取对应的class信息
        Class<AbstractHttpHandler> clz = request.getHandlerInfo().getClz();
        //获取url对应的handler
        return instanceHandler(clz);
    }

    private AbstractHttpHandler instanceHandler(Class<AbstractHttpHandler> clz) throws InstantiationException, IllegalAccessException {
        return InstanceTool.instanceSingleton(INSTANCED_HANDLERS, clz);
    }
}

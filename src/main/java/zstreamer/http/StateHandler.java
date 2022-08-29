package zstreamer.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.http.entity.response.WrappedResponse;

import java.util.HashMap;

/**
 * @author 张贝易
 * 控制请求是否需要继续读取，启动下一个请求处理流程的handler
 */
@ChannelHandler.Sharable
public class StateHandler extends ChannelDuplexHandler {
    private static final StateHandler INSTANCE = new StateHandler();

    /**
     * 某一个channel当前请求的状态
     */
    private static final FastThreadLocal<HashMap<ChannelId, Boolean>> HANDLE_REQUEST = new FastThreadLocal<>();

    private StateHandler() {
    }

    public static StateHandler getInstance() {
        return INSTANCE;
    }

    public static boolean ifHandleRequest(ChannelHandlerContext ctx) {
        return HANDLE_REQUEST.get().get(ctx.channel().id());
    }

    /**
     * channel启动时初始化ThreadLocal的信息
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (HANDLE_REQUEST.get() == null) {
            HANDLE_REQUEST.set(new HashMap<>());
        }
        HANDLE_REQUEST.get().put(ctx.channel().id(), true);
        ctx.fireChannelActive();
    }

    /**
     * 当一个响应发出时，这个请求的接下来的数据可用忽略
     *
     * @param ctx 上下文
     * @param msg 响应
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        //停止处理该请求
        HANDLE_REQUEST.get().put(ctx.channel().id(), false);
        ctx.write(msg, promise).addListener((future -> {
            //在这个响应写完之后，启动自动读取，因为不会混合响应了
            ctx.channel().config().setAutoRead(true);
            ((WrappedResponse) msg).finish();
        }));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //新请求到了，开启处理
        if (evt instanceof HttpRequest) {
            HANDLE_REQUEST.get().put(ctx.channel().id(), true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //如果异常没有被下层处理，直接关闭整个通道
        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HANDLE_REQUEST.get().remove(ctx.channel().id());
        super.channelInactive(ctx);
    }
}

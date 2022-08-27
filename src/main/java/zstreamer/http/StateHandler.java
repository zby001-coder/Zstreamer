package zstreamer.http;

import io.netty.channel.*;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.http.entity.response.AbstractWrappedResponse;

import java.util.HashMap;

/**
 * @author 张贝易
 * 上下文信息，放在链条最后，使上下文信息最后被删除
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

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise){
        HANDLE_REQUEST.get().put(ctx.channel().id(), false);
        ctx.write(msg, promise).addListener((future -> {
            HANDLE_REQUEST.get().put(ctx.channel().id(), true);
            ((AbstractWrappedResponse) msg).finish();
        }));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HANDLE_REQUEST.get().remove(ctx.channel().id());
        super.channelInactive(ctx);
    }
}

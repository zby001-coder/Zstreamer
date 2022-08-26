package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.HttpEvent;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.entity.MessageState;

import java.util.HashMap;

/**
 * @author 张贝易
 * 上下文信息，放在链条最后，使上下文信息最后被删除
 */
@ChannelHandler.Sharable
public class ContextHandler extends ChannelDuplexHandler {
    private static final ContextHandler INSTANCE = new ContextHandler();

    /**
     * 某一个channel当前请求的状态
     */
    private static final FastThreadLocal<HashMap<ChannelId, MessageInfo>> INFO_MAP = new FastThreadLocal<>();

    /**
     * 某一个channel当前请求的状态
     */
    private static final FastThreadLocal<HashMap<ChannelId, MessageState>> MESSAGE_STATE = new FastThreadLocal<>();

    private ContextHandler() {
    }

    public static ContextHandler getInstance() {
        return INSTANCE;
    }

    public static MessageInfo getMessageInfo(ChannelHandlerContext ctx) {
        return INFO_MAP.get().get(ctx.channel().id());
    }

    public static void putMessageInfo(ChannelHandlerContext ctx, MessageInfo messageInfo) {
        INFO_MAP.get().put(ctx.channel().id(), messageInfo);
    }

    public static MessageState getMessageState(ChannelHandlerContext ctx) {
        return MESSAGE_STATE.get().get(ctx.channel().id());
    }

    /**
     * channel启动时初始化ThreadLocal的信息
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (INFO_MAP.get() == null) {
            INFO_MAP.set(new HashMap<>());
        }
        if (MESSAGE_STATE.get() == null) {
            MESSAGE_STATE.set(new HashMap<>());
        }
        MESSAGE_STATE.get().put(ctx.channel().id(), MessageState.initState());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除当前HTTP请求信息
        ContextHandler.INFO_MAP.get().remove(ctx.channel().id());
        ctx.fireChannelInactive();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof HttpEvent)) {
            return;
        }
        handleEvent((HttpEvent) evt, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleEvent(HttpEvent.EXCEPTION, ctx);
    }

    private void handleEvent(HttpEvent event, ChannelHandlerContext ctx) {
        MessageState newState = MESSAGE_STATE.get().get(ctx.channel().id()).changeState(event);
        MESSAGE_STATE.get().put(ctx.channel().id(), newState);
        switch (event) {
            case NOT_FOUND:
                ctx.writeAndFlush(InstanceTool.getNotFoundResponse());
                return;
            case EXCEPTION:
                ctx.writeAndFlush(InstanceTool.getExceptionResponse());
                return;
            case WRONG_METHOD:
                ctx.writeAndFlush(InstanceTool.getWrongMethodResponse());
                return;
            default:
                break;
        }
        if (newState instanceof MessageState.Error) {
            ctx.writeAndFlush(InstanceTool.getExceptionResponse());
            ctx.channel().close();
        }
    }
}

package zstreamer.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractHttpHandler extends SimpleChannelInboundHandler<DefaultHttpObject> {
    private static final ConcurrentHashMap<ChannelId, ChannelState> CHANNEL_SATES = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        try {
            if (msg instanceof WrappedHttpRequest) {
                handleHeader(ctx, (WrappedHttpRequest) msg);
            } else if (msg instanceof DefaultHttpContent && CHANNEL_SATES.get(ctx.channel().id()).inUse) {
                handleContent(ctx, msg);
            }
        } catch (Exception e) {
            handleException(ctx);
            throw e;
        }
    }

    /**
     * 对Http的header进行处理，主要是确定当前的Method
     * @param ctx 上下文
     * @param msg 包装过的Header信息
     */
    private void handleHeader(ChannelHandlerContext ctx, WrappedHttpRequest msg) throws Exception {
        ChannelState state = CHANNEL_SATES.get(ctx.channel().id());
        HttpMethod method = msg.method();
        if (state == null) {
            CHANNEL_SATES.put(ctx.channel().id(), new ChannelState(method, true));
        } else {
            state.currentMethod = method;
            state.inUse = true;
        }
        dispatchHttpObject(ctx, msg);
    }

    /**
     * 对Body进行处理，这部分直接让儿子们做
     * @param ctx 上下文
     * @param content Body的部分内容
     */
    private void handleContent(ChannelHandlerContext ctx, DefaultHttpObject content) throws Exception {
        dispatchHttpObject(ctx, content);
    }

    /**
     * 根据当前channel的请求类型调用不同的处理方法，这个数据不一定是完整的
     *
     * @param ctx 上下文
     * @param msg 请求数据的一部分
     */
    private void dispatchHttpObject(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        HttpMethod currentMethod = CHANNEL_SATES.get(ctx.channel().id()).currentMethod;
        boolean finished = false;
        if (currentMethod.equals(HttpMethod.GET)) {
            finished = handleGet(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.POST)) {
            finished = handlePost(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.PUT)) {
            finished = handlePut(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.DELETE)) {
            finished = handleDelete(ctx, msg);
        } else {
            finished = true;
            handleWrongMethod(ctx);
        }
        if (finished) {
            endResponse(ctx);
        }
    }

    /**
     * 处理并响应Get请求
     *
     * @return 返回true表明这个请求已经处理完了，可以进行endResponse操作了
     */
    protected boolean handleGet(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
        return true;
    }

    protected boolean handlePost(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
        return true;
    }

    protected boolean handlePut(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
        return true;
    }

    protected boolean handleDelete(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
        return true;
    }

    /**
     * 由于HTTPCodec需要使用LastContent重置状态才能发送下一个响应
     * 所以在响应完成后需要write一个LastContent
     *
     * @param ctx 上下文
     */
    private void endResponse(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new DefaultLastHttpContent());
    }

    protected abstract void onException(ChannelHandlerContext ctx) throws Exception;

    private void handleWrongMethod(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
        response.headers().set("content-length", 0);
        ctx.writeAndFlush(response);
    }

    private void handleException(ChannelHandlerContext ctx) throws Exception {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        res.headers().set("content-length", "0");
        ctx.channel().writeAndFlush(res);
        ChannelState state = CHANNEL_SATES.get(ctx.channel().id());
        state.inUse = false;
        state.currentMethod = null;
        onException(ctx);
    }

    private static class ChannelState {
        private HttpMethod currentMethod;
        /**
         * inUse字段用来处理Http1.1的流水线发送
         * 如果某个请求在数据处理到一半的时候就确定不用处理了，可以抛出异常，使当前状态变为不可用
         * 那么这个请求的剩余部分就不会被继续处理，直到流水线下一个请求过来才继续开放
         */
        private boolean inUse;

        public ChannelState(HttpMethod currentMethod, boolean inUse) {
            this.currentMethod = currentMethod;
            this.inUse = inUse;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CHANNEL_SATES.remove(ctx.channel().id());
        super.channelInactive(ctx);
    }
}

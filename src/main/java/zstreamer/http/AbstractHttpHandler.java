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

    private void handleContent(ChannelHandlerContext ctx, DefaultHttpObject content) throws Exception {
        dispatchHttpObject(ctx, content);
    }

    private void dispatchHttpObject(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        HttpMethod currentMethod = CHANNEL_SATES.get(ctx.channel().id()).currentMethod;
        if (currentMethod.equals(HttpMethod.GET)) {
            handleGet(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.POST)) {
            handlePost(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.PUT)) {
            handlePut(ctx, msg);
        } else if (currentMethod.equals(HttpMethod.DELETE)) {
            handleDelete(ctx, msg);
        } else {
            handleWrongMethod(ctx);
        }
    }

    protected void handleGet(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
    }

    protected void handlePost(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
    }

    protected void handlePut(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
    }

    protected void handleDelete(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        handleWrongMethod(ctx);
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
        private boolean inUse;

        public ChannelState(HttpMethod currentMethod, boolean inUse) {
            this.currentMethod = currentMethod;
            this.inUse = inUse;
        }

        public HttpMethod getCurrentMethod() {
            return currentMethod;
        }

        public void setCurrentMethod(HttpMethod currentMethod) {
            this.currentMethod = currentMethod;
        }

        public boolean isInUse() {
            return inUse;
        }

        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CHANNEL_SATES.remove(ctx.channel().id());
        super.channelInactive(ctx);
    }
}

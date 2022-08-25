package zstreamer.http.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import zstreamer.http.entity.request.WrappedHttpObject;
import zstreamer.http.entity.request.WrappedHttpRequest;

/**
 * @author 张贝易
 * 处理Http业务的handler的抽象类
 */
public abstract class AbstractHttpHandler extends SimpleChannelInboundHandler<WrappedHttpObject> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        if (msg instanceof WrappedHttpRequest) {
            requestStart(ctx);
        }
        try {
            dispatchHttpObject(ctx, msg);
        } catch (Exception e) {
            handleException(ctx, msg);
        }
    }

    /**
     * 根据当前channel的请求类型调用不同的处理方法，这个数据不一定是完整的
     *
     * @param ctx 上下文
     * @param msg 请求数据的一部分
     */
    private void dispatchHttpObject(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        HttpMethod currentMethod = msg.getMethod();
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
            responseWrongMethod(ctx);
        }
        //告知HttpCodec某一个响应已经完成，重置它的状态
        if (finished) {
            endResponse(ctx, msg);
            requestEnd(ctx);
        }
    }

    /**
     * 钩子函数，让子类在请求来的时候进行初始化工作
     */
    protected void requestStart(ChannelHandlerContext ctx) throws Exception {

    }

    /**
     * 钩子函数，让子类在请求结束的时候进行终止操作
     *
     * @param ctx 上下文
     */
    protected void requestEnd(ChannelHandlerContext ctx) throws Exception {

    }

    /**
     * 处理并响应Get请求
     *
     * @return 返回true表明这个请求已经处理完了，可以进行endResponse操作了
     */
    protected boolean handleGet(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        return true;
    }

    protected boolean handlePost(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        return true;
    }

    protected boolean handlePut(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        return true;
    }

    protected boolean handleDelete(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        return true;
    }

    private void endResponse(ChannelHandlerContext ctx, WrappedHttpObject msg) {
        ctx.writeAndFlush(new DefaultLastHttpContent());
        msg.getState().setInUse(false);
    }

    /**
     * 子类必须在异常发生的时候做出处理
     *
     * @param ctx 上下文
     * @throws Exception 未知的异常
     */
    protected abstract void onException(ChannelHandlerContext ctx) throws Exception;

    public void handleException(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        msg.getState().setInUse(false);
        onException(ctx);
        responseException(ctx);
    }

    /**
     * 响应405，方法错误
     *
     * @param ctx 上下文
     */
    private void responseWrongMethod(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
        response.headers().set("content-length", 0);
        ctx.writeAndFlush(response);
    }

    /**
     * 响应500，服务器内部错误
     *
     * @param ctx 上下文
     */
    private void responseException(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        res.headers().set("content-length", "0");
        ctx.channel().writeAndFlush(res);
    }
}

package zstreamer.http.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.HttpMethod;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

/**
 * @author 张贝易
 * 处理Http业务的handler的抽象类
 */
public abstract class AbstractHttpHandler extends SimpleChannelInboundHandler<WrappedRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WrappedRequest msg) {
        try {
            dispatchHttpObject(ctx, msg);
        } catch (Exception e) {
            ctx.channel().writeAndFlush(InstanceTool.getExceptionResponse(msg));
        }
    }

    /**
     * 根据当前channel的请求类型调用不同的处理方法，这个数据不一定是完整的
     *
     * @param ctx 上下文
     * @param msg 请求数据的一部分
     */
    private void dispatchHttpObject(ChannelHandlerContext ctx, WrappedRequest msg) throws Exception {
        HttpMethod currentMethod = msg.method();
        AbstractWrappedResponse result = null;
        if (currentMethod.equals(HttpMethod.GET)) {
            result = handleGet(msg);
        } else if (currentMethod.equals(HttpMethod.POST)) {
            result = handlePost(msg);
        } else if (currentMethod.equals(HttpMethod.PUT)) {
            result = handlePut(msg);
        } else if (currentMethod.equals(HttpMethod.DELETE)) {
            result = handleDelete(msg);
        } else {
            result = InstanceTool.getNotFoundResponse(msg);
        }
        handleResult(ctx, result);
    }

    private void handleResult(ChannelHandlerContext ctx, DefaultHttpObject result) {
        if (result == null) {
            return;
        }
        ctx.channel().writeAndFlush(result);
    }

    /**
     * 处理并响应Get请求
     *
     * @return 返回true表明这个请求已经处理完了，可以进行endResponse操作了
     */
    protected AbstractWrappedResponse handleGet(WrappedRequest msg) throws Exception {
        return InstanceTool.getWrongMethodResponse(msg);
    }

    protected AbstractWrappedResponse handlePost(WrappedRequest msg) throws Exception {
        return InstanceTool.getWrongMethodResponse(msg);
    }

    protected AbstractWrappedResponse handlePut(WrappedRequest msg) throws Exception {
        return InstanceTool.getWrongMethodResponse(msg);
    }

    protected AbstractWrappedResponse handleDelete(WrappedRequest msg) throws Exception {
        return InstanceTool.getWrongMethodResponse(msg);
    }
}

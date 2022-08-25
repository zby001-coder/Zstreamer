package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.service.AbstractHttpHandler;

/**
 * @author 张贝易
 * 根据请求路径解析url，获取参数，分发消息给handler
 */
@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<DefaultHttpObject> {
  
    private static final RequestDispatcher INSTANCE = new RequestDispatcher();

    private RequestDispatcher() {

    }

    public static RequestDispatcher getInstance() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        AbstractHttpHandler handler = getServiceHandler(ctx);
        if(handler==null){
            responseNoFound(ctx);
            return;
        }
        handler.channelRead(ctx, msg);
    }

    /**
     * 响应404的信息
     *
     * @param ctx 上下文
     */
    private void responseNoFound(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response);
        //不处理下面的content
        ctx.pipeline().get(RequestResolver.class).getMessageInfo(ctx).setState(MessageInfo.DISABLED);
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
        MessageInfo info = ctx.pipeline().get(RequestResolver.class).getMessageInfo(ctx);
        //获取对应的class信息
        Class<AbstractHttpHandler> clz = info.getClassInfo().getClz();
        //获取url对应的handler
        return ContextHandler.instanceHandler(clz);
    }
}

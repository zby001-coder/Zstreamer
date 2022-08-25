package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FastThreadLocal;
import zstreamer.commons.loader.HandlerClassResolver;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.entity.request.RequestState;
import zstreamer.http.entity.request.WrappedHttpContent;
import zstreamer.http.entity.request.WrappedHttpRequest;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;

/**
 * @author 张贝易
 * 将请求的url解析并将请求包装起来
 */
@ChannelHandler.Sharable
public class RequestResolver extends SimpleChannelInboundHandler<DefaultHttpObject> {
    /**
     * 某一个channel当前请求的状态
     */
    private static final FastThreadLocal<HashMap<ChannelId, RequestState>> REQUEST_STATES = new FastThreadLocal<>();
    private static final RequestResolver INSTANCE = new RequestResolver();
    private static final UrlResolver URL_RESOLVER = UrlResolver.getInstance();

    private RequestResolver() {
    }

    public static RequestResolver getInstance() {
        return INSTANCE;
    }

    /**
     * channel启动时初始化ThreadLocal的信息
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (REQUEST_STATES.get() == null) {
            REQUEST_STATES.set(new HashMap<>());
        }
        REQUEST_STATES.get().put(ctx.channel().id(), new RequestState(null, false));
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            handleHeader(ctx, (DefaultHttpRequest) msg);
        } else {
            handleContent(ctx, (DefaultHttpContent) msg);
        }
    }

    /**
     * 处理请求行和请求体
     *
     * @param ctx 上下文
     * @param msg 请求行和请求头
     */
    private void handleHeader(ChannelHandlerContext ctx, DefaultHttpRequest msg) throws Exception {
        //解析url，获取其对应handler信息
        String url = msg.uri();
        UrlClassTier.ClassInfo<AbstractHttpHandler> info = HandlerClassResolver.getInstance().resolveHandler(url);
        if (info != null) {
            //设置请求的状态
            RequestState state = REQUEST_STATES.get().get(ctx.channel().id());
            state.setInUse(true);
            state.setCurrentMethod(msg.method());
            //将url中的参数解析出来，包装后传递下去
            UrlResolver.RestfulUrl restfulUrl = URL_RESOLVER.resolveUrl(url, info.getUrlPattern());
            ctx.fireChannelRead(new WrappedHttpRequest(restfulUrl, state, msg, info.getClz()));
        } else {
            //info为空，说明解析url失败，报404
            responseNoFound(ctx);
        }
    }

    /**
     * 处理请求体
     *
     * @param ctx 上下文
     * @param msg 请求体
     */
    private void handleContent(ChannelHandlerContext ctx, DefaultHttpContent msg) throws Exception {
        //根据当前请求使用的handler分发请求体
        RequestState state = REQUEST_STATES.get().get(ctx.channel().id());
        //包装并分发content
        if (state.isInUse()) {
            ctx.fireChannelRead(new WrappedHttpContent(state, msg));
        }
        //如果当前请求的state被置为false，说明下面的handler抛出异常了，不继续处理content
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
        REQUEST_STATES.get().get(ctx.channel().id()).setInUse(false);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除当前HTTP请求信息
        REQUEST_STATES.get().remove(ctx.channel().id());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //下层业务handler抛出异常，将当前请求设置为不处理
        REQUEST_STATES.get().get(ctx.channel().id()).setInUse(false);
    }
}

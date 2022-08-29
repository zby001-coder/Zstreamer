package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import zstreamer.commons.loader.FilterClassResolver;
import zstreamer.commons.loader.HandlerClassResolver;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedContent;
import zstreamer.http.entity.request.WrappedHead;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.List;

/**
 * @author 张贝易
 * 将请求的url解析并将请求包装起来
 */
@ChannelHandler.Sharable
public class RequestResolver extends SimpleChannelInboundHandler<HttpObject> {

    private static final RequestResolver INSTANCE = new RequestResolver();

    private RequestResolver() {
    }

    public static RequestResolver getInstance() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            ctx.fireUserEventTriggered(msg);
            handleHeader(ctx, (HttpRequest) msg);
        } else {
            handleContent(ctx, (HttpContent) msg);
        }
    }

    /**
     * 处理请求行和请求体
     *
     * @param ctx 上下文
     * @param msg 请求行和请求头
     */
    private void handleHeader(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        //解析url，获取其对应handler信息
        String url = msg.uri();
        UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo = HandlerClassResolver.getInstance().resolveHandler(url);
        if (handlerInfo != null) {
            List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = FilterClassResolver.getInstance().resolveFilter(handlerInfo.getUrlPattern());
            //将url中的参数解析出来，包装后传递下去
            UrlResolver.RestfulUrl restfulUrl = UrlResolver.getInstance().resolveUrl(url, handlerInfo.getUrlPattern());
            //传递消息
            ctx.fireChannelRead(new WrappedHead(ctx.channel().id(), msg, restfulUrl, handlerInfo, filterInfo));
        } else {
            //找不到，报404
            ctx.channel().writeAndFlush(InstanceTool.getNotFoundResponse(new WrappedHead(ctx.channel().id(), msg)));
        }
    }

    /**
     * 处理请求体
     *
     * @param ctx 上下文
     * @param msg 请求体
     */
    private void handleContent(ChannelHandlerContext ctx, HttpContent msg) throws Exception {
        if (StateHandler.ifHandleRequest(ctx)) {
            //一个请求结尾，暂停读取，防止两个响应混合起来
            if (msg instanceof LastHttpContent) {
                ctx.channel().config().setAutoRead(false);
            }
            msg.retain();
            ctx.fireChannelRead(new WrappedContent(ctx.channel().id(), msg));
        }
        //如果当前请求的state被置为disabled，就不处理下面的数据了
    }
}

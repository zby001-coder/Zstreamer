package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.DefaultHttpRequest;
import zstreamer.commons.loader.FilterClassResolver;
import zstreamer.commons.loader.HandlerClassResolver;
import zstreamer.commons.loader.UrlClassTier;
import zstreamer.commons.loader.UrlResolver;
import zstreamer.http.entity.HttpEvent;
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.entity.MessageState;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.List;

/**
 * @author 张贝易
 * 将请求的url解析并将请求包装起来
 */
@ChannelHandler.Sharable
public class RequestResolver extends SimpleChannelInboundHandler<DefaultHttpObject> {

    private static final RequestResolver INSTANCE = new RequestResolver();

    private RequestResolver() {
    }

    public static RequestResolver getInstance() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpObject msg) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            ctx.fireUserEventTriggered(HttpEvent.RECEIVE_REQUEST);
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
        UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo = HandlerClassResolver.getInstance().resolveHandler(url);
        if (handlerInfo != null) {
            List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = FilterClassResolver.getInstance().resolveFilter(handlerInfo.getUrlPattern());
            //将url中的参数解析出来，包装后传递下去
            UrlResolver.RestfulUrl restfulUrl = UrlResolver.getInstance().resolveUrl(url, handlerInfo.getUrlPattern());
            //设置请求的信息
            MessageInfo messageInfo = new MessageInfo(msg.method(), restfulUrl, handlerInfo, filterInfo);
            ContextHandler.putMessageInfo(ctx, messageInfo);
            //传递消息
            ctx.fireChannelRead(msg);
        } else {
            //没有对应的handler，报404
            ctx.fireUserEventTriggered(HttpEvent.NOT_FOUND);
        }
    }

    /**
     * 处理请求体
     *
     * @param ctx 上下文
     * @param msg 请求体
     */
    private void handleContent(ChannelHandlerContext ctx, DefaultHttpContent msg) throws Exception {
        MessageState state = ContextHandler.getMessageState(ctx);
        if (!(state instanceof MessageState.WaitRequest)) {
            ctx.fireChannelRead(msg);
        }
        //如果当前请求的state被置为disabled，就不处理下面的数据了
    }
}

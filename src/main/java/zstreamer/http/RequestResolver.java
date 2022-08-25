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
import zstreamer.http.entity.MessageInfo;
import zstreamer.http.filter.AbstractHttpFilter;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.HashMap;
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

    public MessageInfo getMessageInfo(ChannelHandlerContext ctx) {
        return ContextHandler.INFO_MAP.get().get(ctx.channel().id());
    }

    /**
     * channel启动时初始化ThreadLocal的信息
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ContextHandler.INFO_MAP.get() == null) {
            ContextHandler.INFO_MAP.set(new HashMap<>());
        }
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
        UrlClassTier.ClassInfo<AbstractHttpHandler> handlerInfo = HandlerClassResolver.getInstance().resolveHandler(url);
        if (handlerInfo != null) {
            List<UrlClassTier.ClassInfo<AbstractHttpFilter>> filterInfo = FilterClassResolver.getInstance().resolveFilter(handlerInfo.getUrlPattern());
            //将url中的参数解析出来，包装后传递下去
            UrlResolver.RestfulUrl restfulUrl = UrlResolver.getInstance().resolveUrl(url, handlerInfo.getUrlPattern());
            //设置请求的状态
            MessageInfo messageInfo = new MessageInfo(msg.method(), restfulUrl, handlerInfo, filterInfo);
            messageInfo.setState(MessageInfo.RECEIVED_HEADER);
            ContextHandler.INFO_MAP.get().put(ctx.channel().id(), messageInfo);
            //传递消息
            ctx.fireChannelRead(msg);
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
        MessageInfo messageInfo = ContextHandler.INFO_MAP.get().get(ctx.channel().id());
        //传递content
        int state = messageInfo.getState();
        if (state != MessageInfo.DISABLED) {
            ctx.fireChannelRead(msg);
        }
        //如果当前请求的state被置为disabled，就不处理下面的数据了
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //下层业务handler抛出异常，将当前请求设置为不处理
        ContextHandler.INFO_MAP.get().get(ctx.channel().id()).setState(MessageInfo.DISABLED);
    }
}

package zstreamer.http;

import zstreamer.commons.util.HandlerClassResolver;
import zstreamer.commons.util.UrlResolver;
import zstreamer.commons.util.WrappedHttpRequest;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 根据请求路径解析url，获取参数，注入handler
 */
@ChannelHandler.Sharable
public class RequestDispatcher extends SimpleChannelInboundHandler<DefaultHttpRequest> {
    private static final ConcurrentHashMap<Class<? extends AbstractHttpHandler>, AbstractHttpHandler> INSTANCED_HANDLERS = new ConcurrentHashMap<>();
    private static final RequestDispatcher INSTANCE = new RequestDispatcher();

    private RequestDispatcher(){

    }

    public static RequestDispatcher getInstance(){
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpRequest msg) throws Exception {
        String url = UrlResolver.getInstance().getRawUrl(msg.uri());
        HandlerClassResolver classResolver = HandlerClassResolver.getInstance();
        HandlerClassResolver.ClassInfo info = classResolver.resolveHandler(url);
        AbstractHttpHandler handler = null;
        if (info != null) {
            handler = instanceHandler(info);
            if (ctx.pipeline().get(handler.getClass()) == null) {
                ctx.pipeline().addLast(handler);
            }
            UrlResolver.RestfulUrl restfulUrl = UrlResolver.getInstance().resolveUrl(url, info.getUrlPattern());
            handler.channelRead(ctx, new WrappedHttpRequest(msg, restfulUrl));
        } else {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            response.headers().set("content-length", 0);
            ctx.writeAndFlush(response);
        }
    }

    /**
     * 实例化单例Handler
     *
     * @param info handler的class信息
     * @return 实例化后的handler
     */
    private AbstractHttpHandler instanceHandler(HandlerClassResolver.ClassInfo info) throws InstantiationException, IllegalAccessException {
        AbstractHttpHandler handler = null;
        if (!INSTANCED_HANDLERS.containsKey(info.getClz())) {
            synchronized (this) {
                if (!INSTANCED_HANDLERS.containsKey(info.getClz())) {
                    handler = info.getClz().newInstance();
                    INSTANCED_HANDLERS.put(info.getClz(), handler);
                } else {
                    handler = INSTANCED_HANDLERS.get(info.getClz());
                }
            }
        } else {
            handler = INSTANCED_HANDLERS.get(info.getClz());
        }
        return handler;
    }
}

package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;

/**
 * @author 张贝易
 * 处理跨域
 */
@ChannelHandler.Sharable
public class HttpCrossOriginHandler extends ChannelDuplexHandler {
    private static final HttpCrossOriginHandler INSTANCE = new HttpCrossOriginHandler();

    private HttpCrossOriginHandler() {

    }

    public static HttpCrossOriginHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DefaultHttpResponse) {
            ((DefaultHttpResponse) msg).headers()
                    .set("Access-Control-Allow-Origin", "*")
                    .set("Access-Control-Allow-Method", "*")
                    .set("Access-Control-Allow-Headers", "*")
                    .set("Connection", "keep-alive");
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            if (((DefaultHttpRequest) msg).method().equals(HttpMethod.OPTIONS)) {
                DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                res.headers().set("content-length", "0");
                ctx.channel().writeAndFlush(res);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}

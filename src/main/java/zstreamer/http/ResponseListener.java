package zstreamer.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import zstreamer.http.entity.HttpEvent;

/**
 * @author 张贝易
 * 放在链条头部，监听响应事件，防止多次响应
 */
@ChannelHandler.Sharable
public class ResponseListener extends ChannelOutboundHandlerAdapter {
    private static final ResponseListener INSTANCE = new ResponseListener();

    public static ResponseListener getInstance() {
        return INSTANCE;
    }

    private ResponseListener() {
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DefaultHttpResponse) {
            ctx.fireUserEventTriggered(HttpEvent.SEND_HEAD);
        }
        super.write(ctx, msg, promise);
    }
}

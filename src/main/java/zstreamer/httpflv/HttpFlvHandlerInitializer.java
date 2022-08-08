package zstreamer.httpflv;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import zstreamer.Config;

import java.net.InetSocketAddress;

/**
 * @author 张贝易
 * httpFlv初始化工具，根据端口确定该channel是否为用户的
 */
public class HttpFlvHandlerInitializer extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().localAddress();
            int port = address.getPort();
            if (port == Config.HTTP_FLV_PORT) {
                initMessageHandlers(ctx);
                ctx.pipeline().remove(this.getClass());
            }
        }
        super.channelRegistered(ctx);
    }

    private void initMessageHandlers(ChannelHandlerContext ctx) {
        ctx.pipeline()
                .addLast(new ChannelTrafficShapingHandler(1024*512,1024*512,1000))
                .addLast(new HttpServerCodec())
                .addLast(new AudienceHandler());
    }
}

package zstreamer.rtmp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import zstreamer.Config;
import zstreamer.rtmp.chunk.ChunkCodec;
import zstreamer.rtmp.handshake.RtmpHandShaker;
import zstreamer.rtmp.message.codec.RtmpMessageDecoder;
import zstreamer.rtmp.message.codec.RtmpMessageEncoder;
import zstreamer.rtmp.message.handlers.MessageHandlerInitializer;
import zstreamer.rtmp.message.handlers.control.AckSenderReceiver;

import java.net.InetSocketAddress;

public class RtmpHandlerInitializer extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().localAddress();
            int port = address.getPort();
            if (port == Config.RTMP_PORT) {
                initMessageHandlers(ctx);
                ctx.pipeline().remove(this.getClass());
            }
        }
        super.channelRegistered(ctx);
    }

    private void initMessageHandlers(ChannelHandlerContext ctx) {
        ctx.pipeline()
                .addLast(new RtmpHandShaker())
                .addLast(new AckSenderReceiver())
                .addLast(new ChunkCodec())
                .addLast(new RtmpMessageDecoder())
                .addLast(new RtmpMessageEncoder())
                .addLast(new MessageHandlerInitializer());
    }
}

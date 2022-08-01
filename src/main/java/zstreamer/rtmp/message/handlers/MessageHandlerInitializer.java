package zstreamer.rtmp.message.handlers;

import zstreamer.rtmp.message.handlers.media.MetaDataHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import zstreamer.rtmp.message.handlers.command.CommandHandler;
import zstreamer.rtmp.message.handlers.control.ChunkSizeHandler;
import zstreamer.rtmp.message.handlers.control.PeerBandWidthHandler;
import zstreamer.rtmp.message.handlers.control.WindowAckSizeHandler;

/**
 * @author 张贝易
 * 初始化各种rtmp消息处理器的handler
 */
public class MessageHandlerInitializer extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        initMessageHandlers(ctx);
        super.channelRegistered(ctx);
        ctx.pipeline().remove(this.getClass());
    }

    private void initMessageHandlers(ChannelHandlerContext ctx) {
        ctx.pipeline()
                .addLast(new ChunkSizeHandler())
                .addLast(new CommandHandler())
                .addLast(new WindowAckSizeHandler())
                .addLast(new PeerBandWidthHandler())
                .addLast(new MetaDataHandler());
    }
}

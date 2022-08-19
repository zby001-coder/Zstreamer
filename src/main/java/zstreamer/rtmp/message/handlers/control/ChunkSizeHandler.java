package zstreamer.rtmp.message.handlers.control;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import zstreamer.rtmp.chunk.ChunkCodec;
import zstreamer.rtmp.message.messageType.control.ChunkSizeMessage;

/**
 * @author 张贝易
 * 处理分片大小的消息的handler，主要就是设置ChunkCodec中的chunkSize，这个指令是双向的
 * @see ChunkCodec
 */
public class ChunkSizeHandler extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ChunkSizeMessage) {
            ChunkSizeMessage chunkSizeMessage = (ChunkSizeMessage) msg;
            ChunkCodec chunkCodec = ctx.pipeline().get(ChunkCodec.class);
            chunkCodec.setInChunkSize(chunkSizeMessage.getChunkSize());
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ChunkSizeMessage) {
            ChunkSizeMessage chunkSizeMessage = (ChunkSizeMessage) msg;
            ChunkCodec chunkCodec = ctx.pipeline().get(ChunkCodec.class);
            chunkCodec.setOutChunkSize(chunkSizeMessage.getChunkSize());
        }
        ctx.write(msg, promise);
    }
}

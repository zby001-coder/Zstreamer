package zstreamer.rtmp.message.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import zstreamer.rtmp.chunk.ChunkCodec;
import zstreamer.rtmp.message.messageType.RawMessage;
import zstreamer.rtmp.message.messageType.RtmpMessage;

import java.util.List;

/**
 * @author 张贝易
 * 将各种RtmpMessage都转变成RawMessage
 */
public class RtmpMessageEncoder extends MessageToMessageEncoder<RtmpMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RtmpMessage msg, List<Object> out) {
        ChunkCodec chunkCodec = ctx.pipeline().get(ChunkCodec.class);
        RawMessage rawMessage = new RawMessage(msg, true, chunkCodec.getOutChunkSize());
        //将message的body编码，写入RawMessage中
        msg.encodeContent(rawMessage);
        out.add(rawMessage);
    }
}

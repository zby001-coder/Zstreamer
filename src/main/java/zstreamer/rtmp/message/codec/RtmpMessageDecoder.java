package zstreamer.rtmp.message.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.rtmp.chunk.ChunkCodec;
import zstreamer.rtmp.handshake.RtmpHandShaker;
import zstreamer.rtmp.message.afm.AfmDecoder;
import zstreamer.rtmp.message.messageType.RawMessage;
import zstreamer.rtmp.message.messageType.RtmpMessage;
import zstreamer.rtmp.message.messageType.command.CommandMessage;
import zstreamer.rtmp.message.messageType.control.ChunkSizeMessage;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;

/**
 * 目前三次握手已经完成、Chunk已经被封装成RawMessage(头部解析了，身体没解析)
 * 本handler需要将RawMessage解析成各种类型的Message
 * 同时，body也支持Afm解码
 *
 * @author 张贝易
 * @see RtmpHandShaker
 * @see ChunkCodec
 * @see AfmDecoder
 */
public class RtmpMessageDecoder extends SimpleChannelInboundHandler<RawMessage> {
    private int clientVersion;

    private RtmpMessage handleRawMessage(RawMessage msg) {
        int messageType = msg.getMessageTypeId();
        switch (messageType) {
            case 1:
                return new ChunkSizeMessage(msg, msg.getContent());
//            case 2:
//                return new RtmpMessage.AbortMessage();
//            case 3:
//                return new RtmpMessage.AckMessage();
//            case 5:
//                return new RtmpMessage.WindowAckMessage();
//            case 6:
//                return new RtmpMessage.SetPeerBandWithMessage();
            case 8:
                return new MediaMessage.AudioMessage(msg, msg.getContent());
            case 9:
                return new MediaMessage.VideoMessage(msg, msg.getContent());
            case 18:
                return new DataMessage(msg, msg.getContent());
            case 20:
                return new CommandMessage(msg, msg.getContent());
            default:
                return new RawMessage(false, 0);
        }
    }

    public int getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(int clientVersion) {
        this.clientVersion = clientVersion;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RawMessage msg) throws Exception {
        RtmpMessage rtmpMessage = handleRawMessage(msg);
        ctx.fireChannelRead(rtmpMessage);
    }
}

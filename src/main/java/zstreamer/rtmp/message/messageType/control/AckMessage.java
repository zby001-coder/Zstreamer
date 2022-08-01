package zstreamer.rtmp.message.messageType.control;

import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;

/**
 * @author 张贝易
 * 接收到一定数量的byte后向另一边发送确认消息，消息体是一个unsignedInt
 */
public class AckMessage extends RtmpMessage {
    public static final byte TYPE_ID = 3;
    private long receivedSize;

    public AckMessage(long receivedSize) {
        this.receivedSize = receivedSize;
    }

    public AckMessage(ByteBuf in) {
        this.receivedSize = in.readUnsignedInt();
    }

    public AckMessage(RtmpMessage message, long receivedSize) {
        super(message);
        this.receivedSize = receivedSize;
    }

    public AckMessage(RtmpMessage message, ByteBuf in) {
        super(message);
        this.receivedSize = in.readUnsignedInt();
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE_ID;
        //由于是固定长度，所以在这里就直接设置了
        this.messageLength = 4;
        this.messageStreamId = 0;
        this.chunkStreamId = 3;
    }

    public long getReceivedSize() {
        return receivedSize;
    }

    public void setReceivedSize(long receivedSize) {
        this.receivedSize = receivedSize;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        out.writeInt((int) this.receivedSize);
    }
}

package zstreamer.rtmp.message.messageType.control;

import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;

/**
 * @author 张贝易
 * 设置接收message的一方的Ack的窗口大小
 * 比如设置为1024，发送给客户端，那么客户端发送1024byte后需要等服务端发送Ack
 * 消息体是一个unsignedInt的窗口大小和1byte表示遵守窗口的严格程度
 */
public class PeerBandWidthMessage extends RtmpMessage {
    public static final long DEFAULT_WINDOW_SIZE = 5000000;
    public static final int HARD = 0;
    public static final int SOFT = 1;
    public static final int DYNAMIC = 2;
    private static final int TYPE = 6;
    private long size;
    private int limit;

    public PeerBandWidthMessage(long size, int limit) {
        this.size = size;
        this.limit = limit;
    }

    public PeerBandWidthMessage(ByteBuf in) {
        this.size = in.readUnsignedInt();
        this.limit = in.readUnsignedByte();
    }

    public PeerBandWidthMessage(RtmpMessage message, long size, int limit) {
        super(message);
        this.size = size;
        this.limit = limit;
    }

    public PeerBandWidthMessage(RtmpMessage message, ByteBuf in) {
        super(message);
        this.size = in.readUnsignedInt();
        this.limit = in.readUnsignedByte();
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE;
        this.messageStreamId = 0;
        this.chunkStreamId = 2;
        //由于它是固定长度的，所以直接设置
        this.messageLength = 5;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        out.writeInt((int) size);
        out.writeByte(limit);
    }

    @Override
    public String toString() {
        return "PeerBandWidthMessage{" +
                "size=" + size +
                ", limit=" + limit +
                '}';
    }
}

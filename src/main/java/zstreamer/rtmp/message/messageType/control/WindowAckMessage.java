package zstreamer.rtmp.message.messageType.control;

import io.netty.buffer.ByteBuf;
import zstreamer.rtmp.message.messageType.RtmpMessage;

/**
 * @author 张贝易
 * WindowAcknowledge类型的消息，用来告诉对方自己的窗口大小
 * 发送方在发送完窗口大小的数据量后等待对方的确认消息
 * 比如窗口大小为1024，则在发送完1024byte后等待对方确认
 * 消息体是一个unsignedInt表示窗口大小
 */
public class WindowAckMessage extends RtmpMessage {
    public static final long DEFAULT_WINDOW_SIZE = 5000000;
    public static final byte TYPE_ID = 5;
    /**
     * 虽然用long，但其实是unsignedInt，长度为4byte
     */
    private long windowSize;

    public WindowAckMessage(ByteBuf in) {
        windowSize = in.readUnsignedInt();
    }

    public WindowAckMessage(long windowSize) {
        this.windowSize = windowSize;
    }

    public WindowAckMessage(RtmpMessage message, ByteBuf in) {
        super(message);
        windowSize = in.readUnsignedInt();
    }

    public WindowAckMessage(RtmpMessage message, long windowSize) {
        super(message);
        this.windowSize = windowSize;
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE_ID;
        //由于它是固定长度的，所以直接设置
        this.messageLength = 4;
        this.messageStreamId = 0;
        this.chunkStreamId = 2;
    }

    public void setWindowSize(long windowSize) {
        this.windowSize = windowSize;
    }

    public long getWindowSize() {
        return windowSize;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        int windowSize = (int) this.windowSize;
        out.writeInt((int) this.windowSize);
    }

    @Override
    public String toString() {
        return "WindowAckMessage{" +
                "windowSize=" + windowSize +
                '}';
    }
}
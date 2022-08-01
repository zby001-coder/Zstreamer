package zstreamer.rtmp.message.messageType.control;

import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;

/**
 * ChunkSize类型的message，message体为一个数字，设置分片大小
 *
 * @author 张贝易
 */
public class ChunkSizeMessage extends RtmpMessage {
    public static final byte TYPE_ID = 1;
    public static final int DEFAULT_CHUNK_SIZE = 4096;
    /**
     * 虽然chunkSize是4字节，但是它不会超过int，因为messageSize只有3字节，chunk不能比message大
     */
    private int chunkSize;

    public ChunkSizeMessage(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public ChunkSizeMessage(ByteBuf buf) {
        chunkSize = buf.readInt();
    }

    public ChunkSizeMessage(RtmpMessage message, int chunkSize) {
        super(message);
        this.chunkSize = chunkSize;
    }

    public ChunkSizeMessage(RtmpMessage message, ByteBuf buf) {
        super(message);
        this.chunkSize = buf.readInt();
    }

    @Override
    protected void preInitialize() {
        this.messageTypeId = TYPE_ID;
        //由于它是固定长度的，所以直接设置
        this.messageLength = 4;
        this.messageStreamId = 0;
        this.chunkStreamId = 2;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    protected void doEncode(ByteBuf out) {
        out.writeInt(chunkSize);
        this.messageLength = 4;
    }

    @Override
    public String toString() {
        return "ChunkSizeMessage{" +
                "chunkSize=" + chunkSize +
                '}';
    }
}
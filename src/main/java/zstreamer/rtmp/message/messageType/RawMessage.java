package zstreamer.rtmp.message.messageType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 还未确定类型的Message，body保持二进制
 *
 * @author 张贝易
 */
public class RawMessage extends RtmpMessage {
    private ByteBuf content;

    public RawMessage(boolean allocate, int chunkSize) {
        if (allocate) {
            content = Unpooled.buffer(chunkSize);
        }
    }

    public RawMessage(RtmpMessage message, boolean allocate, int chunkSize) {
        //先把所有描述信息复制一遍
        super(message);
        if (allocate) {
            if (this.messageLength != 0) {
                this.content = Unpooled.buffer(this.messageLength);
            } else {
                this.content = Unpooled.buffer(chunkSize);
            }
        }
    }

    public ByteBuf getContent() {
        return content;
    }

    public boolean isFull() {
        return content.readableBytes() == messageLength;
    }

    @Override
    protected void preInitialize() {
        //这里什么都不做
    }

    @Override
    protected void doEncode(ByteBuf out) {
        out.writeBytes(content);
    }
}
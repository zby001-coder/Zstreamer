package zstreamer.rtmp.message.messageType.media;

import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;

/**
 * @author 张贝易
 * 媒体信息
 */
public abstract class MediaMessage extends RtmpMessage {
    protected ByteBuf content;

    public MediaMessage(ByteBuf content) {
        this.content = content;
        this.messageLength = content.readableBytes();
    }

    public MediaMessage(RtmpMessage message, ByteBuf content) {
        super(message);
        this.content = content;
        this.messageLength = content.readableBytes();
    }

    public ByteBuf getContent() {
        return content;
    }

    /**
     * 复制当前的mediaMessage，content用浅拷贝
     * 主要是为了不用分辨audio和video
     *
     * @return 复制后的结果
     */
    public abstract MediaMessage copyMessage();

    public static class AudioMessage extends MediaMessage {
        public static final byte TYPE_ID = 8;

        public AudioMessage(ByteBuf content) {
            super(content);
        }

        public AudioMessage(RtmpMessage message, ByteBuf content) {
            super(message, content);
        }

        @Override
        public MediaMessage copyMessage() {
            return new AudioMessage(this, this.content);
        }

        @Override
        protected void preInitialize() {
            this.messageTypeId = TYPE_ID;
            this.chunkStreamId = 4;
        }

        @Override
        protected void doEncode(ByteBuf out) {
            out.writeBytes(content);
        }

        @Override
        public String toString() {
            return "AudioMessage{" + timeStamp + "," + messageLength + "}";
        }
    }

    public static class VideoMessage extends MediaMessage {
        public static final byte TYPE_ID = 9;

        public VideoMessage(ByteBuf content) {
            super(content);
        }

        public VideoMessage(RtmpMessage message, ByteBuf content) {
            super(message, content);
        }

        @Override
        public MediaMessage copyMessage() {
            return new VideoMessage(this, this.content);
        }

        @Override
        protected void preInitialize() {
            this.messageTypeId = TYPE_ID;
            this.chunkStreamId = 4;
        }

        @Override
        protected void doEncode(ByteBuf out) {
            out.writeBytes(content);
        }

        @Override
        public String toString() {
            return "VideoMessage{" + timeStamp + "," + messageLength + "}";
        }
    }
}

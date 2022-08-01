package zstreamer.rtmp.message.messageType;

import zstreamer.rtmp.message.afm.AfmDecoder;
import zstreamer.rtmp.message.afm.AfmEncoder;
import io.netty.buffer.ByteBuf;

/**
 * @author 张贝易
 * Rtmp信息的抽象类，它的子类是各种类型的Rtmp消息
 */
public abstract class RtmpMessage {
    /**
     * timeStamp为绝对时间戳，由lastTimeStamp+delta计算得来
     * timeDelta是当前时间戳相对于上一个时间戳的增量
     */
    protected int timeStamp;
    protected int messageLength;
    protected int messageTypeId;
    protected long messageStreamId;
    protected int chunkStreamId;
    protected int timeDelta;
    protected static final AfmDecoder AFM_DECODER = new AfmDecoder();
    protected static final AfmEncoder AFM_ENCODER = new AfmEncoder();

    public RtmpMessage() {
        preInitialize();
    }

    public RtmpMessage(RtmpMessage message) {
        this.timeStamp = message.timeStamp;
        this.messageLength = message.messageLength;
        this.messageTypeId = message.messageTypeId;
        this.messageStreamId = message.messageStreamId;
        this.chunkStreamId = message.chunkStreamId;
        this.timeDelta = message.timeDelta;
        preInitialize();
    }

    /**
     * 在子类构造还没执行的时候做的初始化工作
     * 用来设置type、length(如果定长的话)、chunkStreamId(如果它是固定的话)、messageStreamId(如果它是固定的话)
     */
    protected abstract void preInitialize();

    /**
     * 让各种Message自己决定应该body如何将自己编码
     *
     * @param out 写出缓冲区
     */
    protected abstract void doEncode(ByteBuf out);

    /**
     * 各种message编码的模板方法，暴露在外
     * 将body转二进制写入RawMessage，同时设置RawMessage的body的长度
     * 通过编码后的bufferSize得知messageLength
     *
     * @param msg 将body的内容写入RawMessage
     */
    public void encodeContent(RawMessage msg) {
        int start = msg.getContent().readableBytes();
        doEncode(msg.getContent());
        int end = msg.getContent().readableBytes();
        msg.messageLength = end - start;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getTimeDelta() {
        return timeDelta;
    }

    public void setTimeDelta(int timeDelta) {
        this.timeDelta = timeDelta;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public RtmpMessage setMessageLength(int messageLength) {
        this.messageLength = messageLength;
        return this;
    }

    public int getChunkStreamId() {
        return chunkStreamId;
    }

    public void setChunkStreamId(int chunkStreamId) {
        this.chunkStreamId = chunkStreamId;
    }

    public int getMessageTypeId() {
        return messageTypeId;
    }

    public void setMessageTypeId(int messageTypeId) {
        this.messageTypeId = messageTypeId;
    }

    public long getMessageStreamId() {
        return messageStreamId;
    }

    public void setMessageStreamId(long messageStreamId) {
        this.messageStreamId = messageStreamId;
    }

    //    public static class AbortMessage extends RtmpMessage {
//        private int streamId;
//
//        public int getStreamId() {
//            return streamId;
//        }
//
//        public void setStreamId(int streamId) {
//            this.streamId = streamId;
//        }
//    }
//
//    public static class AckMessage extends RtmpMessage {
//        private int sequence;
//
//        public int getSequence() {
//            return sequence;
//        }
//
//        public void setSequence(int sequence) {
//            this.sequence = sequence;
//        }
//    }
//
//
//    public static class SetPeerBandWithMessage extends RtmpMessage {
//        private int windowSize;
//        private int limitType;
//
//        public int getWindowSize() {
//            return windowSize;
//        }
//
//        public void setWindowSize(int windowSize) {
//            this.windowSize = windowSize;
//        }
//
//        public int getLimitType() {
//            return limitType;
//        }
//
//        public void setLimitType(int limitType) {
//            this.limitType = limitType;
//        }
//    }
//
//    public static class UserEventMessage extends RtmpMessage {
//        private int eventType;
//        private byte[] eventData;
//
//        public int getEventType() {
//            return eventType;
//        }
//
//        public void setEventType(int eventType) {
//            this.eventType = eventType;
//        }
//
//        public byte[] getEventData() {
//            return eventData;
//        }
//
//        public void setEventData(byte[] eventData) {
//            this.eventData = eventData;
//        }
//    }
//

//
//    public static class DataMessage extends RtmpMessage {
//        private byte[] data;
//
//        public byte[] getData() {
//            return data;
//        }
//
//        public void setData(byte[] data) {
//            this.data = data;
//        }
//    }
//
//    public static class ShareMessage extends RtmpMessage {
//
//    }
//
//    public static class AudioMessage extends RtmpMessage {
//        private byte[] data;
//
//        public byte[] getData() {
//            return data;
//        }
//
//        public void setData(byte[] data) {
//            this.data = data;
//        }
//    }
//
//    public static class VideoMessage extends RtmpMessage {
//        private byte[] data;
//
//        public byte[] getData() {
//            return data;
//        }
//
//        public void setData(byte[] data) {
//            this.data = data;
//        }
//    }
//
//    public static class AggregateMessage extends RtmpMessage {
//
//    }
}

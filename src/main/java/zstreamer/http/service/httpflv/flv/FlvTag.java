package zstreamer.http.service.httpflv.flv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;
/**
 * @author 张贝易
 * FLV文件的Tag内容，有两种: 音视频/Script
 * 在尾部包含prevLen信息，在头部不包含prevLen信息
 */
public class FlvTag {
    private static final int ONE_BYTE_MASK = 0xFF;
    private int type;
    private int timeStamp;
    private int timeStampExtended;
    /**
     * 这里使用dataLen，是因为ByteBuf中取出来的Array长度不一定等于dataLen
     * 为了少复制一次，使用dataLen
     */
    private int dataLen;
    private byte[] data;

    /**
     * 媒体tag
     *
     * @param message        媒体信息
     * @param basicTimeStamp 基础时间戳，即第一个媒体信息的时间戳
     */
    public FlvTag(MediaMessage message, int basicTimeStamp) {
        this.type = message.getMessageTypeId();
        //FLV的timeStamp是相对与第一个FLVTag的时间戳
        this.timeStamp = message.getTimeStamp() - basicTimeStamp;
        this.timeStampExtended = (message.getTimeDelta() >> 24) & ONE_BYTE_MASK;
        try {
            this.data = message.getContent().array();
        } catch (UnsupportedOperationException e) {
            this.data = new byte[message.getContent().readableBytes()];
            message.getContent().readBytes(data);
        }
        //从MediaMessage中取出的Array是正好的
        dataLen = data.length;
    }

    /**
     * 脚本tag
     *
     * @param message 脚本信息
     */
    public FlvTag(DataMessage message) {
        this.type = message.getMessageTypeId();
        this.timeStamp = 0;
        this.timeStampExtended = 0;

        ByteBuf buf = Unpooled.buffer(message.getMessageLength());
        message.encodeForFlv(buf);
        //这里特殊处理，长度不一定等于messageLength，因为有些参数不会加入FLV中
        this.data = buf.array();
        this.dataLen = buf.readableBytes();
    }

    public int getType() {
        return type;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public int getTimeStampExtended() {
        return timeStampExtended;
    }

    public int getDataLen() {
        return dataLen;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] generateTag() {
        //总长度是tagHeader+tagContent，为prevLen留空间，减少一次复制
        byte[] bytes = new byte[11 + dataLen + 4];
        int preSize = 11 + dataLen;

        bytes[0] = (byte) type;
        for (int i = 1; i <= 3; i++) {
            bytes[i] = (byte) ((dataLen >> ((3 - i) * 8)) & ONE_BYTE_MASK);
        }
        for (int i = 4; i <= 6; i++) {
            bytes[i] = (byte) ((timeStamp >> ((6 - i) * 8)) & ONE_BYTE_MASK);
        }
        bytes[7] = (byte) timeStampExtended;
        //header中的StreamId为全0
        for (int i = 8; i <= 10; i++) {
            bytes[i] = 0;
        }
        //将content复制进去
        System.arraycopy(data, 0, bytes, 11, dataLen);
        //将prevLen写入
        for (int i = 0; i < 4; i++) {
            bytes[preSize + i] = (byte) ((preSize >> (3 - i) * 8) & 0xFF);
        }
        return bytes;
    }
}

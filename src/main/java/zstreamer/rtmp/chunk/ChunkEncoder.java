package zstreamer.rtmp.chunk;

import zstreamer.rtmp.message.messageType.RtmpMessage;
import io.netty.buffer.ByteBuf;
import zstreamer.rtmp.message.messageType.RawMessage;

/**
 * @author 张贝易
 * 编码chunk头和message头的工具
 * 由于没有堆上的变量，所以线程安全，使用单例
 */
public class ChunkEncoder {
    private static final ChunkEncoder INSTANCE = new ChunkEncoder();
    private static final int FORMAT_MOVE = 6;

    public static ChunkEncoder getInstance() {
        return INSTANCE;
    }

    private ChunkEncoder() {
    }

    /**
     * 编码chunk的basicHead，确定chunkStreamId和messageHeader类型
     * csId用的是小端存储，所以大于一个byte时要注意
     */
    public void encodeBasicHead(ByteBuf out, ChunkHeader chunkHeader) {
        //编码第一个字节
        int basic = 0;
        basic = (basic + chunkHeader.getType()) << FORMAT_MOVE;
        int chunkStreamId = chunkHeader.getChunkStreamId();
        if (chunkStreamId <= 63) {
            //6bit之内可以装完，直接拼接即可
            basic = basic | chunkStreamId;
            out.writeByte(basic);
        } else if (chunkStreamId <= 319) {
            //还需要1bit
            out.writeByte(basic);
            out.writeByte(chunkStreamId - 64);
        } else {
            //还需要2bit
            out.writeByte(basic | 1);
            chunkStreamId -= 64;
            byte[] bytes = {(byte) (chunkStreamId - 64), (byte) ((chunkStreamId - 64) >> 8)};
            out.writeBytes(bytes);
        }
    }

    /**
     * 编码MessageHeader
     *
     * @param msg  需要编码的message信息
     * @param out  输出缓冲区
     * @param type messageHeader的压缩等级
     */
    public void encodeMessageHead(RawMessage msg, ByteBuf out, int type) throws Exception {
        switch (type) {
            case 0:
                encodeMessageHeader0(out, msg);
                break;
            case 1:
                encodeMessageHeader1(out, msg);
                break;
            case 2:
                encodeMessageHeader2(out, msg);
                break;
            default:
                //当压缩等级为3时，说明当前chunk是一个message分成多片的情况，不需要重复写messageHeader
                break;
        }
    }

    /**
     * 不压缩，将整个messageHeader写入缓冲区
     * 用的是绝对时间戳
     *
     * @param out     输出缓冲区
     * @param message message信息
     */
    private void encodeMessageHeader0(ByteBuf out, RtmpMessage message) {
        encodeTimeStamp(out, message, true);
        encodeLenType(out, message);
        encodeMessageStreamId(out, message);
    }

    /**
     * 压缩等级1，将messageStreamId省略
     * 用的是相对时间戳
     *
     * @param out     输出缓冲区
     * @param message message信息
     */
    private void encodeMessageHeader1(ByteBuf out, RtmpMessage message) {
        encodeTimeStamp(out, message, false);
        encodeLenType(out, message);
    }

    /**
     * 压缩等级2，只需要写时间戳
     * 用的是相对时间戳
     *
     * @param out     输出缓冲区
     * @param message message信息
     */
    private void encodeMessageHeader2(ByteBuf out, RtmpMessage message) {
        encodeTimeStamp(out, message, false);
    }

    /**
     * 对timeStamp编码
     *
     * @param out      输出缓冲区
     * @param message  message描述信息
     * @param absolute 是写绝对时间戳还是相对时间戳
     */
    private void encodeTimeStamp(ByteBuf out, RtmpMessage message, boolean absolute) {
        if (absolute) {
            int timeStamp = message.getTimeStamp();
            byte[] bytes = new byte[]{(byte) (timeStamp & (0xFF << 16)), (byte) (timeStamp & (0xFF << 8)), (byte) (timeStamp & (0xFF))};
            out.writeBytes(bytes);
        } else {
            int timeDelta = message.getTimeDelta();
            byte[] bytes = new byte[]{(byte) (timeDelta & (0xFF << 16)), (byte) (timeDelta & (0xFF << 8)), (byte) (timeDelta & (0xFF))};
            out.writeBytes(bytes);
        }
    }

    /**
     * 对messageLength和messageType编码
     *
     * @param out     输出缓冲区
     * @param message message描述信息
     */
    private void encodeLenType(ByteBuf out, RtmpMessage message) {
        int messageLength = message.getMessageLength();
        int messageTypeId = message.getMessageTypeId();
        byte[] bytes = new byte[]{(byte) (messageLength & (0xFF << 16)), (byte) (messageLength & (0xFF << 8)), (byte) (messageLength & (0xFF))};
        out.writeBytes(bytes);
        out.writeByte(messageTypeId);
    }

    /**
     * 编码messageStreamId，注意是小端存储
     *
     * @param out     输出缓冲区
     * @param message 描述信息
     */
    private void encodeMessageStreamId(ByteBuf out, RtmpMessage message) {
        long messageStreamId = message.getMessageStreamId();
        byte[] bytes = new byte[]{(byte) (messageStreamId & 0xFF), (byte) (messageStreamId & (0xFF << 8)), (byte) (messageStreamId & (0xFF << 16)), (byte) (messageStreamId & (0xFF << 24))};
        out.writeBytes(bytes);
    }
}

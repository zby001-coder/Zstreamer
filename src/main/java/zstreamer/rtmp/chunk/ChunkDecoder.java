package zstreamer.rtmp.chunk;

import io.netty.buffer.ByteBuf;
import zstreamer.rtmp.message.messageType.RawMessage;

/**
 * @author 张贝易
 * 解析chunk头和message头的工具
 * 由于没有堆上的变量，所以线程安全，使用单例
 */
public class ChunkDecoder {
    private static final ChunkDecoder INSTANCE = new ChunkDecoder();
    private static final Exception NOT_ENOUGH_BYTES = new Exception("NOT ENOUGH BYTES!");

    private ChunkDecoder() {
    }

    public static ChunkDecoder getInstance() {
        return INSTANCE;
    }

    /**
     * basicHead的构造：2bit的type 和 6bit的csId，使用位运算解出这两个信息
     */
    private static final int FORMAT_MOVE = 6;
    private static final int FORMAT_MASK = 0x03;
    private static final int STREAM_ID_MASK = 0x3f;

    /**
     * 解析Chunk的BasicHead
     * 注意当CsId超过一byte时要按照小端解码
     *
     * @param in 读缓冲区
     * @return 解析出来的Chunk信息
     * @throws Exception 当Byte不足时抛出异常
     */
    public ChunkHeader decodeBasicHead(ByteBuf in) throws Exception {
        //获取第一个字节
        int basic = in.readUnsignedByte();
        //用第一个字节前2bit确定message的格式
        int type = (basic >> FORMAT_MOVE) & FORMAT_MASK;
        //第一个字节后6bit确定csId
        int chunkStreamId = basic & STREAM_ID_MASK;
        if (chunkStreamId == 0) {
            //后6bit二进制和为0，说明id为后2byte
            if (in.readableBytes() > 1) {
                chunkStreamId = 64 + in.readUnsignedByte() + in.readUnsignedByte() * 255;
            } else {
                throw new Exception("NOT ENOUGH BYTES!");
            }
        } else if (chunkStreamId == 1) {
            //后6bit二进制和为1，说明id为后1byte
            if (in.readableBytes() > 0) {
                chunkStreamId = in.readUnsignedByte() + 64;
            } else {
                throw new Exception("NOT ENOUGH BYTES!");
            }
        }
        return new ChunkHeader(chunkStreamId, type);
    }

    /**
     * 解析message头，用来确定整个message的大小、id等等
     * messageHeader可能被压缩过
     * 有四种message的头类型，需要根据type和lastMessage将现在的messageHeader解压缩
     * 解压缩后的当前messageHeader信息直接写入传入的profile中，这是为了减少创建对象
     *
     * @param in          读缓冲区
     * @param profile     上一次的messageHeader信息
     * @param chunkHeader 分片信息，即basicHeader
     * @throws Exception 在byte不足时抛出异常
     */
    public RawMessage decodeMessageHead(ByteBuf in, RawMessage profile, ChunkHeader chunkHeader) throws Exception {
        RawMessage newProfile = new RawMessage(profile, false, 0);
        switch (chunkHeader.getType()) {
            case 0:
                decodeMessageHead0(in, newProfile);
                break;
            case 1:
                decodeMessageHead1(in, newProfile);
                break;
            case 2:
                decodeMessageHead2(in, newProfile);
                break;
            default:
                //type3用在同一个message分成多个片的情况下，直接合并多个chunk，所以不用解header
                break;
        }
        return newProfile;
    }

    /**
     * header没有被压缩过，需要整个解码
     * 同时，type0的header用的是绝对时间戳
     */
    private void decodeMessageHead0(ByteBuf in, RawMessage last) throws Exception {
        if (in.readableBytes() >= 11) {
            decodeTimeStamp(in, last, true);
            decodeLenType(in, last);
            decodeMessageStreamId(in, last);
        } else {
            throw NOT_ENOUGH_BYTES;
        }
    }

    /**
     * header的streamId被压缩过，不需解streamId
     * 同时，type1的header用的是相对时间戳
     */
    private void decodeMessageHead1(ByteBuf in, RawMessage last) throws Exception {
        if (in.readableBytes() >= 7) {
            decodeTimeStamp(in, last, false);
            decodeLenType(in, last);
        } else {
            throw NOT_ENOUGH_BYTES;
        }
    }

    /**
     * 只有时间戳没有被压缩过，只需要解时间戳即可
     * 同时，type2用的是相对时间戳
     */
    private void decodeMessageHead2(ByteBuf in, RawMessage last) throws Exception {
        if (in.readableBytes() >= 3) {
            decodeTimeStamp(in, last, false);
        } else {
            throw NOT_ENOUGH_BYTES;
        }
    }

    /**
     * 解析时间戳
     *
     * @param in       输入缓冲区
     * @param last     上一次的描述信息，同时也是这次保存的位置
     * @param absolute 是否为绝对时间戳
     */
    private void decodeTimeStamp(ByteBuf in, RawMessage last, boolean absolute) {
        int timeStamp = 0;
        if (absolute) {
            last.setTimeDelta(0);
            last.setTimeStamp(timeStamp | (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | (in.readUnsignedByte()));
        } else {
            last.setTimeDelta(timeStamp | (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | (in.readUnsignedByte()));
            last.setTimeStamp(last.getTimeStamp() + last.getTimeDelta());
        }
    }

    /**
     * 解析长度和类型
     *
     * @param in   输入缓冲区
     * @param last 上一次的描述信息，同时也是这次保存的位置
     */
    private void decodeLenType(ByteBuf in, RawMessage last) {
        int messageLen = 0;
        last.setMessageLength(messageLen | (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | in.readUnsignedByte());
        last.setMessageTypeId(in.readUnsignedByte());
    }

    /**
     * 解析messageStreamId，注意这里用的是小端存储，所以和前面的解法不同
     *
     * @param in   输入缓冲区
     * @param last 上一次的描述信息，同时也是这次保存的位置
     */
    private void decodeMessageStreamId(ByteBuf in, RawMessage last) {
        long messageStreamId = 0;
        last.setMessageStreamId(messageStreamId | in.readUnsignedByte() | (in.readUnsignedByte() << 8) | (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 24));
    }
}

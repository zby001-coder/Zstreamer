package zstreamer.rtmp.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import zstreamer.rtmp.message.messageType.RawMessage;

import java.util.HashMap;
import java.util.List;

/**
 * @author 张贝易
 * 将chunk合并成RawMessage
 * 将RawMessage切分成chunk
 * 任务委托给两个内部类
 * 暂时没有对messageStream的开闭进行处理，即当前没有create一个messageStream也可以将它作为messageStreamId上传
 * @see MessageSplitter
 * @see MessageMerger
 */
public class ChunkCodec extends ByteToMessageCodec<RawMessage> {
    private final MessageMerger messageMerger = new MessageMerger();
    private final MessageSplitter messageSplitter = new MessageSplitter();

    @Override
    protected void encode(ChannelHandlerContext ctx, RawMessage msg, ByteBuf out) throws Exception {
        messageSplitter.encode(ctx, msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        messageMerger.decode(ctx, in, out);
    }

    public int getOutChunkSize() {
        return messageSplitter.chunkSize;
    }

    public void setOutChunkSize(int size) {
        messageSplitter.setChunkSize(size);
    }

    public void setInChunkSize(int size) {
        messageMerger.setChunkSize(size);
    }

    public int getInChunkSize() {
        return messageMerger.chunkSize;
    }

    /**
     * @author 张贝易
     * 合并message分片的解码器，生成RawMessage，即body没有解析的Message
     * 解析header的任务委托给ChunkResolver
     * 支持多个message的chunk交叉解析
     * @see ChunkDecoder
     */
    private class MessageMerger {
        /**
         * key是chunkStreamId
         * value是该chunkStream的最近一个RawMessage，只带基本信息的RawMessage
         * 为了处理Rtmp的压缩messageHead
         */
        private final HashMap<Integer, RawMessage> lastMessage = new HashMap<>();
        /**
         * 用来合并chunk的Map，由于一条MessageStream上的Message，包括它们的chunk总是按顺序到达
         * 所以可以用MessageStreamId当作Key来合并Message
         */
        private final HashMap<Long, RawMessage> mergeMap = new HashMap<>();
        private final ChunkDecoder chunkDecoder = ChunkDecoder.getInstance();
        private int chunkSize = 128;

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        /**
         * 将ByteBuf中的内容解码成RawMessage
         *
         * @param ctx channel的上下文
         * @param in  输入缓冲区
         * @param out 输出内容
         */
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            in.markReaderIndex();
            try {
                //先把BasicHead解码，获取message类型信息和csId
                ChunkHeader head = chunkDecoder.decodeBasicHead(in);
                //获取当前chunkStream上一次传输的message基本信息，用于解压header
                RawMessage lastProfile = lastMessage.getOrDefault(head.getChunkStreamId(), new RawMessage(false, chunkSize));
                //解出当前message的header
                RawMessage newProfile = chunkDecoder.decodeMessageHead(in, lastProfile, head);
                //将当前message合并，这个函数主要处理单message分成多chunk的情况
                mergeMessage(in, newProfile);
                lastMessage.put(head.getChunkStreamId(), newProfile);
                if (mergeMap.get(newProfile.getMessageStreamId()).isFull()) {
                    //如果整个message都读完了，就将它传递出去
                    out.add(mergeMap.get(newProfile.getMessageStreamId()));
                    mergeMap.remove(newProfile.getMessageStreamId());
                }
            } catch (Exception e) {
                in.resetReaderIndex();
            }
        }

        /**
         * 将相同的message的各部分chunk合并起来
         *
         * @param in      输入缓冲区
         * @param profile message的描述信息
         * @throws Exception 当byte数量不足时抛出异常
         */
        private void mergeMessage(ByteBuf in, RawMessage profile) throws Exception {
            //如果这个chunk是某个message的第一个分片，那么创建一个新的RawMessage接收分片，注意要复制message基本信息
            //如果这个chunk是某个message的后几个分片，就将它合并到后面去
            RawMessage merge = mergeMap.getOrDefault(profile.getMessageStreamId(), new RawMessage(profile, true, chunkSize));
            //根据该message目前已经写入的byte数和整个messageBody的大小确定剩余字节数
            int restBytes = merge.getMessageLength() - merge.getContent().readableBytes();
            //本次chunk应当读取的字节数是:chunk窗口的大小和剩余字节数的较小值
            int shouldRead = Integer.min(chunkSize, restBytes);
            if (in.readableBytes() >= shouldRead) {
                merge.getContent().writeBytes(in, shouldRead);
                mergeMap.put(merge.getMessageStreamId(), merge);
                return;
            }
            throw new Exception("NOT ENOUGH BYTES!");
        }
    }

    /**
     * @author 张贝易
     * 将message分片
     * <p>
     * 由于目前没怎么看到支持交叉解析的客户端，所以不支持多个messageStream的messageChunk交叉在一个chunkStream上发送
     * 也就是说，message虽然分片了，但是这些片还是按message的顺序发送的
     * 即 m1c1 m1c2 m2c1 m2c2，不支持 m1c1 m2c1 m1c2 m2c2
     * <p>
     * csid按照规范是可以随便选的，因为它只是为了压缩messageHead，但是不同的推流器会定死几个csid
     * 本应用暂时支持obs，所以csid的选择按照obs的来，如果对接其他应用需要调整
     * csid 2 用于传输 SetChunkSize。比如ChuckSizeMessage、PeerBandWidthMessage
     * csid 3 用于传输信令，比如 connect | releaseStream | FCPublish | createStream | publish
     * csid 4 用于传输 metaData + 音频数据 + 视频数据。
     */
    private class MessageSplitter {
        private final ChunkEncoder chunkEncoder = ChunkEncoder.getInstance();
        private int chunkSize = 128;
        /**
         * 由于chunkStream连续写同一个messageStream时可以压缩messageHead，所以记录下来
         * key为chunkStreamId，value为在该chunkStream上写的最近一个messageStream的描述信息
         */
        private final HashMap<Integer, RawMessage> lastMessage = new HashMap<>();

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        /**
         * 将RawMessage编码成二进制
         *
         * @param ctx 上下文
         * @param msg RawMessage完整内容
         * @param out 输出缓冲区
         */
        protected void encode(ChannelHandlerContext ctx, RawMessage msg, ByteBuf out) throws Exception {
            int chunkStreamId = msg.getChunkStreamId();
            //循环切片，这是为了比较大的message准备的
            while (msg.getContent().readableBytes() > 0) {
                //根据message和csId解析出header压缩等级
                int type = handleType(msg, chunkStreamId);
                //这一段将basicHeader的编码并写入缓冲区
                chunkEncoder.encodeBasicHead(out, new ChunkHeader(chunkStreamId, type));
                //这一段完成messageHeader的编码并写入缓冲区
                chunkEncoder.encodeMessageHead(msg, out, type);
                //这一段根据窗口大小将message的一部分body/整个body写出去
                writeContent(out, msg);
                //更新最近写的message信息，这里仅仅复制基本信息，不会将缓冲区内容也放到map中
                lastMessage.put(chunkStreamId, new RawMessage(msg, false, chunkSize));
            }
        }

        /**
         * 解析message压缩等级
         *
         * @param message       需要解析的message
         * @param chunkStreamId 它所在的chunkStreamId
         * @return 压缩等级
         */
        private int handleType(RawMessage message, int chunkStreamId) {
            int type = 0;
            if (lastMessage.containsKey(chunkStreamId)) {
                RawMessage last = lastMessage.get(chunkStreamId);
                if (last.getMessageStreamId() == message.getMessageStreamId()) {
                    //messageStreamId相同，至少是1
                    type++;
                    if (last.getMessageLength() == message.getMessageLength() && last.getMessageTypeId() == message.getMessageTypeId()) {
                        //len和type相同，至少是2
                        type++;
                        if (last.getTimeStamp() == message.getTimeStamp()) {
                            //timeStamp也相同，是3
                            type++;
                        }
                    }
                }
            }
            return type;
        }

        /**
         * 将RawMessage的body部分写入输出缓冲区
         *
         * @param out     输出缓冲区
         * @param message 需要写出的RawMessage
         */
        private void writeContent(ByteBuf out, RawMessage message) {
            //每次写出的byte不能比窗口大小还大
            int shouldWrite = Integer.min(chunkSize, message.getContent().readableBytes());
            out.writeBytes(message.getContent(), shouldWrite);
        }
    }
}

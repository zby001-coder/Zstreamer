package zstreamer.httpflv;

import io.netty.channel.socket.nio.NioSocketChannel;
import zstreamer.MediaMessagePool;
import zstreamer.httpflv.flv.FlvHeader;
import zstreamer.httpflv.flv.FlvTag;
import zstreamer.rtmp.message.messageType.media.MediaMessage;
import io.netty.channel.Channel;

/**
 * @author 张贝易
 * 观众对象
 */
public class Audience extends NioSocketChannel {
    private final Channel channel;
    private final String roomName;
    private int basicTimeStamp = -1;
    /**
     * 当前要推的message和上一个推的message
     */
    private MediaMessagePool.Node now;
    private MediaMessagePool.Node last;
    private volatile boolean roomClosed = false;
    private volatile boolean audienceLeft = false;

    public Audience(Channel channel, String roomName) {
        this.channel = channel;
        this.roomName = roomName;
    }

    /**
     * 用户从当前房间拉一段FLV数据并写出到channel中
     *
     * @return 返回1表明拉到了，0表示没拉到
     */
    public int pullMessage() throws NoSuchFieldException, IllegalAccessException {
        if (now != null && now.getMessage() != null) {
            if (basicTimeStamp == -1) {
                writeBasic(now);
            }
            FlvTag mediaTag = new FlvTag(now.getMessage(), basicTimeStamp);
            //IO线程刷新缓冲区的速度不一定跟得上服务器发送的速度
            //所以如果缓冲区满了就不继续发送
            if (channel.isActive() && channel.isWritable()) {
                channel.writeAndFlush(mediaTag.generateTag());
            }
            last = now;
            now = now.getNext();
            return 1;
        } else if (last != null && last.hasNext()) {
            //这个分支是为了处理上一次拉流时 now.next = null的情况
            now = last.getNext();
            return pullMessage();
        }
        return 0;
    }

    /**
     * 第一次拉流时会额外拉到一个FLVHeader和一个ScriptTag
     *
     * @param node 媒体信息节点
     */
    private void writeBasic(MediaMessagePool.Node node) {
        MediaMessage firstMedia = node.getMessage();
        basicTimeStamp = firstMedia.getTimeStamp();

        FlvHeader header = new FlvHeader((byte) 1, (byte) 1);
        channel.writeAndFlush(header.generateHeader());

        FlvTag scriptTag = new FlvTag(node.getMetaData());
        channel.writeAndFlush(scriptTag.generateTag());

        FlvTag avc = new FlvTag(node.getAvcSequenceHeader(), node.getAvcSequenceHeader().getTimeStamp());
        channel.writeAndFlush(avc.generateTag());

        FlvTag aac = new FlvTag(node.getAacSequenceHeader(), node.getAacSequenceHeader().getTimeStamp());
        channel.writeAndFlush(aac.generateTag());

        //由于sei信息不是必须的，所以要判空
        if (now.getSei() != null) {
            FlvTag sei = new FlvTag(node.getSei(), node.getSei().getTimeStamp());
            channel.writeAndFlush(sei.generateTag());
        }
    }

    /**
     * 直播间关闭了，观众的流也要关闭
     */
    public void onCloseRoom() {
        roomClosed = true;
        MediaMessagePool.unRegisterAudience(roomName, this);
        channel.writeAndFlush(new byte[0]);
        channel.close();
    }

    /**
     * 观众离开了，关闭流，同时从直播间解除注册
     */
    public void onLeave() {
        audienceLeft = true;
        MediaMessagePool.unRegisterAudience(roomName, this);
        channel.writeAndFlush(new byte[0]);
        channel.close();
    }

    /**
     * 返回这个观众是否已经关闭，用来给外界决定释放Audience引用
     * PullerPool在使用这个决定是否释放引用
     *
     * @return 观众是否关闭
     */
    public boolean closed() {
        return roomClosed || audienceLeft;
    }

    /**
     * 用户进入观看的房间，并且拉一个切片出来
     * 选择最后一个小于等于timeStamp位置的切片
     *
     * @param roomName  房间的名称
     * @param timeStamp 开始拉流的时间戳
     * @throws Exception 在房间不存在的时候抛出异常
     */
    public void enterRoom(String roomName, int timeStamp) throws Exception {
        MediaMessagePool.registerAudience(roomName, this);
        now = MediaMessagePool.pullMediaMessage(roomName, timeStamp);
        last = null;
    }
}

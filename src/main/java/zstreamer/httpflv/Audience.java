package zstreamer.httpflv;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import zstreamer.MediaMessagePool;
import zstreamer.httpflv.flv.FlvHeader;
import zstreamer.httpflv.flv.FlvTag;
import zstreamer.rtmp.Streamer;
import zstreamer.rtmp.message.messageType.media.MediaMessage;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

/**
 * @author 张贝易
 * 观众对象
 */
public class Audience {
    private final Channel channel;
    private final String roomName;
    private int basicTimeStamp = -1;
    private int storedMessage = 0;
    private final Runnable pullTask;
    /**
     * 最多连续拉流失败次数
     */
    private static final int MAX_EMPTY_MESSAGE_TIME = 10;
    /**
     * 当前要推的message和上一个推的message
     */
    private MediaMessagePool.Node now;
    private MediaMessagePool.Node last;
    private final Streamer streamer;
    private boolean closed = false;

    public Audience(Channel channel, Streamer streamer) {
        this.channel = channel;
        this.roomName = streamer.getRoomName();
        this.streamer = streamer;
        pullTask = new Runnable() {
            private final EventLoop loop = channel.eventLoop();
            private int waitCnt = 0;

            @Override
            public void run() {
                if (!closed && pullMessage() > 0) {
                    loop.execute(this);
                    waitCnt = 0;
                } else if (!closed) {
                    if (waitCnt > MAX_EMPTY_MESSAGE_TIME) {
                        onClose();
                        return;
                    }
                    loop.schedule(this, 30, TimeUnit.MILLISECONDS);
                    waitCnt++;
                }
            }
        };
    }

    /**
     * 用户从当前房间拉一段FLV数据并写出到channel中
     *
     * @return 返回1表明拉到了，0表示没拉到
     */
    private int pullMessage() {
        int write = 0;
        if (now != null && now.getMessage() != null) {
            if (basicTimeStamp == -1) {
                writeBasic(now);
            }
            FlvTag mediaTag = new FlvTag(now.getMessage(), basicTimeStamp);
            //IO线程刷新缓冲区的速度不一定跟得上服务器发送的速度
            //所以如果缓冲区满了就不继续发送
            if (channel.isActive() && channel.isWritable()) {
                channel.write(mediaTag.generateTag());
                storedMessage++;
                write++;
            }
            last = now;
            now = now.getNext();
        } else if (last != null && last.hasNext()) {
            //这个分支是为了处理上一次拉流时 now.next = null的情况
            now = last.getNext();
            write += pullMessage();
        }
        //批量flush
        if ((write == 0 && storedMessage > 0) || storedMessage > 5) {
            channel.flush();
        }
        return write;
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
     * 直播间关闭或者观众离开
     */
    public void onClose() {
        closed = true;
        MediaMessagePool.unRegisterAudience(roomName, this);
        channel.writeAndFlush(new byte[0]);
        channel.close();
    }

    /**
     * 用户进入观看的房间，并且拉一个切片出来
     * 选择最后一个小于等于timeStamp位置的切片
     *
     * @param roomName  房间的名称
     * @param timeStamp 开始拉流的时间戳
     * @throws Exception 在房间不存在的时候抛出异常
     */
    public void enterRoom(String roomName, int timeStamp, final ChannelHandlerContext ctx) throws Exception {
        now = MediaMessagePool.pullMediaMessage(roomName, timeStamp);
        last = null;
        ctx.channel().eventLoop().execute(pullTask);
    }
}

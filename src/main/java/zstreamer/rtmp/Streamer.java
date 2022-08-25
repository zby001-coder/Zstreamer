package zstreamer.rtmp;

import io.netty.channel.ChannelHandlerContext;
import zstreamer.MediaMessagePool;
import zstreamer.http.service.httpflv.Audience;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;

import java.util.concurrent.ConcurrentHashMap;

public class Streamer {
    private DataMessage metaData;
    private MediaMessage aac;
    private MediaMessage avc;
    private MediaMessage sei;
    /**
     * 房间的名称
     */
    private final String roomName;
    private volatile boolean closed = false;
    /**
     * 用一个concurrentHashMap保存观众列表，它的key就是观众
     * 这里用map是为了remove快一点
     */
    private final ConcurrentHashMap<Audience, String> audiences = new ConcurrentHashMap<>();
    private final ChannelHandlerContext context;

    public Streamer(String roomName, ChannelHandlerContext context) {
        this.context = context;
        this.roomName = roomName;
    }

    /**
     * 真正执行关闭直播间的工作
     * 设置关闭状态并且通知观众直播间关闭了
     */
    public void doCloseRoom() {
        closed = true;
        //通知所有观众直播间关闭了
        for (Audience audience : audiences.keySet()) {
            audience.onClose();
        }
        //关闭本channel
        context.channel().close();
    }

    /**
     * 观众进入直播间
     *
     * @param audience 观众信息
     * @return 是否成功进入
     */
    public boolean registerAudience(Audience audience) {
        //如果一进来直播间就是关的，直接触发关闭事件，返回false
        if (closed) {
            audience.onClose();
            return false;
        }
        audiences.put(audience, roomName);
        //这里用双重判定防止观众在关闭的时候恰好进入，导致观众没有被触发关闭事件
        if (closed) {
            audience.onClose();
            return false;
        }
        return true;
    }

    /**
     * 观众离开直播间
     *
     * @param audience 观众
     */
    public void unregisterAudience(Audience audience) {
        audiences.remove(audience);
    }

    public void pushNewMessage(MediaMessage msg) throws Exception {
        if (avc == null && msg instanceof MediaMessage.VideoMessage) {
            //avc一定先于所有videoMessage
            this.avc = msg;
        } else if (aac == null && msg instanceof MediaMessage.AudioMessage) {
            //aac一定先于所有audioMessage
            this.aac = msg;
        } else if (sei == null && msg instanceof MediaMessage.VideoMessage && msg.getTimeStamp() == 0) {
            //sei必须在avc之后，而且timestamp是0，可以将它和其他的videMessage区分开来
            this.sei = msg;
        } else {
            //将MediaMessage推送到信息的池子里
            MediaMessagePool.pushMediaMessage(roomName, msg, this);
        }
    }

    public DataMessage getMetaData() {
        return metaData;
    }

    public void setMetaData(DataMessage metaData) {
        this.metaData = metaData;
    }

    public MediaMessage getAac() {
        return aac;
    }

    public MediaMessage getAvc() {
        return avc;
    }

    public MediaMessage getSei() {
        return sei;
    }

    public String getRoomName() {
        return roomName;
    }
}

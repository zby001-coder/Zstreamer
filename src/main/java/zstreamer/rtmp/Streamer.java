package zstreamer.rtmp;

import io.netty.channel.ChannelHandlerContext;
import zstreamer.MediaMessagePool;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;

public class Streamer {
    private DataMessage metaData;
    private MediaMessage aac;
    private MediaMessage avc;
    private MediaMessage sei;
    /**
     * 房间的名称
     */
    private final String roomName;
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
        //关闭本channel
        context.channel().close();
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

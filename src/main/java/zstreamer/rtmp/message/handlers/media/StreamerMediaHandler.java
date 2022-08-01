package zstreamer.rtmp.message.handlers.media;

import zstreamer.MediaMessagePool;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.httpflv.Audience;
import zstreamer.PullerPool;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 主播的媒体流处理器，完成推媒体流到池中，写出FLV文件的工作
 */
public class StreamerMediaHandler extends SimpleChannelInboundHandler<MediaMessage> {
    private DataMessage metaData;
    private MediaMessage aac;
    private MediaMessage avc;
    private MediaMessage sei;
    /**
     * 房间的名称
     */
    private String roomName;
    private volatile boolean closed = false;
    /**
     * 用一个concurrentHashMap保存观众列表，它的key就是观众
     * 这里用map是为了remove快一点
     */
    private final ConcurrentHashMap<Audience, String> audiences = new ConcurrentHashMap<>();
    private ChannelHandlerContext context;

    /**
     * 创建房间，向媒体流池中开启一个房间
     *
     * @param roomName 房间的名称
     */
    public void createRoom(String roomName) {
        this.roomName = roomName;
        MediaMessagePool.createRoom(roomName, this);
    }

    /**
     * 关闭某个直播间，调用媒体流池中的关闭方法
     * 这是为了能顶掉旧的直播间而且不关闭正在建立的直播间
     * 因为媒体流池中的流必然是正在运行的房间的流，但当前的流可能是正在建立的房间的流
     *
     * @param roomName 直播间的名称
     */
    public void closeRoom(String roomName) throws IOException {
        // 这里调用MediaMessagePool的closeRoom
        // 最终还是会调用本类的doCloseRoom
        // 但是被关闭的对象不一定是本对象，而是MediaMessagePool中保存的那个Streamer
        // 为了应对一个streamer重复开启直播间的情况
        MediaMessagePool.closeRoom(roomName);
    }

    /**
     * 真正执行关闭直播间的工作
     * 设置关闭状态并且通知观众直播间关闭了
     */
    public void doCloseRoom() {
        closed = true;
        //通知所有观众直播间关闭了
        for (Audience audience : audiences.keySet()) {
            audience.onCloseRoom();
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
            audience.onCloseRoom();
            return false;
        }
        audiences.put(audience, roomName);
        //这里用双重判定防止观众在关闭的时候恰好进入，导致观众没有被触发关闭事件
        if (closed) {
            audience.onCloseRoom();
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MediaMessage msg) throws Exception {
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
        //有新的内容了，唤醒拉流器
        PullerPool.wakeUpPuller();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
        super.channelRegistered(ctx);
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
}

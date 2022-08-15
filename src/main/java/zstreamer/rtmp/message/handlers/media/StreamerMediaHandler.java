package zstreamer.rtmp.message.handlers.media;

import zstreamer.MediaMessagePool;
import zstreamer.rtmp.Streamer;
import zstreamer.rtmp.message.messageType.media.MediaMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author 张贝易
 * 主播的媒体流处理器，完成推媒体流到池中，写出FLV文件的工作
 */
public class StreamerMediaHandler extends SimpleChannelInboundHandler<MediaMessage> {
    private Streamer streamer;
    private ChannelHandlerContext context;
    /**
     * 创建房间，向媒体流池中开启一个房间
     *
     * @param roomName 房间的名称
     */
    public void createRoom(String roomName) {
        Streamer streamer = new Streamer(roomName, context);
        this.streamer = streamer;
        MediaMessagePool.createRoom(roomName, streamer);
    }

    public Streamer getStreamer() {
        return streamer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MediaMessage msg) throws Exception {
        streamer.pushNewMessage(msg);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
        super.channelRegistered(ctx);
    }
}

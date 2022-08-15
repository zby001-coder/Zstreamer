package zstreamer.rtmp.message.handlers.command;

import zstreamer.MediaMessagePool;
import zstreamer.rtmp.message.afm.AfmObject;
import zstreamer.rtmp.message.handlers.media.StreamerMediaHandler;
import zstreamer.rtmp.message.messageType.control.ChunkSizeMessage;
import zstreamer.rtmp.message.messageType.control.PeerBandWidthMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.rtmp.message.messageType.command.CommandMessage;
import zstreamer.rtmp.message.messageType.control.WindowAckMessage;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author 张贝易
 * 处理各种命令类型message的handler
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
        if (CommandMessage.CONNECT.equals(msg.getCommand().getValue())) {
            //connect事件
            setWindowAckSize(ctx);
            setPeerBandWidth(ctx);
            setChunkSize(ctx);
            permitConnect(ctx, msg);
        } else if (CommandMessage.RELEASE.equals(msg.getCommand().getValue())) {
            AfmObject.StringObject roomName = (AfmObject.StringObject) msg.getParams().get(1);
            //将处理房间的信息的handler添加进去
            StreamerMediaHandler streamer = new StreamerMediaHandler();
            ctx.pipeline().addLast(streamer);
            //关掉同名的直播间，如果直播间不存在则不会发生任何事情
            MediaMessagePool.closeRoom((String) roomName.getValue());
        } else if (CommandMessage.CREATE_STREAM.equals(msg.getCommand().getValue())) {
            //因为服务器没有做messageStream管理，所以仅仅返回确认信息即可
            permitCreateStream(ctx, msg);
        } else if (CommandMessage.PUBLISH.equals(msg.getCommand().getValue())) {
            StreamerMediaHandler streamer = ctx.pipeline().get(StreamerMediaHandler.class);
            streamer.createRoom((String) msg.getParams().get(1).getValue());
            //允许主播开始推流
            permitPublish(ctx, msg);
        }else if (CommandMessage.FC_UNPUBLISH.equals(msg.getCommand().getValue())){
            //关闭直播间
            MediaMessagePool.closeRoom((String) msg.getParams().get(1).getValue());
        }else if (CommandMessage.DELETE_STREAM.equals(msg.getCommand().getValue())){
            //因为服务器没有做stream的管理，所以什么都不做
        }
    }

    /**
     * 允许连接建立，这里的数据都是写死的，可以通过抓包看
     * @param ctx 上下文
     * @param msg 传过来的控制信息
     */
    private void permitConnect(ChannelHandlerContext ctx, CommandMessage msg) {
        HashMap<String, AfmObject> map1 = new HashMap<>(2);
        map1.put("fmsVer", new AfmObject.StringObject("FMS/3,0,1,123"));
        map1.put("capabilities", new AfmObject.NumberObject(31.0));
        AfmObject.ComplexObject object1 = new AfmObject.ComplexObject(map1);

        HashMap<String, AfmObject> map2 = new HashMap<>(4);
        map2.put("level", new AfmObject.StringObject("status"));
        map2.put("code", new AfmObject.StringObject("NetConnection.Connect.Success"));
        map2.put("description", new AfmObject.StringObject("Connection succeeded."));
        map2.put("objectEncoding", new AfmObject.NumberObject(0.0));
        AfmObject.ComplexObject object2 = new AfmObject.ComplexObject(map2);

        LinkedList<AfmObject> objects = new LinkedList<>();
        objects.add(object1);
        objects.add(object2);
        CommandMessage commandMessage = new CommandMessage(
                new AfmObject.StringObject(CommandMessage.RESULT),
                new AfmObject.NumberObject((Double) msg.getTransactionId().getValue()),
                objects);
        ctx.writeAndFlush(commandMessage);
    }

    /**
     * 允许创建流，这里的数据都是写死的，可以通过抓包看
     * @param ctx 上下文
     * @param msg 传过来的控制信息
     */
    private void permitCreateStream(ChannelHandlerContext ctx, CommandMessage msg) {
        AfmObject.StringObject command = new AfmObject.StringObject(CommandMessage.RESULT);
        AfmObject.NullObject nullObject = new AfmObject.NullObject();
        AfmObject.NumberObject numberObject = new AfmObject.NumberObject(1.0);
        LinkedList<AfmObject> params = new LinkedList<>();
        params.add(nullObject);
        params.add(numberObject);
        CommandMessage commandMessage = new CommandMessage(command, msg.getTransactionId(), params);
        ctx.writeAndFlush(commandMessage);
    }

    /**
     * 允许主播推流，这里的数据都是写死的，可以通过抓包看
     * @param ctx 上下文
     * @param msg 传过来的控制信息
     */
    private void permitPublish(ChannelHandlerContext ctx, CommandMessage msg) {
        AfmObject.StringObject command = new AfmObject.StringObject(CommandMessage.ON_STATUS);
        AfmObject.NullObject nullObject = new AfmObject.NullObject();
        HashMap<String, AfmObject> map = new HashMap<>(2);
        map.put("level", new AfmObject.StringObject("status"));
        map.put("code", new AfmObject.StringObject("NetStream.Publish.Start"));
        map.put("description", new AfmObject.StringObject("Start publishing"));
        AfmObject.ComplexObject complexObject = new AfmObject.ComplexObject(map);
        LinkedList<AfmObject> params = new LinkedList<>();
        params.add(nullObject);
        params.add(complexObject);
        CommandMessage commandMessage = new CommandMessage(command, msg.getTransactionId(), params);
        ctx.writeAndFlush(commandMessage);
    }

    private void setChunkSize(ChannelHandlerContext ctx) {
        ChunkSizeMessage chunkSizeMessage = new ChunkSizeMessage(ChunkSizeMessage.DEFAULT_CHUNK_SIZE);
        ctx.channel().writeAndFlush(chunkSizeMessage);
    }

    private void setWindowAckSize(ChannelHandlerContext ctx) {
        WindowAckMessage windowAckMessage = new WindowAckMessage(WindowAckMessage.DEFAULT_WINDOW_SIZE);
        ctx.channel().writeAndFlush(windowAckMessage);
    }

    private void setPeerBandWidth(ChannelHandlerContext ctx) {
        PeerBandWidthMessage peerBandWidthMessage = new PeerBandWidthMessage(PeerBandWidthMessage.DEFAULT_WINDOW_SIZE, PeerBandWidthMessage.DYNAMIC);
        ctx.channel().writeAndFlush(peerBandWidthMessage);
    }
}

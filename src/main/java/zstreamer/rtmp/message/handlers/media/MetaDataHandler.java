package zstreamer.rtmp.message.handlers.media;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.rtmp.message.messageType.media.DataMessage;

/**
 * @author 张贝易
 * 用这个handler来保存最近一次Stream的元数据，即@setDataFrame里的内容
 */
public class MetaDataHandler extends SimpleChannelInboundHandler<DataMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DataMessage msg) throws Exception {
        if (DataMessage.META_DATA.equals(msg.getCommand().getValue())) {
            StreamerMediaHandler streamerMediaHandler = ctx.pipeline().get(StreamerMediaHandler.class);
            streamerMediaHandler.setMetaData(msg);
        }
    }
}

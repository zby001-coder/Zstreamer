package zstreamer.rtmp.message.handlers.control;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.rtmp.message.messageType.control.PeerBandWidthMessage;

/**
 * @author 张贝易
 * 处理Ack窗口大小的工具，这个是对面告诉我们它的接收窗口，也就是这里的发送窗口
 * 当发送Ack窗口大小的信息后需要等待对方发送Ack消息然后再发送
 * 不过这个只是控制流量的，大部分情况下没什么用
 * 主要和AckSenderReceiver、WindowAckSize联动
 * @see AckSenderReceiver
 * @see WindowAckSizeHandler
 */
public class PeerBandWidthHandler extends SimpleChannelInboundHandler<PeerBandWidthMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PeerBandWidthMessage msg) throws Exception {
        AckSenderReceiver ackSenderReceiver = ctx.pipeline().get(AckSenderReceiver.class);
        ackSenderReceiver.setOutAckSize(msg.getSize());
    }
}

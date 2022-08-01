package zstreamer.rtmp.message.handlers.control;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zstreamer.rtmp.message.messageType.control.WindowAckMessage;

/**
 * @author 张贝易
 * 处理Ack窗口大小的类，这个是对方告诉我们的它的发送窗口大小，也就是我们的接收窗口大小
 * 我们接收到窗口大小的数据量之后需要发送一个Ack消息对面才会继续发送
 * 不过这个只是控制流量的，大部分情况下没什么用
 * 主要和AckSenderReceiver、PeerBandWidth联动
 * @see AckSenderReceiver
 * @see PeerBandWidthHandler
 */
public class WindowAckSizeHandler extends SimpleChannelInboundHandler<WindowAckMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WindowAckMessage msg) throws Exception {
        AckSenderReceiver ackSenderReceiver = ctx.pipeline().get(AckSenderReceiver.class);
        ackSenderReceiver.setInAckSize(msg.getWindowSize());
    }
}

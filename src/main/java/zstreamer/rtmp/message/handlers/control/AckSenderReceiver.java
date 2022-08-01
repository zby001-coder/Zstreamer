package zstreamer.rtmp.message.handlers.control;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import zstreamer.rtmp.message.messageType.control.AckMessage;

/**
 * @author 张贝易
 * 发送Ack消息的handler
 * 由于Ack没什么用，所以就不做等待客户端Ack的逻辑了
 */
public class AckSenderReceiver extends ChannelDuplexHandler {
    private long inAckSize = 0;
    private long bytesReceived = 0;
    private long outAckSize = 0;
    private long bytesSent = 0;

    public long getInAckSize() {
        return inAckSize;
    }

    public void setInAckSize(long inAckSize) {
        this.inAckSize = inAckSize;
    }

    public long getOutAckSize() {
        return outAckSize;
    }

    public void setOutAckSize(long outAckSize) {
        this.outAckSize = outAckSize;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        bytesReceived += ((ByteBuf) msg).readableBytes();
        if (bytesReceived >= inAckSize && inAckSize != 0) {
            ctx.channel().writeAndFlush(new AckMessage(bytesReceived));
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        bytesSent += ((ByteBuf) msg).readableBytes();
        ctx.write(msg, promise);
    }
}

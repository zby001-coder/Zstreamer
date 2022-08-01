package zstreamer.rtmp.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import zstreamer.rtmp.message.codec.RtmpMessageDecoder;

import java.util.List;

/**
 * @author 张贝易
 * 三次握手的处理器，为了方便循环读取数据，就直接继承Decoder了
 */
public class RtmpHandShaker extends ByteToMessageDecoder {
    /**
     * 各个流程的状态值
     */
    private static final int HAND_SHAKE_S0 = 0;
    private static final int HAND_SHAKE_S1 = 1;
    private static final int HAND_SHAKE_S2 = 2;

    /**
     * 三次握手阶段的静态数据
     */
    private static final int SERVER_VERSION = 3;
    private static final int VERSION_SIZE = 1;
    private static final int TIME_STAMP_SIZE = 4;
    private static final int RANDOM_SIZE = 1528;

    /**
     * 握手阶段的非静态数据
     */
    private byte[] client;
    private byte[] server;
    private int state = HAND_SHAKE_S0;


    private void handShake0(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= VERSION_SIZE) {
            int clientVersion = (int) in.readByte();
            RtmpMessageDecoder rtmpMessageDecoder = ctx.pipeline().get(RtmpMessageDecoder.class);
            rtmpMessageDecoder.setClientVersion(clientVersion);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{SERVER_VERSION}));
            this.state = HAND_SHAKE_S1;
        }
    }

    private void handShake1(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= 2 * TIME_STAMP_SIZE + RANDOM_SIZE) {
            client = new byte[2 * TIME_STAMP_SIZE + RANDOM_SIZE];
            in.readBytes(client);
            server = new byte[2 * TIME_STAMP_SIZE + RANDOM_SIZE];
            System.arraycopy(client, 2 * TIME_STAMP_SIZE, server, 2 * TIME_STAMP_SIZE, RANDOM_SIZE);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(server));
            this.state = HAND_SHAKE_S2;
        }
    }

    private void handShake2(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= 2 * TIME_STAMP_SIZE + RANDOM_SIZE) {
            in.readBytes(client);
            System.arraycopy(server, 0, server, TIME_STAMP_SIZE, TIME_STAMP_SIZE);
            System.arraycopy(client, TIME_STAMP_SIZE, server, 0, TIME_STAMP_SIZE);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(server));
            //handShake完之后要把自己去掉
            ctx.pipeline().remove(ctx.handler());
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case HAND_SHAKE_S0:
                handShake0(ctx, in);
                break;
            case HAND_SHAKE_S1:
                handShake1(ctx, in);
                break;
            case HAND_SHAKE_S2:
                handShake2(ctx, in);
                break;
            default:
                throw new Exception("WRONG HAND SHAKE STATE!");
        }
    }
}

package zstreamer.httpflv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;

/**
 * @author 张贝易
 * 处理http分片的处理器
 */
public class ChunkWriter extends ChannelOutboundHandlerAdapter {
    private static final int CHUNK_SIZE = 4096;
    private static final String SPLITTER = "\r\n";

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            int idx = 0;
            //当长度为0时，说明流停止了
            if (bytes.length == 0) {
                //停止就设置一个长度为0的chunk
                String header = Integer.toHexString(0);

                ByteBuf out = Unpooled.buffer(header.length() + 2 * SPLITTER.length());
                out.writeBytes(header.getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));

                ctx.write(out);
                return;
            }
            //正常流分片传输
            while (idx < bytes.length) {
                int restBytes = bytes.length - idx;
                int bodySize = Integer.min(restBytes, CHUNK_SIZE);

                String header = Integer.toHexString(bodySize);

                ByteBuf out = Unpooled.buffer(bodySize + header.length() + 2 * SPLITTER.length());
                out.writeBytes(header.getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(bytes, idx, bodySize);
                out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));

                ctx.write(out);
                idx += bodySize;
            }
        }
    }
}

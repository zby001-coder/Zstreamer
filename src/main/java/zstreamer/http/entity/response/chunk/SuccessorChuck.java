package zstreamer.http.entity.response.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.nio.charset.StandardCharsets;

/**
 * @author 张贝易
 * chunk信息的包装类
 */
public class SuccessorChuck {
    private static final String SPLITTER = "\r\n";
    private final byte[] content;

    public SuccessorChuck(byte[] content) {
        this.content = content;
    }

    public ByteBuf getChunkContent() {
        String header = Integer.toHexString(content.length);
        ByteBuf out = PooledByteBufAllocator.DEFAULT.directBuffer(content.length + header.length() + 2 * SPLITTER.length());
        out.writeBytes(header.getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(content);
        out.writeBytes(SPLITTER.getBytes(StandardCharsets.US_ASCII));
        return out;
    }

    public boolean isEnd() {
        return content.length == 0;
    }
}

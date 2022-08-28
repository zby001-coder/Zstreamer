package zstreamer.http.entity.response.chunk;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

/**
 * @author 张贝易
 * 为chunked类型的响应设计
 */
public abstract class ChunkedResponse extends AbstractWrappedResponse {

    public ChunkedResponse(DefaultHttpResponse header, WrappedRequest request) {
        super(header,request);
        header.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    }

    /**
     * 生产下一个chunk的内容
     * 由于HttpCodec会自动添加分隔符和长度，所以只需要我们生产内容即可
     * @return chunk的内容
     */
    public abstract SuccessorChuck generateChunk();
}

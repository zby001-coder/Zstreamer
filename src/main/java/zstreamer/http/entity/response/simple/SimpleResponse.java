package zstreamer.http.entity.response.simple;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.WrappedResponse;

/**
 * @author 张贝易
 * 普通的响应，它必须由一个FullResponse构成
 */
public class SimpleResponse extends WrappedResponse {

    public SimpleResponse(DefaultFullHttpResponse header, WrappedRequest request) {
        super(header,request);
    }
}

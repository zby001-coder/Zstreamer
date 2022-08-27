package zstreamer.http.entity.response.simple;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

public class SimpleResponse extends AbstractWrappedResponse {

    public SimpleResponse(DefaultFullHttpResponse header, WrappedRequest request) {
        super(header,request);
    }
}

package zstreamer.http.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import zstreamer.commons.annotation.FilterPath;

/**
 * @author 张贝易
 * 处理跨域
 */
@FilterPath("/*")
public class CrossOriginFilter extends AbstractHttpFilter {

    @Override
    public boolean handleIn(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        if (request.method().equals(HttpMethod.OPTIONS)) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
            ctx.writeAndFlush(response);
            return false;
        }
        return true;
    }

    @Override
    public void handleOut(DefaultHttpResponse response) {
        response.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "*")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
}

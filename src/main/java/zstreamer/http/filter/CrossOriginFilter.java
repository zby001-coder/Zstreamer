package zstreamer.http.filter;

import io.netty.handler.codec.http.*;
import zstreamer.commons.annotation.FilterPath;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

/**
 * @author 张贝易
 * 处理跨域
 */
@FilterPath("/*")
public class CrossOriginFilter extends AbstractHttpFilter {

    @Override
    public AbstractWrappedResponse handleIn(WrappedRequest request) {
        if (request.method().equals(HttpMethod.OPTIONS)) {
            return InstanceTool.getEmptyOkResponse(request);
        }
        return null;
    }

    @Override
    public void handleOut(AbstractWrappedResponse response) {
        response.getDelegate().headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "*")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
}

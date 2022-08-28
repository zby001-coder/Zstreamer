package zstreamer.http.service.httpflv;

import io.netty.handler.codec.http.*;
import zstreamer.MediaMessagePool;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.commons.util.InstanceTool;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;
import zstreamer.http.service.AbstractHttpHandler;


/**
 * @author 张贝易
 * 观众拉流处理器
 */
@RequestPath("/live/audience/{roomName}")
public class AudienceHandler extends AbstractHttpHandler {

    @Override
    protected AbstractWrappedResponse handleGet(WrappedRequest msg) throws Exception {
        String roomName = (String) msg.getParam("roomName");
        if (MediaMessagePool.hasRoom(roomName)) {
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv");
            return new FlvChunkResponse(response, msg,roomName, 0);
        } else {
            return InstanceTool.getNotFoundResponse(msg);
        }
    }
}

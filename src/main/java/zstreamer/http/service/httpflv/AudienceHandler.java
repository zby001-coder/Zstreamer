package zstreamer.http.service.httpflv;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.*;
import zstreamer.MediaMessagePool;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.http.entity.request.WrappedHttpObject;
import zstreamer.http.entity.request.WrappedHttpRequest;
import zstreamer.http.service.AbstractHttpHandler;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author 张贝易
 * 观众登录处理器
 */
@RequestPath("/live/audience/{roomName}")
public class AudienceHandler extends AbstractHttpHandler {
    private static final ConcurrentHashMap<ChannelId, Audience> AUDIENCE_MAP = new ConcurrentHashMap<>();

    @Override
    protected boolean handleGet(ChannelHandlerContext ctx, WrappedHttpObject msg) throws Exception {
        WrappedHttpRequest request = (WrappedHttpRequest) msg;
        String roomName = request.getParam("roomName");
        Audience audience = new Audience(ctx.channel(), MediaMessagePool.getStreamer(roomName));
        AUDIENCE_MAP.put(ctx.channel().id(), audience);

        if (MediaMessagePool.registerAudience(roomName, audience)) {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv");
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

            ctx.writeAndFlush(response);
            ctx.pipeline().remove(HttpServerCodec.class);
            ctx.pipeline().addLast(ChunkWriter.getInstance());

            audience.enterRoom(roomName, 0, ctx);
        } else {
            ctx.channel().close();
        }
        return false;
    }

    @Override
    protected void onException(ChannelHandlerContext ctx) throws Exception {
        onClose(ctx);
    }

    private void onClose(ChannelHandlerContext ctx) {
        Audience audience = AUDIENCE_MAP.get(ctx.channel().id());
        if (audience != null) {
            audience.onClose();
        }
        AUDIENCE_MAP.remove(ctx.channel().id());
    }
}

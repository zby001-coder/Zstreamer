package zstreamer.httpflv;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import zstreamer.MediaMessagePool;


/**
 * @author 张贝易
 * 观众登录处理器，将观众托管给puller
 */
public class AudienceHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private Audience audience;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        if (!msg.method().equals(HttpMethod.GET)) {
            ctx.fireChannelRead(msg);
            return;
        }
        String uri = msg.uri();
        int idx = uri.lastIndexOf('/');
        String roomName = uri.substring(idx + 1);
        audience = new Audience(ctx.channel(), MediaMessagePool.getStreamer(roomName));

        if (MediaMessagePool.registerAudience(roomName, audience)) {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set("Content-Type", "video/x-flv");
            response.headers().set("Transfer-Encoding", "chunked");
            response.headers().set("Connection", "Keep-Alive");
            response.headers().set("Access-Control-Allow-Origin", "*");

            ctx.writeAndFlush(response);

            ctx.pipeline().addLast(new ChunkWriter());
            ctx.pipeline().remove(HttpServerCodec.class);

            audience.enterRoom(roomName, 0,ctx);
        }else {
            ctx.channel().close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (audience != null) {
            audience.onClose();
        }
        super.channelUnregistered(ctx);
    }
}

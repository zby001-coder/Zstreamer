package zstreamer;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import zstreamer.commons.Config;
import zstreamer.http.*;
import zstreamer.ssl.SslHandlerBuilder;
import zstreamer.rtmp.chunk.ChunkCodec;
import zstreamer.rtmp.handshake.RtmpHandShaker;
import zstreamer.rtmp.message.codec.RtmpMessageDecoder;
import zstreamer.rtmp.message.codec.RtmpMessageEncoder;
import zstreamer.rtmp.message.handlers.MessageHandlerInitializer;
import zstreamer.rtmp.message.handlers.control.AckSenderReceiver;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;

/**
 * @author 张贝易
 * 初始化所有handler的工具
 */
@ChannelHandler.Sharable
public class HandlerInjector extends ChannelInitializer<SocketChannel> {
    private static final HandlerInjector INSTANCE = new HandlerInjector();

    private HandlerInjector() {

    }

    public static HandlerInjector getInstance() {
        return INSTANCE;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new ChannelTrafficShapingHandler(Config.BYTE_OUT_PER_SECOND, Config.BYTE_IN_PER_SECOND, Config.CHECK_INTERVAL));
        InetSocketAddress address = ch.localAddress();
        int port = address.getPort();
        if (port == Config.HTTP_PORT) {
            initHttp(ch.pipeline());
        } else if (port == Config.RTMP_PORT) {
            initRtmp(ch.pipeline());
        }
        ch.pipeline().remove(this.getClass());
    }

    private void initHttp(ChannelPipeline pipeline) throws SSLException {
        if (Config.SSL_ENABLED) {
            pipeline.addLast(SslHandlerBuilder.instance(pipeline.channel()));
        }
        pipeline
                .addLast(new HttpServerCodec())
                .addLast(new ChunkedWriteHandler())
                .addLast(ResponseListener.getInstance())
                .addLast(RequestResolver.getInstance())
                .addLast(FilterExecutor.getInstance())
                .addLast(RequestDispatcher.getInstance())
                .addLast(ContextHandler.getInstance());
    }

    private void initRtmp(ChannelPipeline pipeline) {
        pipeline
                .addLast(new RtmpHandShaker())
                .addLast(new AckSenderReceiver())
                .addLast(new ChunkCodec())
                .addLast(new RtmpMessageDecoder())
                .addLast(new RtmpMessageEncoder())
                .addLast(new MessageHandlerInitializer());
    }
}

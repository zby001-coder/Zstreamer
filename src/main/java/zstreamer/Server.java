package zstreamer;

import zstreamer.httpflv.HttpFlvHandlerInitializer;
import zstreamer.rtmp.RtmpHandlerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                //允许监听多个端口，方便rtmp、http-flv多协议使用
                .option(ChannelOption.SO_REUSEADDR, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new RtmpHandlerInitializer())
                                .addLast(new HttpFlvHandlerInitializer());
                    }
                });
        try {
            ChannelFuture channelFuture1 = server.bind(new InetSocketAddress("192.168.1.103", 1935)).sync();
            ChannelFuture channelFuture2 = server.bind(new InetSocketAddress("192.168.1.103", 1936)).sync();
            channelFuture1.channel().closeFuture().sync();
            channelFuture2.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

package zstreamer.http.filter;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;

/**
 * @author 张贝易
 * 过滤器
 */
@ChannelHandler.Sharable
public abstract class AbstractHttpFilter{

    protected boolean handleIn(ChannelHandlerContext ctx,DefaultHttpRequest request) {
        return true;
    }

    protected void handleOut(DefaultHttpResponse response) {

    }
}

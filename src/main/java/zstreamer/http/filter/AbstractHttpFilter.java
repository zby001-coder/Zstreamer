package zstreamer.http.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;

/**
 * @author 张贝易
 * 过滤器
 */
public abstract class AbstractHttpFilter{

    public boolean handleIn(ChannelHandlerContext ctx,DefaultHttpRequest request) {
        return true;
    }

    public void handleOut(DefaultHttpResponse response) {

    }
}

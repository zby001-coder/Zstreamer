package zstreamer.http.filter;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

/**
 * @author 张贝易
 * 过滤器
 */
@ChannelHandler.Sharable
public abstract class AbstractHttpFilter extends ChannelDuplexHandler {
    public AbstractWrappedResponse handleIn(WrappedRequest request) {
        return null;
    }

    public void handleOut(AbstractWrappedResponse response) {

    }
}

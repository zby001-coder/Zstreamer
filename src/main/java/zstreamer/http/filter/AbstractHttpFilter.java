package zstreamer.http.filter;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.AbstractWrappedResponse;

/**
 * @author 张贝易
 * 过滤器的基类，可以处理出入两个方向的数据
 */
@ChannelHandler.Sharable
public abstract class AbstractHttpFilter extends ChannelDuplexHandler {
    /**
     * 过滤成功就不返回值
     * 过滤失败可以返回一个响应
     * @param request 请求数据
     * @return 过滤失败时返回一个响应
     */
    public AbstractWrappedResponse handleIn(WrappedRequest request) {
        return null;
    }

    public void handleOut(AbstractWrappedResponse response) {

    }
}

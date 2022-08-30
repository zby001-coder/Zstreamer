package zstreamer.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import zstreamer.commons.Config;
import zstreamer.http.entity.request.RequestInfo;

import java.util.concurrent.TimeUnit;

/**
 * @author 张贝易
 * 控制请求是否需要继续读取，启动下一个请求处理流程的handler
 */
public class ContextHandler extends ChannelDuplexHandler {
    private RequestInfo requestInfo;
    private Runnable expireTask;
    private long expireTime;

    /**
     * 某一个channel当前请求的状态
     */
    private boolean handleRequest = true;
    private boolean inTransaction = false;

    public boolean ifHandleRequest() {
        return handleRequest;
    }

    /**
     * 当一个响应发出时，这个请求的接下来的数据可用忽略
     *
     * @param ctx 上下文
     * @param msg 响应
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        //停止处理该请求
        handleRequest = false;
        ctx.write(msg, promise).addListener((future -> {
            //在这个响应写完之后，启动自动读取，因为不会混合响应了
            ctx.channel().config().setAutoRead(true);
            expireTime = System.currentTimeMillis() + Config.CONNECTION_MAX_IDLE_TIME;
            inTransaction = false;

            //添加定时关闭空闲连接的任务
            if (expireTask == null) {
                expireTask = new Runnable() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        if (!inTransaction && now >= expireTime) {
                            ctx.channel().close();
                            ctx.channel().config().setAutoRead(false);
                        } else {
                            long next = expireTime - now;
                            ctx.channel().eventLoop().schedule(this, next > 0 ? next : Config.CONNECTION_MAX_IDLE_TIME, TimeUnit.MILLISECONDS);
                        }
                    }
                };
                ctx.channel().eventLoop().schedule(expireTask, Config.CONNECTION_MAX_IDLE_TIME, TimeUnit.MILLISECONDS);
            }
        }));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //新请求到了，填充参数，开启处理
        if (evt instanceof RequestInfo) {
            this.requestInfo = (RequestInfo) evt;
            handleRequest = true;
            inTransaction = true;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //如果异常没有被下层处理，直接关闭整个通道
        ctx.channel().close();
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }
}

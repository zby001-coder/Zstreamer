package zstreamer.http.entity.request;

import io.netty.handler.codec.http.HttpMethod;

public class RequestState {
    private HttpMethod currentMethod;
    /**
     * inUse字段用来处理Http1.1的流水线发送
     * 如果某个请求在数据处理到一半的时候就确定不用处理了，可以抛出异常，使当前状态变为不可用
     * 那么这个请求的剩余部分就不会被继续处理，直到流水线下一个请求过来才继续开放
     */
    private boolean inUse;

    public RequestState(HttpMethod currentMethod, boolean inUse) {
        this.currentMethod = currentMethod;
        this.inUse = inUse;
    }

    public HttpMethod getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(HttpMethod currentMethod) {
        this.currentMethod = currentMethod;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}

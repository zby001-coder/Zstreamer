package zstreamer.commons.util;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import zstreamer.http.ContextHandler;
import zstreamer.http.entity.request.WrappedRequest;
import zstreamer.http.entity.response.WrappedResponse;
import zstreamer.http.entity.response.simple.SimpleResponse;

import java.util.concurrent.ConcurrentHashMap;

public class InstanceTool {
    public static <T> T instanceSingleton(ConcurrentHashMap<Class<T>, T> map, Class<T> clz) throws InstantiationException, IllegalAccessException {
        if (clz == null) {
            return null;
        }
        T singleton = null;
        if (!map.containsKey(clz)) {
            //没有实例化过，进行实例化
            synchronized (ContextHandler.class) {
                if (!map.containsKey(clz)) {
                    singleton = clz.newInstance();
                    map.put(clz, singleton);
                } else {
                    singleton = map.get(clz);
                }
            }
        } else {
            //从实例化过的中获取
            singleton = map.get(clz);
        }
        return singleton;
    }

    public static WrappedResponse getNotFoundResponse(WrappedRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        return new SimpleResponse(response,request);
    }

    public static WrappedResponse getWrongMethodResponse(WrappedRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        return new SimpleResponse(response,request);
    }

    public static WrappedResponse getExceptionResponse(WrappedRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
        return new SimpleResponse(response,request);
    }

    public static WrappedResponse getEmptyOkResponse(WrappedRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
        return new SimpleResponse(response,request);
    }
}

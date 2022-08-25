package zstreamer.ssl;

import io.netty.channel.Channel;
import io.netty.handler.ssl.*;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Arrays;

/**
 * @author 张贝易
 * 生产SslHandler的工具类
 */
public class SslHandlerBuilder {
    public static SslHandler instance(Channel ch) throws SSLException {
        //选择SSL工具类型：JDK的还是OpenSsl的（OpenSsl的更好一点）
        SslProvider provider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
        //用SslContextBuilder生产一个上下文
        SslContext sslCtx = SslContextBuilder
                //给服务器用的，填写证书和私钥文件位置
                .forServer(new File("C:\\Users\\31405\\Desktop\\certificate\\localhost.crt"), new File("C:\\Users\\31405\\Desktop\\certificate\\localhost.key"))
                //注入Ssl工具
                .sslProvider(provider)
                .ciphers(Arrays.asList("TLS_AES_128_GCM_SHA256"))
                //配置Ssl
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        //使用的Ssl协议类型
                        ApplicationProtocolConfig.Protocol.ALPN,
                        //找不到合适的Ssl协议时做什么，不进行提示
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        //Ssl协商失败后的行为，继续执行
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        //Ssl上层支持的协议，Http1.1
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
        return new SslHandler(sslCtx.newEngine(ch.alloc()));
    }
}

package zstreamer.commons;

public class Config {
    /**
     * 下面两个参数分别为需要自动注入的handler的包名和文件上传/下载的根路径
     */
    public static final String HANDLER_PACKAGE = "zstreamer.http";
    public static final String BASE_URL = "C:\\Users\\31405\\Desktop\\";
    /**
     * 下面三个参数分别为：占位符起始符、占位符终止符、占位符统一替换成的字符
     */
    public static final String PLACE_HOLDER_START = "{";
    public static final String PLACE_HOLDER_END = "}";
    public static final String PLACE_HOLDER_REPLACER = "*";

    /**
     * 下面三个参数分别为：http的端口、rtmp的端口、是否启动https
     */
    public static final int HTTP_PORT = 1937;
    public static final int RTMP_PORT = 1936;
    public static final boolean SSL_ENABLED = false;
}

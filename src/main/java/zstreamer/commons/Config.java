package zstreamer.commons;

public class Config {
    /**
     * 下面三个参数分别为需要自动注入的handler的包名、文件上传/下载的根路径、自动注入的filter的包名
     */
    public static final String HANDLER_PACKAGE = "zstreamer.http.service";
    public static final String BASE_URL = "C:\\Users\\31405\\Desktop\\";
    public static final String FILTER_PACKAGE = "zstreamer.http.filter";
    /**
     * 下面三个参数分别为：占位符起始符、占位符终止符、占位符统一替换成的字符
     */
    public static final char PLACE_HOLDER_START = '{';
    public static final char PLACE_HOLDER_END = '}';
    public static final String PLACE_HOLDER_REPLACER_STR = "*";
    public static final char PLACE_HOLDER_REPLACER_CHAR = '*';
    /**
     * 下面三个参数分别为：http的端口、rtmp的端口、是否启动https
     */
    public static final int HTTP_PORT = 1937;
    public static final int RTMP_PORT = 1936;
    public static final boolean SSL_ENABLED = false;

    /**
     * 下面三个参数为TrafficShaping：每秒写入byte、每秒写出byte、检测间隔。出入byte为0表示没有限制
     */
    public static final int BYTE_OUT_PER_SECOND = 0;
    public static final int BYTE_IN_PER_SECOND = 0;
    public static final int CHECK_INTERVAL = 1024 * 1024;

    /**
     * 文件分片传输时的分片大小
     */
    public static final int FILE_CHUNK_SIZE = 8192;

}

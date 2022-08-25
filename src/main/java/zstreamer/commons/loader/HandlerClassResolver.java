package zstreamer.commons.loader;

import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.commons.util.UrlTool;
import zstreamer.http.service.AbstractHttpHandler;

import java.io.IOException;
import java.util.List;


/**
 * @author 张贝易
 * 解析path对应的handler
 * 先用前缀数匹配整个url，然后得到handler的url模式串，用模式串中的占位符去取出url中的参数
 * 模式串的占位符可以使用*来表示，让其可以匹配任何字符串且优先级最低
 */
public class HandlerClassResolver {
    private static final HandlerClassResolver INSTANCE = new HandlerClassResolver();
    /**
     * 请求路径于对应的Handler的class对象的映射
     */
    private static final UrlClassTier<AbstractHttpHandler> TIER = new UrlClassTier<>();
    private static final BasePackageClassloader CLASSLOADER = BasePackageClassloader.getInstance();

    private volatile boolean loaded = false;

    private HandlerClassResolver() {
    }

    public static HandlerClassResolver getInstance() {
        return INSTANCE;
    }

    public UrlClassTier.ClassInfo<AbstractHttpHandler> resolveHandler(String requestPath) throws IOException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    loadClasses(CLASSLOADER.getClassNamesFromBasePackage(Config.HANDLER_PACKAGE));
                    loaded = true;
                }
            }
        }
        return doResolveHandler(requestPath);
    }

    private UrlClassTier.ClassInfo<AbstractHttpHandler> doResolveHandler(String url) {
        return TIER.matchHandler(url);
    }

    /**
     * 完成类加载，将path和对应handler的class对象映射
     *
     * @param clzNames 类名列表
     */
    private void loadClasses(List<String> clzNames) {
        for (String name : clzNames) {
            try {
                Class<?> clazz = Class.forName(name);
                RequestPath path = clazz.getDeclaredAnnotation(RequestPath.class);
                if (path == null || !AbstractHttpHandler.class.isAssignableFrom(clazz)) {
                    continue;
                }
                TIER.addPrefix(path.value(), (Class<AbstractHttpHandler>) clazz, UrlTool::formatHandlerPath);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

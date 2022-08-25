package zstreamer.commons.loader;

import zstreamer.commons.Config;
import zstreamer.commons.annotation.FilterPath;
import zstreamer.commons.util.UrlTool;
import zstreamer.http.filter.AbstractHttpFilter;

import java.io.IOException;
import java.util.List;

/**
 * @author 张贝易
 * filter的匹配工具
 */
public class FilterClassResolver {
    private static final FilterClassResolver INSTANCE = new FilterClassResolver();
    /**
     * 请求路径于对应的Handler的class对象的映射
     */
    private static final UrlClassTier<AbstractHttpFilter> TIER = new UrlClassTier<>();
    private static final BasePackageClassloader CLASSLOADER = BasePackageClassloader.getInstance();

    private volatile boolean loaded = false;

    private FilterClassResolver() {
    }

    public static FilterClassResolver getInstance() {
        return INSTANCE;
    }

    public List<UrlClassTier.ClassInfo<AbstractHttpFilter>> resolveFilter(String pathPattern) throws IOException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    loadClasses(CLASSLOADER.getClassNamesFromBasePackage(Config.FILTER_PACKAGE));
                    loaded = true;
                }
            }
        }
        return doResolveFilter(pathPattern);
    }

    private List<UrlClassTier.ClassInfo<AbstractHttpFilter>> doResolveFilter(String pattern) {
        return TIER.matchFilter(pattern);
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
                FilterPath path = clazz.getDeclaredAnnotation(FilterPath.class);
                if (path == null || !AbstractHttpFilter.class.isAssignableFrom(clazz)) {
                    continue;
                }
                TIER.addPrefix(path.value(), (Class<AbstractHttpFilter>) clazz, UrlTool::formatFilterPath);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

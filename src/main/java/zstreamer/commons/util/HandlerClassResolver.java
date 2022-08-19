package zstreamer.commons.util;

import zstreamer.commons.Config;
import zstreamer.commons.annotation.RequestPath;
import zstreamer.http.AbstractHttpHandler;
import io.netty.channel.ChannelHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


/**
 * @author 张贝易
 * 解析path对应的handler
 * 先用前缀数匹配整个url，然后得到handler的url模式串，用模式串中的占位符去取出url中的参数
 * 模式串的占位符可以使用*来表示，让其可以匹配任何字符串且优先级最低
 */
public class HandlerClassResolver {
    private static final String CLASS_SUFFIX = ".class";
    private static final HandlerClassResolver INSTANCE = new HandlerClassResolver();
    private BasePackageClassloader loader;
    private String contextClassPath = null;
    /**
     * 请求路径于对应的Handler的class对象的映射
     * 由于只会在第一次进行put操作，所以可以用HashMap
     */
    private final Tier urls = new Tier();
    private volatile boolean loaded = false;

    private HandlerClassResolver() {
    }

    public static HandlerClassResolver getInstance() {
        return INSTANCE;
    }

    public ClassInfo resolveHandler(String requestPath) throws IOException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    init();
                    loadClasses(loader.getClassNamesFromBasePackage(Config.HANDLER_PACKAGE));
                    loaded = true;
                }
            }
        }
        return doResolveHandler(requestPath);
    }

    private ClassInfo doResolveHandler(String url) {
        String[] prefixes = url.split("/");
        return Tier.matchUrl(prefixes, 0, urls);
    }

    /**
     * 初始化。
     * 获取BasePackageClassLoader
     */
    private void init() {
        loader = BasePackageClassloader.getInstance();
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
                if (path != null && ChannelHandler.class.isAssignableFrom(clazz)) {
                    urls.addPrefix(path.value(), (Class<? extends AbstractHttpHandler>) clazz);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Tier {
        private final HashMap<String, Tier> children = new HashMap<>();
        private final String present;
        private String url;
        private Class<? extends AbstractHttpHandler> handlerClz;

        public Tier() {
            present = "";
        }

        /**
         * 用restful风格的url的字符串构造一个前缀树
         *
         * @param url        url字符串
         * @param handlerClz url对应的handler的class对象
         */
        public Tier(String url, Class<? extends AbstractHttpHandler> handlerClz) {
            present = "";
            String[] splitPrefix = url.split("/");
            children.put(splitPrefix[0], new Tier(splitPrefix, 0, handlerClz, url));
        }

        /**
         * 构造前缀树节点，同时也是一个前缀树
         *
         * @param splitPrefix 所有前缀的数组
         * @param idx         当前前缀的索引
         * @param handlerClz  url对应的handler的class对象
         */
        public Tier(String[] splitPrefix, int idx, Class<? extends AbstractHttpHandler> handlerClz, String url) {
            present = splitPrefix[idx];
            if (idx + 1 < splitPrefix.length) {
                children.put(splitPrefix[idx + 1], new Tier(splitPrefix, idx + 1, handlerClz, url));
            } else {
                this.handlerClz = handlerClz;
                this.url = url;
            }
        }

        /**
         * 从头开始添加前缀
         *
         * @param url        url字符串
         * @param handlerClz url对应的handler的class对象
         */
        public void addPrefix(String url, Class<? extends AbstractHttpHandler> handlerClz) {
            String[] splitPrefix = url.split("/");
            //将占位符替换成 *
            for (int i = 0; i < splitPrefix.length; i++) {
                if (splitPrefix[i].startsWith(Config.PLACE_HOLDER_START) && splitPrefix[i].endsWith(Config.PLACE_HOLDER_END)) {
                    splitPrefix[i] = Config.PLACE_HOLDER_REPLACER;
                }
            }
            if (!children.containsKey(splitPrefix[0])) {
                children.put(splitPrefix[0], new Tier(splitPrefix, 0, handlerClz, url));
            } else if (splitPrefix.length > 1) {
                children.get(splitPrefix[0]).addPrefix(splitPrefix, 1, handlerClz, url);
            } else {
                children.get(splitPrefix[0]).handlerClz = handlerClz;
                children.get(splitPrefix[0]).url = url;
            }
        }

        /**
         * 添加儿子前缀
         *
         * @param splitPrefix 前缀数组
         * @param childIdx    当前tier的儿子前缀的索引，即下一个tier的前缀索引
         * @param handlerClz  url对应的handler的class对象
         * @param url         url字符串
         */
        private void addPrefix(String[] splitPrefix, int childIdx, Class<? extends AbstractHttpHandler> handlerClz, String url) {
            if (!children.containsKey(splitPrefix[childIdx])) {
                children.put(splitPrefix[childIdx], new Tier(splitPrefix, childIdx, handlerClz, url));
            } else if (splitPrefix.length > childIdx + 1) {
                children.get(splitPrefix[childIdx]).addPrefix(splitPrefix, childIdx + 1, handlerClz, url);
            } else {
                children.get(splitPrefix[childIdx]).handlerClz = handlerClz;
                children.get(splitPrefix[childIdx]).url = url;
            }
        }

        /**
         * 用一个url找出与之对应的handler
         *
         * @param prefixes url切分出来的前缀
         * @param childIdx 当前tier的儿子前缀的索引，即下一个tier的前缀索引
         * @param tier     当前前缀树
         * @return handler信息
         */
        public static ClassInfo matchUrl(String[] prefixes, int childIdx, Tier tier) {
            ClassInfo result = null;
            if (childIdx < prefixes.length && tier.children.containsKey(prefixes[childIdx])) {
                result = matchExactly(prefixes, childIdx, tier);
            }
            if (childIdx < prefixes.length && result == null && tier.children.containsKey(Config.PLACE_HOLDER_REPLACER)) {
                result = matchPlaceholder(prefixes, childIdx, tier);
            }
            return result;
        }

        /**
         * 儿子匹配精确的前缀（孙子可以用占位符）
         *
         * @param prefixes 前缀数组
         * @param childIdx 儿子的前缀的索引
         * @param tier     当前的前缀树
         * @return 如果能一直匹配到底就返回handler信息，否则返回null
         */
        private static ClassInfo matchExactly(String[] prefixes, int childIdx, Tier tier) {
            if (childIdx + 1 >= prefixes.length) {
                Tier child = tier.children.get(prefixes[childIdx]);
                return (child.url != null && child.handlerClz != null) ? new ClassInfo(child.handlerClz, child.url) : null;
            } else {
                return matchUrl(prefixes, childIdx + 1, tier.children.get(prefixes[childIdx]));
            }
        }

        /**
         * 儿子匹配占位符（孙子可以用精确的字符）
         *
         * @param prefixes 前缀数组
         * @param childIdx 儿子的前缀的索引
         * @param tier     当前的前缀树
         * @return 如果能一直匹配到底就返回handler信息，否则返回null
         */
        private static ClassInfo matchPlaceholder(String[] prefixes, int childIdx, Tier tier) {
            if (childIdx + 1 >= prefixes.length) {
                Tier child = tier.children.get(Config.PLACE_HOLDER_REPLACER);
                return (child.url != null && child.handlerClz != null) ? new ClassInfo(child.handlerClz, child.url) : null;
            } else {
                return matchUrl(prefixes, childIdx + 1, tier.children.get(Config.PLACE_HOLDER_REPLACER));
            }
        }
    }

    public static class ClassInfo {
        private final Class<? extends AbstractHttpHandler> clz;
        private final String urlPattern;

        public ClassInfo(Class<? extends AbstractHttpHandler> clz, String urlPattern) {
            this.clz = clz;
            this.urlPattern = urlPattern;
        }

        public Class<? extends AbstractHttpHandler> getClz() {
            return clz;
        }

        public String getUrlPattern() {
            return urlPattern;
        }
    }
}

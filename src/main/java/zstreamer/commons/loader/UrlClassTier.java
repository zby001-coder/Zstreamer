package zstreamer.commons.loader;

import zstreamer.commons.Config;
import zstreamer.commons.util.UrlTool;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * @param <T> 保存的class的类型
 * @author 张贝易
 * 将url对应的class保存在一颗前缀树中
 */
public class UrlClassTier<T> {
    private final HashMap<String, UrlClassTier<T>> children = new HashMap<>();
    private final String present;
    private String url;
    private final LinkedList<Class<T>> handlerClz = new LinkedList<>();

    public UrlClassTier() {
        present = "";
    }

    /**
     * 构造前缀树节点，同时也是一个前缀树
     *
     * @param splitPrefix 所有前缀的数组
     * @param idx         当前前缀的索引
     * @param handlerClz  url对应的handler的class对象
     */
    private UrlClassTier(String[] splitPrefix, int idx, Class<T> handlerClz, String url) {
        present = splitPrefix[idx];
        if (idx + 1 < splitPrefix.length) {
            children.put(splitPrefix[idx + 1], new UrlClassTier<T>(splitPrefix, idx + 1, handlerClz, url));
        } else {
            this.handlerClz.add(handlerClz);
            this.url = url;
        }
    }

    /**
     * 从头开始添加前缀
     *
     * @param url        url字符串
     * @param handlerClz url对应的handler的class对象
     */
    public void addPrefix(String url, Class<T> handlerClz, Function<String, String> initializer) {
        String origin = url;
        url = initializer.apply(url);
        String[] splitPrefix = url.split("/");
        if (!children.containsKey(splitPrefix[0])) {
            children.put(splitPrefix[0], new UrlClassTier<T>(splitPrefix, 0, handlerClz, origin));
        } else if (splitPrefix.length > 1) {
            children.get(splitPrefix[0]).addPrefix(splitPrefix, 1, handlerClz, url);
        } else {
            children.get(splitPrefix[0]).handlerClz.add(handlerClz);
            children.get(splitPrefix[0]).url = url;
        }
    }

    /**
     * 添加儿子前缀
     *
     * @param splitPrefix 前缀数组
     * @param childIdx    当前tier的儿子前缀的索引，即下一个tier的前缀索引
     * @param handlerClz  url对应的handler的class对象
     * @param origin      原始的url
     */
    private void addPrefix(String[] splitPrefix, int childIdx, Class<T> handlerClz, String origin) {
        if (!children.containsKey(splitPrefix[childIdx])) {
            children.put(splitPrefix[childIdx], new UrlClassTier<T>(splitPrefix, childIdx, handlerClz, origin));
        } else if (splitPrefix.length > childIdx + 1) {
            children.get(splitPrefix[childIdx]).addPrefix(splitPrefix, childIdx + 1, handlerClz, origin);
        } else {
            children.get(splitPrefix[childIdx]).handlerClz.add(handlerClz);
            children.get(splitPrefix[childIdx]).url = origin;
        }
    }

    public ClassInfo<T> matchHandler(String url) {
        url = UrlTool.formatHandlerPath(url);
        return matchHandler(url.split("/"), 0, this);
    }

    /**
     * 用一个url找出与之对应的handler
     *
     * @param prefixes url切分出来的前缀
     * @param childIdx 当前tier的儿子前缀的索引，即下一个tier的前缀索引
     * @param tier     当前前缀树
     * @return handler信息
     */
    private ClassInfo<T> matchHandler(String[] prefixes, int childIdx, UrlClassTier<T> tier) {
        ClassInfo<T> result = null;
        if (tier == null) {
            return null;
        }
        if (childIdx < prefixes.length && tier.children.containsKey(prefixes[childIdx])) {
            result = matchHandlerExactly(prefixes, childIdx, tier);
        }
        if (childIdx < prefixes.length && result == null && tier.children.containsKey(Config.PLACE_HOLDER_REPLACER_STR)) {
            result = matchHandlerPlaceholder(prefixes, childIdx, tier);
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
    private ClassInfo<T> matchHandlerExactly(String[] prefixes, int childIdx, UrlClassTier<T> tier) {
        if (childIdx + 1 >= prefixes.length) {
            UrlClassTier<T> child = tier.children.get(prefixes[childIdx]);
            return (child.url != null && hasElement(child.handlerClz)) ? new ClassInfo<T>(child.handlerClz.get(0), child.url) : null;
        } else {
            return matchHandler(prefixes, childIdx + 1, tier.children.get(prefixes[childIdx]));
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
    private ClassInfo<T> matchHandlerPlaceholder(String[] prefixes, int childIdx, UrlClassTier<T> tier) {
        if (childIdx + 1 >= prefixes.length) {
            UrlClassTier<T> child = tier.children.get(Config.PLACE_HOLDER_REPLACER_STR);
            return (child.url != null && hasElement(child.handlerClz)) ? new ClassInfo<>(child.handlerClz.get(0), child.url) : null;
        } else {
            return matchHandler(prefixes, childIdx + 1, tier.children.get(Config.PLACE_HOLDER_REPLACER_STR));
        }
    }

    /**
     * 用请求的url匹配filter
     *
     * @param url 请求的url
     * @return 对应的filter链表
     */
    public List<ClassInfo<T>> matchFilter(String url) {
        url = UrlTool.formatHandlerPath(url);
        return matchFilter(url.split("/"), 0, this);
    }

    /**
     * 用url匹配filter
     *
     * @param prefixes 前缀数组
     * @param childIdx 前缀树儿子要匹配的前缀下标
     * @param tier     前缀树
     * @return 对应的filter链表
     */
    private List<ClassInfo<T>> matchFilter(String[] prefixes, int childIdx, UrlClassTier<T> tier) {
        LinkedList<ClassInfo<T>> result = new LinkedList<>();
        if (tier == null) {
            return result;
        }
        if (tier.children.containsKey(Config.PLACE_HOLDER_REPLACER_STR)) {
            UrlClassTier<T> child = tier.children.get(Config.PLACE_HOLDER_REPLACER_STR);
            for (Class<T> tClass : child.handlerClz) {
                result.add(new ClassInfo<>(tClass, child.url));
            }
        }
        if (childIdx + 1 >= prefixes.length) {
            UrlClassTier<T> child = tier.children.get(prefixes[childIdx]);
            if (child != null && !prefixes[childIdx].equals(Config.PLACE_HOLDER_REPLACER_STR)) {
                for (Class<T> tClass : child.handlerClz) {
                    result.add(new ClassInfo<>(tClass, child.url));
                }
            }
        } else {
            result.addAll(matchFilter(prefixes, childIdx + 1, tier.children.get(prefixes[childIdx])));
        }
        return result;
    }

    /**
     * 判定一个链表是否有元素
     *
     * @param list 链表
     */
    private boolean hasElement(LinkedList<Class<T>> list) {
        return (list != null && list.size() > 0);
    }

    /**
     * url对应的class信息
     *
     * @param <T> class类型
     */
    public static class ClassInfo<T> {
        private final Class<T> clz;
        private final String urlPattern;

        public ClassInfo(Class<T> clz, String urlPattern) {
            this.clz = clz;
            this.urlPattern = urlPattern;
        }

        public Class<T> getClz() {
            return clz;
        }

        public String getUrlPattern() {
            return urlPattern;
        }
    }
}

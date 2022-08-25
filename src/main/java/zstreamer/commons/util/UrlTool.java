package zstreamer.commons.util;

import zstreamer.commons.Config;

/**
 * @author 张贝易
 * 处理Url的工具类
 */
public class UrlTool {
    /**
     * 将placeholder替换成配置的符号
     *
     * @param in 输入url
     */
    public static String replacePlaceHolder(String in) {
        boolean inPlaceHolder = false;
        StringBuilder out = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            if (Config.PLACE_HOLDER_START == c) {
                inPlaceHolder = true;
                out.append(Config.PLACE_HOLDER_REPLACER_STR);
                continue;
            } else if (Config.PLACE_HOLDER_END == c) {
                inPlaceHolder = false;
                continue;
            }
            if (!inPlaceHolder) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * 将url解析成路径，不包括参数
     *
     * @param in 输入url
     */
    public static String getRawUrl(String in) {
        in = removeSpace(in);
        int paramStart = in.indexOf('?');
        String path = "";
        if (paramStart != -1) {
            path = in.substring(0, paramStart);
        } else {
            path = in;
        }
        return path;
    }

    /**
     * 将空格去除
     *
     * @param in 输入url
     */
    public static String removeSpace(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将首尾的/都去除
     *
     * @param in 输入的url
     * @return
     */
    public static String formatSplitter(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            if (i == 0 && in.charAt(i) == '/' || i == in.length() - 1 && in.charAt(i) == '/') {
                continue;
            }
            sb.append(in.charAt(i));
        }
        return sb.toString();
    }

    /**
     * 将handler的url格式化，用于匹配handler
     * 去除空格、去除参数、格式化分隔符、替换占位符
     *
     * @param in 输入url
     */
    public static String formatHandlerPath(String in) {
        in = removeSpace(in);
        in = getRawUrl(in);
        in = formatSplitter(in);
        in = replacePlaceHolder(in);
        return in;
    }

    /**
     * 将过滤器的路径格式化，用于匹配filter
     * 去除空格、格式化分隔符
     *
     * @param in 输入的路径
     */
    public static String formatFilterPath(String in) {
        in = removeSpace(in);
        in = formatSplitter(in);
        StringBuilder out = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            if (c == Config.PLACE_HOLDER_REPLACER_CHAR) {
                out.append(c);
                break;
            }
            out.append(c);
        }
        return out.toString();
    }
}

package zstreamer.commons.util;

import zstreamer.commons.Config;

import java.util.HashMap;

public class UrlResolver {
    private static final UrlResolver INSTANCE = new UrlResolver();

    public static UrlResolver getInstance() {
        return INSTANCE;
    }

    private UrlResolver() {
    }

    public String getRawUrl(String url) {
        url = removeSpace(url);
        int paramStart = url.indexOf('?');
        String path = "";
        if (paramStart != -1) {
            path = url.substring(0, paramStart);
        } else {
            path = url;
        }
        return path;
    }

    public RestfulUrl resolveUrl(String url, String urlPattern) {
        RestfulUrl result = resolveFilters(url);
        String[] rawPrefixes = result.url.split("/");
        String[] patternPrefixes = urlPattern.split("/");
        for (int i = 0; i < rawPrefixes.length; i++) {
            if (patternPrefixes[i].startsWith(Config.PLACE_HOLDER_START) && patternPrefixes[i].endsWith(Config.PLACE_HOLDER_END)) {
                String key = patternPrefixes[i].substring(Config.PLACE_HOLDER_START.length(), patternPrefixes[i].length() - Config.PLACE_HOLDER_END.length());
                result.params.put(key, rawPrefixes[i]);
            }
        }
        return result;
    }

    private RestfulUrl resolveFilters(String url) {
        url = removeSpace(url);
        int paramStart = url.indexOf('?');
        String path = "";
        String paramStr = "";
        if (paramStart != -1) {
            path = url.substring(0, paramStart);
            paramStr = url.substring(paramStart + 1);
        } else {
            path = url;
        }
        return new RestfulUrl(path, resolveParams(paramStr));
    }


    private String removeSpace(String url) {
        StringBuilder sb = new StringBuilder(url.length());
        for (char c : url.toCharArray()) {
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private HashMap<String, String> resolveParams(String paramStr) {
        String[] splits = paramStr.split("&");
        HashMap<String, String> params = new HashMap<>(splits.length);
        for (String split : splits) {
            String[] kv = split.split("=");
            if (kv.length > 1) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    public static class RestfulUrl {
        private final String url;
        private final HashMap<String, String> params;

        public RestfulUrl(String url, HashMap<String, String> params) {
            this.url = url;
            this.params = params;
        }

        public String getUrl() {
            return url;
        }

        public String getParam(String key) {
            return params.get(key);
        }
    }
}

package zstreamer.commons.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author 张贝易
 * 过滤器的url
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterPath {
    String value();
}

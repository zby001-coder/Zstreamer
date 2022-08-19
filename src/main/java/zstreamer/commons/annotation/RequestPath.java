package zstreamer.commons.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author 张贝易
 * 请求的路径的注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPath {
    /**
     * 请求的路径的值
     * @return 请求路径
     */
    String value();
}

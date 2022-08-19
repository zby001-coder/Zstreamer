package zstreamer.commons.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author 张贝易
 * 将某个包下的类加载到内存中的工具类
 */
public class BasePackageClassloader {
    private static final String CLASS_SUFFIX = ".class";
    private static final BasePackageClassloader INSTANCE = new BasePackageClassloader();
    private String contextClassPath = null;

    private BasePackageClassloader() {
        init();
    }

    public static BasePackageClassloader getInstance() {
        return INSTANCE;
    }

    /**
     * 获取对应包下所有类的全类名
     *
     * @param packageName 包名
     * @return 所有类的全类名
     */
    public List<String> getClassNamesFromBasePackage(String packageName) throws IOException {
        String packagePath = packageName.replace('.', '/');
        if (contextClassPath != null) {
            //非jar包形式，可以用file去访问
            packagePath = contextClassPath + packagePath;
            return resolveClassInFile(packagePath);
        } else {
            //jar包形式，只能用JarFIle遍历文件目录
            return resolveClassInJar(packagePath);
        }
    }

    /**
     * 初始化。
     * 获取ClassPath信息
     */
    private void init() {
        URL classPathResource = Thread.currentThread().getContextClassLoader().getResource("");
        if (classPathResource != null) {
            contextClassPath = classPathResource.getPath();
            try {
                contextClassPath = URLDecoder.decode(contextClassPath, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将某一个file类型的包下的所有类的全类名提取出来
     *
     * @param packagePath 包的绝对路径
     * @return 包下的所有类的全类名
     */
    private List<String> resolveClassInFile(String packagePath) {
        List<String> classNames = new LinkedList<>();
        File file = new File(packagePath);
        File[] subFiles = file.listFiles();
        if (subFiles != null) {
            for (File subFile : subFiles) {
                String subFilePath = subFile.getPath();
                if (subFilePath.endsWith(CLASS_SUFFIX)) {
                    String className = classFilePathToName(subFilePath);
                    classNames.add(className);
                } else {
                    classNames.addAll(resolveClassInFile(subFile.getPath()));
                }
            }
        }
        return classNames;
    }

    /**
     * 将某一个jar类型的包下的所有类的全类名提取出来
     *
     * @param packagePath 包的绝对路径
     * @return 包下的所有类的全类名
     */
    private List<String> resolveClassInJar(String packagePath) throws IOException {
        List<String> classNames = new LinkedList<>();
        String pathname = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        JarFile jarFile = new JarFile(pathname);
        Enumeration<JarEntry> entries = jarFile.entries();

        boolean matched = false;
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (!jarEntry.getName().startsWith(packagePath)) {
                if (matched) {
                    //由于文件的路径是有序的，所以匹配成功后再失配就说明匹配结束了
                    break;
                } else {
                    continue;
                }
            }
            if (!jarEntry.isDirectory() && !jarEntry.getName().contains("$")) {
                //非包且非内部类就视为我们需要的类
                classNames.add(classJarPathToName(jarEntry.getName()));
            }
            matched = true;
        }
        return classNames;
    }

    /**
     * 将类的绝对路径转换为全类名
     *
     * @param classFilePath 类的绝对路径
     * @return 类的全类名
     */
    private String classFilePathToName(String classFilePath) {
        String className = classFilePath.substring(contextClassPath.length() - 1,
                classFilePath.length() - CLASS_SUFFIX.length());
        className = className.replace('/', '.');
        className = className.replace('\\', '.');
        return className;
    }

    private String classJarPathToName(String classJarPath) {
        String className = classJarPath.substring(0, classJarPath.length() - CLASS_SUFFIX.length());
        className = className.replace('/', '.');
        className = className.replace('\\', '.');
        return className;
    }
}

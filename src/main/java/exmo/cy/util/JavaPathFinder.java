package exmo.cy.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Java路径查找工具类
 * 用于查找系统中可用的Java安装路径
 */
public final class JavaPathFinder {
    
    // 防止实例化
    private JavaPathFinder() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 查找系统中所有可用的Java路径
     * @return Java可执行文件路径列表
     */
    public static List<String> findAvailableJavaPaths() {
        List<String> javaPaths = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            javaPaths.addAll(findWindowsJavaPaths());
        } else {
            javaPaths.addAll(findUnixJavaPaths());
        }
        
        return javaPaths;
    }
    
    /**
     * 查找Windows系统中的Java路径
     * @return Java路径列表
     */
    private static List<String> findWindowsJavaPaths() {
        List<String> javaPaths = new ArrayList<>();
        String[] commonPaths = {
            "C:\\Program Files\\Java",
            "C:\\Program Files (x86)\\Java",
            System.getProperty("java.home")
        };
        
        for (String basePath : commonPaths) {
            javaPaths.addAll(searchJavaInDirectory(basePath, "java.exe"));
        }
        
        return javaPaths;
    }
    
    /**
     * 查找Unix/Linux系统中的Java路径
     * @return Java路径列表
     */
    private static List<String> findUnixJavaPaths() {
        List<String> javaPaths = new ArrayList<>();
        String[] commonPaths = {
            "/usr/lib/jvm",
            "/usr/java",
            "/Library/Java/JavaVirtualMachines",
            System.getProperty("java.home")
        };
        
        for (String basePath : commonPaths) {
            javaPaths.addAll(searchJavaInDirectory(basePath, "java"));
        }
        
        // 检查当前目录下的java目录
        javaPaths.addAll(searchJavaInDirectory("java", "java"));
        
        return javaPaths;
    }
    
    /**
     * 在指定目录中搜索Java可执行文件
     * @param basePath 基础路径
     * @param javaExecutableName Java可执行文件名（java或java.exe）
     * @return 找到的Java路径列表
     */
    private static List<String> searchJavaInDirectory(String basePath, String javaExecutableName) {
        List<String> javaPaths = new ArrayList<>();
        Path basePathObj = Paths.get(basePath);
        
        if (!Files.exists(basePathObj)) {
            return javaPaths;
        }
        
        try (Stream<Path> paths = Files.list(basePathObj)) {
            paths.filter(Files::isDirectory)
                .forEach(dir -> {
                    Path javaExe = dir.resolve("bin").resolve(javaExecutableName);
                    if (FileUtils.isFileExecutable(javaExe)) {
                        javaPaths.add(javaExe.toAbsolutePath().toString());
                    }
                });
        } catch (IOException e) {
            Logger.debug("搜索Java路径时出错: " + basePath);
        }
        
        return javaPaths;
    }
    
    /**
     * 验证Java路径是否有效
     * @param javaPath Java路径
     * @return 如果有效返回true
     */
    public static boolean isValidJavaPath(String javaPath) {
        if (javaPath == null || javaPath.trim().isEmpty()) {
            return false;
        }
        
        File javaFile = new File(javaPath);
        return javaFile.exists() && javaFile.canExecute();
    }
    
    /**
     * 获取默认Java路径
     * 如果找不到Java安装，返回"java"（依赖系统PATH）
     * @return Java路径
     */
    public static String getDefaultJavaPath() {
        List<String> javaPaths = findAvailableJavaPaths();
        return javaPaths.isEmpty() ? "java" : javaPaths.get(0);
    }
}
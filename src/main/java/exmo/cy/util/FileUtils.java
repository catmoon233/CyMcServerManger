package exmo.cy.util;

import exmo.cy.exception.ServerOperationException;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 文件操作工具类
 * 提供文件和目录的常用操作方法
 */
public final class FileUtils {
    
    // 防止实例化
    private FileUtils() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     * @param dirPath 目录路径
     * @throws ServerOperationException 如果创建失败
     */
    public static void ensureDirectoryExists(String dirPath) throws ServerOperationException {
        ensureDirectoryExists(Paths.get(dirPath));
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     * @param dirPath 目录路径
     * @throws ServerOperationException 如果创建失败
     */
    public static void ensureDirectoryExists(Path dirPath) throws ServerOperationException {
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                Logger.debug("创建目录: " + dirPath);
            } catch (IOException e) {
                throw new ServerOperationException("创建目录失败: " + dirPath, e);
            }
        }
    }
    
    /**
     * 复制文件
     * @param source 源文件路径
     * @param target 目标文件路径
     * @throws ServerOperationException 如果复制失败
     */
    public static void copyFile(Path source, Path target) throws ServerOperationException {
        try {
            ensureDirectoryExists(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Logger.debug("复制文件: " + source + " -> " + target);
        } catch (IOException e) {
            throw new ServerOperationException("复制文件失败: " + source, e);
        }
    }
    
    /**
     * 递归复制目录
     * @param source 源目录路径
     * @param target 目标目录路径
     * @throws ServerOperationException 如果复制失败
     */
    public static void copyDirectory(Path source, Path target) throws ServerOperationException {
        try {
            Files.walk(source).forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        ensureDirectoryExists(targetPath);
                    } else {
                        copyFile(sourcePath, targetPath);
                    }
                } catch (ServerOperationException e) {
                    Logger.error("复制目录内容时出错", e);
                }
            });
        } catch (IOException e) {
            throw new ServerOperationException("复制目录失败: " + source, e);
        }
    }
    
    /**
     * 递归删除目录
     * @param directory 要删除的目录
     * @throws ServerOperationException 如果删除失败
     */
    public static void deleteDirectory(Path directory) throws ServerOperationException {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        Logger.error("删除文件失败: " + path, e);
                    }
                });
            Logger.debug("删除目录: " + directory);
        } catch (IOException e) {
            throw new ServerOperationException("删除目录失败: " + directory, e);
        }
    }
    
    /**
     * 解压ZIP文件
     * @param zipPath ZIP文件路径
     * @param targetDir 目标目录
     * @throws ServerOperationException 如果解压失败
     */
    public static void extractZip(Path zipPath, Path targetDir) throws ServerOperationException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                
                // 安全检查：防止ZIP路径遍历攻击
                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    throw new ServerOperationException("ZIP文件包含非法路径: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    ensureDirectoryExists(entryPath);
                } else {
                    ensureDirectoryExists(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
            Logger.info("解压ZIP文件: " + zipPath + " -> " + targetDir);
        } catch (IOException e) {
            throw new ServerOperationException("解压ZIP文件失败: " + zipPath, e);
        }
    }
    
    /**
     * 从ZIP文件中读取指定文件内容
     * @param zipPath ZIP文件路径
     * @param entryName 要读取的文件名
     * @return 文件内容，如果不存在返回空字符串
     */
    public static String readFromZip(Path zipPath, String entryName) {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry != null && !entry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(entry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    return reader.lines().collect(Collectors.joining("\n")).trim();
                }
            }
        } catch (IOException e) {
            Logger.debug("从ZIP读取文件失败: " + entryName + " 在 " + zipPath);
        }
        return "";
    }
    
    /**
     * 列出目录中的所有JAR文件
     * @param dirPath 目录路径
     * @return JAR文件列表
     */
    public static List<String> listJarFiles(Path dirPath) {
        if (!Files.exists(dirPath)) {
            return Collections.emptyList();
        }
        
        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths
                .filter(path -> path.toString().endsWith(".jar"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error("列出JAR文件失败: " + dirPath, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 列出目录中的所有ZIP文件和子目录
     * @param dirPath 目录路径
     * @return 文件和目录名列表
     */
    public static List<String> listMapsAndZips(Path dirPath) {
        if (!Files.exists(dirPath)) {
            return Collections.emptyList();
        }
        
        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths
                .filter(path -> path.toString().endsWith(".zip") || Files.isDirectory(path))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error("列出地图文件失败: " + dirPath, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 读取文本文件的全部内容
     * @param filePath 文件路径
     * @return 文件内容
     * @throws ServerOperationException 如果读取失败
     */
    public static String readTextFile(Path filePath) throws ServerOperationException {
        try {
            return Files.readString(filePath).trim();
        } catch (IOException e) {
            throw new ServerOperationException("读取文件失败: " + filePath, e);
        }
    }
    
    /**
     * 检查文件是否存在且可读
     * @param filePath 文件路径
     * @return 如果存在且可读返回true
     */
    public static boolean isFileReadable(Path filePath) {
        return Files.exists(filePath) && Files.isReadable(filePath) && !Files.isDirectory(filePath);
    }
    
    /**
     * 检查文件是否存在且可执行
     * @param filePath 文件路径
     * @return 如果存在且可执行返回true
     */
    public static boolean isFileExecutable(Path filePath) {
        return Files.exists(filePath) && Files.isExecutable(filePath);
    }
}
package exmo.cy.config;

/**
 * 应用程序常量配置类
 * 集中管理所有魔法数字和硬编码字符串
 */
public final class Constants {
    
    // 防止实例化
    private Constants() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    // 目录常量
    public static final String CORES_DIR = "cores";
    public static final String SERVERS_DIR = "servers";
    public static final String MAPS_DIR = "maps";
    public static final String BACKUPS_DIR = "backups";
    public static final String PRESET_DIR = "preset";
    
    // 文件常量
    public static final String CONFIG_FILE = "serverList.json";
    public static final String LAST_LAUNCH_CONFIG = "lastLaunch.json";
    public static final String PRESET_FILE = "preset.txt";
    public static final String VERSION_FILE = "version.txt";
    public static final String CORE_JAR = "Core.jar";
    public static final String WORLD_DIR = "world";
    
    // JVM参数常量
    public static final String DEFAULT_MIN_MEMORY = "128M";
    public static final String DEFAULT_MAX_MEMORY = "23347M";
    public static final String TIMEZONE = "Asia/Shanghai";
    public static final String FILE_ENCODING = "UTF-8";
    public static final int MAX_FUNCTION_PERMISSION_LEVEL = 4;
    public static final int MAX_OP_PERMISSION_LEVEL = 4;
    
    // 启动模式常量
    public static final int LAUNCH_MODE_CORE = 1;
    public static final int LAUNCH_MODE_MODPACK = 2;
    public static final int LAUNCH_MODE_BASIC = 3;
    public static final int LAUNCH_MODE_BASIC_FIX = 4;
    public static final int LAUNCH_MODE_CUSTOM = 5;
    
    // 文件扩展名
    public static final String JAR_EXTENSION = ".jar";
    public static final String ZIP_EXTENSION = ".zip";
    
    // 时间格式
    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    
    // 默认值
    public static final String DEFAULT_JAVA_PATH = "java";
    public static final int INVALID_CHOICE = -1;
}
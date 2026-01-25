package exmo.cy.util;

/**
 * 控制台颜色工具类
 * 提供跨平台的颜色输出支持，包括Windows CMD
 */
public class ConsoleColor {
    
    // ANSI转义序列颜色代码
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // 亮色
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // 背景色
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";
    
    // 亮色背景
    public static final String BG_BRIGHT_BLACK = "\u001B[100m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE = "\u001B[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";

    // 检测是否支持ANSI颜色
    private static Boolean ansiSupported = null;

    /**
     * 检测当前环境是否支持ANSI颜色
     * @return 如果支持ANSI颜色返回true，否则返回false
     */
    public static boolean isAnsiSupported() {
        if (ansiSupported == null) {
            // 检查系统属性
            String os = System.getProperty("os.name").toLowerCase();
            String term = System.getenv("TERM");
            String colorTerm = System.getenv("COLORTERM");

            // 在Windows上，如果启用了ANSI支持（例如在新版本的CMD或PowerShell中）或者在WSL中
            boolean isWindows = os.contains("win");
            boolean isUnixLike = os.contains("linux") || os.contains("mac") || os.contains("nix");

            // 检查是否显式禁用颜色
            String disableColors = System.getProperty("terminal.color", System.getenv("NO_COLOR"));
            if (disableColors != null && (disableColors.equalsIgnoreCase("true") || disableColors.equals("1"))) {
                ansiSupported = false;
            } else if (isUnixLike) {
                // Unix-like系统通常支持ANSI
                ansiSupported = true;
            } else if (isWindows) {
                // Windows支持取决于版本和终端
                // 如果是Windows 10及以上，并且启用虚拟终端支持，或使用PowerShell，则支持ANSI
                try {
                    String osVersion = System.getProperty("os.version");
                    if (osVersion != null) {
                        String[] versionParts = osVersion.split("\\.");
                        if (versionParts.length >= 2) {
                            int majorVersion = Integer.parseInt(versionParts[0]);
                            // Windows 10 (NT 10.0)及以上版本支持ANSI
                            ansiSupported = majorVersion >= 10;
                        } else {
                            ansiSupported = false;
                        }
                    } else {
                        ansiSupported = false;
                    }
                } catch (Exception e) {
                    ansiSupported = false;
                }
            } else {
                // 其他系统根据TERM和COLORTERM环境变量判断
                ansiSupported = (term != null && !term.isEmpty()) || 
                               (colorTerm != null && !colorTerm.isEmpty());
            }
        }

        return ansiSupported;
    }

    /**
     * 如果支持ANSI颜色，则返回带颜色的字符串，否则返回原始字符串
     * @param color 颜色代码
     * @param text 要着色的文本
     * @return 带颜色的字符串（如果支持）或原始字符串
     */
    public static String colorize(String color, String text) {
        if (isAnsiSupported()) {
            return color + text + RESET;
        }
        return text;
    }

    /**
     * 为不同日志级别提供颜色
     * @param level 日志级别
     * @param text 要着色的文本
     * @return 带颜色的字符串（如果支持）或原始字符串
     */
    public static String colorizeLogLevel(Logger.LogLevel level, String text) {
        switch (level) {
            case DEBUG:
                return colorize(BRIGHT_BLACK, text);
            case INFO:
                return colorize(GREEN, text);
            case WARN:
                return colorize(YELLOW, text);
            case ERROR:
                return colorize(RED, text);
            default:
                return text;
        }
    }

    /**
     * 为不同命令类型提供颜色
     * @param commandType 命令类型
     * @param text 要着色的文本
     * @return 带颜色的字符串（如果支持）或原始字符串
     */
    public static String colorizeCommand(String commandType, String text) {
        switch (commandType.toLowerCase()) {
            case "start":
            case "create":
                return colorize(GREEN, text);
            case "stop":
            case "delete":
                return colorize(RED, text);
            case "help":
            case "list":
                return colorize(BLUE, text);
            case "warn":
            case "warning":
                return colorize(YELLOW, text);
            default:
                return colorize(CYAN, text);
        }
    }

    /**
     * 重置ANSI支持检测缓存
     */
    public static void resetAnsiSupportCache() {
        ansiSupported = null;
    }

    /**
     * 强制启用ANSI支持
     */
    public static void forceEnableAnsi() {
        ansiSupported = true;
    }

    /**
     * 强制禁用ANSI支持
     */
    public static void forceDisableAnsi() {
        ansiSupported = false;
    }
}
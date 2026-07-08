package cn.advicenext.utility.client.system;

public class OSUtils {
    
    public enum OS {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String OS_ARCH = System.getProperty("os.arch");
    
    public static OS getOS() {
        if (OS_NAME.contains("win")) {
            return OS.WINDOWS;
        } else if (OS_NAME.contains("mac") || OS_NAME.contains("darwin")) {
            return OS.MACOS;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }
    
    public static boolean isWindows() {
        return getOS() == OS.WINDOWS;
    }
    
    public static boolean isMacOS() {
        return getOS() == OS.MACOS;
    }
    
    public static boolean isLinux() {
        return getOS() == OS.LINUX;
    }
    
    public static String getOSName() {
        return OS_NAME;
    }
    
    public static String getOSVersion() {
        return OS_VERSION;
    }
    
    public static String getOSArch() {
        return OS_ARCH;
    }
    
    public static boolean is64Bit() {
        return OS_ARCH.contains("64");
    }
    
    public static String getOSInfo() {
        return String.format("%s %s (%s)", getOS().name(), OS_VERSION, OS_ARCH);
    }
}
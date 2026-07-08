package cn.advicenext.utility.client.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Enumeration;

public class HWIDUtils {
    
    private static String cachedHWID = null;
    
    public static String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }
        
        StringBuilder hwid = new StringBuilder();
        
        // CPU信息
        hwid.append(getCPUInfo());
        
        // 主板序列号
        hwid.append(getMotherboardSerial());
        
        // MAC地址
        hwid.append(getMACAddress());
        
        // 系统信息
        hwid.append(System.getProperty("os.name"));
        hwid.append(System.getProperty("user.name"));
        
        cachedHWID = hashString(hwid.toString());
        return cachedHWID;
    }
    
    private static String getCPUInfo() {
        try {
            if (OSUtils.isWindows()) {
                Process process = Runtime.getRuntime().exec("wmic cpu get ProcessorId");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() > 0 && !line.contains("ProcessorId")) {
                        return line.trim();
                    }
                }
            } else if (OSUtils.isLinux()) {
                Process process = Runtime.getRuntime().exec("cat /proc/cpuinfo");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("processor")) {
                        return line.split(":")[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            return "unknown_cpu";
        }
        return "unknown_cpu";
    }
    
    private static String getMotherboardSerial() {
        try {
            if (OSUtils.isWindows()) {
                Process process = Runtime.getRuntime().exec("wmic baseboard get SerialNumber");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() > 0 && !line.contains("SerialNumber")) {
                        return line.trim();
                    }
                }
            } else if (OSUtils.isLinux()) {
                Process process = Runtime.getRuntime().exec("sudo dmidecode -s baseboard-serial-number");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if (line != null) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            return "unknown_mb";
        }
        return "unknown_mb";
    }
    
    private static String getMACAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            return "unknown_mac";
        }
        return "unknown_mac";
    }
    
    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return input.hashCode() + "";
        }
    }
    
    public static String getShortHWID() {
        return getHWID().substring(0, 16);
    }
    
    public static boolean validateHWID(String expectedHWID) {
        return getHWID().equals(expectedHWID);
    }
    
    public static void clearCache() {
        cachedHWID = null;
    }
}
package com.jd.rec.nl.core.utils;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.ump.profiler.CallerInfo;
import com.jd.ump.profiler.proxy.Profiler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by linmx on 2018/3/8.
 */
public abstract class MonitorUtils {

    //保存于线程变量中的ump对象Map
    private static final ThreadLocal<Map<String, CallerInfo>> umpCallers = new InheritableThreadLocal<>();

    private static String keyPrefix;

    private static String appName;

    /**
     * 开始监控
     *
     * @param key
     */
    public static void start(String key) {
        if (ConfigBase.getSystemConfig().hasPath("debug.standalone")
                && ConfigBase.getSystemConfig().getBoolean("debug.standalone")) {
            return;
        }
        if (umpCallers.get() == null) {
            umpCallers.set(new HashMap<>());
        }
        Map<String, CallerInfo> callerInfoMap = umpCallers.get();
        if (callerInfoMap.containsKey(key)) {
            // 如果已經存在監控實例，說明程序處理問題，上一個監控未結束，直接end
            end(key);
        }
        if (keyPrefix == null) {
            keyPrefix = ConfigBase.getSystemConfig().hasPath("instanceName") ?
                    ConfigBase.getSystemConfig().getString("instanceName") : "nrt-platform";
        }
        if (appName == null) {
            appName = ConfigBase.getSystemConfig().getString("monitor.appName");
        }
        final CallerInfo ump = Profiler.registerInfo(new StringBuilder(keyPrefix).append(".").append(key).toString(),
                appName, true, true);
        callerInfoMap.put(key, ump);
    }

    /**
     * 特定监控异常
     *
     * @param key       监控key
     * @param throwable 异常
     */
    public static void error(String key, Throwable throwable) {
        Profiler.countAccumulate(key + "_ERROR");
        if (umpCallers.get() == null) {
            return;
        }
        Profiler.functionError(umpCallers.get().get(key));
    }

    /**
     * 程序异常，监控停止
     *
     * @param key       监控key
     * @param throwable 异常对象
     */
    public static void errorEnd(String key, Throwable throwable) {
        error(key, throwable);
        end(key);
    }

    /**
     * 停止监控
     */
    public static void end(String key) {
        if (umpCallers.get() == null) {
            return;
        }
        Profiler.registerInfoEnd(umpCallers.get().get(key));
        umpCallers.get().remove(key);
    }

    /**
     * 清空线程变量中保存的监控对象，一般在finally中使用，防止出现内存溢出
     */
    public static void clear() {
        umpCallers.remove();
    }

    public static String getHost() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage(); // host = "hostname: hostname"
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return "UnknownHost";
        }
    }

    public static String getHostIP() {
        try {
            return (InetAddress.getLocalHost()).getHostAddress();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage(); // host = "hostname: hostname"
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return "UnknownHost";
        }

    }
}

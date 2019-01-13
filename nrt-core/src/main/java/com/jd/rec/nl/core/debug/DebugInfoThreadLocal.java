package com.jd.rec.nl.core.debug;

import com.jd.rec.nl.core.config.ConfigBase;

/**
 * @author linmx
 * @date 2018/6/28
 */
public class DebugInfoThreadLocal {

    private static final ThreadLocal<DebugInfo> info = new InheritableThreadLocal<>();

    public static void init(String uid) {
        DebugInfo debugInfo = new DebugInfo();
        debugInfo.setUid(uid);
        if (ConfigBase.getSystemConfig().hasPath("trace.uid") && ConfigBase.getSystemConfig().getStringList("trace.uid")
                .contains(uid)) {
            debugInfo.setTrace(true);
        }
        info.set(debugInfo);
    }

    public static void release() {
        info.remove();
    }

    public static String getCurrentUid() {
        if (info.get() == null) {
            return null;
        }
        return info.get().getUid();
    }

    public static boolean isTrace() {
        if (info.get() == null) {
            return false;
        }
        return info.get().isTrace();
    }

    static class DebugInfo {
        String uid;
        boolean trace = false;

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public boolean isTrace() {
            return trace;
        }

        public void setTrace(boolean trace) {
            this.trace = trace;
        }
    }
}

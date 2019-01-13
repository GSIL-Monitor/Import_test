package com.jd.rec.nl.service.common.monitor;

import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.guice.config.Initializer;
import com.jd.rec.nl.service.common.monitor.domain.GCInfo;
import com.jd.rec.nl.service.common.monitor.domain.JVMInfo;
import com.jd.rec.nl.service.common.trace.RuntimeTrace;
import org.slf4j.Logger;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 对jvm进行一个监控，实现初始化方法，这样在项目第一次启动时进行调用
 *
 * @author wl
 * @date 2018/11/13
 */
public class JvmMonitor implements Initializer {

    private static final Logger LOGGER = getLogger(JvmMonitor.class);

    static final long MB = 1024 * 1024;

    RuntimeTrace runtimeTrace;

    @Override
    public void initialize() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
        executorService.scheduleAtFixedRate(() -> {
            JVMInfo jvmInfo = new JVMInfo();
            //jvm进程id
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            //jvm内存最大上限
            long jvmMaxMemory = Runtime.getRuntime().maxMemory();
            jvmInfo.setJvmMaxMemory(jvmMaxMemory / MB);
            jvmInfo.setProcessId(runtime.getName().split("@")[0]);
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            //堆的信息 最大上限 使用多少
            MemoryUsage headMemory = memory.getHeapMemoryUsage();
            jvmInfo.setHeapMemoryMax(headMemory.getMax() / MB);
            jvmInfo.setHeapMemoryUsed(headMemory.getUsed() / MB);
            //非堆的信息 最大上限 使用多少
            MemoryUsage nonheadMemory = memory.getNonHeapMemoryUsage();
            jvmInfo.setNoheapMemoryMax(nonheadMemory.getMax() / MB);
            jvmInfo.setNoheapMemoryUsed(nonheadMemory.getUsed() / MB);
            List<GCInfo> gcInfos = new ArrayList<>();
            List<GarbageCollectorMXBean> garbages = ManagementFactory.getGarbageCollectorMXBeans();
            for (int i = garbages.size(); i > 0; i--) {
                GCInfo gcInfo = new GCInfo();
                gcInfo.setName(garbages.get(i - 1).getName());
                gcInfo.setCount(garbages.get(i - 1).getCollectionCount());
                gcInfo.setTime(garbages.get(i - 1).getCollectionTime());
                gcInfos.add(gcInfo);
            }
//            for (GarbageCollectorMXBean garbage : garbages) {
//                GCInfo gcInfo = new GCInfo();
//                gcInfo.setName(garbage.getName());
//                gcInfo.setCount(garbage.getCollectionCount());
//                gcInfo.setTime(garbage.getCollectionTime());
//                gcInfos.add(gcInfo);
//            }
            jvmInfo.setGcInfos(gcInfos);
            LOGGER.debug("jvmInfo:" + jvmInfo);
            getRuntimeTrace().info("JVMMonitor", jvmInfo);
        }, 2, 10, TimeUnit.MINUTES);
    }

    private RuntimeTrace getRuntimeTrace() {
        if (runtimeTrace == null) {
            synchronized (this) {
                if (runtimeTrace == null) {
                    runtimeTrace = InjectorService.getCommonInjector().getInstance(RuntimeTrace.class);
                }
            }
        }
        return runtimeTrace;
    }
}

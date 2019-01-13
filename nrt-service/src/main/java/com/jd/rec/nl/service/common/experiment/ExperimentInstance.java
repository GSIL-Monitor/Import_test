package com.jd.rec.nl.service.common.experiment;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.experiment.sharding.Sharding;
import com.jd.rec.nl.core.experiment.sharding.ShardingFactory;
import com.jd.rec.nl.core.guice.InjectorService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.Serializable;
import java.util.List;

/**
 * @author linmx
 * @date 2018/5/17
 */
public class ExperimentInstance<N extends Named> {

    public static final String SEP_TAB = "$";

    private static final int basicExpId = 0;

    private static final long basicPlacementId = 0;

    /**
     * 推荐位
     */
    private long placementId = basicPlacementId;

    /**
     * 外部实验id,用于匹配实验配置项目的实验id
     */
    private int outerExpId = basicExpId;

    /**
     * 分片器
     */
    private List<Sharding> shardings;

    /**
     * 参数
     */
    private Config parameters;

    /**
     * 实验配置对应的处理器
     */
    private N executor;

    /**
     * 构造函数
     *
     * @param configValue 实验配置信息
     */
    public ExperimentInstance(Config configValue, N named) {
        this.parameters = configValue.resolve();
        if (this.parameters.hasPath("placementId")) {
            this.placementId = this.parameters.getLong("placementId");
        }
        if (this.parameters.hasPath("expId")) {
            this.outerExpId = this.parameters.getInt("expId");
        }
        Config param = ConfigFactory.empty();
        if (this.parameters.hasPath("params")) {
            param = this.parameters.getConfig("params");
        }
        shardings = ShardingFactory.getShardings(this.parameters, named);
        executor = InjectorService
                .bindParameters((Class<N>) named.getClass(), param.withFallback(this.parameters.withoutPath("params")));
        String name =
                new StringBuilder().append(named.getName()).append(ExperimentInstance.SEP_TAB).append(this.placementId)
                        .append(SEP_TAB).append(this.outerExpId).toString();
        executor.setName(name);
    }

    public Config getParameters() {
        return parameters;
    }

    public N getExecutor() {
        return executor;
    }

    public boolean match(Serializable input) {
        for (Sharding sharding : shardings) {
            if (!sharding.match(input)) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return this.executor.getName();
    }
}

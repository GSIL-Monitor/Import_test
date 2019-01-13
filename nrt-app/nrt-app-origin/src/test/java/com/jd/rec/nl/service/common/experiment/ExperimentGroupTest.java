package com.jd.rec.nl.service.common.experiment;

import com.jd.rec.nl.app.origin.modules.promotionalburst.AccumulateUVAndScore;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.core.pubsub.EventPublisher;
import com.jd.rec.nl.test.junit5.callback.annontation.MockInfrastructure;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/11/7
 */
@MockInfrastructure
public class ExperimentGroupTest {

    @Test
    public void testListener() throws InterruptedException {
        AccumulateUVAndScore accumulateUVAndScore = new AccumulateUVAndScore();
        ExperimentGroup<AccumulateUVAndScore> experimentGroup = new ExperimentGroup<>(accumulateUVAndScore);
        experimentGroup.getAllExecutor()
                .forEach(accumulate -> System.out.println(accumulate.getName() + "->" + accumulate.windowSize()));

        Config config = ConfigBase.getAppConfig(accumulateUVAndScore.getName());
        Map newConfig = new HashMap();
        newConfig.put("params.windowSize", "10minutes");
        //        newConfig.put("enable", false);
        config = ConfigFactory.parseMap(newConfig).withFallback(config);
        ParameterChangeEvent event = new ParameterChangeEvent("test", accumulateUVAndScore.getName(), 619182, 118,
                config, 0);
        EventPublisher eventPublisher = InjectorService.getCommonInjector().getInstance(EventPublisher.class);
        eventPublisher.publishEvent(event);
        Thread.sleep(5000);

        experimentGroup.getAllExecutor()
                .forEach(accumulate -> System.out.println(accumulate.getName() + "->" + accumulate.windowSize()));
    }

}
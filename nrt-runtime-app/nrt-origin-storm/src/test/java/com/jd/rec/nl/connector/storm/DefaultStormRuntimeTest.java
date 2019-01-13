package com.jd.rec.nl.connector.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import com.jd.rec.nl.connector.storm.domain.InitializeParameters;
import com.jd.rec.nl.connector.storm.spout.TestSpout;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.input.SourceInfo;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import com.jd.si.util.MurmurHash;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/6/28
 */
public class DefaultStormRuntimeTest {

    private static List<String> createSpouts(final TopologyBuilder topologyBuilder, final ApplicationLoader loader,
                                             final
                                             InitializeParameters params) {
        List<String> spoutNames = new ArrayList<>();

        final SourceInfo sourceInfo = loader.getSourceInfo();
        for (SourceConfig sourceConfig : sourceInfo.getConfigs()) {
            KafkaConfig kafkaConfig = (KafkaConfig) sourceConfig;
            //            final String kafkaZk = kafkaConfig.getKafkaZk();
            //            final String offsetZk = kafkaConfig.getOffsetZk();
            final String topic = kafkaConfig.getTopic();
            final IRichSpout spout = new TestSpout(sourceConfig);
            topologyBuilder.setSpout(topic, spout, params.getSpouts());
            spoutNames.add(topic);
        }
        return spoutNames;
    }

    @Before
    public void prepareDebug() {
        Map config = new HashMap();
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.DBProxy", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Clerk", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Predictor", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Jimdb", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UnifiedOutput", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Zeus", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.Behavior", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.UserModel", true);
        config.put("debug.mock.infrastructure.com.jd.rec.nl.service.infrastructure.ILogKafka", true);
        List<String> enabledApps = new ArrayList<>();
        enabledApps.add("test");
        config.put("debug.apps", enabledApps);
        config.put("quartet.intervalUnit", "10seconds");

        config.put("burst.params.windowSize", "10seconds");

        config.put("themeBurst.params.windowSize", "20seconds");
        List<String> featureList = new ArrayList<>();
        featureList.add("20seconds");
        config.put("themeBurst.params.featureList", featureList);

        //        config.put("themeBurst.params.featureList", Arrays.asList("1minutes","3minutes","6minutes"));
        //        config.put("activityBurst.params.windowSize", "20seconds");
        com.typesafe.config.Config thread = ConfigFactory.parseMap(config);
        if (thread.hasPath("debug.apps")) {
            List<String> enableApps = thread.getStringList("debug.apps");
            Set<String> allApps = ConfigBase.getAppNames();
            Map appConfig =
                    allApps.stream().filter(appName -> !enableApps.contains(appName)).collect(Collectors.toMap(name
                                    -> name,
                            name -> {
                                Map<String, Boolean> value = new HashMap<>();
                                value.put("enable", false);
                                return value;
                            }));
            thread = ConfigFactory.parseMap(appConfig).withFallback(thread);
        }
        ConfigBase.setThreadConfig(thread);
    }

    @Test
    public void test() throws InterruptedException {
        String[] args = {"1", "10000", "200", "1", "1", "1", "2", "1", "3", "1", "-k1", "1", "-k2", "1"};
        final InitializeParameters params = new InitializeParameters(args);

        ApplicationLoader loader = new ApplicationLoader();
        loader.load();

        final TopologyBuilder topologyBuilder = new TopologyBuilder();

        final List<String> spoutNames = createSpouts(topologyBuilder, loader, params);

        DefaultStormRuntime.createBolts(topologyBuilder, spoutNames, loader, params);

        Config config = DefaultStormRuntime.configure(params);

        //        StormSubmitter.submitTopology(config, topologyBuilder.createTopology());
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("storm", config, topologyBuilder.createTopology());
        Thread.sleep(1000000);
    }

    @Test
    public void testTime() {
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void testSharp() {
        System.out.println(Math.abs(MurmurHash.hash32("fcc5523362d73db1e124c143e499fcd30d54ae6c")) % 100);
    }

    @Test
    public void testProduct() throws Exception {
        String[] args = {"1", "10000", "200", "1", "1", "1", "2", "1", "3", "1", "1"};
        DefaultStormRuntime.main(args);
    }
}
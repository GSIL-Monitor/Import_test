package com.jd.rec.nl.connector.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.gson.Gson;
import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import storm.kafka.StringScheme;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author linmx
 * @date 2018/7/2
 */
public class TestSpout extends BaseRichSpout {

    private static final String testPath = "./input.json";

    String topic;

    SpoutOutputCollector spoutOutputCollector;
    private SourceConfig config;
    private Queue<MockContent> waitForPop = new ArrayDeque<>();

    public TestSpout(SourceConfig config) {
        this.config = config;
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.spoutOutputCollector = spoutOutputCollector;
        topic = this.config.getName();
        Gson gson = new Gson();
        File testFile = new File(testPath);
        if (!testFile.exists()) {
            return;
        }
        Reader reader = null;
        try {
            reader = new FileReader(testPath);
        } catch (FileNotFoundException e) {
            throw new WrongConfigException(e);
        }
        Type type = ParameterizedTypeImpl.make(List.class, new Type[]{MockContent.class}, null);
        List<MockContent> contents = gson.fromJson(reader, type);
        contents.forEach(mockContent -> {
            if (topic.equals(mockContent.getName())) {
                waitForPop.add(mockContent);
            }
        });
    }

    @Override
    public void nextTuple() {
        while (waitForPop.size() > 0) {
            this.spoutOutputCollector.emit(new Values(waitForPop.poll().getValue()));
//            try {
//                Thread.sleep(1000L);
//            } catch (InterruptedException e) {
//            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields fields = new Fields(StringScheme.STRING_SCHEME_KEY);
        outputFieldsDeclarer.declare(fields);
    }
}

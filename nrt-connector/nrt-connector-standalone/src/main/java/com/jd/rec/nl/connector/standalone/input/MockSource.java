package com.jd.rec.nl.connector.standalone.input;

import com.google.gson.Gson;
import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.input.Source;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/6/19
 */
public class MockSource implements Source<Message<String>> {

    private static final Logger LOGGER = getLogger(MockSource.class);

    private static final String testPath = "./input.json";
    List<MockContent> runedMsg = new ArrayList<>();
    private String name = "mock-source";
    private List<String> configuredSourceName = new ArrayList<>();
    private Queue<MockContent> waitForPop = new ArrayDeque<>();
    // 当前的处理位置
    private int index = 0;

    public MockSource(Set<SourceConfig> configs) throws FileNotFoundException {
        LOGGER.debug("jsonFile init");
        configs.stream().forEach(config -> configuredSourceName.add(config.getName()));
        Gson gson = new Gson();
        File testFile = new File(testPath);
        if (!testFile.exists()) {
            return;
        }
        Reader reader = new FileReader(testPath);
        Type type = ParameterizedTypeImpl.make(List.class, new Type[]{MockContent.class}, null);
        List<MockContent> contents = gson.fromJson(reader, type);
        contents.forEach(mockContent -> {
            if (configuredSourceName.contains(mockContent.getName())) {
                waitForPop.add(mockContent);
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Message<String> get() {
        if (waitForPop.size() != 0) {
            MockContent content = waitForPop.poll();
            runedMsg.add(content);
            Message<String> message = new Message<>(content.getName(), content.getValue());
            return message;
        }
        return null;
    }
}

package com.jd.rec.nl.core.input;

import com.jd.rec.nl.core.input.domain.SourceConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据来源信息,使用source config来确定source对象,本身并不能生成source对象,需要由运行环境实现的
 * {@link SourceBuilder#build(SourceInfo)}
 * 生成对应的source对象
 *
 * @author linmx
 * @date 2018/5/29
 */
public class SourceInfo {

    private Set<SourceConfig> configs = new HashSet<>();

    public SourceInfo(Set<SourceConfig> configs) {
        this.configs = configs;
    }


    public Set<SourceConfig> getConfigs() {
        return configs;
    }

    public void setConfig(SourceConfig... config) {
        configs.addAll(Arrays.asList(config));
    }

}

package com.jd.rec.nl.core.experiment;

import com.jd.rec.nl.core.domain.Named;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 被分片单元的持有者,实际持有的是单元处于不同桶中的实例
 * 根据配置自动调整处理单元的不同以实现多个处理路径,目前只有实验处理的实现,建议仅仅用于实验和处理单元上线下线的实现
 * Created by linmx on 2018/5/14.
 */
public interface MultiVersionHolder<IN, N extends Named> extends Serializable {

    /**
     * 注册处理器
     *
     * @param node
     */
    void register(N... node);

    /**
     * 实际构建
     *
     * @return
     */
    MultiVersionHolder build();

    /**
     * 获取处理器的名字列表
     *
     * @return
     */
    Set<String> getNodesName();

    /**
     * 获取分支数
     *
     * @param nodeName
     * @return
     */
    int getBranchNum(String nodeName);

    /**
     * 获取满足条件的分支处理列表
     *
     * @param nodeName 处理链上的位置
     * @param input    本次输入
     * @return
     */
    Collection<N> getMatchedBranches(String nodeName, IN input);

    /**
     * 获取注册的所有处理单元的实例
     *
     * @return
     */
    List<N> getAllBranches();

    /**
     * 获取处理单元的所有实例
     *
     * @param nodeName 处理单元名
     * @return
     */
    List<N> getNodeAllBranches(String nodeName);

}

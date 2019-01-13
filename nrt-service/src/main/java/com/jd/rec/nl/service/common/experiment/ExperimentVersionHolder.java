package com.jd.rec.nl.service.common.experiment;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.experiment.MultiVersionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Created by linmx on 2018/5/15.
 */
public class ExperimentVersionHolder<I extends Serializable, N extends Named> implements MultiVersionHolder<I, N> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentVersionHolder.class);

    protected Map<String, Named> rawNodes = new HashMap<>();

    protected Map<String, ExperimentGroup> experimentSet = new HashMap<>();

    @Override
    public void register(N... nodes) {
        for (N node : nodes)
            rawNodes.put(node.getName(), node);
    }


    @Override
    public MultiVersionHolder build() {
        this.rawNodes.forEach((name, named) -> {
            ExperimentGroup experimentGroup = new ExperimentGroup(named);
            experimentSet.put(name, experimentGroup);
        });
        return this;
    }

    @Override
    public Set<String> getNodesName() {
        return rawNodes.keySet();
    }

    @Override
    public int getBranchNum(String nodeName) {
        if (experimentSet.containsKey(nodeName))
            return experimentSet.get(nodeName).getAllExecutor().size();
        else
            return 0;
    }

    @Override
    public Collection<N> getMatchedBranches(String nodeName, I input) {
        return this.experimentSet.get(nodeName).getMatched(input);
    }

    @Override
    public List<N> getAllBranches() {
        List<N> branches = new ArrayList<>();
        experimentSet.forEach((name, experimentGroup) -> branches.addAll(experimentGroup.getAllExecutor()));
        return branches;
    }

    @Override
    public List<N> getNodeAllBranches(String nodeName) {
        return new ArrayList<>(experimentSet.get(nodeName).getAllExecutor());
    }
}

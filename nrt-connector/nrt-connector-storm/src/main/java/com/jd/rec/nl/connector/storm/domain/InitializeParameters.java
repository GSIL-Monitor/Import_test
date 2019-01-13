package com.jd.rec.nl.connector.storm.domain;

import com.jd.rec.nl.connector.storm.util.ParameterTool;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class InitializeParameters {

    private static String toString = null;

    private int workers;

    private int timeout;

    private int maxPending;

    private int ackTaskNum;

    private int spouts;

    private int preBolts;

    private int appBolts;

    private int saverBolts;

    private int windowBolts;

    private int reducerBolts;

    private ParameterTool parameter;

    public InitializeParameters(String[] args) {
        workers = args.length > 0 ? Integer.parseInt(args[0]) : workers;
        timeout = args.length > 1 ? Integer.parseInt(args[1]) : timeout;
        maxPending = args.length > 2 ? Integer.parseInt(args[2]) : maxPending;
        ackTaskNum = args.length > 3 ? Integer.parseInt(args[3]) : -1;
        spouts = args.length > 4 ? Integer.parseInt(args[4]) : spouts;
        preBolts = args.length > 5 ? Integer.parseInt(args[5]) : preBolts;
        appBolts = args.length > 6 ? Integer.parseInt(args[6]) : appBolts;
        saverBolts = args.length > 7 ? Integer.parseInt(args[7]) : saverBolts;
        windowBolts = args.length > 8 ? Integer.parseInt(args[8]) : windowBolts;
        reducerBolts = args.length > 9 ? Integer.parseInt(args[9]) : reducerBolts;
        parameter = ParameterTool.fromArgs(Arrays.copyOfRange(args, 10, args.length));
    }

    public String getParam(String name) {
        return parameter.getParam(name);
    }

    public int getReducerBolts() {
        return reducerBolts;
    }

    public int getWindowBolts() {
        return windowBolts;
    }

    private int getValue(String[] args, int point, int defaultValue) {
        return args.length > point ? Integer.parseInt(args[point]) : defaultValue;
    }


    public int getWorkers() {
        return workers;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getMaxPending() {
        return maxPending;
    }

    public int getPreBolts() {
        return preBolts;
    }

    public int getAppBolts() {
        return appBolts;
    }


    public int getSaverBolts() {
        return saverBolts;
    }

    public int getAckTaskNum() {
        return ackTaskNum;
    }

    public int getSpouts() {
        return spouts;
    }


    @Override
    public String toString() {
        if (toString == null) {
            Field[] fields = this.getClass().getDeclaredFields();
            StringBuilder sb = new StringBuilder();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    sb.append(String.format("%s=%d  ", field.getName(), field.getInt(this)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            toString = sb.toString();
        }
        return toString;
    }
}

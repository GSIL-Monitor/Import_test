package com.jd.rec.nl.connector.storm.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by linmx on 2018/4/10.
 */
public class ParameterTool {

    protected static final String NO_VALUE_KEY = "__NO_VALUE_KEY";

    private Map<String, String> args = new HashMap<>();

    public static ParameterTool fromArgs(String[] args) {
        ParameterTool parameterTool = new ParameterTool();
        parameterTool.args = new HashMap<String, String>(args.length / 2);

        String key = null;
        String value = null;
        boolean expectValue = false;
        for (String arg : args) {
            // check for -- argument
            if (arg.startsWith("--")) {
                if (expectValue) {
                    // we got into a new key, even though we were a value --> current key is one without value
                    if (value != null) {
                        throw new IllegalStateException("Unexpected state");
                    }
                    parameterTool.args.put(key, NO_VALUE_KEY);
                    // key will be overwritten in the next step
                }
                key = arg.substring(2);
                expectValue = true;
            } // check for - argument
            else if (arg.startsWith("-")) {
                // we are waiting for a value, so this is a - prefixed value (negative number)
                if (expectValue) {
                    boolean isNum = true;
                    try {
                        new BigDecimal(arg);
                    } catch (Exception e) {
                        isNum = false;
                    }
                    if (isNum) {
                        // negative number
                        value = arg;
                        expectValue = false;
                    } else {
                        if (value != null) {
                            throw new IllegalStateException("Unexpected state");
                        }
                        // We waited for a value but found a new key. So the previous key doesnt have a value.
                        parameterTool.args.put(key, NO_VALUE_KEY);
                        key = arg.substring(1);
                        expectValue = true;
                    }
                } else {
                    // we are not waiting for a value, so its an argument
                    key = arg.substring(1);
                    expectValue = true;
                }
            } else {
                if (expectValue) {
                    value = arg;
                    expectValue = false;
                } else {
                    throw new RuntimeException(
                            "Error parsing arguments '" + Arrays.toString(args) + "' on '" + arg + "'. " +
                                    "Unexpected value. Please prefix values with -- or -.");
                }
            }

            if (value == null && key == null) {
                throw new IllegalStateException("Value and key can not be null at the same time");
            }
            if (key != null && value == null && !expectValue) {
                throw new IllegalStateException("Value expected but flag not set");
            }
            if (key != null && value != null) {
                parameterTool.args.put(key, value);
                key = null;
                value = null;
                expectValue = false;
            }
            if (key != null && key.length() == 0) {
                throw new IllegalArgumentException(
                        "The input " + Arrays.toString(args) + " contains an empty argument");
            }

            if (key != null && !expectValue) {
                parameterTool.args.put(key, NO_VALUE_KEY);
                key = null;
                expectValue = false;
            }
        }
        if (key != null) {
            parameterTool.args.put(key, NO_VALUE_KEY);
        }

        return parameterTool;
    }

    public String getParam(String key) {
        return args.get(key);
    }
}

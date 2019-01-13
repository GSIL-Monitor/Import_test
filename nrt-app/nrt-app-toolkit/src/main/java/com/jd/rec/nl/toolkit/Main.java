package com.jd.rec.nl.toolkit;

import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.toolkit.config.ZKConfigEditor;
import com.jd.rec.nl.toolkit.util.ParameterTool;
import com.jd.rec.nl.toolkit.util.ParameterTool.OpType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author linmx
 * @date 2018/10/11
 */
public class Main {

    private static final String configChangeFile = "./change.conf";
//    private static ZeusPrint zeusPrint = InjectorService.getCommonInjector().getInstance(ZeusPrint.class);

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        if (params.getOp() == OpType.help) {
            Arrays.stream(OpType.values())
                    .forEach(opType -> System.out.println(opType.name().concat(" : ").concat(opType.getDesc())));
            return;
        }
        prepareConfig();
        ZKConfigEditor zkConfigEditor = new ZKConfigEditor();
        if (params.getOp() == OpType.clearConfig) {
            if (doubleCheck("将清理全部app参数，如果线上应用正在运行将导致所有app动态下线，请确认")) {
                zkConfigEditor.clearAllConfig();
            }
        }
        if (params.getOp() == OpType.flushConfig) {
            if (doubleCheck("将刷新app参数，请务必确保使用参数的完整和最新，同时建议停止线上应用")) {
                zkConfigEditor.refreshAllConfig();
            }
        }
        if (params.getOp() == OpType.updateAppConfig) {
            String appName = params.getApp();
            if (check("将更新 ".concat(appName).concat(" 应用的基本业务参数"))) {
                zkConfigEditor.updateAppConfig(appName);
            }
        }
        if (params.getOp() == OpType.updateExpConfig) {
            String appName = params.getApp();
            String placement = params.getParam("p");
            String expId = params.getParam("e");
            if (check("将更新 ".concat(appName).concat(" 应用 ").concat(placement).concat("-").concat(expId)
                    .concat(" 实验位配置"))) {
                zkConfigEditor.updateExpConfig(appName, Long.parseLong(placement), Integer.parseInt(expId));
            }
        }
        if (params.getOp() == OpType.enableApp) {
            String appName = params.getApp();
            if (check("将动态上线 ".concat(appName).concat(" 应用"))) {
                zkConfigEditor.changeAppStatus(appName, true);
            }
        }
        if (params.getOp() == OpType.disableApp) {
            String appName = params.getApp();
            if (check("将动态下线 ".concat(appName).concat(" 应用"))) {
                zkConfigEditor.changeAppStatus(appName, false);
            }
        }
        if (params.getOp() == OpType.enableExp) {
            String appName = params.getApp();
            String placement = params.getParam("p");
            String expId = params.getParam("e");
            if (check("将动态上线 ".concat(appName).concat(" 应用 ").concat(placement).concat("-").concat(expId)
                    .concat(" 实验位配置"))) {
                zkConfigEditor.changeExpStatus(appName, Long.parseLong(placement), Integer.parseInt(expId), true);
            }
        }
        if (params.getOp() == OpType.disableExp) {
            String appName = params.getApp();
            String placement = params.getParam("p");
            String expId = params.getParam("e");
            if (check("将动态下线 ".concat(appName).concat(" 应用 ").concat(placement).concat("-").concat(expId)
                    .concat(" 实验位配置"))) {
                zkConfigEditor.changeExpStatus(appName, Long.parseLong(placement), Integer.parseInt(expId), false);
            }
        }

        System.out.println("finish!");
        System.exit(0);
    }

    public static boolean doubleCheck(String warning) {
        if (check(warning)) {
            System.out.print("请再次确认 (yes/no):");
            Scanner sc = new Scanner((System.in));  //键盘输入
            String userInput = sc.next();
            if (userInput.equals("yes")) {
                return true;
            }
        }
        return false;
    }

    public static boolean check(String warning) {
        System.out.print(warning.concat(" (yes/no):"));
        Scanner sc = new Scanner((System.in));  //键盘输入
        String userInput = sc.next();
        if (userInput.equals("yes")) {
            return true;
        }
        return false;
    }

    private static void prepareConfig() {
        File configFile = new File(configChangeFile);
        if (configFile.exists()) {
            Config changeConfig = ConfigFactory.parseFile(configFile);
            ConfigBase.setThreadConfig(changeConfig);
        }
    }
}

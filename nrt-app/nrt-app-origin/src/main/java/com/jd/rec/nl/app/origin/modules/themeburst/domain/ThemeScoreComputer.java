package com.jd.rec.nl.app.origin.modules.themeburst.domain;

import org.slf4j.Logger;
import recsys.prediction_service.UnifiedItemBurstFeatureOuterClass;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * 用户不同召回得分的计算
 *
 * @author linmx
 * @date 2018/9/29
 */
public abstract class ThemeScoreComputer {

    private static final Logger LOGGER = getLogger(ThemeScoreComputer.class);

    static List<ThemeScoreComputer> computers = Arrays.asList(new WeightingVersion(), new HcVersion());

    public static List<ThemeScoreComputer> getScoreComputers() {
        return computers;
    }

    /**
     * 标识
     *
     * @return
     */
    public abstract String getName();

    public abstract double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder);

    public static class CPUVersion extends ThemeScoreComputer {

        @Override
        public String getName() {
            return "v1";
        }

        @Override
        public double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder) {
            double score = builder.getFeatureBuilderList().stream().filter(feature -> feature.getProper().equals("cpu10")).map
                    (feature -> feature.getValue()).mapToDouble(Double::doubleValue).sum();
            LOGGER.debug("{}_{} : {} [{}]", builder.getItemId(), this.getClass().getSimpleName(), score, "10min click per user");
            return score;
        }
    }


    public static class WeightingVersion extends ThemeScoreComputer {

        @Override
        public String getName() {
            return "v2";
        }

        @Override
        public double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder) {
            StringBuilder sb = new StringBuilder();
            double score = builder.getFeatureBuilderList().stream()
                    .filter(feature -> feature.getProper().equals("uv10") || feature.getProper().equals("clk10"))
                    .map(feature -> {
                        if (feature.getProper().equals("uv10")) {
                            sb.append(feature.getValue()).append("* 0.3 +");
                            return feature.getValue() * 0.3;
                        } else {
                            sb.append(feature.getValue()).append("* 0.7 +");
                            return feature.getValue() * 0.7;
                        }
                    }).mapToDouble(Double::doubleValue).sum();
            LOGGER.debug("{}_{} : {} [{}]", builder.getItemId(), this.getClass().getSimpleName(), score, sb.toString());
            return score;
        }
    }

    public static class CPURateVersion extends ThemeScoreComputer {

        @Override
        public String getName() {
            return "v3";
        }

        @Override
        public double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder) {
            double c10 = builder.getFeatureBuilderList().stream().filter(feature -> feature.getProper().equals("cpu10")).map
                    (feature -> feature.getValue()).mapToDouble(Double::doubleValue).sum();
            double c60 = builder.getFeatureBuilderList().stream().filter(feature -> feature.getProper().equals("cpu60")).map
                    (feature -> feature.getValue()).mapToDouble(Double::doubleValue).sum();
            double score = c60 == 0 ? 0 : BigDecimal.valueOf(c10).divide(BigDecimal.valueOf(c60), 4, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
            LOGGER.debug("{}_{} : {} [{}/{}]", builder.getItemId(), this.getClass().getSimpleName(), score, c10, c60);
            return score;
        }
    }


    public static class WeightingRateVersion extends ThemeScoreComputer {

        @Override
        public String getName() {
            return "v4";
        }

        @Override
        public double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder) {
            StringBuilder sb = new StringBuilder("(");
            double c10 = builder.getFeatureBuilderList().stream()
                    .filter(feature -> feature.getProper().equals("uv10") || feature.getProper().equals("clk10"))
                    .map(feature -> {
                        if (feature.getProper().equals("uv10")) {
                            sb.append(feature.getValue()).append("* 0.3 +");
                            return feature.getValue() * 0.3;
                        } else {
                            sb.append(feature.getValue()).append("* 0.7 +");
                            return feature.getValue() * 0.7;
                        }
                    }).mapToDouble(Double::doubleValue).sum();
            sb.append(") / (");
            double c60 = builder.getFeatureBuilderList().stream()
                    .filter(feature -> feature.getProper().equals("uv60") || feature.getProper().equals("clk60"))
                    .map(feature -> {
                        if (feature.getProper().equals("uv60")) {
                            sb.append(feature.getValue()).append("* 0.3 +");
                            return feature.getValue() * 0.3;
                        } else {
                            sb.append(feature.getValue()).append("* 0.7 +");
                            return feature.getValue() * 0.7;
                        }
                    }).mapToDouble(Double::doubleValue).sum();
            sb.append(")");
            double score = c60 == 0 ? 0 : BigDecimal.valueOf(c10).divide(BigDecimal.valueOf(c60), 4, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
            LOGGER.debug("{}_{} : {} [{}]", builder.getItemId(), this.getClass().getSimpleName(), score, sb.toString());
            return score;
        }
    }

    public static class HcVersion extends ThemeScoreComputer {

        @Override
        public String getName() {
            return "v5";
        }

        @Override
        public double getScore(UnifiedItemBurstFeatureOuterClass.UnifiedItemBurstFeature.Builder builder) {
            //            score = (uv + punishment) / (this.preHc + punishment);
            double uv = builder.getFeatureList().stream().filter(feature -> feature.getProper().equals("uv10"))
                    .mapToDouble(feature -> feature.getValue()).findFirst().getAsDouble();
            double preHc = builder.getFeatureList().stream().filter(feature -> feature.getProper().equals("hc10"))
                    .mapToDouble(feature -> feature.getValue()).findFirst().getAsDouble();
            BigDecimal score = BigDecimal.valueOf(uv).add(BigDecimal.valueOf(10)).divide(BigDecimal.valueOf(preHc).divide
                    (BigDecimal.valueOf(10000), 4, BigDecimal.ROUND_HALF_UP)
                    .add(BigDecimal.valueOf(10)), 4, BigDecimal.ROUND_HALF_UP);
            LOGGER.debug("{}_{} : {} [({} + 10) / ({} + 10)]", builder.getItemId(), this.getClass().getSimpleName(), score
                    .doubleValue(), uv, preHc);
            return score.doubleValue();
        }
    }
}

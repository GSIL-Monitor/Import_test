package com.jd.rec.nl.app.origin.modules;

import com.jd.rec.nl.app.origin.modules.activityburst.ActivityAccumulateUVAndScore;
import com.jd.rec.nl.app.origin.modules.activityburst.ActivityBurstCoefficientService;
import com.jd.rec.nl.app.origin.modules.activityburst.ActivityCheckUV;
import com.jd.rec.nl.app.origin.modules.cid3preference.Cid3CofficientService;
import com.jd.rec.nl.app.origin.modules.cid3preference.Cid3Preference;
import com.jd.rec.nl.app.origin.modules.entrance.*;
import com.jd.rec.nl.app.origin.modules.entrance.Service.EntranceDBSevice;
import com.jd.rec.nl.app.origin.modules.popularshop.AccumulateShopUVAndScore;
import com.jd.rec.nl.app.origin.modules.popularshop.CheckShopUV;
import com.jd.rec.nl.app.origin.modules.popularshop.ReduceShopBurstRecall;
import com.jd.rec.nl.app.origin.modules.popularshop.ShopBurstCoefficientService;
import com.jd.rec.nl.app.origin.modules.pricegrade.PriceGrade;
import com.jd.rec.nl.app.origin.modules.promotionalburst.AccumulateUVAndScore;
import com.jd.rec.nl.app.origin.modules.promotionalburst.BurstCoefficientService;
import com.jd.rec.nl.app.origin.modules.promotionalburst.CheckSkuUV;
import com.jd.rec.nl.app.origin.modules.promotionalburst.ReduceBurstRecall;
import com.jd.rec.nl.app.origin.modules.rbcidblacklist.RbcidBlacklistUpdater;
import com.jd.rec.nl.app.origin.modules.statisticsuids.UidsCount;
import com.jd.rec.nl.app.origin.modules.themeburst.AccumulateThemeUV;
import com.jd.rec.nl.app.origin.modules.themeburst.ReduceRecall;
import com.jd.rec.nl.app.origin.modules.themeburst.ThemeUVCheck;
import com.jd.rec.nl.core.cache.CacheDefine;
import com.jd.rec.nl.core.cache.guava.GuavaCacheDefine;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.service.base.quartet.Exporter;
import com.jd.rec.nl.service.base.quartet.Reducer;
import com.jd.rec.nl.service.base.quartet.Updater;
import com.jd.rec.nl.service.base.quartet.WindowCollector;
import com.jd.rec.nl.service.common.quartet.operator.impl.JimDbExporter;

/**
 * 用于app注册
 */
@Configuration
public class ModuleConfig {

    //    public SkuExposureUpdater skuExposureAccumulator() {
    //        return new SkuExposureUpdater();
    //    }
    //
    //        public Cid3BlacklistUpdater cid3BlacklistUpdater() {
    //            return new Cid3BlacklistUpdater();
    //        }
    //
    //    public BrandPreference brandPreference(){
    //        return new BrandPreference();
    //    }

    //
    // 爆品配置
    public Updater countSkuUV() {
        return new CheckSkuUV();
    }

    public WindowCollector accumulateUV() {
        return new AccumulateUVAndScore();
    }

    public Reducer reduceBurstRecall() {
        return new ReduceBurstRecall();
    }

    public CacheDefine burstHCCache() {
        GuavaCacheDefine cacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return BurstCoefficientService.cacheName;
            }
        };
        cacheDefine.setMaximumSize(100000L);
        cacheDefine.setStatisticsInterval(600000);
        return cacheDefine;
    }

    // 三级类偏好配置
    public Cid3Preference cid3Preference() {
        return new Cid3Preference();
    }

    public CacheDefine Cid3PreferenceCache() {
        GuavaCacheDefine cacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return Cid3CofficientService.cacheName;
            }
        };
        cacheDefine.setMaximumSize(10000L);
        cacheDefine.setStatisticsInterval(600000);
        return cacheDefine;
    }

    // 设置输出(本期统一输出接口无法支持,暂时直接使用jimdb(dbproxy)进行输出)
    public Exporter cidPreferenceExporter() {
        JimDbExporter jimDbExporter = new JimDbExporter();
        jimDbExporter.setName("cid3Preference");
        return jimDbExporter;
    }

    // 用户购买力
    public PriceGrade priceGrade() {
        return new PriceGrade();
    }

    // 设置输出(本期统一输出接口无法支持,暂时直接使用jimdb(dbproxy)进行输出)
    public Exporter priceGradeExporter() {
        JimDbExporter jimDbExporter = new JimDbExporter();
        jimDbExporter.setName("priceGrade");
        return jimDbExporter;
    }


    //活动爆品配置

    public Updater countActivityUV() {
        return new ActivityCheckUV();
    }

    public WindowCollector accumulateActivityUV() {
        return new ActivityAccumulateUVAndScore();
    }

    //    public Reducer reduceActivityBurstRecall() {
    //        return new ActivityReduceBurstRecall();
    //    }

    public CacheDefine burstActivityHCCache() {
        GuavaCacheDefine cacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return ActivityBurstCoefficientService.cacheName;
            }
        };

        cacheDefine.setMaximumSize(10000L);

        cacheDefine.setStatisticsInterval(600000);
        return cacheDefine;
    }


    //店铺曝光配置
    public Updater checkShopUV() {
        return new CheckShopUV();
    }

    public WindowCollector accmulateUVandScore() {
        return new AccumulateShopUVAndScore();
    }

    public Reducer reduceShopBurstRecall() {
        return new ReduceShopBurstRecall();
    }

    public CacheDefine burstShopCCache() {
        GuavaCacheDefine cacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return ShopBurstCoefficientService.cacheName;
            }
        };
        cacheDefine.setMaximumSize(10000L);
        cacheDefine.setStatisticsInterval(600000);
        return cacheDefine;
    }

    // 热门主题 feature 和 recall
    public Updater themeUVCheck() {
        return new ThemeUVCheck();
    }

    public WindowCollector themeUVClkCount() {
        return new AccumulateThemeUV();
    }

    public Reducer themeRecall() {
        return new ReduceRecall();
    }

    /**
     * 用户统计
     *
     * @return
     */
    public Updater UidsCount() {
        return new UidsCount();
    }

    //rbcid黑名单
    public Updater RbcidBlacklistUpdater() {
        return new RbcidBlacklistUpdater();
    }

    // 设置输出(本期统一输出接口无法支持,暂时直接使用jimdb(dbproxy)进行输出)
    public Exporter rbcidBlacklistExporter() {
        JimDbExporter jimDbExporter = new JimDbExporter();
        jimDbExporter.setName("rbcidBlacklist");
        return jimDbExporter;
    }
    public Updater EntranceFloorUpdate() {
        return new EntranceFloorUpdate();
    }

    public Updater EntranceChannelUpdate() {
        return new EntranceChannelUpdate();
    }

    public Updater EntranceDidUpdate() {
        return new EntranceDidUpdate();
    }

    public Updater EntranceTagUpdate() {
        return new EntranceTagUpdate();
    }

    public CacheDefine EntranceDBSeviceCache() {
        GuavaCacheDefine cacheDefine = new GuavaCacheDefine() {
            @Override
            public String getCacheName() {
                return EntranceDBSevice.cacheName;
            }
        };
        cacheDefine.setMaximumSize(2000L);
        cacheDefine.setStatisticsInterval(600000);
        return cacheDefine;
    }
}

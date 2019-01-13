package com.jd.rec.nl.app.origin;

import com.jd.rec.nl.app.origin.common.filter.CheatUserFilter;
import com.jd.rec.nl.app.origin.common.kafka.parser.*;
import com.jd.rec.nl.core.guice.config.Configuration;
import com.jd.rec.nl.service.common.parse.MessageParse;
import com.jd.rec.nl.service.common.quartet.filter.MethodFilter;

/**
 * @author linmx
 * @date 2018/6/19
 */
@Configuration
public class ApplicationConfig {

    /**
     * 公共的刷单用户过滤
     *
     * @return
     * @throws NoSuchMethodException
     */
    public MethodFilter cheaterFilter() throws NoSuchMethodException {
        return new CheatUserFilter();
    }

    /**
     * 点击流处理
     *
     * @return
     */
    public ClickParse clickParse() {
        return new ClickParse();
    }

    public RecommendExposureParse homeShowParse() {
        return new RecommendExposureParse("appsdk_click.HomeShow");
    }

    public RecommendExposureParse cartShowParse() {
        return new RecommendExposureParse("appsdk_click.CartShow");
    }


    public RecommendExposureParse myJDShowParse() {
        return new RecommendExposureParse("appsdk_click.MyJDShow");
    }

    public RecommendExposureParse detailShowParse() {
        return new RecommendExposureParse("appsdk_click.DetailShow");
    }

    public RecommendContentExposureParse worthBuyShowParse() {
        return new RecommendContentExposureParse("appsdk_click.WorthBuyList_ProductExpo");
    }


    public ActivityPVParse acitvityParse() {
        return new ActivityPVParse("appsdk_click.Babel_PVFinish_V2");
    }


    public MessageParse dongGardenShowParse() {
        return new DongGardenShowParse();
    }

    public MessageParse dongGardenItemClkParse() {
        return new DongGardenItemClickParse();
    }

    //热门店铺的解析
    public ShopExposureParse shopShopMain() {
        return new ShopExposureParse("appsdk_pv.Shop_ShopMain");
    }

    public ShopExposureParse shopAllProducts() {
        return new ShopExposureParse("appsdk_pv.Shop_AllProducts");
    }

    public ShopExposureParse shopProducesHot() {
        return new ShopExposureParse("appsdk_pv.Shop_ProductHot");
    }

    public ShopExposureParse shopProductNew() {
        return new ShopExposureParse("appsdk_pv.Shop_ProductNew");
    }

    public ShopExposureParse myFollowShopIndependentDynamicState() {
        return new ShopExposureParse("appsdk_pv.MyFollow_ShopIndependentDynamicState");
    }

    public AppSearchParse appSearchParse() {
        return new AppSearchParse();
    }

    public OrderParse orderParse() {
        return new OrderParse();
    }
}

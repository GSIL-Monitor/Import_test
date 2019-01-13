package com.jd.rec.nl.service.modules.user.domain;

public enum BehaviorField {
    CLICK("clk"),
    FOLLOW("flw"),
    CART("cart"),
    ORDER("ord"),
    SEARCHKEY("sky"),
    SHOP("shop"),
    UNORDER("unord"),
    CARTDEL("cartdel"),
    ORDERPAY("ordpay"),
    DVC("dvc"),
    VID("vid"),
    STU("stu"),
    ALB("alb"),
    INV("inv"),
    ART("art"),
    SEARCH("srh"),
    EXPOSURE("");

    /**
     * 对应的基础设施BaseBehavior中的 field name
     */
    String fieldName;

    BehaviorField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}

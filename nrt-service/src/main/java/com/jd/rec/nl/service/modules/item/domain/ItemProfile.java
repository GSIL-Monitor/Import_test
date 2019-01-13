package com.jd.rec.nl.service.modules.item.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Created by linmx on 2018/3/8.
 */
public class ItemProfile implements Serializable {

    public List<String> extendAttributes;
    private long sku;
    private int cid3;
    private int brandId;
    private long parentId;
    private int skuHistoryClick;
    private Long shopId;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public int getCid3() {
        return cid3;
    }

    public void setCid3(int cid3) {
        this.cid3 = cid3;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public int getSkuHistoryClick() {
        return skuHistoryClick;
    }

    public void setSkuHistoryClick(int skuHistoryClick) {
        this.skuHistoryClick = skuHistoryClick;
    }

    public List<String> getExtendAttributes() {
        return extendAttributes;
    }

    public void setExtendAttributes(List<String> extendAttributes) {
        this.extendAttributes = extendAttributes;
    }

    @Override
    public String toString() {
        return "ItemProfile{" +
                "sku=" + sku +
                ", cid3=" + cid3 +
                ", brandId=" + brandId +
                ", parentId=" + parentId +
                ", skuHistoryClick=" + skuHistoryClick +
                ", extendAttributes=" + (extendAttributes == null ? "null" : extendAttributes.toString()) +
                '}';
    }
}

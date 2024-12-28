package entities;

import java.util.Objects;

import com.alibaba.fastjson.annotation.JSONField;

public final class Goods {
    @JSONField(name = "goods_id", ordinal = 1)
    private int goods_id;
    @JSONField(name = "goods_name", ordinal = 2)
    private String goods_name;
    @JSONField(name = "goods_link", ordinal = 3)
    private String goods_link;
    @JSONField(name = "img_url", ordinal = 4)
    private String img_url;
    @JSONField(name = "price", ordinal = 5)
    private double price;
    @JSONField(name = "platform", ordinal = 6)
    private String platform;
    @JSONField(name = "sku_id", ordinal = 6)
    private int sku_id;

    public Goods() {
    }

    public Goods(int goods_id, String goods_name, String goods_link, String img_url, double price, String platform) {
        this.goods_id = goods_id;
        this.goods_name = goods_name;
        this.goods_link = goods_link;
        this.img_url = img_url;
        this.price = price;
        this.platform = platform;
    }

    @Override
    public int hashCode() {
        return Objects.hash(goods_name, platform);
    }

    public int getGoodsId() {
        return goods_id;
    }

    public void setGoodsId(int goods_id) {
        this.goods_id = goods_id;
    }

    public String getGoodsName() {
        return goods_name;
    }

    public void setGoodsName(String goods_name) {
        this.goods_name = goods_name;
    }

    public String getGoodsLink() {
        return goods_link;
    }

    public void setGoodsLink(String goods_link) {
        this.goods_link = goods_link;
    }

    public String getImgUrl() {
        return img_url;
    }

    public void setImgUrl(String img_url) {
        this.img_url = img_url;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getSkuId() {
        return sku_id;
    }

    public void setSkuId(int sku_id) {
        this.sku_id = sku_id;
    }
}

    

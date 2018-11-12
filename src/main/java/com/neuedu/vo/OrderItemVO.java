package com.neuedu.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderItemVO implements Serializable {
    private Long orderNo;
    private Integer productId;
    private String productName;
    private String productImage;
    private BigDecimal currenUnitPrice;
    private Integer quantity;

    public void setOrderNo(Long orderNo) {
        this.orderNo = orderNo;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public void setCurrenUnitPrice(BigDecimal currenUnitPrice) {
        this.currenUnitPrice = currenUnitPrice;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Long getOrderNo() {
        return orderNo;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public BigDecimal getCurrenUnitPrice() {
        return currenUnitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCreateTime() {
        return createTime;
    }

    private BigDecimal totalPrice;
    private String createTime;
}

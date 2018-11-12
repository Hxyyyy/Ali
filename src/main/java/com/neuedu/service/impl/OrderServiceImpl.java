package com.neuedu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.neuedu.common.Const;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.*;
import com.neuedu.pojo.*;
import com.neuedu.service.IOrderService;
import com.neuedu.utils.BigDecimalUtils;
import com.neuedu.utils.DateUtils;
import com.neuedu.utils.PropertiesUtils;
import com.neuedu.vo.CartOrderItemVO;
import com.neuedu.vo.OrderItemVO;
import com.neuedu.vo.OrderVO;
import com.neuedu.vo.ShippingVO;
import com.sun.tools.corba.se.idl.constExpr.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;

@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    CartMapper cartMapper;
    @Autowired
    ProductMapper productMapper;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    @Autowired
    ShippingMapper shippingMapper;
    @Override
    public ServerResponse createOrder(Integer userId, Integer shippingId) {
        //参数校验
        if(shippingId==null){
            return ServerResponse.serverResponseByError("错误");
        }

        //userId查询购物车中已选中的商品--》List<Cart>
        List<Cart> cartList=cartMapper.findCartListByUserIdAndChecked(userId);
        //List<Cart>-->List<OrderItem>
        ServerResponse serverResponse=getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }

        //创建订单并保存到数据库
        //计算订单价格
        BigDecimal orderTotalPrice=new BigDecimal("0");
        List<OrderItem> orderItemList=(List<OrderItem>)serverResponse.getData();
        if(orderItemList==null||orderItemList.size()==0){
            return ServerResponse.serverResponseByError("购物车为空");
        }
        orderTotalPrice=getOrderPrice(orderItemList);


        Order order=createOrder(userId,shippingId,orderTotalPrice);
        if(order==null){
            return ServerResponse.serverResponseByError("订单创建失败");
        }
        //将订单List<OrderItem> 保存到数据库
        for(OrderItem orderItem:orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //批量插入
        orderItemMapper.insertBatch(orderItemList);

        //扣库存
        reduceProductStock(orderItemList);

        //购物车中清空已下单的商品
        cleanCart(cartList);
        OrderVO orderVO=assembleOrderVO(order,orderItemList,shippingId);
        return ServerResponse.serverResponseBySuccess(orderVO);
    }

    @Override
    public ServerResponse cancel(Integer userId, Long orderNo) {
        if(orderNo==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        //根据userId和orderNo查询订单
        Order order=orderMapper.findOrderByUseridAndOrderNo(userId,orderNo);
        if(order==null){
            return ServerResponse.serverResponseByError("订单不存在");
        }
        //判断订单状态并取消
        if(order.getStatus()!=Const.OrderCheckedEnum.ORDER_UN_PAY.getCode()){
            return ServerResponse.serverResponseByError("订单不可取消");
        }
        order.setStatus(Const.OrderCheckedEnum.ORDER_CANCELED.getCode());
        int result=orderMapper.updateByPrimaryKey(order);
        if(result>0){
            return ServerResponse.serverResponseBySuccess();
        }
        //库存回归
        List<OrderItem> orderItemList=orderItemMapper.selectByOrderNo(orderNo);
        addProductStock(orderItemList);
        //返回结果

        return ServerResponse.serverResponseByError("订单取消失败");
    }

    @Override
    public ServerResponse get_order_cart_product(Integer userId) {
        //查询购物车
        List<Cart> cartList=cartMapper.findCartListByUserIdAndChecked(userId);
        //List<Cart>-->List<OrderItem>
        ServerResponse serverResponse=getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        //组装vo
        CartOrderItemVO cartOrderItemVO=new CartOrderItemVO();
        cartOrderItemVO.setImageHost(PropertiesUtils.readByKey("imageHost"));
        List<OrderItem> orderItemList=(List<OrderItem>)serverResponse.getData();
        List<OrderItemVO> orderItemVOList=Lists.newArrayList();
        if(orderItemList==null||orderItemList.size()==0){
            return ServerResponse.serverResponseByError("购物车空");
        }
        for(OrderItem orderItem:orderItemList){
            orderItemVOList.add(assembleOrderItemVO(orderItem));
        }
        cartOrderItemVO.setOrderItemVOList(orderItemVOList);
        cartOrderItemVO.setTotalPrice(getOrderPrice(orderItemList));
        return ServerResponse.serverResponseBySuccess(cartOrderItemVO);
    }

    @Override
    public ServerResponse list(Integer userId,Integer pageNum,Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList=Lists.newArrayList();
        if(userId==null){
           //查询所有
            orderList=orderMapper.selectAll();
        }else {//查询当前用户
            orderList=orderMapper.findOrderByUserid(userId);
        }
        if(orderList==null||orderList.size()==0){
            return ServerResponse.serverResponseByError("没有查询到订单信息");
        }
        List<OrderVO> orderVOList=Lists.newArrayList();
        for(Order order:orderList){
           List<OrderItem> orderItemList=orderItemMapper.findOrderItemsByOrderno(order.getOrderNo());
            OrderVO orderVO=assembleOrderVO(order,orderItemList,order.getShippingId());
            orderVOList.add(orderVO);
        }
        PageInfo pageInfo=new PageInfo(orderVOList);
        return ServerResponse.serverResponseBySuccess(pageInfo);
    }

    @Override
    public ServerResponse detail(Long orderNo) {
        if(orderNo==null){
            return  ServerResponse.serverResponseByError("");
        }
        //查询订单
       Order order= orderMapper.findOrderByOrderNo(orderNo);
        if(order==null){
            return ServerResponse.serverResponseByError("meiy");
        }
        List<OrderItem> list=orderItemMapper.findOrderItemsByOrderno(order.getOrderNo());
        OrderVO orderVO=assembleOrderVO(order,list,order.getShippingId());


        return ServerResponse.serverResponseBySuccess(orderVO);
    }


    private OrderVO assembleOrderVO(Order order,List<OrderItem> orderItemList,Integer shippingId){
        OrderVO orderVO=new OrderVO();
        List<OrderItemVO> orderItemVOS=Lists.newArrayList();
        for(OrderItem orderItem:orderItemList){
            OrderItemVO orderItemVO=assembleOrderItemVO(orderItem);
            orderItemVOS.add(orderItemVO);
        }
        orderVO.setOrderItemVOList(orderItemVOS);
        orderVO.setImageHost(PropertiesUtils.readByKey("imageHost"));
        Shipping shipping=shippingMapper.selectByPrimaryKey(shippingId);
        if(shipping!=null){
            orderVO.setShippingId(shippingId);
            ShippingVO shippingVO=assembleShippingVO(shipping);
            orderVO.setShippingVO(shippingVO);
            orderVO.setReceiverName(shipping.getReceiverName());
        }
        orderVO.setStatus(order.getStatus());
        Const.OrderCheckedEnum orderCheckedEnum=Const.OrderCheckedEnum.codeOf(order.getStatus());
        if(orderCheckedEnum!=null){
            orderVO.setStatusDesc(orderCheckedEnum.getDesc());
        }

        orderVO.setPostage(0);
        orderVO.setPayment(order.getPayment());
        orderVO.setPaymentType(order.getPaymentType());
        Const.PayEnum payEnum=Const.PayEnum.codeOf(order.getPaymentType());
        if(payEnum!=null){
            orderVO.setPaymentTypeDesc(payEnum.getDesc());

        }
        orderVO.setOrderNo(order.getOrderNo());
        return orderVO;
    }
    private ShippingVO assembleShippingVO(Shipping shipping){
        ShippingVO shippingVO=new ShippingVO();
        if(shipping!=null){
            shippingVO.setReceiverAddress(shipping.getReceiverAddress());
            shippingVO.setReceiverCity(shipping.getReceiverCity());
            shippingVO.setReceiverDistrict(shipping.getReceiverDistrict());
            shippingVO.setReceiverMobile(shipping.getReceiverMobile());
            shippingVO.setReceiverName(shipping.getReceiverName());
            shippingVO.setReceiverPhone(shipping.getReceiverPhone());
            shippingVO.setReceiverProvince(shipping.getReceiverProvince());
            shippingVO.setReceiverZip(shipping.getReceiverZip());

        }
        return shippingVO;
    }
    private OrderItemVO assembleOrderItemVO(OrderItem orderItem){
        OrderItemVO orderItemVO=new OrderItemVO();
        if(orderItem!=null){
            orderItemVO.setQuantity(orderItem.getQuantity());
            orderItemVO.setCreateTime(DateUtils.dateToStr(orderItem.getCreateTime()));
            orderItemVO.setCurrenUnitPrice(orderItem.getCurrentUnitPrice());
            orderItemVO.setOrderNo(orderItem.getOrderNo());
            orderItemVO.setProductId(orderItem.getProductId());
            orderItemVO.setProductImage(orderItem.getProductImage());
            orderItemVO.setProductName(orderItem.getProductName());
            orderItemVO.setTotalPrice(orderItem.getTotalPrice());
        }
        return orderItemVO;
    }



    //清空=购物车中已选择的商品
    private void cleanCart(List<Cart> cartList){
        if(cartList!=null&&cartList.size()>0){
            cartMapper.batchDelete(cartList);
        }

    }
    //扣库存
    private void reduceProductStock(List<OrderItem> orderItemList){
        if(orderItemList!=null&&orderItemList.size()>0){
            for(OrderItem orderItem:orderItemList){
                Integer productId=orderItem.getProductId();
                Integer quantity=orderItem.getQuantity();
                Product product=productMapper.selectByPrimaryKey(productId);
                product.setStock(product.getStock()-quantity);
                productMapper.updateByPrimaryKey(product);
            }
        }
    }
    //增加库存
    private void addProductStock(List<OrderItem> orderItemList){
        if(orderItemList!=null&&orderItemList.size()>0){
            for(OrderItem orderItem:orderItemList){
                Integer productId=orderItem.getProductId();
                Integer quantity=orderItem.getQuantity();
                Product product=productMapper.selectByPrimaryKey(productId);
                product.setStock(product.getStock()+quantity);
                productMapper.updateByPrimaryKey(product);
            }
        }
    }
    //创建订单
    private Order createOrder(Integer userId, Integer shippingId, BigDecimal orderTotalPrice){
        Order order=new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShippingId(shippingId);
        order.setStatus(Const.OrderCheckedEnum.ORDER_UN_PAY.getCode());
        //订单金额
        order.setPayment(orderTotalPrice);
        order.setPostage(0);
        order.setPaymentType(Const.PayEnum.PAY_ONLINE.getCode());
        int result=orderMapper.insert(order);
        if(result>0){
            return order;
        }
        return null;
    }
    //生成订单编号
    private Long generateOrderNo(){
        return System.currentTimeMillis()+new Random().nextInt(100);
    }
    private ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        if(cartList==null||cartList.size()==0){
            return ServerResponse.serverResponseByError("购物车空");
        }
        List<OrderItem> orderItemList= Lists.newArrayList();
        for(Cart cart:cartList){
            OrderItem orderItem=new OrderItem();
            orderItem.setUserId(userId);
            Product product=productMapper.selectByPrimaryKey(cart.getProductId());
            if(product==null){
                return ServerResponse.serverResponseByError("id为"+cart.getProductId()+"商品不存在");
            }
            if(product.getStatus()!= Const.ProductStatusEnum.PRODUCT_ONLINE.getCode()){
                return ServerResponse.serverResponseByError("id为"+product.getId()+"的商品已经下架");
            }
            if(product.getStock()<cart.getQuantity()){
                return ServerResponse.serverResponseByError("id为"+product.getId()+"库存不足");
            }
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setProductId(product.getId());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setProductName(product.getName());
            orderItem.setTotalPrice(BigDecimalUtils.mul(product.getPrice().doubleValue(),cart.getQuantity().doubleValue()));

            orderItemList.add(orderItem);
        }
        return ServerResponse.serverResponseBySuccess(orderItemList);
    }
    //计算订单总价格
    private BigDecimal getOrderPrice(List<OrderItem> orderItemList){
        BigDecimal bigDecimal=new BigDecimal("0");
        for(OrderItem orderItem:orderItemList){
            bigDecimal=BigDecimalUtils.add(bigDecimal.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }

        return bigDecimal;
    }
}

package com.neuedu.service.impl;

import com.google.common.collect.Lists;
import com.neuedu.common.Const;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.CartMapper;
import com.neuedu.dao.ProductMapper;
import com.neuedu.pojo.Cart;
import com.neuedu.pojo.Product;
import com.neuedu.service.ICartService;
import com.neuedu.service.IProductService;
import com.neuedu.utils.BigDecimalUtils;
import com.neuedu.vo.CartProductVO;
import com.neuedu.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    CartMapper cartMapper;
    @Autowired
    ProductMapper productMapper;
//添加商品
    @Override
    public ServerResponse add(Integer userId,Integer productId, Integer count) {
        //step1:非空参数校验

        if (productId==null&&productId.equals("")){
            return ServerResponse.serverResponseByError("商品id为空");
        }
        if (count==null&&count.equals("")){
            return ServerResponse.serverResponseByError("商品数量为空");
        }
        Product product=productMapper.selectByPrimaryKey(productId);
        if(product==null){
            return ServerResponse.serverResponseByError("要加的商品不存在");
        }
        //step2:根据productId和userId查询购物信息
        Cart cart=cartMapper.selectCartByUserIdAndProductId(userId,productId);
        if (cart==null){
            //添加
            Cart cart1=new Cart();
            cart1.setUserId(userId);
            cart1.setProductId(productId);
            cart1.setQuantity(count);
            cart1.setChecked(Const.CartCheckedEnum.PRODUCT_CHECKED.getCode());
            cartMapper.insert(cart1);
        }else{
            //更新
            Cart cart1=new Cart();
            cart1.setId(cart.getId());
            cart1.setUserId(userId);
            cart1.setProductId(productId);
            cart1.setQuantity(count+cart.getQuantity());
            cart1.setChecked(Const.CartCheckedEnum.PRODUCT_CHECKED.getCode());
            cartMapper.updateByPrimaryKey(cart1);
        }
        CartVO cartVO=getCartVOLimit(userId);
        return ServerResponse.serverResponseBySuccess(cartVO);
    }
//显示列表
    @Override
    public ServerResponse list(Integer userId) {
        CartVO cartVO=getCartVOLimit(userId);
        return ServerResponse.serverResponseBySuccess(cartVO);
    }
    //更新购物车某个商品的数量
    @Override
    public ServerResponse update(Integer userId,Integer productId, Integer count) {
        //非空判断
        if (productId==null&&productId.equals("")){
            return ServerResponse.serverResponseByError("商品id为空");
        }
        if (count==null&&count.equals("")){
            return ServerResponse.serverResponseByError("商品数量为空");
        }

        //查询购物车
        Cart cart=cartMapper.selectCartByUserIdAndProductId(userId,productId);

        //更新数量
        if(cart!=null){
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKey(cart);
        }
        //返回cartVO
        return ServerResponse.serverResponseBySuccess(getCartVOLimit(userId));
    }
    //删除商品
    @Override
    public ServerResponse delete_product(Integer userId, String productIds) {
        //非空校验
        if(productIds==null||productIds.equals("")){
            ServerResponse.serverResponseByError("参数不能为空");
        }
        //productIds-->List<Integer>
        List<Integer> productList= Lists.newArrayList();
        String[] productIdsArr=productIds.split(",");
        if(productIdsArr!=null&&productIdsArr.length>0){
            for(String productIdstr:productIdsArr){
                Integer productId=Integer.parseInt(productIdstr);
                productList.add(productId);
            }
        }
        //删除
        cartMapper.deleteByUseridAndProductIds(userId,productList);
        //返回

        return ServerResponse.serverResponseBySuccess(getCartVOLimit(userId));
    }

    @Override
    public ServerResponse select(Integer userId, Integer productId,Integer check) {
//
        //调用dao接口
         cartMapper.selectOrUnselectProduct(userId,productId,check);


        return ServerResponse.serverResponseBySuccess(getCartVOLimit(userId));
    }
    //获取所有数量
    @Override
    public ServerResponse get_cart_product_count(Integer userId) {
        int sum=cartMapper.get_cart_product_count(userId);
        return ServerResponse.serverResponseBySuccess(sum);
    }

    private CartVO getCartVOLimit(Integer userId){
        CartVO cartVO=new CartVO();
        //step1://根据userId查询购物信息--》List<Cart>
        List<Cart> cartList=cartMapper.selectCartByUserId(userId);
        //step2:List<Cart>-->List<CartProduct>
        List<CartProductVO> cartProductVOList= Lists.newArrayList();
        //商品总价格
        BigDecimal carttoalprice=new BigDecimal("0");
        if(cartList!=null&&cartList.size()>0){
            for (Cart cart:cartList){
                CartProductVO cartProductVO=new CartProductVO();
                cartProductVO.setId(cart.getId());
                cartProductVO.setQuantity(cart.getQuantity());
                cartProductVO.setUserId(cart.getUserId());
                cartProductVO.setProductChecked(cart.getChecked());
                //查询商品
                Product product= productMapper.selectByPrimaryKey(cart.getProductId());
                if (product!=null){
                    cartProductVO.setProductId(cart.getProductId());
                    cartProductVO.setProductMainImage(product.getMainImage());
                    cartProductVO.setProductName(product.getName());
                    cartProductVO.setProductPrice(product.getPrice());
                    cartProductVO.setProductStatus(product.getStatus());
                    cartProductVO.setProductStock(product.getStock());
                    cartProductVO.setProductSubtitle(product.getSubtitle());
                    int stock=product.getStock();
                    int limitProductCount=0;
                    if (stock>cart.getQuantity()){
                        limitProductCount=cart.getQuantity();
                        cartProductVO.setLimitQuantity("LIMIT_NUM_SUCCESS");
                    }else {//商品库存不足
                        limitProductCount=stock;
                        //更新购物车中的商品的数量
                        Cart cart1=new Cart();
                        cart1.setId(cart1.getId());
                        cart1.setQuantity(stock);
                        cart1.setProductId(cart.getProductId());
                        cart1.setChecked(cart1.getChecked());
                        cart1.setUserId(cart.getUserId());
                        cartMapper.updateByPrimaryKey(cart1);
                        cartProductVO.setLimitQuantity("LIMIT_NUM_FAIL");
                    }
                    cartProductVO.setQuantity(limitProductCount);
                    cartProductVO.setProductTotalPrice(BigDecimalUtils.mul(product.getPrice().doubleValue(),Double.valueOf(cartProductVO.getQuantity())));
                }
                //step3:计算总价格
                if(cartProductVO.getProductChecked()==Const.CartCheckedEnum.PRODUCT_CHECKED.getCode()) {
                    carttoalprice = BigDecimalUtils.add(carttoalprice.doubleValue(), cartProductVO.getProductTotalPrice().doubleValue());
                }
                cartProductVOList.add(cartProductVO);
            }
        }
        cartVO.setCartProductVOList(cartProductVOList);
        cartVO.setCarttoralprice(carttoalprice);
        //step4:判断购物车是否全选
        int result=cartMapper.isCheckedAll(userId);
        if(result>0){
            //未全选
            cartVO.setIsallchecked(false);
        }else {
            cartVO.setIsallchecked(true);
        }
        //step5:返回结果
        return cartVO;
    }




}

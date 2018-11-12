package com.neuedu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.ShippingMapper;
import com.neuedu.pojo.Shipping;
import com.neuedu.service.IAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AddresServiceImpl implements IAddressService {
    @Autowired
    ShippingMapper shippingMapper;
    @Override
    public ServerResponse add(Integer userId, Shipping shipping) {
        if(shipping==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        shipping.setUserId(userId);
        shippingMapper.insert(shipping);
        Map<String,Integer> map= Maps.newHashMap();
        map.put("shippingId",shipping.getId());

        return ServerResponse.serverResponseBySuccess(map);
    }
//    删除地址
    @Override
    public ServerResponse del(Integer userId, Integer shippingId) {
        if(shippingId==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        int result=shippingMapper.deleteByUserIdShippingId(userId,shippingId);
        if(result>0){
            return ServerResponse.serverResponseBySuccess("删除成功");
        }
        return ServerResponse.serverResponseByError("删除失败");
    }

    @Override
    public ServerResponse update(Shipping shipping) {
        if(shipping==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        int result=shippingMapper.updateBySelectiveKey(shipping);
        if(result>0){
            return ServerResponse.serverResponseBySuccess();
        }
        return ServerResponse.serverResponseByError("失败");
    }

    @Override
    public ServerResponse select(Integer shippingId) {
        if(shippingId==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        Shipping shipping=shippingMapper.selectByPrimaryKey(shippingId);
        return ServerResponse.serverResponseBySuccess(shipping);
    }

    @Override
    public ServerResponse list(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> list=shippingMapper.selectAll();
        PageInfo pageInfo=new PageInfo(list);
        return ServerResponse.serverResponseBySuccess(pageInfo);
    }
}

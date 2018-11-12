package com.neuedu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.neuedu.common.Const;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.CategoryMapper;
import com.neuedu.dao.ProductMapper;
import com.neuedu.pojo.Category;
import com.neuedu.pojo.Product;
import com.neuedu.service.ICategoryService;
import com.neuedu.service.IProductService;
import com.neuedu.utils.DateUtils;
import com.neuedu.utils.PropertiesUtils;
import com.neuedu.vo.ProductDetailVO;
import com.neuedu.vo.ProductListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductServiceImpl implements IProductService {
    @Autowired
    ProductMapper productMapper;
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    ICategoryService categoryService;
    @Override
    public ServerResponse saveOrUpdate(Product product) {
        //非空非空校验


        //设置商品的主图
        String subImage=product.getSubImages();
        if(subImage!=null&&!subImage.equals("")){
            String[] subImageArr=subImage.split(",");
            if(subImageArr.length>0){
                product.setMainImage(subImageArr[0]);
            }
        }

        //商品添加或更新
        if(product.getId()==null){
            int result=productMapper.insert(product);
            if(result>0){
                return ServerResponse.serverResponseBySuccess();
            }else {
                return ServerResponse.serverResponseByError("添加失败");
            }
        }else {
            int result=productMapper.updateByPrimaryKey(product);
            if(result>0){
                return ServerResponse.serverResponseBySuccess();
            }else {
                return ServerResponse.serverResponseByError("更新失败");
            }
        }
    }

    @Override
    public ServerResponse set_sale_status(Integer productId, Integer status) {
        if(productId==null){
            return ServerResponse.serverResponseByError("商品id不能为空");
        }if(status==null){
            return ServerResponse.serverResponseByError("状态不能为空");
        }
        //更新商品的状态
        Product product=new Product();
        product.setId(productId);
        product.setStatus(status);
        int result=productMapper.updateProductKeySelective(product);
        if(result>0){
            return ServerResponse.serverResponseBySuccess();
        }else {
            return ServerResponse.serverResponseByError("更新失败");
        }

    }

    @Override
    public ServerResponse detail(Integer productId) {
        //参数校验
        if(productId==null){
            return ServerResponse.serverResponseByError("商品id不能为空");
        }
        //查询product
        Product product=productMapper.selectByPrimaryKey(productId);
        if(product==null){
            return ServerResponse.serverResponseByError("商品不存在");
        }
        //product转换为productDetailVO
        ProductDetailVO productDetailVO=assembleProductDetailVo(product);
        //返回结果


        return ServerResponse.serverResponseBySuccess(productDetailVO);
    }

    @Override
    public ServerResponse list(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);

        //查询商品数据 select * from product limit (pageNum-1)*pageSize pageSize
        List<Product> productList=productMapper.selectAll();
        List<ProductListVO> productListVOS= Lists.newArrayList();
        if(productList!=null&&productList.size()>0){
            for(Product product:productList){
                ProductListVO productListVO=assembleProductListVO(product);
                productListVOS.add(productListVO);
            }
        }

        PageInfo pageInfo=new PageInfo(productListVOS);

        return ServerResponse.serverResponseBySuccess(pageInfo);
    }
    //根据id'或name查找
    @Override
    public ServerResponse search(Integer productId, String productName, Integer pageNum, Integer pageSize) {
       PageHelper.startPage(pageNum,pageSize);

       if(productName!=null&&!productName.equals("")){
           productName="%"+productName+"%";
       }

        List<Product> productList=productMapper.findProductByProductIdAndProductName(productId,productName);
        List<ProductListVO> productListVOS= Lists.newArrayList();
        if(productList!=null&&productList.size()>0){
            for(Product product:productList){
                ProductListVO productListVO=assembleProductListVO(product);
                productListVOS.add(productListVO);
            }
        }
        PageInfo pageInfo=new PageInfo(productListVOS);

        return ServerResponse.serverResponseBySuccess(pageInfo);
    }

    @Override
    public ServerResponse upload(MultipartFile file, String path) {
        if(file==null){
            return ServerResponse.serverResponseByError();
        }

        //step1:获取图片名称
        String  orignalFileName=  file.getOriginalFilename();
        //获取图片的扩展名
        String exName=  orignalFileName.substring(orignalFileName.lastIndexOf(".")); // .jpg
        //为图片生成新的唯一的名字
        String newFileName= UUID.randomUUID().toString()+exName;

        File pathFile=new File(path);
        if(!pathFile.exists()){
            pathFile.setWritable(true);
            pathFile.mkdirs();
        }

        File file1=new File(path,newFileName);

        try {
            file.transferTo(file1);
            //上传到图片服务器
            //.....
            Map<String,String> map= Maps.newHashMap();
            map.put("uri",newFileName);
            map.put("url",PropertiesUtils.readByKey("imageHost")+"/"+newFileName);
            return ServerResponse.serverResponseBySuccess(map);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }
    //前台接口-商品详情
    @Override
    public ServerResponse detail_portal(Integer productId) {
        //参数校验
        if(productId==null){
            return ServerResponse.serverResponseByError("商品id不能为空");
        }
        //查询product
        Product product=productMapper.selectByPrimaryKey(productId);
        if(product==null){
            return ServerResponse.serverResponseByError("商品为空");
        }
        //校验商品状态
        if(product.getStatus()!= Const.ProductStatusEnum.PRODUCT_ONLINE.getCode()){
            return ServerResponse.serverResponseByError("商品下架或删除");
        }
        //获取productDetailVO
        ProductDetailVO productDetailVO=assembleProductDetailVo(product);

        return ServerResponse.serverResponseBySuccess(productDetailVO);
    }

    @Override
    public ServerResponse list_portal(Integer categoryId, String keyword, Integer pageNum, Integer pageSize, String orderBy) {
        //参数校验，categoryId和keyword不能同时为空
        if(categoryId==null&&(keyword==null||keyword.equals(""))){
            return ServerResponse.serverResponseByError("参数错误");
        }
        //categoryId
        Set<Integer> integerSet= Sets.newHashSet();
        if(categoryId!=null){
            Category category=categoryMapper.selectByPrimaryKey(categoryId);
            if(category==null&&(keyword==null||keyword.equals(""))){
                //说明没有商品数据
                PageHelper.startPage(pageNum,pageSize);
                List<ProductListVO> productListVOList=Lists.newArrayList();
                PageInfo pageInfo=new PageInfo(productListVOList);
                return ServerResponse.serverResponseBySuccess(pageInfo);
            }
            ServerResponse categoryList=categoryService.get_deep_category(categoryId);
            if(categoryList.isSuccess()){
                integerSet=(Set<Integer>)categoryList.getData();
            }

        }
        //keyword
        if(keyword!=null&&!keyword.equals("")){
            keyword="%"+keyword+"%";
        }

        if(orderBy.equals("")){
            PageHelper.startPage(pageNum,pageSize);
        }else {
            String[] strings=orderBy.split("_");
            if(strings.length>1){
                PageHelper.startPage(pageNum,pageSize,strings[0]+" "+strings[1]);
            }else {
                PageHelper.startPage(pageNum,pageSize);
            }
        }
        //product-》productVO
        List<Product> productList=productMapper.searchProduct(integerSet,keyword);
        List<ProductListVO> productListVOList=Lists.newArrayList();
        if(productList!=null&&productList.size()>0){
            for(Product product:productList){
                ProductListVO productListVO=assembleProductListVO(product);
                productListVOList.add(productListVO);
            }
        }

        //分页
        PageInfo pageInfo=new PageInfo();
        pageInfo.setList(productListVOList);
        //返回
        return ServerResponse.serverResponseBySuccess(pageInfo);

    }

    private ProductListVO assembleProductListVO(Product product){
        ProductListVO productListVO=new ProductListVO();
        productListVO.setCategoryId(product.getCategoryId());
        productListVO.setId(product.getId());
        productListVO.setMainImage(product.getMainImage());
        productListVO.setName(product.getName());
        productListVO.setPrice(product.getPrice());
        productListVO.setStatus(product.getStatus());
        productListVO.setSubtitle(product.getSubtitle());
        return productListVO;
    }
    private ProductDetailVO assembleProductDetailVo(Product product){

        ProductDetailVO productDetailVO=new ProductDetailVO();
        productDetailVO.setCategoryId(product.getCategoryId());
        productDetailVO.setCreateTime(DateUtils.dateToStr(product.getCreateTime()));
        productDetailVO.setDetail(product.getDetail());
        productDetailVO.setImageHost(PropertiesUtils.readByKey("imageHost"));
        productDetailVO.setName(product.getName());
        productDetailVO.setMainImage(product.getMainImage());
        productDetailVO.setId(product.getId());
        productDetailVO.setPrice(product.getPrice());
        productDetailVO.setStatus(product.getStatus());
        productDetailVO.setStock(product.getStock());
        productDetailVO.setSubImages(product.getSubImages());
        productDetailVO.setSubtitle(product.getSubtitle());
        productDetailVO.setUpdateTime(DateUtils.dateToStr(product.getUpdateTime()));
        Category category=categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category!=null){
            productDetailVO.setParentCategoryId(category.getParentId());
        }else{
            //默认跟节点
            productDetailVO.setParentCategoryId(0);
        }
        return productDetailVO;
    }
}

package com.neuedu.service.impl;

import com.google.common.collect.Sets;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.CategoryMapper;
import com.neuedu.dao.UserInfoMapper;
import com.neuedu.pojo.Category;
import com.neuedu.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    CategoryMapper categoryMapper;
    @Override
    //获取品类子节点(平级)
    public ServerResponse get_category(Integer categoryId) {
        if(categoryId==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        if(category==null){
            return ServerResponse.serverResponseByError("类别不存在");
        }
        List<Category> list=categoryMapper.findChildCategory(categoryId);
        return ServerResponse.serverResponseBySuccess(list);
    }
    //增加节点
    @Override
    public ServerResponse add_category(Integer parentId, String categoryName) {
        if(categoryName==null||categoryName.equals("")){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        int result2=categoryMapper.findCategoryByName(categoryName);
        if(result2>0){
            return ServerResponse.serverResponseByError("该类别已添加");
        }
        Category category=new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(1);
        int result=categoryMapper.insert(category);
        if(result>0){
            return ServerResponse.serverResponseBySuccess("添加成功");
        }
        return ServerResponse.serverResponseByError("添加失败");
    }

    @Override
    /**
     * 修改节点
     * */
    public ServerResponse set_category_name(Integer categoryId, String categoryName) {
        if(categoryName==null||categoryName.equals("")){
            return ServerResponse.serverResponseByError("类名不能为空");
        }
        if(categoryId==null||categoryId.equals("")){
            return ServerResponse.serverResponseByError("ID不能为空");
        }
        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        if(category==null){
            return ServerResponse.serverResponseByError("要修改的类不存在");
        }
        category.setName(categoryName);
        int result=categoryMapper.updateByPrimaryKey(category);
        if(result>0){
            return ServerResponse.serverResponseBySuccess("修改成功");
        }
        return ServerResponse.serverResponseByError("修改失败");
    }

    //递归显示节点
    @Override
    public ServerResponse get_deep_category(Integer categoryId) {

        //step1:参数非空校验
        if(categoryId==null){
            return ServerResponse.serverResponseByError("类别id不能为空");
        }
        //step2:查询
        Set<Category> categorySet= Sets.newHashSet();
        categorySet= findAllChildCategory(categorySet,categoryId);

        Set<Integer> integerSet=Sets.newHashSet();

        Iterator<Category> categoryIterator=categorySet.iterator();
        while(categoryIterator.hasNext()){
            Category category=categoryIterator.next();
            integerSet.add(category.getId());
        }

        return ServerResponse.serverResponseBySuccess(integerSet);
    }

    private Set<Category> findAllChildCategory(Set<Category> categorySet,Integer categoryId){

        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        if(category!=null){
            categorySet.add(category);//id
        }
        //查找categoryId下的子节点(平级)
        List<Category> categoryList=categoryMapper.findChildCategory(categoryId);
        if(categoryList!=null&&categoryList.size()>0){
            for(Category category1:categoryList){
                findAllChildCategory(categorySet,category1.getId());
            }
        }
        return categorySet;
    }

}

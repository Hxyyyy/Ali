package com.neuedu.service.impl;

import com.neuedu.common.Const;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.UserInfoMapper;
import com.neuedu.pojo.UserInfo;
import com.neuedu.service.IUserService;
import com.neuedu.utils.MD5Utils;
import com.neuedu.utils.TokenCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class UserServiceImpl  implements IUserService{

    @Autowired
    UserInfoMapper userInfoMapper;

    @Override
    public ServerResponse login(String username, String password) {

        //参数的非空校验
        if(username==null||username.equals("")){
            return ServerResponse.serverResponseByError("用户名不能为空");
        }
        if(password==null||password.equals("")){
            return ServerResponse.serverResponseByError("密码不能为空");
        }
        //检查用户名是否存在
        int result= userInfoMapper.checkUsername(username);
        if(result==0){
            return  ServerResponse.serverResponseByError("用户名不存在");
        }
        //根据用户名和密码查找用户信息
        UserInfo userInfo= userInfoMapper.selectUserInfoByUsernameAndPassword(username, MD5Utils.getMD5Code(password));
        if(userInfo==null){
            return ServerResponse.serverResponseByError("密码错误");
        }
        //返回结果
        userInfo.setPassword("");
        return ServerResponse.serverResponseBySuccess(userInfo);
    }

    @Override
    public ServerResponse register(UserInfo userInfo) {

        //参数非空校验
        if(userInfo==null){
            return ServerResponse.serverResponseByError("参数必需");
        }

        //校验用户名
        int result= userInfoMapper.checkUsername(userInfo.getUsername());
        if(result>0){
            return  ServerResponse.serverResponseByError("用户名已存在");
        }
        //校验邮箱
        int result_email= userInfoMapper.checkEmail(userInfo.getEmail());
        if(result_email>0){//邮箱存在
            return  ServerResponse.serverResponseByError("邮箱已存在");
        }
        //注册
        userInfo.setRole(Const.RoleEnum.ROLE_CUSTOMER.getCode());
        userInfo.setPassword(MD5Utils.getMD5Code(userInfo.getPassword()));
        int count=userInfoMapper.insert(userInfo);
        if(count>0){
            return ServerResponse.serverResponseBySuccess("注册成功");
        }
        //返回结果

        return ServerResponse.serverResponseByError("注册失败");
    }

    @Override
    public ServerResponse forget_get_question(String username) {
        if (username==null||username.equals("")){
            return ServerResponse.serverResponseByError("用户名不能为空");
        }
        //校验username
        int result=userInfoMapper.checkUsername(username);
        if (result==0){
            return ServerResponse.serverResponseByError("用户名不存在");
        }
        //查找密保问题
        String question=userInfoMapper.selectQuestionByUsername(username);
        if(question==null||question.equals("")){
            return ServerResponse.serverResponseByError("密保问题为空");
        }
        return ServerResponse.serverResponseBySuccess(question);
    }

    @Override
    public ServerResponse forget_check_answer(String username, String question, String answer) {
        //参数校验
        if (username==null||username.equals("")){
            return ServerResponse.serverResponseByError("用户名不能为空");
        }
        if (question==null||question.equals("")){
            return ServerResponse.serverResponseByError("问题不能为空");
        }
        if (answer==null||answer.equals("")){
            return ServerResponse.serverResponseByError("答案不能为空");
        }
        //根据username，question，answer查询
        int result=userInfoMapper.selectByUsernameAndQuestionAndAnswer(username,question,answer);
        if (result==0){
            return ServerResponse.serverResponseByError("答案错误");
        }
        //服务端生成一个token保存并将token返回客户端
        String forgetToken= UUID.randomUUID().toString();
        //guava cache
        TokenCache.set(username,forgetToken);

        return ServerResponse.serverResponseBySuccess(forgetToken);
    }

    @Override
    public ServerResponse forget_check_password(String username, String passwordNew, String forToken) {

        if (username==null||username.equals("")){
            return ServerResponse.serverResponseByError("用户名不能为空");
        }
        if (passwordNew==null||passwordNew.equals("")){
            return ServerResponse.serverResponseByError("密码不能为空");
        }
        if (forToken==null||forToken.equals("")){
            return ServerResponse.serverResponseByError("token不能为空");
        }
        //token校验
        String token= TokenCache.get(username);
        if(token==null){
            return ServerResponse.serverResponseByError("token过期");
        }
        if(!token.equals(forToken)){
            return ServerResponse.serverResponseByError("无效的token");
        }
        //修改密码
        int result=userInfoMapper.updatePasswordNewByusername(username,MD5Utils.getMD5Code(passwordNew));
        if(result==0){
            return ServerResponse.serverResponseByError("密码修改失败");
        }
        return ServerResponse.serverResponseBySuccess("密码修改成功");
    }

    @Override
    public ServerResponse reset_password(String username,String passwordOld, String passwordNew) {
        if(username==null||username.equals("")){
            return ServerResponse.serverResponseByError("用户名不能为空");
        }
        if (passwordOld==null||passwordOld.equals("")){
            return ServerResponse.serverResponseByError("密码不能为空");
        }
        if (passwordNew==null||passwordNew.equals("")){
            return ServerResponse.serverResponseByError("新密码不能为空");
        }
        UserInfo userInfo=userInfoMapper.selectUserInfoByUsernameAndPassword(username,MD5Utils.getMD5Code(passwordOld));
        if(userInfo==null){
            return ServerResponse.serverResponseByError("密码错误");
        }
        userInfo.setPassword(MD5Utils.getMD5Code(passwordNew));
        int result=userInfoMapper.updateByPrimaryKey(userInfo);
        if(result==0){
            return ServerResponse.serverResponseByError("修改失败");
        }
        return ServerResponse.serverResponseBySuccess("修改成功");
    }
    /**
     * 登录状态下更新个人信息
     * */
    @Override
    public ServerResponse update_information(UserInfo user) {
        if(user==null){
            return ServerResponse.serverResponseByError("参数不能为空");
        }
        int result=userInfoMapper.updateUserBySelectActive(user);
        if(result>0){
            return ServerResponse.serverResponseBySuccess("成功");
        }
        return ServerResponse.serverResponseByError("更新失败");
    }

    @Override
    public UserInfo findUserInfoByUserid(Integer userId) {
        return userInfoMapper.selectByPrimaryKey(userId);
    }


}

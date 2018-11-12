package com.neuedu.controller.backend;

import com.neuedu.common.Const;
import com.neuedu.common.ServerResponse;
import com.neuedu.dao.UserInfoMapper;
import com.neuedu.pojo.UserInfo;
import com.neuedu.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "/manage/user")
public class UserManageController {
    @Autowired
    IUserService userService;
    @RequestMapping(value = "/login.do")
    public ServerResponse login(HttpSession session,String username, String password){
        ServerResponse serverResponse=userService.login(username,password);
        if(serverResponse.isSuccess()){
            UserInfo userInfo=(UserInfo) serverResponse.getData();
            if(userInfo.getRole()==1){
                return ServerResponse.serverResponseByError("没有权限");
            }
            session.setAttribute(Const.CURRENTUSER,userInfo);
        }
        return serverResponse;
    }
}

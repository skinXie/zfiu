package io.jpom.controller.user;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.RoleService;
import io.jpom.service.user.UserService;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Controller
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
public class UserEditController extends BaseServerController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

//    @RequestMapping(value = "edit", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        UserModel item = userService.getItem(id);
//        if (item != null) {
//            item.setPassword(null);
//            setAttribute("userItem", item);
//        }
//        List<RoleModel> list = roleService.list();
//        JSONArray roles = new JSONArray();
//        list.forEach(userModel -> {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("title", userModel.getName());
//            jsonObject.put("value", userModel.getId());
//            roles.add(jsonObject);
//        });
//        setAttribute("roles", roles);
//        return "user/edit";
//    }

    /**
     * ????????????
     *
     * @param id ?????????
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.AddUer)
    @Feature(method = MethodFeature.EDIT)
    @ResponseBody
    public String addUser(String id) {
        if (JpomApplication.SYSTEM_ID.equalsIgnoreCase(id)) {
            return JsonMessage.getString(400, "???????????????????????????????????????");
        }
        UserModel userName = getUser();
        //
        int size = userService.userSize();
        if (size >= ServerExtConfigBean.getInstance().userMaxCount) {
            return JsonMessage.getString(500, "????????????????????????????????????");
        }

        UserModel userModel = userService.getItem(id);
        if (userModel != null) {
            return JsonMessage.getString(401, "?????????????????????");
        }
        userModel = new UserModel();
        // ??????????????????????????????
        if (userName.isSystemUser()) {
            userModel.setParent(UserModel.SYSTEM_OCCUPY_NAME);
        } else {
            userModel.setParent(userName.getId());
        }
        String msg = parseUser(userModel, true);
        if (msg != null) {
            return msg;
        }
        userService.addItem(userModel);
        return JsonMessage.getString(200, "????????????");
    }

    private String parseUser(UserModel userModel, boolean create) {
        String id = getParameter("id");
        if (StrUtil.isEmpty(id) || id.length() < UserModel.USER_NAME_MIN_LEN) {
            return JsonMessage.getString(400, "?????????????????????,???????????????????????????" + UserModel.USER_NAME_MIN_LEN);
        }
        if (UserModel.SYSTEM_OCCUPY_NAME.equals(id) || UserModel.SYSTEM_ADMIN.equals(id)) {
            return JsonMessage.getString(401, "????????????????????????????????????");
        }
        if (!checkPathSafe(id)) {
            return JsonMessage.getString(400, "?????????????????????????????????");
        }
        userModel.setId(id);

        String name = getParameter("name");
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(405, "?????????????????????");
        }
        int len = name.length();
        if (len > 10 || len < 2) {
            return JsonMessage.getString(405, "?????????????????????2-10");
        }
        userModel.setName(name);

        UserModel userName = getUser();
        String password = getParameter("password");
        if (create || StrUtil.isNotEmpty(password)) {
            if (StrUtil.isEmpty(password)) {
                return JsonMessage.getString(400, "??????????????????");
            }
            // ????????????
            if (!create && !userName.isSystemUser()) {
                return JsonMessage.getString(401, "?????????????????????????????????????????????");
            }
            userModel.setPassword(password);
        }
        //
        String roles = getParameter("roles");
        JSONArray jsonArray = JSONArray.parseArray(roles);
        List<String> rolesList = jsonArray.toJavaList(String.class);
        if (rolesList == null || rolesList.isEmpty()) {
            return JsonMessage.getString(405, "?????????????????????");
        }
        userModel.setRoles(rolesList);
        return null;
    }

    /**
     * ????????????
     *
     * @param id ?????????
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.EditUer)
    @Feature(method = MethodFeature.EDIT)
    @ResponseBody
    public String updateUser(String id) {
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "????????????:-1");
        }
        // ?????????????????????????????????
        if (userModel.isSystemUser()) {
            return JsonMessage.getString(401, "WEB????????????????????????????????????");
        }
        UserModel me = getUser();
        if (userModel.getId().equals(me.getId())) {
            return JsonMessage.getString(401, "???????????????????????????");
        }
        // ????????????????????????????????????????????????
        if (!me.isSystemUser() && userModel.isDemoUser()) {
            return JsonMessage.getString(402, "?????????????????????????????????");
        }
        String msg = parseUser(userModel, false);
        if (msg != null) {
            return msg;
        }
        // ????????????????????????????????????????????????
        userModel.setModifyTime(DateUtil.currentSeconds());
        userService.updateItem(userModel);
        return JsonMessage.getString(200, "????????????");
    }
}

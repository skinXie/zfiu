package io.jpom.controller.onekey.host;

import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.jcraft.jsch.Session;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.BaseModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.PackageHostModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.zfpackage.host.HostService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping(value = "onekey/host")
@Feature(cls = ClassFeature.ONEKEY_HOST)
public class HostController extends BaseServerController {

	@Resource
	private HostService hostService;


	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<PackageHostModel>> listData() {
		List<PackageHostModel> list = hostService.list();
		if (list != null) {
			// 读取节点信息
			List<NodeModel> list1 = nodeService.list();
			Map<String, NodeModel> map = new HashMap<>(10);
			list1.forEach(nodeModel -> {
				String sshId = nodeModel.getSshId();
				if (StrUtil.isNotEmpty(sshId)) {
					map.put(sshId, nodeModel);
				}
			});
			list.forEach(PackageHostModel -> {
				// 不返回密码
				PackageHostModel.setPassword(null);
//				PackageHostModel.setPrivateKey(null);
				// 节点信息
				BaseModel nodeModel = map.get(PackageHostModel.getId());
				PackageHostModel.setNodeModel(nodeModel);
			});
		}
		return new JsonMessage<>(200, "", list);
	}

	/**
	 * @param name
	 * @param host
	 * @param user
	 * @param password
	 * @param connectType
	 * @param privateKey
	 * @param port
	 * @param charset
	 * @param fileDirs
	 * @param id
	 * @param type        {'add': 新增, 'edit': 修改}
	 * @return
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@OptLog(UserOperateLogV1.OptType.EditSsh)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "主机名称不能为空") String name,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
					   String password,
					   PackageHostModel.ConnectType connectType,
					   String privateKey,
					   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
					   String charset, String fileDirs,
					   String id, String type, String notAllowedCommand) {
		// 优先判断参数 如果是 password 在修改时可以不填写
		if (connectType == PackageHostModel.ConnectType.PASS && StrUtil.isEmpty(password) && "add".equals(type)) {
			return JsonMessage.getString(405, "请填写登录密码");
		}
		if (connectType == PackageHostModel.ConnectType.PUBKEY && StrUtil.isEmpty(privateKey)) {
			return JsonMessage.getString(405, "请填写证书内容");
		}
		PackageHostModel packageHostModel;
		if ("edit".equals(type)) {
			packageHostModel = hostService.getItem(id);
			if (packageHostModel == null) {
				return JsonMessage.getString(500, "不存在对应主机");
			}
		} else {
			packageHostModel = new PackageHostModel();
		}
		// 目录
		if (StrUtil.isEmpty(fileDirs)) {
			packageHostModel.setFileDirs(null);
		} else {
			List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
			packageHostModel.setFileDirs(list);
		}
		packageHostModel.setHost(host);
		// 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
		if (!StrUtil.isEmpty(password)) {
			packageHostModel.setPassword(password);
		}
		packageHostModel.setPort(port);
		packageHostModel.setUser(user);
		packageHostModel.setName(name);
		packageHostModel.setNotAllowedCommand(notAllowedCommand);
		packageHostModel.setConnectType(connectType);
		packageHostModel.setPrivateKey(privateKey);
		try {
			Charset.forName(charset);
			packageHostModel.setCharset(charset);
		} catch (Exception e) {
			return JsonMessage.getString(405, "请填写正确的编码格式");
		}
		try {
			Session session = HostService.getSession(packageHostModel);
			JschUtil.close(session);
		} catch (Exception e) {
			return JsonMessage.getString(505, "主机连接失败：" + e.getMessage());
		}
		if ("add".equalsIgnoreCase(type)) {
			hostService.addItem(packageHostModel);
		} else {
			hostService.updateItem(packageHostModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}

//    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        if (StrUtil.isNotEmpty(id)) {
//            PackageHostModel PackageHostModel = sshService.getItem(id);
//            if (PackageHostModel != null) {
//                setAttribute("item", PackageHostModel);
//                //
//                String fileDirs = AgentWhitelist.convertToLine(PackageHostModel.getFileDirs());
//                setAttribute("fileDirs", fileDirs);
//            }
//        }
//        Collection<Charset> charsets = Charset.availableCharsets().values();
//        Collection<Charset> collect = charsets.stream().filter(charset -> !StrUtil.startWithAny(charset.name(), "x", "w", "IBM")).collect(Collectors.toList());
//        setAttribute("charsets", collect);
//        //
//        PackageHostModel.ConnectType[] values = PackageHostModel.ConnectType.values();
//        setAttribute("connectTypes", values);
//        return "node/ssh/edit";
//    }

//    @RequestMapping(value = "terminal.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.TERMINAL)
//    public String terminal(String id) {
//        PackageHostModel PackageHostModel = sshService.getItem(id);
//        setAttribute("item", PackageHostModel);
//        return "node/ssh/terminal";
//    }

	/**
	 * 根据 nodeId 查询 SSH 列表
	 *
	 * @param nodeId
	 * @return
	 * @description for dev 3.x
	 * @author Hotstrip
	 */

	@RequestMapping(value = "list_by_node_id", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String listByNodeId(String nodeId) {
		JSONArray sshList = hostService.listSelect(nodeId);
		return JsonMessage.getString(200, "success", sshList);
	}
}

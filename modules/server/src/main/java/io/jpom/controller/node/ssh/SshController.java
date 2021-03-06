package io.jpom.controller.node.ssh;

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
import io.jpom.model.data.PackageModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.zfpackage.install.PackageService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Controller
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshController extends BaseServerController {

	@Resource
	private SshService sshService;

	@Resource
	private PackageService packageService;

//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        return "node/ssh/list";
//    }

	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<SshModel>> listData() {
		List<SshModel> list = sshService.list();
		if (list != null) {
			// ??????????????????
			List<NodeModel> list1 = nodeService.list();
			Map<String, NodeModel> map = new HashMap<>(10);
			list1.forEach(nodeModel -> {
				String sshId = nodeModel.getSshId();
				if (StrUtil.isNotEmpty(sshId)) {
					map.put(sshId, nodeModel);
				}
			});
			list.forEach(sshModel -> {
				// ???????????????
				sshModel.setPassword(null);
//				sshModel.setPrivateKey(null);
				// ????????????
				BaseModel nodeModel = map.get(sshModel.getId());
				sshModel.setNodeModel(nodeModel);
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
	 * @param type        {'add': ??????, 'edit': ??????}
	 * @return
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@OptLog(UserOperateLogV1.OptType.EditSsh)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "????????????????????????") String name,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host????????????") String host,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user????????????") String user,
					   String password,
					   SshModel.ConnectType connectType,
					   String privateKey,
					   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port??????") int port,
					   String charset, String fileDirs,
					   String id, String type, String notAllowedCommand) {
		// ?????????????????? ????????? password ???????????????????????????
		if (connectType == SshModel.ConnectType.PASS && StrUtil.isEmpty(password) && "add".equals(type)) {
			return JsonMessage.getString(405, "?????????????????????");
		}
		if (connectType == SshModel.ConnectType.PUBKEY && StrUtil.isEmpty(privateKey)) {
			return JsonMessage.getString(405, "?????????????????????");
		}
		SshModel sshModel;
		if ("edit".equals(type)) {
			sshModel = sshService.getItem(id);
			if (sshModel == null) {
				return JsonMessage.getString(500, "???????????????ssh");
			}
		} else {
			sshModel = new SshModel();
		}
		// ??????
		if (StrUtil.isEmpty(fileDirs)) {
			sshModel.setFileDirs(null);
		} else {
			List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
			sshModel.setFileDirs(list);
		}
		sshModel.setHost(host);
		// ??????????????????????????????????????? ??????????????????????????????????????????????????? password ???????????????
		if (!StrUtil.isEmpty(password)) {
			sshModel.setPassword(password);
		}
		sshModel.setPort(port);
		sshModel.setUser(user);
		sshModel.setName(name);
		sshModel.setNotAllowedCommand(notAllowedCommand);
		sshModel.setConnectType(connectType);
		sshModel.setPrivateKey(privateKey);
		try {
			Charset.forName(charset);
			sshModel.setCharset(charset);
		} catch (Exception e) {
			return JsonMessage.getString(405, "??????????????????????????????");
		}
		try {
			Session session = SshService.getSession(sshModel);
			JschUtil.close(session);
		} catch (Exception e) {
			return JsonMessage.getString(505, "ssh???????????????" + e.getMessage());
		}
		if ("add".equalsIgnoreCase(type)) {
			sshService.addItem(sshModel);
		} else {
			sshService.updateItem(sshModel);
		}
		return JsonMessage.getString(200, "????????????");
	}

	/**
	 * ?????? nodeId ?????? SSH ??????
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
		JSONArray sshList = sshService.listSelect(nodeId);
		return JsonMessage.getString(200, "success", sshList);
	}

	/**
	 * ?????? nodeId ?????? SSH ??????
	 *
	 * @param
	 * @return
	 * @description for dev 3.x
	 * @author Hotstrip
	 */
	@RequestMapping(value = "bind.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String bind(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "??????ID????????????")String id ,
					   List<String> packages) {

		try{
			List<PackageModel> packageModelList = new ArrayList<>();
			SshModel sshModel = sshService.getItem(id);
			packages.stream().forEach(t->{
				final PackageModel item = packageService.getItem(t);
				packageModelList.add(item);
			});
			sshModel.setPackages(packageModelList);
			sshService.updateItem(sshModel);
		}catch (Exception e){
			return JsonMessage.getString(500, "????????????,???????????????");
		}
		return JsonMessage.getString(200, "????????????");
	}
}

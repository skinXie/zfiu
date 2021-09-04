package io.jpom.controller.outgiving;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.jcraft.jsch.Session;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.*;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.ServerWhitelistServer;
import io.jpom.service.zfpackage.install.PackageService;
import io.jpom.service.zfpackage.script.PackageScriptService;
import io.jpom.util.Constant;
import io.jpom.util.SSHUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingWhitelistController extends BaseServerController {
	@Resource
	private ServerWhitelistServer serverWhitelistServer;

	@Resource
	private SshService sshService;

	@Resource
	private PackageScriptService packageScriptService;

	@Resource
	private PackageService packageService;

	@Resource
	private OutGivingServer outGivingServer;


	/**
	 * @return
	 * @author Hotstrip
	 * get whiteList data
	 * 白名单数据接口
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@SystemPermission
	@ResponseBody
	public String whiteList() {
		Map<String, String> map = new HashMap<>();
		UserModel userModel = getUser();
		ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
		if (serverWhitelist != null && userModel.isSystemUser()) {
			List<String> whiteList = serverWhitelist.getOutGiving();
			String strWhiteList = AgentWhitelist.convertToLine(whiteList);
			map.put("whiteList", strWhiteList);
		}
		return JsonMessage.getString(200, "ok", map);
	}

	/**
	 * 保存节点白名单
	 *
	 * @param data 数据
	 * @return json
	 */
	@RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.SaveOutgivingWhitelist)
	@SystemPermission
	public String whitelistDirectorySubmit(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "分发路径不能为空") String data) {
//        List<String> list = StrSplitter.splitTrim(data, StrUtil.LF, true);
//        if (list == null || list.size() <= 0) {
//            return JsonMessage.getString(401, "白名单不能为空");
//        }
//		list = AgentWhitelist.covertToArray(list);
//		if (list == null) {
//			return JsonMessage.getString(401, "项目路径白名单不能位于Jpom目录下");
//		}
//		if (list.isEmpty()) {
//			return JsonMessage.getString(401, "项目路径白名单不能为空");
//		}
		if (!data.trim().startsWith("/")) {
			return JsonMessage.getString(401, "分发路径请配置绝对路径, 以/开头");
		}

		List<String> list = Arrays.asList(data);
		ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
		if (serverWhitelist == null) {
			serverWhitelist = new ServerWhitelist();
		}
		serverWhitelist.setOutGiving(list);
		serverWhitelistServer.saveWhitelistDirectory(serverWhitelist);

		String resultData = AgentWhitelist.convertToLine(list);
		return JsonMessage.getString(200, "保存成功", resultData);
	}


	/**
	 * @param id
	 * @param packageName
	 * @param scriptName
	 * @param sshName
	 * @return
	 */
	@RequestMapping(value = "install_app", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.Install_APP)
	@SystemPermission
	public String installApp(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "分发id不能为空") String id,
							 String packageId,
							 @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "应用不能为空") String packageName,
							 String scriptId,
							 String scriptName,
							 String sshId,
							 @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "目标主机不能为空") String sshName) {

		String result = "" ;

		// 1. ssh连通性校验
		try {
			Session session = SshService.getSession(sshService.getItem(sshId));
			JschUtil.close(session);
		} catch (Exception e) {
			return JsonMessage.getString(505, "主机连接失败：" + e.getMessage());
		}

		// 2. 分发路径
		String remoteFilePath = "/";
		ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
		if (serverWhitelist !=null && !CollectionUtils.isEmpty(serverWhitelist.getOutGiving())){
			remoteFilePath = serverWhitelist.getOutGiving().get(0);
		}

		// 3. 连接ssh ,执行script
		SshModel sshModel = sshService.getItem(sshId);
		SSHUtil sshUtil = new SSHUtil(sshModel.getHost(),sshModel.getUser(),sshModel.getPassword(),sshModel.getPort());
		try {
			sshUtil.putFile(Constant.PACKAGE_ADDRESS, packageService.getItem(packageId).getPackageFile(),remoteFilePath);
			PackageScriptModel scriptModel = packageScriptService.getItem(scriptId);
			result = executeCommand(sshUtil, scriptModel);
		} catch (Exception e) {
			return JsonMessage.getString(505, "文件分发异常：" + e.getMessage());
		}
		return JsonMessage.getString(200, "部署完成 " + result);
	}

	/**
	 * 执行脚本
	 * @param sshUtil
	 * @param scriptModel
	 * @return
	 * @throws Exception
	 */
	private String executeCommand(SSHUtil sshUtil, PackageScriptModel scriptModel) throws Exception {
		String result = "没有需要执行的脚本";
		if (scriptModel != null && !StrUtil.isBlank(scriptModel.getScriptContext())){
			result = sshUtil.runCommand(scriptModel.getScriptContext());
		}
		return result;
	}
}

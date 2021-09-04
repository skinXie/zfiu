package io.jpom.controller.onekey.script;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.PackageModel;
import io.jpom.model.data.PackageScriptModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.zfpackage.install.PackageService;
import io.jpom.service.zfpackage.script.PackageScriptService;
import io.jpom.util.Constant;
import io.jpom.util.MultipartFileUtil;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一键安装脚本管理
 *
 * @author artisan
 * @date 2021/08/14
 */
@Controller
@RequestMapping(value = "onekey/script")
@Feature(cls = ClassFeature.ONEKEY_SCRIPT)
public class PackageScriptController extends BaseServerController {

	@Resource
	private PackageScriptService scriptService;

	@Resource
	private PackageService packageService;

	/**
	 * 获取脚本列表
	 *
	 * @return
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public JsonMessage<List<PackageScriptModel>> scriptList() {
		return new JsonMessage<>(200, "", scriptService.list());
	}


	/**
	 * 保存脚本
	 *
	 * @return json
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.Save_PackageScript)
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "脚本名称不能为空") String scriptName,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "脚本内容不能为空") String scriptContext,
					   String id,
					   String type) {
		PackageScriptModel packageScriptModel;

		// 新增 or 编辑
		if ("add".equals(type)) {
			packageScriptModel = new PackageScriptModel();
			packageScriptModel.setId(IdUtil.simpleUUID());
		} else {
			packageScriptModel = scriptService.getItem(id);
			if (packageScriptModel == null) {
				return JsonMessage.getString(500, "未找到对应脚本");
			}
		}

		/**
		 * 构建PackageScriptModel
		 */
		packageScriptModel.setScriptName(scriptName);
		packageScriptModel.setScriptContext(scriptContext);
		packageScriptModel.setModifyTime(DateUtil.now());
		// 新增 or 编辑
		if ("add".equalsIgnoreCase(type)) {
			scriptService.addItem(packageScriptModel);
		} else {
			scriptService.updateItem(packageScriptModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}


	/**
	 * 删除脚本
	 *
	 * @return
	 */
	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.Del_PackageScript)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {

		PackageScriptModel item = scriptService.getItem(id);
		if (item == null) {
			return JsonMessage.getString(500, "找不到对应的记录");
		}
		// 刪除記錄
		scriptService.deleteItem(id);
		// 删除磁盘文件
		FileUtil.del(Constant.SCRIPT_ADDRESS + item.getScriptName());
		return JsonMessage.getString(200, "删除成功");
	}

	/**
	 * 导入脚本
	 *
	 * @return json
	 */
	@SneakyThrows
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.Upload_PackageScript)
	@Feature(method = MethodFeature.UPLOAD)
	public String upload() {
		PackageScriptModel packageScriptModel = new PackageScriptModel();
		Map<String, MultipartFile> fileMap = getMultiRequest().getFileMap();

		AtomicBoolean validate = new AtomicBoolean(true);
		fileMap.forEach((s, multipartFile) -> {
			try {
				String fileName = multipartFile.getOriginalFilename();
				if (fileName.endsWith(".sh") || fileName.endsWith(".bat")) {
					File dest = new File(Constant.SCRIPT_ADDRESS + fileName);
					if (!dest.getParentFile().exists()) {
						dest.getParentFile().mkdirs();
					}
					// 写入数据文件
					saveScript(packageScriptModel, multipartFile);
					// 转存文件
					multipartFile.transferTo(dest);
				} else {
					validate.set(false);
					DefaultSystemLog.getLog().error("不支持的脚本格式,仅支持sh|bat,当前文件名:" + fileName);
				}
			} catch (IOException e) {
				DefaultSystemLog.getLog().error("上传脚本异常", e);
			}
		});
		return validate.get() ? JsonMessage.getString(200, "上传成功") : JsonMessage.getString(400, "不支持的脚本格式,仅支持sh|bat");
	}

	/**
	 * 写入数据文件 ServerConfigBean.PKG_SCRIPT_LIST
	 *
	 * @param packageScriptModel
	 * @param multipartFile
	 */
	private void saveScript(PackageScriptModel packageScriptModel, MultipartFile multipartFile) {
		packageScriptModel.setId(multipartFile.getOriginalFilename());
		packageScriptModel.setScriptName(multipartFile.getOriginalFilename());
		packageScriptModel.setScriptContext(MultipartFileUtil.convertStreamToString(multipartFile));
		packageScriptModel.setModifyTime(DateUtil.now());
		scriptService.addItem(packageScriptModel);
	}

	/**
	 * 删除脚本
	 *
	 * @return
	 */
	@RequestMapping(value = "bind.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.Del_PackageScript)
	@Feature(method = MethodFeature.DEL)
	public String bind(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String scriptId,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK) String packageId) {

		PackageScriptModel packageScriptModel = scriptService.getItem(scriptId);
		PackageModel packageModel = packageService.getItem(packageId);
		if (packageScriptModel == null || packageModel == null) {
			return JsonMessage.getString(500, "关联异常,请退出重试");
		}

		packageScriptModel.setPackageModel(packageModel);

		scriptService.updateItem(packageScriptModel);

		return JsonMessage.getString(200, "关联成功");
	}

}

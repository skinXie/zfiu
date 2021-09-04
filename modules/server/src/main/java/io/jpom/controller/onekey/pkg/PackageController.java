package io.jpom.controller.onekey.pkg;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.PackageModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.zfpackage.install.PackageService;
import io.jpom.util.Constant;
import io.jpom.util.Md5CaculateUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * 安装包 管理
 *
 * @author artisan
 * @date 2021/08/14
 */
@Controller()
@RequestMapping("onekey/pkg")
@Feature(cls = ClassFeature.ONEKEY_PACAKGE)
public class PackageController extends BaseServerController {

	@Resource
	private PackageService packageService;


	/**
	 * 文件上传
	 *
	 * @param file
	 * @return
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.UPLOAD)
	public String upload(@RequestParam("file") MultipartFile file) {
		// 文件原始名称
		String fileName = file.getOriginalFilename();
		try {
			//  当前项目路径的地址 +  install-packages 为 安装包的存放地址
			File dest = new File(Constant.PACKAGE_ADDRESS + fileName);
			if (!dest.getParentFile().exists()) {
				dest.getParentFile().mkdirs();
			}
			// 转存文件
			file.transferTo(dest);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("安装包上传异常", e);
			return JsonMessage.getString(400, "上传失败");
		}
		return JsonMessage.getString(200, "上传成功");
	}


	/**
	 * 查询安装包信息
	 *
	 * @return
	 */
	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<PackageModel>> listData() {
		List<PackageModel> list = packageService.list();
		return new JsonMessage<>(200, "", list);
	}


	/**
	 * @param packageName           安装包名
	 * @param id             安装包自动生成的id
	 * @param packageFile       上传的安装包名
	 * @param packageVersion 安装包的版本
	 * @param type           {'add': 新增, 'edit': 修改}
	 * @return
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@OptLog(UserOperateLogV1.OptType.EditPackage)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "安装包名称不能为空") String packageName,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "安装包不能为空") String packageFile,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "版本号不能为空") String packageVersion,
					   String id,
					   String type) {

		PackageModel packageModel;

		// 新增 or 编辑
		if ("edit".equals(type)) {
			packageModel = packageService.getItem(id);
			if (packageModel == null) {
				return JsonMessage.getString(500, "不存在对应安装包");
			}
		} else {
			packageModel = new PackageModel();
			packageModel.setId(IdUtil.simpleUUID());
		}

		/**
		 * 构建PackageModel
		 */
		packageModel.setPackageName(packageName);
		packageModel.setPackageFile(packageFile);
		packageModel.setPackageVersion(packageVersion);
		packageModel.setModifyTime(DateUtil.now());
		// 设置MD5
		File targetFile = FileUtil.file(Constant.PACKAGE_ADDRESS + packageFile);
		packageModel.setPackageMd5(Md5CaculateUtil.getMD5(targetFile).toUpperCase());
		//文件大小
		packageModel.setPackageSize(FileUtil.readableFileSize(targetFile));


		// 新增 or 编辑
		if ("add".equalsIgnoreCase(type)) {
			packageService.addItem(packageModel);
		} else {
			packageService.updateItem(packageModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}


	@RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {

		// 删除磁盘上的文件
		PackageModel packageModel = packageService.getItem(id);
		if (packageModel == null) {
			return JsonMessage.getString(500, "找不到对应的记录");
		}
		// 删除数据文件中的记录
		packageService.deleteItem(id);
		// 删除文件
		FileUtil.del( Constant.PACKAGE_ADDRESS  + packageModel.getPackageFile());

		return JsonMessage.getString(200, "删除成功");
	}

	@RequestMapping(value = "download.html", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DOWNLOAD)
	public void downLoadPackage( @ValidatorItem(value = ValidatorRule.NOT_BLANK)  String id) {
		HttpServletResponse response = getResponse();

		PackageModel packageModel = packageService.getItem(id);
		if (packageModel.getPackageFile() == null ) {
			ServletUtil.write(response, "服务器上没有该文件", MediaType.TEXT_HTML_VALUE);
			return;
		}
		File file = FileUtil.file(Constant.PACKAGE_ADDRESS + packageModel.getPackageFile());
		if (file.isFile()) {
			ServletUtil.write(response, file);
		}
	}
}

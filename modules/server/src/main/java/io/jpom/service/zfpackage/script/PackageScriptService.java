package io.jpom.service.zfpackage.script;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.PackageModel;
import io.jpom.model.data.PackageScriptModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 安装脚本Service
 *
 * @author artisan
 */
@Service
public class PackageScriptService extends BaseOperService<PackageScriptModel>  implements BaseDynamicService {


	public PackageScriptService() {
		super(ServerConfigBean.PKG_SCRIPT_LIST);
	}

	@Override
	public void addItem(PackageScriptModel packageScriptModel) {
		super.addItem(packageScriptModel);
	}

	@Override
	public void deleteItem(String id) {
		super.deleteItem(id);
	}


	@Override
	public void updateItem(PackageScriptModel packageScriptModel) {
		packageScriptModel.setModifyTime(DateUtil.now());
		super.updateItem(packageScriptModel);
	}

	@Override
	public JSONArray listToArray(String dataId) {
		return (JSONArray) JSONArray.toJSON(this.list());
	}

	@Override
	public List<PackageScriptModel> list() {
		return (List<PackageScriptModel>) filter(super.list(), ClassFeature.ONEKEY_PACAKGE);
	}


	public PackageScriptModel getPackageScript(PackageModel packageModel){
		PackageScriptModel targetScript = new PackageScriptModel();

		List<PackageScriptModel> packageScriptModelList = list();

		List<PackageScriptModel>  scriptListWithPackages = packageScriptModelList.stream().filter(t -> t.getPackageModel() != null)
				.collect(Collectors.toList());

		for (PackageScriptModel packageScriptModel : scriptListWithPackages) {
			if (packageScriptModel.getPackageModel().getId().equals(packageModel.getId())){
				targetScript = packageScriptModel;
				break;
			}
		}
		return targetScript;
	}
}

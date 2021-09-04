package io.jpom.service.zfpackage.install;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.PackageModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 安装包Service
 *
 * @author artisan
 */
@Service
public class PackageService extends BaseOperService<PackageModel>  implements BaseDynamicService {

    public PackageService() {
        super(ServerConfigBean.PKG_LIST);
    }

    @Override
    public void addItem(PackageModel packageModel) {
        super.addItem(packageModel);
    }

    @Override
    public void deleteItem(String id) {
        super.deleteItem(id);
    }


    @Override
    public void updateItem(PackageModel packageModel) {
		packageModel.setModifyTime(DateUtil.now());
        super.updateItem(packageModel);
    }

	@Override
	public JSONArray listToArray(String dataId) {
		return (JSONArray) JSONArray.toJSON(this.list());
	}

	@Override
	public List<PackageModel> list() {
		return (List<PackageModel>) filter(super.list(), ClassFeature.ONEKEY_PACAKGE);
	}


}

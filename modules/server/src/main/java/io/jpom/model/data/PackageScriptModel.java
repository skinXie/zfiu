package io.jpom.model.data;

import io.jpom.model.BaseModel;
import lombok.Data;

import java.util.List;

/**
 * 脚本 信息
 *
 * @author artisan
 * @date 2021/08/16
 */
@Data
public class PackageScriptModel extends BaseModel {


	/**
	 * 最后执行人员
	 */
	private String scriptName;

    /**
     * 最后修改时间
     */
    private String modifyTime;
    /**
     * 脚本内容
     */
    private String scriptContext;


	/**
	 * 绑定安装包
	 */
	private PackageModel packageModel;


	/**
	 * 绑定脚本
	 */
	private List<PackageModel> PackageModelList;
}

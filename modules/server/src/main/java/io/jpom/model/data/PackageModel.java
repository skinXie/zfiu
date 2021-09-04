package io.jpom.model.data;

import io.jpom.model.BaseModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * package 信息
 *
 * @author artisan
 * @date 2021/08/16
 */

@Data
@NoArgsConstructor
public class PackageModel extends BaseModel {

	/**
	 * 安装包名称
	 */
	private String packageName;

	/**
	 * 安装包对应的文件名称
	 */
	private String packageFile;

	/**
	 * 安装包版本
	 */
	private String packageVersion;


	/**
	 * 安装包MD5
	 */
	private String packageMd5;


	/**
	 * 安装包大小
	 */
	private String packageSize;


	/**
	 * 安裝包修改时间
	 */
	private String modifyTime;


}

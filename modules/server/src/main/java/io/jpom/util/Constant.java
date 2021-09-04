package io.jpom.util;

import java.io.File;

/**
 * @author 小工匠
 * @version 1.0
 * @description: 公共
 * @date 2021/8/17 19:06
 * @mark: show me the code , change the world
 */
public class Constant {

	/**
	 * 应用根目录
	 */
	public static final String PROJ_ROOT = System.getProperty("user.dir");


	/**
	 * 默认安装包的文件存放目录
	 */
	public static final String PACKAGE_ADDRESS = PROJ_ROOT + File.separator + "install-packages" + File.separator;;

	/**
	 * 默认的脚本文件存放目录
	 */
	public static final String SCRIPT_ADDRESS  = PROJ_ROOT + File.separator + "install-scripts" + File.separator;




}

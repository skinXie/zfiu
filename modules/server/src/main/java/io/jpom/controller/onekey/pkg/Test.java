package io.jpom.controller.onekey.pkg;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;

import java.util.Arrays;

/**
 * @author 小工匠
 * @version 1.0
 * @description: TODO
 * @date 2021/8/17 18:40
 * @mark: show me the code , change the world
 */
public class Test {

	public static void main(String[] args) {
		String string = "张三,李四,王五,马六,小气";
		String substring = string.substring(0, string.length() );
		System.out.println(substring);
		String[] split = substring.split(",");//以逗号分割

		for (String string2 : split) {
			System.out.println("数据-->>>" + string2);
		}



	}
}

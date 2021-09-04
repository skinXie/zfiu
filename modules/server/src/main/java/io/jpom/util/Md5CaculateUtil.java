package io.jpom.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import cn.jiangzeyin.common.DefaultSystemLog;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author 小工匠
 * @version 1.0
 * @description: MD5
 * @date 2021/8/17 18:55
 * @mark: show me the code , change the world
 */
public class Md5CaculateUtil {

	/**
	 * 获取一个文件的md5值(可处理大文件)
	 * @return md5 value
	 */
	public static String getMD5(File file) {
		FileInputStream fileInputStream = null;
		try {
			MessageDigest MD5 = MessageDigest.getInstance("MD5");
			fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int length;
			while ((length = fileInputStream.read(buffer)) != -1) {
				MD5.update(buffer, 0, length);
			}
			return new String(Hex.encodeHex(MD5.digest()));
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("getMD5", e);
			return null;
		} finally {
			try {
				if (fileInputStream != null){
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 求一个字符串的md5值
	 * @param target 字符串
	 * @return md5 value
	 */
	public static String MD5(String target) {
		return DigestUtils.md5Hex(target);
	}

	public static void main(String[] args) {
		long beginTime = System.currentTimeMillis();
		File file = new File("D:\\IdeaProjects\\jianhangQhVersion_2.0.rar");
		String md5 = getMD5(file);
		long endTime = System.currentTimeMillis();
		System.out.println("MD5:" + md5 + "\n 耗时:" + ((endTime - beginTime) / 1000) + "s");
	}

}

package io.jpom.util;

import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 小工匠
 * @version 1.0
 * @description: MD5
 * @date 2021/8/17 18:55
 * @mark: show me the code , change the world
 */
public class MultipartFileUtil {

	/**
	 * 获取MultipartFile中的内容
	 *
	 * @param file
	 * @return
	 */
	@SneakyThrows
	public static String convertStreamToString(MultipartFile file) {
		StringBuilder sb;
		try (InputStream inputStream = file.getInputStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			sb = new StringBuilder();

			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}


}

package cn.t.rpc.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class KeyGeneratorUtil {
	private static final String FORMAT = "yyyyMMddhhmmssSSS";
	
	private static final Random RANDOM = new Random();

	/**
	 * 私有化构造方法：工具类不需要实例.
	 */
	private KeyGeneratorUtil() throws IllegalAccessException {
		throw new IllegalAccessException("UTIL CLASS");
	}

	/**
	 * 根据时间(毫秒级)生成key
	 * @return 根据时间(毫秒级)生成的数据库主键
	 */
	public static String generateKey() {
		// 生成时间数据
		String timeId = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(FORMAT));
		return timeId + String.valueOf(RANDOM.nextLong());
	}
}

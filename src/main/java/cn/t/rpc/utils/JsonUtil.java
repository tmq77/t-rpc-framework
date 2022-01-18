package cn.t.rpc.utils;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

	private JsonUtil() throws IllegalAccessException {
		throw new IllegalAccessException("UTIL CLASS");
	}

	private static final ObjectMapper objectMapper = initObjectMapper();

	private static ObjectMapper initObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// 在反序列化时忽略在 json 中存在但 Java 对象不存在的属性
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// null对象仍可以序列化
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		// 在序列化时忽略值为 null 的属性
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper;
	}

	/**
	 * 将数据转化成JSON
	 * @param value 待转换对象
	 * @return 转换后的字符串
	 * @throws JsonProcessingException 异常
	 */
	public static  String convertToString(Object value) throws JsonProcessingException {
		return objectMapper.writeValueAsString(value);
	}

	/**
	 * 将数据转化成Bytes
	 * @param value 待转换对象
	 * @return 转换后的byte数组
	 * @throws JsonProcessingException 异常
	 */
	public static byte[] convertToBytes(Object value) throws JsonProcessingException {
		return objectMapper.writeValueAsBytes(value);
	}

	/**
	 * 将json字符串转化为对象
	 * @param value 待转换JSON字符串
	 * @param valueType 转换后的类型
	 * @param <T> 转换后的类型
	 * @return 转换后的对象
	 * @throws JsonProcessingException 异常
	 */
	public static <T> T convertToObject(String value, Class<T> valueType) throws JsonProcessingException {
		return objectMapper.readValue(value, valueType);
	}
	
	/**
	 * 将字节数组转化为对象
	 * @param value 待转换字节数组
	 * @param valueType 转换后的类型
	 * @param <T> 转换后的类型
	 * @return 转换后的对象
	 * @throws IOException 
	 */
	public static <T> T convertToObject(byte[] value, Class<T> valueType) throws IOException {
		return objectMapper.readValue(value, valueType);
	}
}

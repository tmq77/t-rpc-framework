package cn.t.rpc.core.codec.strategy;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import cn.t.rpc.utils.JsonUtil;

/**
 * 序列化方式选择器
 * @author TMQ
 *
 */
public class SerializationByJson implements SerializationStrategy {
	
	/**
	 * 序列化
	 * @param <T> 类型
	 * @param obj 对象
	 * @return 字节数组
	 * @throws JsonProcessingException
	 */
	public <T> byte[] serialize(T obj) throws JsonProcessingException {
		return JsonUtil.convertToBytes(obj);
	}
	
	/**
	 * 反序列化
	 * @param <T> 类型
	 * @param data 字节数组
	 * @param clazz 类型
	 * @return 对象
	 * @throws IOException
	 */
	public <T> T deserialize(byte[] data ,Class<? extends T> clazz) throws IOException {
		return JsonUtil.convertToObject(data, clazz);
	}
}

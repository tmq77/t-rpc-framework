package cn.t.rpc.core.codec.strategy;

public interface SerializationStrategy {
	
	/**
	 * 序列化
	 * @param <T> 类型
	 * @param obj 对象
	 * @return 字节数据
	 * @throws Exception
	 */
	public <T> byte[] serialize(T obj) throws Exception;
	
	/**
	 * 反序列化
	 * @param <T> 类型
	 * @param data 字节数据
	 * @param clazz 对象类型
	 * @return 对象
	 * @throws Exception
	 */
	public <T> T deserialize(byte[] data ,Class<? extends T> clazz) throws Exception;
	
	

}

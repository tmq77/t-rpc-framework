package cn.t.rpc.core;

import java.io.Serializable;

/**
 * 传输数据格式(字段名尽量短,减少序列化的长度)
 * @author TMQ
 *
 */
public class TrafficData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 668699509694107642L;

	/**
	 * 数据类型
	 */
	private TrafficType type;

	/**
	 * 响应时间(毫秒数)
	 */
	private Long ts = System.currentTimeMillis();

	/**
	 * 连接id
	 */
	private String id;
	
	/**
	 * 字符串消息
	 */
	private String msg;
	

	/**
	 * 子类转换
	 * 
	 * @param <T> 子类型
	 * @return 转换后的子类对象
	 */
	@SuppressWarnings("unchecked")
	public final <T extends TrafficData> T convert() {
		return (T) this;
	}


	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public TrafficType getType() {
		return type;
	}

	public void setType(TrafficType type) {
		this.type = type;
	}


	public Long getTs() {
		return ts;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}
}

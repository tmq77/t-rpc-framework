package cn.t.rpc.core;

/**
 * 数据类型
 * @author TMQ
 *
 */
public enum TrafficType {
	UNKNOW(-1, "未知类型"), REQUEST(0, "客户端请求"), RESPONSE(1, "服务器响应"), IDLE(2, "心跳检测");

	private int type;

	private String name;

	TrafficType(int type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * 获取名称
	 * @param type 枚举值
	 * @return 名称
	 */
	public static String findName(int type) {
		switch (type) {
		case 0:
			return REQUEST.getName();
		case 1:
			return RESPONSE.getName();
		case 2:
			return IDLE.getName();
		default:
			return UNKNOW.getName();
		}
	}
	
	/**
	 * 获取枚举对象
	 * @param type 枚举值
	 * @return 枚举对象
	 */
	public static TrafficType findEnum(int type) {
		switch (type) {
		case 0:
			return REQUEST;
		case 1:
			return RESPONSE;
		case 2:
			return IDLE;
		default:
			return UNKNOW;
		}
	}

	/**
	 * 是否是客户端请求的数据
	 * 
	 * @param type 类型
	 * @return 客户端请求数据
	 */
	public boolean isRequest() {
		return REQUEST.equals(this);
	}

	/**
	 * 是否是服务器响应的数据
	 * 
	 * @param type 类型
	 * @return true 服务器响应数据
	 */
	public boolean isResponse() {
		return RESPONSE.equals(this);
	}

	public int value() {
		return this.type;
	}

	public String getName() {
		return name;
	}
}

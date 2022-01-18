package cn.t.rpc.core.data;

import java.util.Map;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.TrafficType;

/**
 * RPC服务器响应基本数据格式
 * @author Administrator
 *
 */
public class TResponse extends TrafficData {

	public TResponse() {
		super.setType(TrafficType.RESPONSE);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 8294120030587054112L;

	/**
	 * 响应头
	 */
	private Map<String, String> header;

	/**
	 * 响应体
	 */
	private Object body;
	
	/**
	 * 响应值
	 */
	private int status;

	// **************************
	// GETTER AND SETTER
	// **************************

	public Map<String, String> getHeader() {
		return header;
	}

	public void setHeader(Map<String, String> header) {
		this.header = header;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}
	
	public void setId(String trafficId) {
		super.setId(trafficId);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}

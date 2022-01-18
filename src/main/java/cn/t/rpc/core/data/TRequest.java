package cn.t.rpc.core.data;

import java.util.Arrays;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.TrafficType;
import cn.t.rpc.utils.KeyGeneratorUtil;

/**
 * RPC客户端请求基本数据格式
 * @author TMQ
 *
 */
public class TRequest extends TrafficData {
	
	public TRequest() {
		super.setType(TrafficType.REQUEST);
		super.setId(KeyGeneratorUtil.generateKey());
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6464721443830530287L;
	
	/**
	 * 请求调用的类名
	 */
	private String clsNm;
	
	/**
	 * 请求调用的方法
	 */
	private String method;
	
	/**
	 * 请求的参数
	 */
	private Object[] params;
	
	// **************************
	// GETTER AND SETTER
	// **************************
	
	@Override
	public String toString() {
		return "TRequest [" + "请求ID=" + super.getId() + "," + "请求调用类名=" + clsNm + " 请求调用方法="
				+ method + ", 请求参数=" + Arrays.toString(params) +  "]";
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public String getClsNm() {
		return clsNm;
	}

	public void setClsNm(String clsNm) {
		this.clsNm = clsNm;
	}


}

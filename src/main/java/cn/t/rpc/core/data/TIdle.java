package cn.t.rpc.core.data;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.TrafficType;

/**
 * RPC心跳检测基本数据格式
 * @author Administrator
 *
 */
public class TIdle extends TrafficData {
	
	public TIdle() {
		super.setType(TrafficType.IDLE);
		super.setMsg(TRpcConstants.IDLE_MESSAGE);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 553644269790527161L;
	

}

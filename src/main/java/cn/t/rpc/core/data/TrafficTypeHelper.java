package cn.t.rpc.core.data;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.TrafficType;
import cn.t.rpc.core.exception.TRpcTypeException;

/**
 * AbstractTrafficData子类映射关系辅助类
 * @author TMQ
 *
 */
public class TrafficTypeHelper {
	
	/**
	 * 获取实现类类型
	 * @param type 类型
	 * @return 对应的实现类类型
	 */
	public static Class<? extends TrafficData> getTrafficClassType(int type) {
		switch(TrafficType.findEnum(type)) {
		case REQUEST: 
			return TRequest.class;
		case RESPONSE:
			return TResponse.class;
		case IDLE:
			return TIdle.class;
		default:
			throw new TRpcTypeException();
		}
	}

}

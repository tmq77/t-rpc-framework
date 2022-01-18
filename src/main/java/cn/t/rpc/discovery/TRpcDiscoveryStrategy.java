package cn.t.rpc.discovery;

import java.util.List;

import cn.t.rpc.core.client.pool.TRpcRemoteAddress;

/**
 * RPC服务发现策略类
 * @author TMQ
 *
 */
public interface TRpcDiscoveryStrategy {
	
	/**
	 * 发现服务
	 * @return
	 */
	public List<TRpcRemoteAddress> find();
	
	/**
	 * 将自身注册
	 */
	public void register();
	
}

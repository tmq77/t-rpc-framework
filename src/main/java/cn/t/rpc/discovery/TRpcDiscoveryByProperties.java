package cn.t.rpc.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cn.t.rpc.core.client.pool.TRpcRemoteAddress;
import cn.t.rpc.utils.PropertiesUtil;

public class TRpcDiscoveryByProperties implements TRpcDiscoveryStrategy {

	@Override
	public List<TRpcRemoteAddress> find() {
		
		// 读取配置
		PropertiesUtil properties = new PropertiesUtil("rpc-server.properties");
		Set<Entry<Object, Object>> sets = properties.getAllProperties();
		
		List<TRpcRemoteAddress> serverList = new ArrayList<>();
		for(Entry<Object, Object> set : sets) {
			String value = (String) set.getValue();
			if (value.indexOf(",") != -1) {
				for (String ip : value.split(",")) {
					serverList.add(this.getAddress((String)set.getKey(), ip));
				}
			} else {
				serverList.add(this.getAddress((String)set.getKey(), value));
			}
		}
		return serverList;
	}
	
	/**
	 * 拆分id和地址
	 * @param serverId 服务Id
	 * @param value 配置文件的值
	 * @return 服务信息对象
	 */
	private TRpcRemoteAddress getAddress(String serverId, String value) {
		String host = value.split(":")[0];
		int port = Integer.parseInt(value.split(":")[1]);
		return new TRpcRemoteAddress(serverId, host, port);
	}

	@Override
	public void register() {
		// 基于配置文件,没有注册中心...
	}

}

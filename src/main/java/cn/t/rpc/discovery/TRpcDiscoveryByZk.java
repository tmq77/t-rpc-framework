package cn.t.rpc.discovery;

import java.util.ArrayList;
import java.util.List;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.client.pool.TRpcRemoteAddress;
import cn.t.rpc.zk.ZkClient;

public class TRpcDiscoveryByZk implements TRpcDiscoveryStrategy {
	
	private ZkClient zkClient;
	
	private TRpcConfigProperties properties;
	
	public TRpcDiscoveryByZk(TRpcConfigProperties properties) throws Exception {
		this.properties = properties;
		 this.zkClient = new ZkClient(properties.getZkCenter(), this);
	}

	@Override
	public List<TRpcRemoteAddress> find() {
		List<String> nodeInfoList = this.zkClient.getNodeList();
		List<TRpcRemoteAddress> resultList = new ArrayList<>();
		nodeInfoList.forEach(nodeInfo -> {
			String[] info = nodeInfo.split("=");
			String[] addr = info[1].split(":");
			resultList.add(new TRpcRemoteAddress(info[0], addr[0], Integer.parseInt(addr[1])));
		});
		return resultList;
	}

	@Override
	public void register() {
		this.zkClient.register(properties.getServerId(), "localhost:" + this.properties.getPort());
	}

}

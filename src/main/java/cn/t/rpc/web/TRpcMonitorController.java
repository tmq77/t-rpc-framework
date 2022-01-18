package cn.t.rpc.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.client.pool.TClientChannelPool;
import cn.t.rpc.core.server.holder.TLocalServiceHolderContext;

@RequestMapping("/rpc-monitor")
public class TRpcMonitorController {
	
	@Autowired
	private TRpcConfigProperties properties;
	
	/**
	 * 框架信息
	 */
	@RequestMapping("/monitor")
	@ResponseBody
	public Map<String, String> monitor() {
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("是否作为服务器提供远程调用:", this.properties.isExposedService() ? "是" : "否");
		if (this.properties.isExposedService()) {
			resultMap.put("提供的服务的端口:", String.valueOf(this.properties.getPort()));
			resultMap.put("提供的服务数量:", String.valueOf(TLocalServiceHolderContext.size()));
			resultMap.put("当前连接池数:", String.valueOf(TClientChannelPool.instance().size()));
		}
		return resultMap;
	}
}

package cn.t.rpc.config;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import cn.t.rpc.core.annotation.TRpcLocalService;
import cn.t.rpc.core.client.pool.TClientChannelPool;
import cn.t.rpc.core.proxy.TRpcTrafficDataContextHolder;
import cn.t.rpc.core.server.TServer;
import cn.t.rpc.core.server.holder.TLocalServiceHolderContext;
import cn.t.rpc.core.server.holder.TLocalServiceHolderContext.ServiceMetaData;
import cn.t.rpc.discovery.TRpcDiscoveryStrategy;

/**
 * 容器启动后处理,生成本地服务,初始化客户端连接池
 * 
 * @author Administrator
 *
 */
public class TRpcFrameworkAware implements CommandLineRunner, ApplicationContextAware {
	
	private static final Logger logger = LoggerFactory.getLogger(TRpcFrameworkAware.class);

	@Autowired
	private TRpcDiscoveryStrategy rpcDiscoveryStrategy;
	
	@Autowired
	private TRpcConfigProperties properties;

	@Override
	public void run(String... args) throws Exception {

		// 是否同时作为服务器启动
		if (this.properties.isExposedService()) {
			// 启动服务器
			TServer server = new TServer(this.properties);
			new Thread(() -> {
				server.start();
			}, "RPC服务器线程").start();
		}

		// 初始化客户端连接池
		TClientChannelPool.instance().config(this.properties).init(this.rpcDiscoveryStrategy.find());
		// 设置响应工具类的超时时间
		TRpcTrafficDataContextHolder.setTimeout(this.properties.getRequestTimeout());
	}

	/**
	 * 扫描需要发布的本地服务
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// 作为服务器启动时,扫描TRpcLocalService注解,将服务信息注册
		if (this.properties.isExposedService()) {
			// 获取所有TRpcLocalService注解标识的Bean
			Map<String, Object> localRpcBeans = applicationContext.getBeansWithAnnotation(TRpcLocalService.class);
			logger.info("扫描到{}个本地服务类...", localRpcBeans.size());
			for (var obj : localRpcBeans.entrySet()) {
				TRpcLocalService a = obj.getValue().getClass().getAnnotation(TRpcLocalService.class);
				String className = a.className();
				if (className.isBlank()) {
					className = obj.getValue().getClass().getSimpleName();
				}
				ServiceMetaData metaData = new ServiceMetaData(obj.getValue());
				for (var m : obj.getValue().getClass().getDeclaredMethods()) {
					// 只发布静态方法
					if (Modifier.isPublic(m.getModifiers())) {
						metaData.addServiceMethod(m.getName(), m);
					}
				}
				TLocalServiceHolderContext.addService(className, metaData);
			}
			// 将自身注册到注册中心(如果有)
			rpcDiscoveryStrategy.register();
		}
	}

}

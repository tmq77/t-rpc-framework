package cn.t.rpc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import cn.t.rpc.core.annotation.TRpcServiceScanner;
import cn.t.rpc.core.codec.strategy.SerializationByJson;
import cn.t.rpc.core.codec.strategy.SerializationStrategy;
import cn.t.rpc.discovery.TRpcDiscoveryByProperties;
import cn.t.rpc.discovery.TRpcDiscoveryStrategy;
import cn.t.rpc.web.TRpcMonitorController;

@Configuration
@TRpcServiceScanner(basePackages = {"cn.t.rpc"})
@Order // 在最后加载,保证@ConditionalOnMissingBean生效,默认最低优先级
public class TRpcAutoConfiguration {

	/**
	 * 客户端配置类
	 * @return 客户端配置类
	 */
	@Bean
	@ConfigurationProperties(prefix = "rpc.service")
	public TRpcConfigProperties rpcConfigProperties() {
		TRpcConfigProperties properties = new TRpcConfigProperties();
		properties.setSerializationStrategy(serializationStrategy());
		return properties;
	}
	
	/**
	 * 序列化方式工具类<br>
	 * 默认使用Jackson的Json工具类
	 * @return 序列化方式工具类
	 */
	@Bean
	@ConditionalOnMissingBean(SerializationStrategy.class)
	public SerializationStrategy serializationStrategy() {
		return new SerializationByJson();
	}
	
	/**
	 * 容器后处理
	 * @return
	 */
	@Bean
	public TRpcFrameworkAware tRpcFrameworkAware() {
		return new TRpcFrameworkAware();
	}
	
	/**
	 * 框架信息controller
	 * @return
	 */
	@Bean
	public TRpcMonitorController tRpcMonitorController() {
		return new TRpcMonitorController();
	}
	
	/**
	 * 服务发现(@ConditionalOnMissingBean有顺序问题, 默认生成的应该放最下面)
	 * @return 服务发现Bean
	 * @throws Exception 
	 */
//	@Bean
//	public TRpcDiscoveryStrategy rpcDiscoveryZk() throws Exception {
//		return new TRpcDiscoveryByZk(rpcConfigProperties());
//	}
	
	/**
	 * 服务发现
	 * @return 服务发现Bean
	 * @throws Exception 
	 */
	@Bean
	@ConditionalOnMissingBean(TRpcDiscoveryStrategy.class)
	public TRpcDiscoveryStrategy rpcDiscovery() throws Exception {
		return new TRpcDiscoveryByProperties();
	}
}

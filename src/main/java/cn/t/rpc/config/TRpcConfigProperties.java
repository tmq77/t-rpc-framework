package cn.t.rpc.config;

import cn.t.rpc.core.codec.strategy.SerializationStrategy;

public class TRpcConfigProperties {
	
	/**
	 * 本机提供的远程调用服务端口
	 */
	private int port = 18081;
	
	/**
	 * 连接超时时间(毫秒)
	 */
	private int connectionTimeout = 5000;
	
	/**
	 * 获取通道时每个连接池重试次数(失败指定次数后连接池状态将会被标记成不可用)
	 */
	private int retryCnt = 3;
	
	/**
	 * 服务器响应生成到客户端接收到的时间差值阈值,请求超时时间(毫秒)
	 */
	private int requestTimeout = 2000;
	
	/**
	 * 心跳检测次数(通道保持时间: 次数 * 间隔)
	 */
	private int idleCnt = 5;
	
	/**
	 * 心跳检测间隔(秒)
	 */
	private int idleInterval = 60;
	
	/**
	 * 单个客户端连接池最大连接数(通道数)
	 */
	private int maxPoolConnection = 5;
	
	/**
	 * 是否作为RPC服务器暴露服务
	 */
	private boolean exposedService = false;
	
	/**
	 * 服务id(多实例的情况下服务id应该相同)
	 */
	private String serverId = "t-rpc";
	
	/**
	 * 设置后连接池不可用后会每分钟进行重连<br>
	 * 连接池不可用后进行重连的最大时间(分)。不设置或者为-1时默认无限重试(至少会重试一次)
	 */
	private int reconnectionTime = 30;
	
	/**
	 * zookeeper地址
	 */
	private String zkCenter = "127.0.0.1:2181";
	
	/**
	 * 序列化方式工具类
	 */
	private SerializationStrategy serializationStrategy;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getRetryCnt() {
		return retryCnt;
	}

	public void setRetryCnt(int retryCnt) {
		this.retryCnt = retryCnt;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getIdleCnt() {
		return idleCnt;
	}

	public void setIdleCnt(int idleCnt) {
		this.idleCnt = idleCnt;
	}

	public int getIdleInterval() {
		return idleInterval;
	}

	public void setIdleInterval(int idleInterval) {
		this.idleInterval = idleInterval;
	}

	public int getMaxPoolConnection() {
		return maxPoolConnection;
	}

	public void setMaxPoolConnection(int maxPoolConnection) {
		this.maxPoolConnection = maxPoolConnection;
	}

	public boolean isExposedService() {
		return exposedService;
	}

	public void setExposedService(boolean exposedService) {
		this.exposedService = exposedService;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public int getReconnectionTime() {
		return reconnectionTime;
	}

	public void setReconnectionTime(int reconnectionTime) {
		this.reconnectionTime = reconnectionTime;
	}

	public SerializationStrategy getSerializationStrategy() {
		return serializationStrategy;
	}

	public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
		this.serializationStrategy = serializationStrategy;
	}

	public String getZkCenter() {
		return zkCenter;
	}

	public void setZkCenter(String zkCenter) {
		this.zkCenter = zkCenter;
	}
}

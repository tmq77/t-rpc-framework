package cn.t.rpc.core.client.pool;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.exception.TRpcConnectionException;
import cn.t.rpc.core.exception.TRpcCoreException;
import cn.t.rpc.core.exception.TRpcOperationException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

/**
 * RPC通道连接池(单例使用)
 * 
 * @author TMQ
 *
 */
public class TClientChannelPool {

	private static final Logger logger = LoggerFactory.getLogger(TClientChannelPool.class);

	/**
	 * 单例
	 */
	private static volatile TClientChannelPool instance;

	/**
	 * 单例获取
	 * 
	 * @return 连接池单例
	 */
	public static TClientChannelPool instance() {
		if (instance == null) {
			synchronized (TClientChannelPool.class) {
				if (instance == null) {
					instance = new TClientChannelPool();
				}
			}
		}
		return instance;
	}

	/**
	 * 通道属性的键值(指定属于哪个服务器的连接池的哪个下标)
	 */
	private static final String POOL_KEY = "POOL_KEY";

	/**
	 * 配置类
	 */
	private TRpcConfigProperties config;

	/**
	 * 客户端服务端映射关系池(实际)(一个服务器对应一个池子)
	 */
	private AbstractChannelPoolMap<TRpcRemoteAddress, FixedChannelPool> poolMap;

	/**
	 * 客户端服务端映射关系池(缓存) 同id的远程服务会映射在一个list中,在list中随机轮询<br>
	 */
	private Map<String, List<TClientPoolWrapper>> rpcMaps;

	/**
	 * 客户端线程池
	 */
	private EventLoopGroup workerGroup;

	/**
	 * 客户端启动辅助类
	 */
	private Bootstrap bootstrap;

	/**
	 * 随机数生成器
	 */
	private Random rand = new Random();

	/**
	 * 连接池状态(true： 已初始化)
	 */
	private boolean status = false;

	/**
	 * 失败操作信息
	 */
	private StringBuilder failureMsg = new StringBuilder();

	/**
	 * 定时刷新timer
	 */
	private Timer timer;

	/**
	 * 判断连接池是否准备完毕
	 * 
	 * @return
	 */
	public boolean isReady() {
		return this.status;
	}
	
	/**
	 * 连接池大小
	 * @return 连接池大小
	 */
	public int size() {
		return this.poolMap.size();
	}

	/**
	 * 获取失败操作信息
	 * 
	 * @return 失败操作
	 */
	public String getFailureMsg() {
		return this.failureMsg.toString();
	}

	/**
	 * 配置类初始化
	 * 
	 * @param config
	 * @return 当前实例
	 */
	public TClientChannelPool config(TRpcConfigProperties config) {
		this.config = config;
		return instance;
	}

	/**
	 * 初始化连接池
	 * 
	 * @param addrList 服务地址
	 */
	public void init(List<TRpcRemoteAddress> addrList) {

		if (this.config == null) {
			logger.warn(("未配置连接池...连接池初始化失败...需要调用config()方法进行配置..."));
			this.failureMsg.append("1.未配置连接池...连接池初始化失败...需要调用config()方法进行配置...");
		}

		if (addrList == null || addrList.isEmpty()) {
			logger.warn(("未设置远程地址..."));
			this.failureMsg.append("2.未设置远程地址...");
		}

		logger.info("初始化RPC连接池...");
		this.workerGroup = new NioEventLoopGroup();
		this.bootstrap = new Bootstrap();

		this.bootstrap.group(this.workerGroup).channel(NioSocketChannel.class) // 客户端的channel与服务器不一样
				.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getConnectionTimeout());

		// 初始化poolMap
		poolMap = new AbstractChannelPoolMap<TRpcRemoteAddress, FixedChannelPool>() {

			@Override
			protected FixedChannelPool newPool(TRpcRemoteAddress key) {
				// 固定设定值的通道的连接池
				return new FixedChannelPool(bootstrap.remoteAddress(key.getRemoteAddress()),
						new TClientChannelPoolHandler(config), config.getMaxPoolConnection());
			}
		};

		// 预创建
		if (addrList != null && !addrList.isEmpty()) {
			// 构建连接池
			this.initPool(addrList);
			// 清空失败操作信息
			this.failureMsg.delete(0, this.failureMsg.length());
			// 开启定时刷新
			this.startTimer();
		} else {
			logger.warn(("未获取到设置远程地址...连接池预创建未完成..."));
			this.failureMsg.append("3.未获取到设置远程地址,连接池预创建未完成...");
		}
	}

	/**
	 * 刷新远程服务节点信息<br>
	 * 连接池刷新时并没有减少数量,此时仍可以对外服务
	 * 
	 * @param addrList 服务列表
	 */
	public void refreshPool(List<TRpcRemoteAddress> addrList) {
		logger.info("开始刷新远程节点...");
		if (addrList == null || addrList.isEmpty()) {
			logger.warn("未获取到远程节点信息...");
			return;
		}

		this.sweepPool();
		this.initPool(addrList);
	}

	/**
	 * 获取指定服务的通道
	 * 
	 * @param key 服务id
	 * @return 通道
	 */
	public Channel getPooledChannel(String key) {

		if (this.rpcMaps == null || this.rpcMaps.isEmpty()) {
			throw new TRpcOperationException("连接池未初始化...需要调用init()方法进行初始化...");
		}

		List<TClientPoolWrapper> poolList = rpcMaps.get(key);

		if (poolList == null || poolList.isEmpty()) {
			logger.error("远程连接丢失...无可用连接池...");
			throw new TRpcConnectionException("远程连接丢失...无可用连接池...");
		}

		// 简单随机
		// 按数组长度随机取余
		int random = this.rand.nextInt(1024);
		int retryCnt = 0;
		// 保证每个池子都能尝试到
		int limit = poolList.size();
		int index = 0;
		Channel channel = null;
		do {
			index = random % limit;
			channel = poolList.get(index).acquireChannel(this.config.getRetryCnt(), this.config.getReconnectionTime());
			// 获取失败时随机数自增，进行下一个池子的获取
			random++;
			retryCnt++;

		} while (channel == null && retryCnt < limit);

		if (channel == null) {
			throw new TRpcCoreException("无法获取连接通道...");
		}

		// 给当前的通道绑定属性,指定其从属于哪一个池子
		AttributeKey<Map<String, Integer>> attrKey = AttributeKey.valueOf(POOL_KEY);
		Map<String, Integer> metaMap = new HashMap<>();
		metaMap.put(key, index);
		channel.attr(attrKey).set(metaMap);
		return channel;
	}

	/**
	 * 将通道释放回连接池
	 * 
	 * @param ch 通道
	 */
	public void release(Channel ch) {
		Map<String, Integer> metaMap = ch.attr(AttributeKey.<Map<String, Integer>>valueOf(POOL_KEY)).get();
		if (metaMap == null || metaMap.size() == 0) {
			logger.error("连接在{}通道的元数据丢失!", ch.remoteAddress());
			throw new TRpcCoreException("连接在" + ch.remoteAddress() + "通道的元数据丢失!");
		}
		String serverId = null;
		int index = -1;
		for (var e : metaMap.entrySet()) {
			serverId = e.getKey();
			index = e.getValue();
		}
		// 释放连接
		rpcMaps.get(serverId).get(index).releaseChannel(ch);
	}

	/**
	 * 清除DOWN掉的节点连接池
	 */
	private void sweepPool() {
		if (this.rpcMaps != null) {
			this.rpcMaps.entrySet().forEach(entry -> {
				List<TClientPoolWrapper> downList = entry.getValue().stream().filter(p -> p.isDown())
						.collect(Collectors.toList());
				// 映射池中移除
				downList.forEach(d -> {
					this.poolMap.remove(d.key());
				});
				// 缓存池中移除
				entry.getValue().removeAll(downList);
			});
		}
	}

	/**
	 * 构建连接池(外部可以调用次方法进行服务添加)
	 * 
	 * @param addrList 服务列表
	 */
	private void initPool(List<TRpcRemoteAddress> addrList) {
		this.rpcMaps = (this.rpcMaps == null || this.rpcMaps.size() == 0) ? new HashMap<>(32) : this.rpcMaps;
		addrList.forEach(addr -> {
			// poolMap调用get获取时会默认创建连接池,如果已创建则直接返回
			String id = addr.getServerId();
			if (this.rpcMaps.containsKey(id)) {
				// 相同的连接将不会重复添加
				if (!this.poolMap.contains(addr)) {
					logger.info("发现新的远程服务...服务地址:{}...", addr.getAddressString());
					this.rpcMaps.get(id).add(new TClientPoolWrapper(addr, this.poolMap.get(addr)));
				}
			} else {
				List<TClientPoolWrapper> poolList = new ArrayList<>();
				logger.info("发现新的远程服务...服务地址:{}...", addr.getAddressString());
				poolList.add(new TClientPoolWrapper(addr, this.poolMap.get(addr)));
				this.rpcMaps.put(id, poolList);
			}
		});
		
		// 初始化成功
		this.status = true;
	}

	/**
	 * 私有化构造
	 */
	private TClientChannelPool() {
	}

	/**
	 * 私有化构造
	 * 
	 * @param config 配置信息
	 */
	private TClientChannelPool(TRpcConfigProperties config) {
		this.config = config;
	}

	/**
	 * 开始定时清理无用的连接池
	 */
	private void startTimer() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 30);
		this.timer = new Timer();
		// 30分钟后定时清除Down掉的线程池
		this.timer.schedule(new TimerTask() {

			@Override
			public void run() {
				logger.info("定时清理无用的连接池...");
				sweepPool();
			}

		}, cal.getTime(), 1000 * 60 * 30);
	}
}

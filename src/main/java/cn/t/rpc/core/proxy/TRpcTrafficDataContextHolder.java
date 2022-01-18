package cn.t.rpc.core.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.data.InvalidKey;
import cn.t.rpc.core.exception.TRpcCoreException;
import cn.t.rpc.core.exception.TRpcDuplicateKeyException;

/**
 * 保存服务器响应的上下文辅助类,可以配置并保存一个响应超时时间供外部类使用
 * 
 * @author TMQ
 *
 */
public final class TRpcTrafficDataContextHolder {

	private static final Logger logger = LoggerFactory.getLogger(TRpcTrafficDataContextHolder.class);

	/**
	 * 响应结果缓存,根据key隔离不同的响应<br>
	 * 初始化hashMap容量,减少扩容
	 */
	private static final ConcurrentHashMap<String, CompletableFuture<TrafficData>> resultMap = new ConcurrentHashMap<>(
			128);

	/**
	 * 延时无界队列,用于定时清除极端情况下的无效响应
	 */
	private static final DelayQueue<InvalidKey> INVALIDKEY_QUEUE = new DelayQueue<>();

	/**
	 * 缓存清除线程
	 */
	private static final Thread sweeper = new Thread(() -> {
		while (true) {
			try {
				logger.info("keep sweep the cache...");
				resultMap.remove(INVALIDKEY_QUEUE.take().getKey());
			} catch (InterruptedException e) {
				logger.warn("the sweeper has been interrupted...");
			}
		}

	}, "the sweeper");

	static {
		sweeper.start();
	}

	/**
	 * 静态工具类单例对象
	 * 
	 * @throws IllegalAccessException 非法操作
	 */
	private TRpcTrafficDataContextHolder() throws IllegalAccessException {
		throw new IllegalAccessException("Illegal Operation...");
	}

	/**
	 * 超时时间
	 */
	private static int TIMEOUT = 2000;

	/**
	 * 设置超时时间
	 * 
	 * @param timeout 超时时间
	 */
	public static void setTimeout(int timeout) {
		TIMEOUT = timeout;
	}

	/**
	 * 获取设置的超时时间
	 * 
	 * @return 超时时间
	 */
	public static int getTimeout() {
		return TIMEOUT;
	}

	/**
	 * 创建新的结果CompletableFuture对象
	 * 
	 * @param trafficId 唯一ID
	 * @return 空CompletableFuture对象
	 */
	public static CompletableFuture<TrafficData> newFuture(String trafficId) {
		CompletableFuture<TrafficData> future = new CompletableFuture<TrafficData>();
		var old = resultMap.putIfAbsent(trafficId, future);
		if (old != null) {
			throw new TRpcDuplicateKeyException();
		}
		return future;
	}

	/**
	 * 完成指定的future,客户端收到响应后调用
	 * 
	 * @param data 响应数据
	 */
	public static void complete(TrafficData data) {
		CompletableFuture<TrafficData> future = resultMap.remove(data.getId());
		if (future != null) {
			future.complete(data);
		} else {
			throw new TRpcCoreException("future not found...");
		}
	}

	/**
	 * 移除缓存中的响应
	 * 
	 * @param trafficId 唯一ID
	 * @return 缓存中的响应
	 */
	public static CompletableFuture<TrafficData> removeCache(String trafficId) {
		CompletableFuture<TrafficData> invalidData = resultMap.remove(trafficId);
		// 延时再删除(保证极端情况下等待线程达到等待最大值同时客户端handler满足响应条件时缓存无法被正常消费的case)
		if (invalidData == null) {
			INVALIDKEY_QUEUE.add(new InvalidKey(trafficId));
		}
		return invalidData;
	}
}

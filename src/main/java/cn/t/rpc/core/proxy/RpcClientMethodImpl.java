package cn.t.rpc.core.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.annotation.TRpcRemoteService;
import cn.t.rpc.core.client.pool.TClientChannelPool;
import cn.t.rpc.core.data.TRequest;
import cn.t.rpc.core.data.TResponse;
import cn.t.rpc.core.exception.TRpcConnectionException;
import cn.t.rpc.core.exception.TRpcOperationException;
import cn.t.rpc.core.exception.TRpcResponseException;
import cn.t.rpc.core.exception.TRpcTimeoutException;
import io.netty.channel.Channel;

/**
 * 接口代理类<br>
 * 重写action方法处理自定义逻辑<br>
 * 非Spring的使用方式:<br>
 * {@code // 创建接口的代理类}<br>
 * {@code RpcClientMethodImpl<XXXInterface> xxx = new RpcClientMethodImpl<>(XXXInterface.class);}<br>
 * {@code // 初始化RPC客户端}<br>
 * {@code xxx.initClient(TrafficRunner client)}<br>
 * {@code // 获取接口的代理对象,调用接口方法}<br>
 * {@code xxx.getProxy().doSomething();}
 * 
 * @author TMQ
 *
 * @param <T>
 */
public class RpcClientMethodImpl<T> extends RpcProxy<T> {

	private static final Logger logger = LoggerFactory.getLogger(RpcClientMethodImpl.class);

	/**
	 * 客户端连接池(单例)
	 */
	private static TClientChannelPool CHANNEL_POOL = TClientChannelPool.instance();

	public RpcClientMethodImpl(Class<T> clazz) {
		// 初始化接口类型
		super(clazz);
	}

	@Override
	protected Object action(Method method, Object[] args) throws TRpcTimeoutException, TRpcConnectionException {

		if (!CHANNEL_POOL.isReady()) {
			throw new TRpcConnectionException("连接池未初始化成功...操作信息:" + CHANNEL_POOL.getFailureMsg());
		}

		// 获取指定的服务器id
		TRpcRemoteService a = method.getDeclaringClass().getAnnotation(TRpcRemoteService.class);
		String serverId = a.serverId();

		// 获取通信通道
		Channel ch = CHANNEL_POOL.getPooledChannel(serverId);

		TRequest req = new TRequest();

		// 方法名一致才可以调用
		req.setMethod(method.getName());
		req.setParams(args);
		// 调用的服务类名
		String className = a.className();
		if (className.isBlank()) {
			className = method.getDeclaringClass().getSimpleName();
		}
		req.setClsNm(className);

		// 预创建一个响应
		CompletableFuture<TrafficData> trafficFuture = TRpcTrafficDataContextHolder.newFuture(req.getId());
		
		ch.writeAndFlush(req).addListener((f) -> {
			if (f.isSuccess()) {
				logger.debug("发送成功...");
			} else {
				logger.warn("远程调用{}失败...", method.getName());
				// 发送失败清除缓存
				TRpcTrafficDataContextHolder.removeCache(req.getId());
			}
			// 使用完通道后立即释放
			CHANNEL_POOL.release(ch);
		}).syncUninterruptibly();
		
		// 获取响应值-异步操作,此时可能响应还未完成
		try {
			// 此处多加的50ms是给数据传输用的,客户端响应事件中用于判断的条件仅考虑了当前时间和服务器响应时间的差值
			TResponse res = trafficFuture.get(TRpcTrafficDataContextHolder.getTimeout() + 50, TimeUnit.MILLISECONDS).convert();
			if (res.getStatus() != 200) {
				throw new TRpcResponseException(res.getMsg());
			} else {
				return res.getBody();
			}
			
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("RPC调用异常...异常信息:{}", e.getMessage());
			// 出现异常移除本次请求的缓存
			TRpcTrafficDataContextHolder.removeCache(req.getId());
			throw new TRpcOperationException(e.getMessage(), e);
		}
	}
}

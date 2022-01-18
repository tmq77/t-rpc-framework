package cn.t.rpc.core.client.pool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.Future;

/**
 * 连接池包装类,封装了获取通道,释放通道,重连功能
 * 
 * @author TMQ
 *
 */
public class TClientPoolWrapper {

	private static final Logger logger = LoggerFactory.getLogger(TClientPoolWrapper.class);

	/**
	 * 状态枚举
	 * 
	 * @author TMQ
	 *
	 */
	public enum STATUS {
		UP, DOWN, RETRY,
	}

	public TClientPoolWrapper(TRpcRemoteAddress key, FixedChannelPool pool) {
		this.key = key;
		this.pool = pool;
	}

	/**
	 * 在实际客户端服务器映射map中对应的key值(清除Down掉的连接池使用)<br>
	 * key中包含了对应的连接地址
	 */
	private TRpcRemoteAddress key;
	
	/**
	 * 锁,开启重连时使用
	 */
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * 失败次数(进入重连状态的关键)
	 */
	private volatile int failureCnt;

	/**
	 * 连接池状态(提供服务以及退出重连线程的关键)
	 */
	private volatile STATUS status;

	/**
	 * 连接池
	 */
	private FixedChannelPool pool;
	
	/**
	 * 重连线程
	 */
	private Thread reconnectionThread;
	
	/**
	 * 获取当前连接池在对应AbstractChannelPoolMap中的key值
	 * @return key
	 */
	public TRpcRemoteAddress key() {
		return this.key;
	}
	
	/**
	 * 获取通道
	 * 
	 * @param retryCnt         获取通道的重试次数上限
	 * @param reconnectionTime 连接池重连的时间上限
	 * @return 通道
	 */
	public Channel acquireChannel(int retryCnt, int reconnectionTime) {

		// 池子不可用时,直接抛出异常
		if (!this.isHealthy()) {
			logger.warn("连接至{}的连接池不可用...请检查网络状态...", this.key.getAddressString());
			// 尝试下一个连接池
			return null;
		}

		Future<Channel> channeFuture = this.pool.acquire();
		Channel channel = null;
		try {
			channel = channeFuture.get();
			logger.debug("在连接池[{}]中获取通道成功...", this.key.getAddressString());
		} catch (InterruptedException | ExecutionException e) {
			logger.warn("在连接池[{}]中获取通道失败...失败信息[{}]...", this.key.getAddressString(), e.getMessage());
			// 健康检查,重试
			this.increaseFailureCnt(retryCnt, reconnectionTime);
		}
		return channel;
	}

	/**
	 * 释放通道
	 * 
	 * @param ch 通道
	 */
	public void releaseChannel(Channel ch) {
		this.pool.release(ch);
	}
	
	/**
	 * 判断当前池子是否已经挂掉
	 * @return true 池子挂了
	 */
	public boolean isDown() {
		return STATUS.DOWN.equals(this.status);
	}

	/**
	 * 获取池子健康状态
	 * 
	 * @return true 健康
	 */
	private boolean isHealthy() {
		return this.status == null || STATUS.UP.equals(this.status);
	}

	/**
	 * 是否处于重试状态
	 * 
	 * @return true 重试中
	 */
	private boolean isRetrying() {
		return STATUS.RETRY.equals(this.status);
	}

	/**
	 * 失败次数增加<br>
	 * 达到指定次数后,连接池标记为不可用
	 * 
	 * @param limit            通道获取重试次数上限
	 * @param reconnectionTime 重连时间上限(分)
	 */
	private void increaseFailureCnt(int limit, final int reconnectionTime) {
		if ((++this.failureCnt) >= limit) {
			// 保证只有一个线程能够开启重连任务
			if (!this.isRetrying() && this.lock.tryLock()) {
				// 设置状态为重试中
				this.status = STATUS.RETRY;
				// 解锁
				this.lock.unlock();
				
				this.reconnectionThread = new Thread(() -> {
					int counter = reconnectionTime;
					do {
						
						if (reconnectionTime != -1 && (counter < 0)) {
							// 连接池挂了
							this.down();
							return;
						}
						
						try {
							Channel c = this.pool.acquire().get();
							this.pool.release(c);
							this.status = STATUS.UP;
							this.resetStatus();
							return;
						} catch (InterruptedException | ExecutionException e) {
							logger.warn("{}重连中...", this.key.getAddressString());
							try {
								// 1分钟重试
								TimeUnit.MINUTES.sleep(1);
							} catch (InterruptedException e1) {}
						}
						
						if (reconnectionTime == -1) {
							// 防止int溢出?
							counter = 0;
						}
						counter--;
						
					} while (STATUS.RETRY.equals(this.status));
					
					if (this.isHealthy()) {
						logger.info("{}重连成功...", this.key.getAddressString());
					} else {
						logger.warn("{}重连失败...", this.key.getAddressString());
					}

				}, "重连[" + this.key.getAddressString() + "]线程");
				
				this.reconnectionThread.start();
			}
		}
	}
	
	/**
	 * 关闭重连线程,标记连接池不可用
	 */
	private void down() {
		logger.error("!!!重要:连接到{}的连接池不可用...", this.key.getAddressString());
		this.status = STATUS.DOWN;
		// 连接池关闭
		this.pool.closeAsync().syncUninterruptibly();
	}
	
	/**
	 * 清除失败次数
	 */
	private void resetStatus() {
		this.failureCnt = 0;
		this.status = STATUS.UP;
		this.reconnectionThread = null;
	}
}

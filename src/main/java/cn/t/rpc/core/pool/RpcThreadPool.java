package cn.t.rpc.core.pool;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 * @author taomi
 *
 */
public class RpcThreadPool {
	
	/**
	 * 线程池核心线程数
	 */
	private static final int DEFAULT_CORE_SIZE = Runtime.getRuntime().availableProcessors();

	/**
	 * 线程池默认最大线程数
	 */
	private static final int DEFAULT_MAX_POOL_SIZE = DEFAULT_CORE_SIZE * 2;

	/**
	 * 线程默认空闲存活时间
	 */
	private static final long DEFAULT_KEEPALIVE_TIME = 60l;

	/**
	 * 线程池工作队列最大上限
	 */
	private static final int DEFAULT_MAX_QUEUE_NUM = 1024;

	/**
	 * 线程池
	 */
	private ThreadPoolExecutor pool;
	
	/**
	 * 自定义线程工厂
	 */
	private CustomThreadFactory customThreadFactory;

	/**
	 * 获取线程池
	 * @return 线程池
	 */
	public ThreadPoolExecutor getPool() {
		return this.pool;
	}
	
	/**
	 * 获取线程工厂创建的线程数
	 * @return 线程数
	 */
	public int getThreadSize() {
		return this.customThreadFactory.size.get();
	}

	public RpcThreadPool() {
		this.customThreadFactory = new CustomThreadFactory();
	    // 参数说明
	    // 1、corePoolSize，线程池中的核心线程数
	    // 2、maximumPoolSize，线程池中的最大线程数
	    // 3、keepAliveTime，空闲时间，当线程池数量超过核心线程数时，多余的空闲线程存活的时间，即：这些线程多久被销毁。
	    // 4、unit，空闲时间的单位，可以是毫秒、秒、分钟、小时和天，等等
	    // 5、workQueue，等待队列，线程池中的线程数超过核心线程数时，任务将放在等待队列，它是一个BlockingQueue类型的对象
	    // 6、threadFactory，线程工厂，我们可以使用它来创建一个线程
	    // 7、handler，拒绝策略，当线程池和等待队列都满了之后，需要通过该对象的回调函数进行回调处理
		this.pool = new ThreadPoolExecutor(DEFAULT_CORE_SIZE, DEFAULT_MAX_POOL_SIZE, DEFAULT_KEEPALIVE_TIME,
	        TimeUnit.SECONDS, new LinkedBlockingDeque<>(DEFAULT_MAX_QUEUE_NUM), this.customThreadFactory,
	        new CustomRejectHandler());
	  }

	/**
	 * 执行Runnable任务
	 * 
	 * @param r
	 */
	public void execute(Runnable r) {
		if (this.pool != null) {
			this.pool.execute(r);
		}
	}

	/**
	 * 执行Callable任务
	 * @param <T> 类型
	 * @param t 线程任务
	 * @return Future结果
	 */
	public <T> Future<T> submit(Callable<T> t) {
		if (this.pool != null) {
			return this.pool.submit(t);
		}
		return null;
	}

	/**
	 * 关闭线程池
	 */
	public void shutdown() {
		if (this.pool != null) {
			this.pool.shutdown();
		}
	}

	/**
	 * 立即关闭线程池
	 */
	public List<Runnable> shutdownNow() {
		if (this.pool != null) {
			return this.pool.shutdownNow();
		}
		return null;
	}

	/**
	 * 自定义线程工厂
	 * 
	 * @author Administrator
	 *
	 */
	private class CustomThreadFactory implements ThreadFactory {

		private static final String NAME_FORMAT = "[------RPC-THREAD:%s------]";

		/**
		 * 线程工厂创建的线程数
		 */
		private AtomicInteger size = new AtomicInteger(0);

		/**
		 * 创建线程,线程池将会从这个方法取得线程
		 */
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			// 创建一条线程则计数自增
			t.setName(String.format(NAME_FORMAT, RpcThreadPool.class.getSimpleName() + size.addAndGet(1)));
			return t;
		}
	}

	/**
	 * 自定义拒绝策略(当工作队列满的时候执行)
	 * 
	 * @author Administrator
	 *
	 */
	private class CustomRejectHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			try {
				// 在工作队列满的时候进行等待,直到有空余
				executor.getQueue().put(r);
			} catch (InterruptedException e) {
				e.printStackTrace();
				// 异常则丢弃
			}
		}
	}
}

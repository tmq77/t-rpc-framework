package cn.t.rpc.core.data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 非法的，过期的key对象,用于延时删除,清理缓存
 * @author TMQ
 *
 */
public class InvalidKey implements Delayed {
	
	public InvalidKey(String key) {
		this.key = key;
		// 5秒后过期
		this.expiredTs = System.currentTimeMillis() + 5000;
	}
	
	private String key;
	
	private long expiredTs;
	
	@Override
	public int compareTo(Delayed o) {
		return -1;
	}

	/**
	 * 获取延时时间- 固定 5秒后到期执行
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		// 由于队列会不停计算,所以此处的延迟时间需要为动态值，始终大于1的情况下,队列会一直阻塞
		return this.expiredTs - System.currentTimeMillis();
	}

	public String getKey() {
		return key;
	}
}

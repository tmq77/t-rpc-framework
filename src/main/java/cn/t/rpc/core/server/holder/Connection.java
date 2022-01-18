package cn.t.rpc.core.server.holder;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;

/**
 * 连接信息类
 * @author TMQ
 *
 */
public class Connection {
	
	/**
	 * 超时次数
	 */
	private AtomicInteger idle = new AtomicInteger(0);
	
	private Channel channel;

	public Connection(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	/**
	 * 获取当前次数
	 * @return
	 */
	public int getIdle() {
		return idle.get();
	}

	/**
	 * 超时次数增长
	 * @param idle
	 */
	public void incrementIdle() {
		this.idle.incrementAndGet();
	}
	
	/**
	 * 重置计数
	 */
	public void resetIdle() {
		this.idle = new AtomicInteger(0);
	}
}

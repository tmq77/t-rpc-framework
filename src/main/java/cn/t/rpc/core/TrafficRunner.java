package cn.t.rpc.core;

import io.netty.channel.Channel;

public abstract class TrafficRunner {
	
	/**
	 * 通道
	 */
	protected Channel channel;
	
	/**
	 * 运行状态: true:运行中
	 */
	protected boolean status;
	
	/**
	 * 开始
	 * @throws Exception
	 */
	public abstract void start();
	
	/**
	 * 关闭
	 */
	public void close() {
		if (this.channel != null && this.channel.isOpen()) {
			this.channel.close();
		}
	};
	
	/**
	 * 获取运行状态
	 * @return 运行状态
	 */
	public boolean isRunning() {
		return this.status;
	}
	
	/**
	 * 运行中
	 */
	public void statusUp() {
		this.status = true;
	}
	
	/**
	 * 终止运行
	 */
	public void statusDown() {
		this.status = false;
	}
	
	/**
	 * 获取通道
	 * @return 通道
	 */
	public Channel getChannel() {
		return this.channel;
	}
}

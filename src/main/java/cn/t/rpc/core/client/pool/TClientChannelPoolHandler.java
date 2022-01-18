package cn.t.rpc.core.client.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.codec.TrafficDecoder;
import cn.t.rpc.core.codec.TrafficEncoder;
import cn.t.rpc.core.handler.ClientIdleHandler;
import cn.t.rpc.core.handler.ClientRpcHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelPoolHandler;

/**
 * 连接池获取连接的处理器
 * @author TMQ
 *
 */
public class TClientChannelPoolHandler implements ChannelPoolHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(TClientChannelPoolHandler.class);
	
	private TRpcConfigProperties config;
	
	public TClientChannelPoolHandler(TRpcConfigProperties config) {
		this.config = config;
	}

	/**
	 * 释放连接
	 */
	@Override
	public void channelReleased(Channel ch) throws Exception {
		ch.flush();
		logger.debug("连接在{}的通道已释放...", ch.remoteAddress());
	}

	/**
	 * 获取连接
	 */
	@Override
	public void channelAcquired(Channel ch) throws Exception {
		logger.debug("连接到{}的通道已获取...", ch.remoteAddress());
	}

	/**
	 * 创建连接,添加处理器链
	 */
	@Override
	public void channelCreated(Channel ch) throws Exception {
		ch.config().setOption(ChannelOption.SO_KEEPALIVE, true);
		ch.config().setOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getConnectionTimeout());
		
		ch.pipeline().addLast(new TrafficDecoder(this.config.getSerializationStrategy()));
		ch.pipeline().addLast(new TrafficEncoder(this.config.getSerializationStrategy()));
		ch.pipeline().addLast(new ClientIdleHandler());
		ch.pipeline().addLast(ClientRpcHandler.HANDLER_NAME,
				new ClientRpcHandler(this.config.getRequestTimeout()));
	}
}

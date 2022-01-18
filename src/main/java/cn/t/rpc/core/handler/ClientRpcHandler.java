package cn.t.rpc.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.data.TRpcConstants;
import cn.t.rpc.core.proxy.TRpcTrafficDataContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 客户端核心处理器-每次重新连接都需要new一个实例(或者使用线程安全的单例) 保证线程安全
 * @author TMQ
 *
 */
public class ClientRpcHandler extends SimpleChannelInboundHandler<TrafficData> {

	public static final String HANDLER_NAME = "ClientHandler";
	
	private static final Logger logger = LoggerFactory.getLogger(ClientRpcHandler.class);
	
	/**
	 * 请求超时时间
	 */
	private int requestTimeout = 2000;
	
	public ClientRpcHandler(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TrafficData msg) throws Exception {
		logger.debug("{}客户端{}读取响应信息...", TRpcConstants.LOG_CLIENT_MSG_PREFIX, ctx.channel().remoteAddress());
		try {
			
			// 响应开始时间
			long ts = msg.getTs();
			// 当前时间
			long curTs = System.currentTimeMillis();
			// 这里没有考虑数据传输的时间消耗,数据传输的耗时在请求侧进行增益
			// 超过超时时间则丢弃，不放入缓存(极端情况下-刚好达到阈值时 将会进入缓存,然后在清扫线程中被移除)
			if (curTs - ts > this.requestTimeout) {
				logger.warn("{}响应时间过长...数据已被丢弃...", TRpcConstants.LOG_CLIENT_MSG_PREFIX);
			} else {
				// 响应值存入共享map中
				TRpcTrafficDataContextHolder.complete(msg);
			}
		} catch(Exception e) {
			// 移除缓存
			TRpcTrafficDataContextHolder.removeCache(msg.getId());
			logger.error("{}响应处理异常...异常信息:{}", TRpcConstants.LOG_CLIENT_MSG_PREFIX, e.getMessage());
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("{}客户端异常...异常信息:{}", TRpcConstants.LOG_CLIENT_MSG_PREFIX, cause.getMessage());
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.debug("{}响应读取完毕...", TRpcConstants.LOG_CLIENT_MSG_PREFIX);
	}
}

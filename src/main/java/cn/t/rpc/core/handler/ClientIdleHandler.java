package cn.t.rpc.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.data.TIdle;
import cn.t.rpc.core.data.TRpcConstants;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 客户端心跳检测处理器
 * @author TMQ
 *
 */
@Sharable
public class ClientIdleHandler extends SimpleChannelInboundHandler<TIdle> {

	private static final Logger logger = LoggerFactory.getLogger(ClientIdleHandler.class);
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TIdle msg) throws Exception {
		if (TRpcConstants.IDLE_MESSAGE.equals(msg.getMsg())) {
			logger.debug("{}收到服务器心跳信息...", TRpcConstants.LOG_CLIENT_MSG_PREFIX);
			// 发送响应给服务器,提示存活
			ctx.channel().writeAndFlush(new TIdle());
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("{}服务器心跳异常,连接关闭...异常信息:{}", TRpcConstants.LOG_CLIENT_MSG_PREFIX, cause.getMessage());
		ctx.close();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{}客户端断开连接{}...", TRpcConstants.LOG_CLIENT_MSG_PREFIX, ctx.channel().remoteAddress());
		// 当前通道关闭
		ctx.channel().close();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{}建立客户端连接{}...", TRpcConstants.LOG_CLIENT_MSG_PREFIX, ctx.channel().remoteAddress());
	}
}

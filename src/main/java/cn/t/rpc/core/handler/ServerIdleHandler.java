package cn.t.rpc.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.data.TIdle;
import cn.t.rpc.core.data.TRpcConstants;
import cn.t.rpc.core.server.holder.Connection;
import cn.t.rpc.core.server.holder.TConnectionHolderContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 服务器心跳检测处理器
 * 
 * @author TMQ
 *
 */
@Sharable
public class ServerIdleHandler extends SimpleChannelInboundHandler<TIdle> {

	private static final Logger logger = LoggerFactory.getLogger(ServerIdleHandler.class);
	
	/**
	 * 心跳检测次数
	 */
	private int idleCnt = 5;
	
	public ServerIdleHandler(int idleCnt) {
		this.idleCnt = idleCnt;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TIdle msg) throws Exception {
		if (TRpcConstants.IDLE_MESSAGE.equals(msg.getMsg())) {
			logger.debug("{}客户端({})心跳信息=======>存活...", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("{}客户端({})心跳异常,连接关闭...", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
		logger.error("异常信息：" + cause.getMessage());
		this.closeChannel(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{}客户端{}接入连接...", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
		// 连接信息保存
		TConnectionHolderContext.addConnection(ctx.channel().id().asLongText(), new Connection(ctx.channel()));
		// 将事件传递,如果不传递,本次事件在这里就终止了
		// ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{}客户端({})断开连接,连接关闭...", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
		this.closeChannel(ctx);
		// 直接关闭当前连接的整个上下文
		// ctx.close();
	}

	/**
	 * 心跳检测handle将会发送事件,在此处触发<br>
	 * 服务器60S没有读事件,将会触发此方法,主动探测连接
	 * 
	 * @param ctx 通道上下文
	 * @param evt 事件对象
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			// 判断读事件心跳
			if (IdleState.READER_IDLE.equals((event.state()))) {
				
				int idleCnt = TConnectionHolderContext.getIdleCnt(ctx.channel().id().asLongText());
				
				// 5分钟没有实际通信则关闭连接
				if (idleCnt >= this.idleCnt) {
					logger.warn("{}客户端({})长时间未传输业务数据...关闭连接", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
					this.closeChannel(ctx);
				} else {
					TConnectionHolderContext.incrementIdle(ctx.channel().id().asLongText());
					// 发送心跳消息
					logger.debug("{}心跳信息=======>客户端({})", TRpcConstants.LOG_SERVER_MSG_PREFIX, ctx.channel().remoteAddress());
					ctx.channel().writeAndFlush(new TIdle());
				}
			}
		}
		// 事件继续传递
		// super.userEventTriggered(ctx, evt);
	}

	/**
	 * 关闭连接
	 * 
	 * @param ctx
	 */
	private void closeChannel(ChannelHandlerContext ctx) {
		// 移除连接
		TConnectionHolderContext.removeConnection(ctx.channel().id().asLongText());
		// 当前通道关闭
		ctx.channel().close();
	}
}

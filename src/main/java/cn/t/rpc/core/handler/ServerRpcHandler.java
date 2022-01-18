package cn.t.rpc.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.data.TRequest;
import cn.t.rpc.core.data.TResponse;
import cn.t.rpc.core.data.TRpcConstants;
import cn.t.rpc.core.server.holder.TConnectionHolderContext;
import cn.t.rpc.core.server.holder.TLocalServiceHolderContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 服务器核心处理器
 * @author TMQ
 *
 */
@Sharable // 允许次handler多次读写,表明这是个线程安全的单例
public class ServerRpcHandler extends SimpleChannelInboundHandler<TrafficData> {

	private static final Logger logger = LoggerFactory.getLogger(ServerRpcHandler.class);
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TrafficData msg) throws Exception {
		logger.debug("{}读取请求信息...", TRpcConstants.LOG_SERVER_MSG_PREFIX);
		
		TResponse res = new TResponse();
		res.setId(msg.getId());
		try {
			
			if (msg.getType() == null || !msg.getType().isRequest()) {
				logger.warn("{}数据类型异常...本次通信终止...", TRpcConstants.LOG_SERVER_MSG_PREFIX);
				return;
			}
			
			// 重置超时
			TConnectionHolderContext.resetIdle(ctx.channel().id().asLongText());
			
			// 数据转换
			TRequest req = msg.convert();
			logger.debug("{}请求内容:{}", TRpcConstants.LOG_SERVER_MSG_PREFIX, req);
			
			// 输出响应
			Object result = TLocalServiceHolderContext.invoke(req.getClsNm(), req.getMethod(), req.getParams());
			res.setBody(result);
			res.setStatus(HttpStatus.OK.value());
			// 将会进入编码器编码
			// ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
			// 使用ctx进行写会直接找到最近的一个outbound 可能会出现顺序问题
			// 使用channel写则数据总是在尾部的out流到第一个out
			ctx.channel().writeAndFlush(res);
		} catch(Exception e) {
			logger.error("{}请求处理异常...异常信息:{}", TRpcConstants.LOG_SERVER_MSG_PREFIX, e.getMessage());
			res.setMsg(e.getMessage());
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			ctx.channel().writeAndFlush(res);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("{}本次请求进入异常回调方法...异常信息:{}", TRpcConstants.LOG_SERVER_MSG_PREFIX, cause.getMessage());
	}



	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.debug("{}请求读取完毕...", TRpcConstants.LOG_SERVER_MSG_PREFIX);
		// 关闭连接
		// ctx.close();
	}
}

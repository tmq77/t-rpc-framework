package cn.t.rpc.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * DISCARD protocol<br>
 * DISCARD服务(丢弃服务，指的是会忽略所有接收的数据的一种协议)<br>
 * 
 * @author TMQ
 *
 */
@Sharable // 允许次handler多次读写
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(DiscardServerHandler.class);
	
	

	/**
	 * 连接建立后触发
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 单次请求，长连接的情况下才能看到输出
		// ctx.writeAndFlush("Welcome!");
		ctx.channel().writeAndFlush("welcome");
	}

	/**
	 * This method is called with the received message, whenever new data is
	 * received from a client<br>
	 * the type of the received message is ByteBuf.<br>
	 * ByteBuf is a reference-counted object which has to be released explicitly via
	 * the release() method<br>
	 * 消息被通道读取后执行
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		// 直接将消息释放 释放一次则ByteBuf的引用减一，引用为0再释放则抛出IllegalReferenceCountException异常
		// ((ByteBuf) msg).release();
		try {
			ByteBuf in = (ByteBuf) msg;
			logger.info(in.toString(CharsetUtil.UTF_8));
			
		} finally {
			// Please keep in mind that it is the handler's responsibility to release any reference-counted object passed to the handler
			// ByteBuf是一个引用计数对象，需要在handler中释放到达的消息对象
			ReferenceCountUtil.release(msg);
			ctx.close();
		}
	}

	/**
	 * 出现异常时调用此钩子方法，通常出现异常后通道连接应该被关闭
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
	}

}

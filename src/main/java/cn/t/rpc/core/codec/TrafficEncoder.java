package cn.t.rpc.core.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.codec.strategy.SerializationStrategy;
import cn.t.rpc.core.data.TRpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

/**
 * 自定义AbstractTrafficData类编码器
 * 
 * @author TMQ
 *
 */
public class TrafficEncoder extends MessageToByteEncoder<TrafficData> {

	private static final Logger logger = LoggerFactory.getLogger(TrafficEncoder.class);
	
	/**
	 * 序列化工具类
	 */
	private SerializationStrategy strategy;
	
	public TrafficEncoder(SerializationStrategy strategy) {
		this.strategy = strategy;
	}

	/**
	 * 注意客户端无间隔发送消息时，会出现沾包问题(举例，客户端在建立连接后的事件方法中立马发送消息，同时另一线程利用同一个channel也发送消息，两次消息就会发生沾包)
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, TrafficData msg, ByteBuf out) throws Exception {
		
		try {
			
			if (msg.getType() == null) {
				// 数据非法，中断连接
				logger.error("TrafficEncoder: 传输的数据不合法...");
				ctx.close();
				return;
			}
			
			byte[] data = this.strategy.serialize(msg);

			int len = data.length;
			// 头部写入本次传输的报文长度 int占4字节,写入后index向后移动4位
			if (len == 0) {
				// 数据非法，中断连接
				logger.error("TrafficEncoder: 传输的数据不合法...");
				ctx.close();
				return;
			}
			
			// 写入序列化类型-占类型字符串对应的byte数组长度
			out.writeBytes(TRpcConstants.RPC_TRAFFIC_TYPE.getBytes(CharsetUtil.UTF_8)); 
			// 写入长度-占4位
			out.writeInt(len);
			// 写入类型-占4位
			out.writeInt(msg.getType().value());
			// 写入数据
			out.writeBytes(data);
			// 写入终止符
			out.writeBytes(TRpcConstants.RPC_TRAFFIC_EOF.getBytes(CharsetUtil.UTF_8));
			
		} catch(Exception e) {
			logger.error("TrafficEncoder: 编码异常...本次连接关闭");
			logger.error(e.getMessage());
			ctx.close();
		}
	}

}

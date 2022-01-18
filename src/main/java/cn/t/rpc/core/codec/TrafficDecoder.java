package cn.t.rpc.core.codec;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.TrafficData;
import cn.t.rpc.core.TrafficType;
import cn.t.rpc.core.codec.strategy.SerializationStrategy;
import cn.t.rpc.core.data.TRpcConstants;
import cn.t.rpc.core.data.TrafficTypeHelper;
import cn.t.rpc.utils.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

/**
 * 自定义AbstractTrafficData类解码器
 * 
 * @author TMQ
 *
 */
public class TrafficDecoder extends ByteToMessageDecoder {

	private static final Logger logger = LoggerFactory.getLogger(TrafficDecoder.class);

	/**
	 * 序列化工具类
	 */
	private SerializationStrategy strategy;
	
	public TrafficDecoder(SerializationStrategy strategy) {
		this.strategy = strategy;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		try {

			// =======================报文结构==============================
			// 1 ~ N : 报文的数据类型(本解码器只解码对应的类型)
			// N + 1 ~ N + 4 : 报文有效数据的长度(int型 占4位)
			// N + 5 ~ N + 8 : 报文的请求类型(Request还是Response)(int型 占4位)
			// N + 6 ~ M : 有效数据
			// M ~ X : 终止符
			// =============================================================

			// 1.判断长度
			if (in.readableBytes() <= TRpcConstants.RPC_TRAFFIC_VALID_LEN) {
				// 不是约定的合法报文
				logger.warn("TrafficDecoder: 传输的数据不合法...数据长度不正确...移交后续解码器处理...");
				// 中止处理,触发后续管道操作
				ctx.fireChannelRead(in);
				return;
			}

			// 在前期check中非法的case下resetReaderIndex后再return,数据没读完之前会一直进入decode方法,每次都会被return,不影响正常逻辑
			// 2.判断类型
			// 读取序列化类型字节数组
			byte[] types = new byte[TRpcConstants.RPC_TRAFFIC_TYPE_LEN];
			try {
				in.readBytes(types);
			} catch (IndexOutOfBoundsException e) {
				// 数组越界说明不是合法的数据类型了
				logger.debug("TrafficDecoder: 类型不正确...移交后续解码器处理...");
				// 重置读取的index避免影响后续解码器
				in.resetReaderIndex();
				ctx.fireChannelRead(in);
				return;
			}
			if (!TRpcConstants.RPC_TRAFFIC_TYPE.equals(new String(types, CharsetUtil.UTF_8))) {
				logger.debug("TrafficDecoder: 类型不正确...移交后续解码器处理...");
				// 重置读取的index避免影响后续解码器
				in.resetReaderIndex();
				ctx.fireChannelRead(in);
				return;
			}

			// 读取报文的数据长度，读一次int会前进4个index
			int len = in.readInt();
			if (len <= 0) {
				// 不是约定的合法报文
				logger.error("TrafficDecoder: 传输的数据不合法...数据长度不正确...");
				// 直接关闭连接,不再接收,简单防止攻击
				ctx.close();
				return;
			}

			// 读取类型-前进4个index 读完类型后,剩下的就是实际数据和终止符
			int type = in.readInt();

			// 剩余的数据量长度(数据长度加上终止符长度)
			int validLen = len + TRpcConstants.RPC_TRAFFIC_EOF_LEN;
			if (in.readableBytes() < validLen) {
				// 此时有可能数据量大，还未读完，直接重置读取位置,等待下一次读取
				// 重置读取的index
				in.resetReaderIndex();
			} else if (in.readableBytes() > validLen) {
				// 进入这个case中有两种情况
				// 1: 业务设计ok的情况下,进入此分支则出现了沾包问题(两次消息利用同一个channel无间隔发送)
				// 2: 数据被篡改了

				// =====================
				// =======进行拆包======
				// =====================
				byte[] body = new byte[len];
				// 将可读缓冲区有效业务部分字节数据读入临时数组
				in.readBytes(body);
				// 判断之后是否是终止符
				byte[] eof = new byte[TRpcConstants.RPC_TRAFFIC_EOF_LEN];
				in.readBytes(eof);
				// 判断是否是终止符
				if (TRpcConstants.RPC_TRAFFIC_EOF.equals(new String(eof, CharsetUtil.UTF_8))) {
					// 保存当前读取位置,此处拆包完成
					TrafficData traffic = JsonUtil.convertToObject(body, TrafficTypeHelper.getTrafficClassType(type));
					out.add(traffic);
					logger.debug("TrafficDecoder: 数据流(拆包)读取完毕...长度:" + len + " ...类型:"
							+ TrafficType.findName(type));
					// 保存当前的readerIndex位置(下次resetReaderIndex会回到这个位置),读取下一个包的数据
					in.markReaderIndex();
				} else {
					logger.error("TrafficDecoder: 传输的数据不合法...数据被篡改...");
					// 直接关闭连接,不再接收
					ctx.close();
				}
			} else {
				// netty 提供了ReplayingDecoder解码器 可以做到本类中重复调用decode方法(性能较低)
				logger.debug("TrafficDecoder: 数据流读取完毕...长度:" + len + " ...类型:" + TrafficType.findName(type));

				byte[] body = new byte[len];
				// 将剩余的缓冲区字节数据读入临时数组
				in.readBytes(body);
				// 将终止符读完(注意,netty中in.readBytes传入的参数为int类型时会生成新的ByteBuf,此时需要释放这个新的ByteBuf,否则会内存泄露)
				// ReferenceCountUtil.release(in.readBytes(TRpcConstants.RPC_TRAFFIC_EOF_LEN));
				in.readBytes(new byte[TRpcConstants.RPC_TRAFFIC_EOF_LEN]);
				
				TrafficData traffic = this.strategy.deserialize(body, TrafficTypeHelper.getTrafficClassType(type));
				out.add(traffic);
				
				// 保存当前的readerIndex位置(下次resetReaderIndex会回到这个位置)(本次有可能是沾包数据,但是刚好一半一半)
				in.markReaderIndex();
			}
			
		} catch (Exception e) {
			logger.error("TrafficDecoder: 解码异常...本次连接关闭...异常信息{}", e.getMessage());
			// 直接关闭连接
			ctx.close();
		}
	}
}

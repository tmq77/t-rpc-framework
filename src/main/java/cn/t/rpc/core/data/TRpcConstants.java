package cn.t.rpc.core.data;

import io.netty.util.CharsetUtil;

/**
 * 常量类
 * @author TMQ
 *
 */
public class TRpcConstants {
	
	private TRpcConstants() throws IllegalAccessException { throw new IllegalAccessException("UTIL CLASS"); }
	
	/**
	 * LOG前缀
	 */
	public static final String LOG_SERVER_MSG_PREFIX = "服务器信息:";
	
	/**
	 * LOG前缀
	 */
	public static final String LOG_CLIENT_MSG_PREFIX = "客户端信息:";
	
	/**
	 * 心跳检测消息
	 */
	public static final String IDLE_MESSAGE = "t-rpc-idle";
	
	/**
	 * 数据格式约定,编码时头部会写入此值,解码时根据此值判断是否进行解码<br>
	 * 解决沾包问题
	 */
	public static final String RPC_TRAFFIC_TYPE = "traffic";
	
	/**
	 * 数据格式约定,数据包终止标识<br>
	 * 解决沾包问题
	 */
	public static final String RPC_TRAFFIC_EOF = "EOF";
	
	/**
	 * 约定的类型值字节数组长度(数据类型符)
	 */
	public static final int RPC_TRAFFIC_TYPE_LEN = TRpcConstants.RPC_TRAFFIC_TYPE.getBytes(CharsetUtil.UTF_8).length;
	
	/**
	 * 约定的类型值字节数组长度(数据包终止符)
	 */
	public static final int RPC_TRAFFIC_EOF_LEN = TRpcConstants.RPC_TRAFFIC_EOF.getBytes(CharsetUtil.UTF_8).length;
	
	/**
	 * 报文头部有类型值的长度 和 4字节的int值(有效数据长度) 以及报文末尾的终止符长度 ===> 总长度大于此值才算合法
	 */
	public static final int RPC_TRAFFIC_VALID_LEN = RPC_TRAFFIC_TYPE_LEN + 4 + RPC_TRAFFIC_EOF_LEN;
}

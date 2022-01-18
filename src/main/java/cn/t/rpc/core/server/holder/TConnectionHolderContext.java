package cn.t.rpc.core.server.holder;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接信息holder类，保存了接入服务器的连接通道信息，主要为心跳检测使用。
 * @author TMQ
 *
 */
public final class TConnectionHolderContext {
	
	private static ConcurrentHashMap<String, Connection> context = instance();
	
	private static ConcurrentHashMap<String, Connection> instance() {
		if (context == null) {
			synchronized(TConnectionHolderContext.class) {
				if (context == null) {
					context = new ConcurrentHashMap<String, Connection>(32);
				}
			}
			
		}
		return context;
	}
	
	/**
	 * 设置连接信息
	 * @param channelId 通道唯一ID
	 * @param conn 客户端连接
	 */
	public static void addConnection(String channelId, Connection conn) {
		context.put(channelId, conn);
	}
	
	
	/**
	 * 获取心跳超时次数
	 * @param channelId 通道唯一ID
	 * @return 心跳超时次数(-1: 没有此连接信息)
	 */
	public static int getIdleCnt(String channelId) {
		Connection conn = context.get(channelId);
		return conn == null ? -1 : conn.getIdle();
	}
	
	/**
	 * 超时次数增加
	 * @param channelId 通道唯一ID
	 */
	public static void incrementIdle(String channelId) {
		Connection conn = context.get(channelId);
		if (conn != null) {
			conn.incrementIdle();
		}
	}
	
	/**
	 * 重置超时次数
	 * @param channelId 通道唯一ID
	 */
	public static void resetIdle(String channelId) {
		Connection conn = context.get(channelId);
		if (conn != null) {
			conn.resetIdle();
		}
	}
	
	/**
	 * 移除连接
	 * @param channelId 连接信息唯一ID
	 * @return 移除的连接
	 */
	public static Connection removeConnection(String channelId) {
		return context.remove(channelId);
	}

}

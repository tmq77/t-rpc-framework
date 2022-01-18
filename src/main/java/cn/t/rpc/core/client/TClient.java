package cn.t.rpc.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.TrafficRunner;
import cn.t.rpc.core.client.pool.TClientChannelPool;
import cn.t.rpc.core.client.pool.TRpcRemoteAddress;
import cn.t.rpc.core.codec.TrafficDecoder;
import cn.t.rpc.core.codec.TrafficEncoder;
import cn.t.rpc.core.exception.TRpcOperationException;
import cn.t.rpc.core.handler.ClientIdleHandler;
import cn.t.rpc.core.handler.ClientRpcHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * RPC连接客户端
 * 
 * @author TMQ
 * @deprecated 推荐使用连接池进行连接 {@link TClientChannelPool}
 *
 */
@Deprecated
public class TClient extends TrafficRunner {

	private static final Logger logger = LoggerFactory.getLogger(TClient.class);

	/**
	 * 配置类
	 */
	private TRpcConfigProperties config;

	/**
	 * 远程服务器信息
	 */
	private TRpcRemoteAddress address;

	/**
	 * 客户端线程池
	 */
	private EventLoopGroup workerGroup;

	/**
	 * 客户端启动辅助类
	 */
	private Bootstrap bootstrap;

	/**
	 * 默认初始化
	 */
	public TClient() {
		this.config = new TRpcConfigProperties();
		this.address = new TRpcRemoteAddress();
		this.init();
	}

	/**
	 * 默认初始化
	 */
	public TClient(TRpcConfigProperties config, TRpcRemoteAddress address) {
		this.config = config;
		this.address = address;
		this.init();
	}

	/**
	 * 初始化辅助类和线程池
	 */
	private void init() {
		this.workerGroup = new NioEventLoopGroup();
		this.bootstrap = new Bootstrap();
		// 配置
		this.bootstrap.group(this.workerGroup).channel(NioSocketChannel.class) // 客户端的channel与服务器不一样
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {

						ch.pipeline().addLast(new TrafficDecoder(config.getSerializationStrategy()));
						ch.pipeline().addLast(new TrafficEncoder(config.getSerializationStrategy()));
						ch.pipeline().addLast(new ClientIdleHandler());
						ch.pipeline().addLast(ClientRpcHandler.HANDLER_NAME,
								new ClientRpcHandler(config.getRequestTimeout()));
					}
				}).option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getConnectionTimeout()); // 连接时的超时设置-网络正常情况下超时设置将无效(只有网络不通的时候才生效,比如137.0.0.1则会超时)
	}

	/**
	 * 同步执行
	 */
	@Override
	public void start() {

		if (super.isRunning()) {
			logger.warn("客户端已在运行中...");
			throw new TRpcOperationException("客户端已在运行中...无法再次启动...");
		}

		// 重试机制
		int retryCnt = 0;

		// 连接
		ChannelFuture f = this.connect(this.bootstrap);

		// 失败重连
		while (f == null && retryCnt < this.config.getRetryCnt()) {
			logger.warn("开始尝试重连...第{}次重连...", ++retryCnt);
			f = this.connect(this.bootstrap);
		}

		if (f != null) {
			// 持有当前的channel通道
			super.channel = f.channel();
			// 设置状态-运行
			super.statusUp();
			// 等待连接关闭 - sync() 阻塞线程（可被中断） - syncUninterruptibly() 阻塞线程（不可被中断）
			// f.channel().closeFuture().syncUninterruptibly();
			// 连接完成后线程不再阻塞
		} else {
			logger.warn("客户端未能连接到服务器...");
		}
	}

	/**
	 * 关闭整个客户端,无法再重启
	 */
	@Override
	public void close() {
		super.close();
		this.workerGroup.shutdownGracefully();
		this.bootstrap = null;
		this.workerGroup = null;
		logger.info("客户端关闭...");
		// 设置状态-关闭
		super.statusDown();
	}

	/**
	 * 客户端连接处理
	 * 
	 * @param b 启动器辅助类
	 * @throws InterruptedException
	 */
	private ChannelFuture connect(Bootstrap b) {

		try {
			logger.info("客户端正在尝试连接...");
			ChannelFuture f = b.connect(this.address.getRemoteAddress()).sync();

			f.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						logger.info("客户端启动完成...已经连接到[" + address.getRemoteAddress().getHostName() + ":" + address.getRemoteAddress().getPort() + "]");
					}
				}
			});

			return f;
		} catch (Exception e) {
			logger.error("客户端连接异常:{}", e.getMessage());
			return null;
		}
	}
}

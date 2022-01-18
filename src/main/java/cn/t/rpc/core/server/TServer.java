package cn.t.rpc.core.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.config.TRpcConfigProperties;
import cn.t.rpc.core.TrafficRunner;
import cn.t.rpc.core.codec.TrafficDecoder;
import cn.t.rpc.core.codec.TrafficEncoder;
import cn.t.rpc.core.handler.ServerIdleHandler;
import cn.t.rpc.core.handler.ServerRpcHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class TServer extends TrafficRunner {

	private static final Logger logger = LoggerFactory.getLogger(TServer.class);
	
	/**
	 * 配置类
	 */
	private TRpcConfigProperties config;

	public TServer() {
		this.config = new TRpcConfigProperties();
	}

	public TServer(TRpcConfigProperties config) {
		this.config = config;
	}

	/**
	 * 启动服务器
	 */
	@Override
	public void start() {
		new Thread(() -> {
			logger.info("服务器启动中...");
			// NioEventLoopGroup是一个线程池,线程数可以通过构造方法配置
			// 处理接入的连接并分发到worker中
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			// 处理被分发的接入连接
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				// 服务启动辅助类
				ServerBootstrap b = new ServerBootstrap();
				b.group(bossGroup, workerGroup) // 组合分发器和工作器
						.channel(NioServerSocketChannel.class) // 指定通道类型(监听新进来的TCP连接的通道)
						.handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(
								
								new ChannelInitializer<SocketChannel>() {
									@Override
									protected void initChannel(SocketChannel ch) throws Exception {
										// 加载注册自定义handler
										// handlerList.add(new DiscardServerHandler());
										// inbound会根据添加的顺序执行
										// outbound会根据添加的逆序执行
										ChannelPipeline p = ch.pipeline();
										
										// 添加心跳检测处理器(此处理器继承了inout handler) 此处理器需要放在重写了userEventTrigger的handler前面,内部会调用后续的handler处理
										// 参数说明: 1:多久检测一次读事件(channelRead), 2: 写事件, 3:读写事件
										// 设置服务器端每60秒检测一次读事件-60s过后没有读事件则触发userEventTrigger()方法
										p.addLast(new IdleStateHandler(config.getIdleInterval(), 0, 0, TimeUnit.SECONDS));
										
										// 核心解码器- in
										p.addLast(new TrafficDecoder(config.getSerializationStrategy()));
										// 核心编码器- out
										p.addLast(new TrafficEncoder(config.getSerializationStrategy()));
										p.addLast(new ServerIdleHandler(config.getIdleCnt()));
										
										p.addLast(new ServerRpcHandler());
									}

								}) // 配置自定义入站出站handler
						.option(ChannelOption.SO_BACKLOG, 512) // 连接队列的大小
						.childOption(ChannelOption.TCP_NODELAY, true) // 关闭Nagle算法,尽可能实时发送数据而不是等待尽量大的数据量一次发送
						.childOption(ChannelOption.SO_KEEPALIVE, true); // 心跳检测(客户端长时间不发送数据时,发送小数据包探测是否存活，不存活则关闭当前连接)
				// 内存泄露辅助(获取详细日志信息)
				// ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
				// Bind and start to accept incoming connections.
				// 保持阻塞直到连接完成/失败
				ChannelFuture f = b.bind(this.config.getPort()).sync();
				logger.info("服务器启动完成...当前运行在[" + this.config.getPort() + "]端口");

				// 持有channel
				super.channel = f.channel();

				super.statusUp();

				// Wait until the server socket is closed.
				// In this example, this does not happen, but you can do that to gracefully
				// shut down your server.
				f.channel().closeFuture().sync();
			} catch (Exception e) {
				logger.error("服务器异常，即将关闭...");
				logger.error("异常信息：" + e.getMessage());
			} finally {
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
				logger.info("服务器关闭...");
				super.statusDown();
			}
		}).start();
	}

//	/**
//	 * 加载handler
//	 * 
//	 * @return
//	 */
//	private TServerInitializer<SocketChannel> loadHandlers() {
//		List<ChannelHandler> handlerList = new ArrayList<>();
//		// 心跳检测
//		handlerList.add(new ServerIdleHandler());
//		// 核心处理
//		handlerList.add(new ServerRpcHandler());
//		TServerInitializer<SocketChannel> inboundConfig = new TServerInitializer<>(handlerList);
//		return inboundConfig;
//	}
}

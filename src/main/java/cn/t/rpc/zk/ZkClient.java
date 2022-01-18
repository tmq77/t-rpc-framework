package cn.t.rpc.zk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.t.rpc.core.client.pool.TClientChannelPool;
import cn.t.rpc.discovery.TRpcDiscoveryStrategy;

/**
 * zookeeper客户端
 * 
 * @author Administrator
 *
 */
public class ZkClient implements Watcher{

	private static final Logger logger = LoggerFactory.getLogger(ZkClient.class);

	/**
	 * RPC服务信息根节点路径
	 */
	private static final String ROOT_PATH = "/RPC-ROOT";

	/**
	 * 会话超时时间(ms)
	 */
	private static final int SESSION_TIMEOUT = 2000;

	/**
	 * 连接超时时间
	 */
	private static final int CONNECTION_TIMEOUT = 5000;
	
	/**
	 * 线程同步器
	 */
	private final CountDownLatch cdl = new CountDownLatch(1);

	/**
	 * 客户端
	 */
	private ZooKeeper client;

	/**
	 * 服务发现策略
	 */
	private TRpcDiscoveryStrategy strategy;
	
	/**
	 * 连接信息
	 */
	private String connectionString;
	
	/**
	 * 自身的节点位置
	 */
	private String selfNode;
	

	public ZkClient(String connectionString, TRpcDiscoveryStrategy strategy) throws Exception {
		// 服务发现策略
		this.strategy = strategy;
		this.connectionString = connectionString;

		// 连接地址|会话超时时间|监听
		ZooKeeper zk = new ZooKeeper(connectionString, SESSION_TIMEOUT, this);
		this.cdl.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
		this.client = zk;
		// 初始化根节点
		this.initRpcRootNode();
	}

	/**
	 * 创建临时节点,注册本地服务的地址到zookeeper
	 * 
	 * @param path 节点路径(服务ID)
	 * @param data 节点信息(ip和端口: 127.0.0.1:8080)
	 */
	public void register(String path, String data) {
		try {
			String nodePath = ROOT_PATH + "/" + path;
			// 创建临时节点
			this.selfNode = this.client.create(nodePath, (path + "=" + data).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			logger.info("{}已注册到注册中心...", this.selfNode);
		} catch (KeeperException | InterruptedException e) {
			logger.error("注册服务时出现错误...{}", e.getMessage());
		}
	}

	/**
	 * 获取节点信息列表
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public List<String> getNodeList() {
		// 第二个参数使用true会自动调用初始化时指定的watcher
		// 子节点地址列表
		try {
			List<String> childNodeList = this.client.getChildren(ROOT_PATH, true);
			List<String> resultList = new ArrayList<>();
			childNodeList.forEach(child -> {
				if (!(ROOT_PATH + "/" + child).equals(this.selfNode)) {
					try {
						resultList.add(new String(client.getData(ROOT_PATH + "/" + child, false, null)));
					} catch (KeeperException | InterruptedException e) {
						logger.error("获取子节点{}信息失败...", ROOT_PATH + "/" + child);
					}
				}
			});
			return resultList;
		} catch (KeeperException | InterruptedException e) {
			logger.error("获取服务列表失败...{}", e.getMessage());
		}
		return new ArrayList<>();
	}
	
	@Override
	public void process(WatchedEvent event) {
		if (Event.EventType.None.equals(event.getType())) {
			// 非数据变更事件
			switch (event.getState()) {
			case SyncConnected:
				logger.info("zookeeper连接完成...");
				this.cdl.countDown();
				break;
			case Expired:
				logger.warn("zookeeper连接超时...");
				try {
					this.client = new ZooKeeper(connectionString, SESSION_TIMEOUT, this);
				} catch (IOException e) {
					logger.error("超时重连失败...{}", e.getMessage());
				}
			default:
				break;
			}
		} else {
			switch (event.getType()) {
			case NodeChildrenChanged:
				logger.info("zookeeper节点发生变化...");
				// 刷新RPC连接池
				reloadServerInfo();
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * 初始化一次根节点
	 * 
	 * @throws Exception
	 */
	private void initRpcRootNode() throws Exception {
		try {
			// 根节点未创建时
			if (this.client.exists(ROOT_PATH, false) == null) {
				logger.warn("RPC服务信息根节点未创建...");
				// zk的节点只能创建一个,并发下只有一个会创建成功
				try {
					String node = this.client.create(ROOT_PATH, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
							CreateMode.PERSISTENT);
					logger.info("RPC服务信息根节点{}已创建...", node);
				} catch (KeeperException | InterruptedException e) {
					logger.warn("创建根节点失败...根节点已被创建? 警告信息:{}", e.getMessage());
				}
			} else {
				logger.info("RPC服务信息根节点已找到...");
			}
		} catch (KeeperException | InterruptedException e) {
			logger.error("初始化RPC根节点失败...{}", e.getMessage());
			throw e;
		}
	}
	
	/**
	 * 重新刷新信息并绑定事件
	 */
	private void reloadServerInfo() {
		TClientChannelPool.instance().refreshPool(this.strategy.find());
	}
}

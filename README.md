# `t-rpc-framework`

GITHUB上的代码可能不是最新，
最新版请查看[t-rpc-framework](https://gitee.com/tmq777/t-rpc-framework)

## 开发环境

1. `jdk 11.0.6`
2. `springboot 2.6.2`
3. `netty-all 4.1.72.Final`
4. `zookeeper 3.7.0`

---

## 快速开始

### 框架可设置属性参考(`application.yml`)

```yaml
rpc:
  service:
    serverId: app # 分布式服务ID(多实例时应该指定同一个ID)
    exposedService: true # 是否作为服务提供者启动(设置为false将只会初始化客户端,不能提供服务)
    port: 18081 # 对外提供服务的端口
    connectionTimeout: 5000 # 客户端连接的超时时间(毫秒)
    requestTimeout: 2000 # 服务器响应生成到客户端接收到的时间差值阈值,每次请求的超时时间(毫秒)
    retryCnt: 5 # 连接池取不到连接时,标记一次失败,失败次数达到指定值时,连接池将会被标记为不可用
    idleCnt: 5 # 心跳检测次数(达到该次数将会断开通道(重新请求时会重新建立通道))
    idleInterval: 60 # 心跳检测时间间隔(秒)
    maxPoolConnection: 5 # 客户端单个连接池最大通道数 
    reconnectionTime: 30 # 连接池不可用后进行重连的最大时间(分)。不设置或者为-1时默认无限重试(至少会重试一次,所以设置30,实际上会重连31分钟), 尽量不要设置无限重试
    zkCenter: 127.0.0.1:2181 # 如果需要使用zookeeper,在此指定ip并且将框架自带的zk发现策略注册为bean
```

### 引入工程并启动

1. 代码`clone`下来之后执行`maven`的`clean install`打包，然后引入自己的工程。[配置框架](#框架可设置属性参考(`application.yml`))后即可使用。

   ```xml
   <dependency>
   	<groupId>cn.t.rpc</groupId>
   	<artifactId>t-rpc-spring-boot-starter</artifactId>
   	<version>1.0.0</version>
   </dependency>
   ```

2. ##### 在`SpringBoot`启动类或者任意`@Configuration`标识的配置类上添加`@TRpcServiceScanner`注解，同时指定远程服务的接口包地址，如下：

   ```java
   // 例如
   @TRpcServiceScanner(basePackages = {"cn.t.rpc"})
   public class XXXConfiguration {
       // ...
   }
   ```

3. ##### 在上述指定包路径下(支持多层扫描)创建需要调用的远程服务接口，同时指定`@TRpcRemoteService`注解，如下：

   ```JAVA
   @TRpcRemoteService(serverId = "app", className = "ServerTimeService")
   public interface ServerTimeService {
       // 接口中的方法需要和远程服务的方法名一致
   	String getServerTime(String user);
   }
   ```

   > 说明: 注解中的`serverId`为必须值，该值为远程服务的服务ID，即基础值配置中的`rpc.service.serverId`的值；`className`非必须，该值未设置时使用当前接口名作为值传递。
   >
   > 注意：如果未设置`className`，那么远程服务(下文说明)的`className`属性也必须为当前接口名。

4. ##### 如果本机需要发布服务，那么`rpc.service.exposedService`值需要设置为`true`。同时在需要发布为服务的类上标识`@TRpcLocalService`注解，如下：

   ```java
   @TRpcLocalService(className = "ServerTimeService")
   public class ServerTimeService {
   
   private static final String FORMAT = "yyyy年MM月dd日 hh时mm分ss秒";
   	/**
   	 * 获取当前服务器的时间
   	 * @param user
   	 * @return 服务时间
   	 */
   	public String getServerTime(String user) {
   		return "Hello!" + user + "现在是:" + new SimpleDateFormat(FORMAT).format(new Date());
   	}
   }
   ```

   > 说明：`@TRpcLocalService`标识的类需要为具体的类而不是接口。注解中的`className`属性为非必须，默认为空时，使用当前类名作为实际值，客户端调用时指定对应的值即可

5. ##### 使用zookeeper作为注册中心时，设置`zkCenter`属性即可，否则会默认使用`properties`文件作为服务信息来源,文件格式参照[客户端服务发现策略](#客户端服务发现策略)。

6. ##### 客户端在需要调用的地方注入接口类型，然后正常使用即可。

   ```java
   @RestController
   @RequestMapping("/t-rpc")
   public class WebController {
   	// 和普通Bean一样注入
   	@Autowired
   	private ServerTimeService test;
   	// 和普通Bean一样使用
   	@RequestMapping("/test")
   	public String index() {
   		return test.getServerTime("TMQ");
   	}
   }
   
   ```



---

## 核心点介绍

基于`netty`的`rpc`框架，基于注解生成接口的代理类，进行远程RPC调用。

1. 搭配`SpringBoot`启用，启动时会自动将标识了`@TRpcRemoteService(serverId= "serverId")`的**接口**生成代理类，在**需要的位置依赖注入**即可。

2. 客户端使用`ChannelPoolMap`关系型连接池，一个连接池对应一个服务器。单个连接池的连接数**可配置**。相同服务ID的连接池会被聚合，调用时会进行**随机轮询**。

3. 每个连接池获取通道时提供了循环重试的功能，**配置的重试次数**超过指定值后会开启连接池重连机制。

4. 连接池开始重连后会触发至少一次的重连服务器的操作，可配置重连的**最大上限时间**。默认30分钟(间隔一分钟)。重连超过最大上限时间后连接池将会进入`Down`状态。进入`重连`和`Down`的连接池将不会被使用。

5. 连接池`DOWN`掉之后会关闭所占用的资源，但是上下文中还是会存在这个对象，连接池辅助类中在初始化时会开启定时任务，30分钟后开始，每30分钟清除一次`DOWN`掉的连接池，同时清除操作也会在**刷新池子**的操作中完成。

6. 客户端的`socket`连接为**长连接**，连接可配置**心跳检测时间**，心跳超时则会关闭通道。

7. 使用策略模式指定**序列化方式**(目前默认使用`jackson`的`Json`序列化方式)。

8. 使用策略模式指定**服务发现方式**(已提供`zookeeper`的服务发现策略，需要手动注册`bean`。基于`zookeeper`的策略已经实现了服务动态刷新功能)。

9. 根据约定的规则进行数据传输，解决沾包拆包问题。

10. 客户端使用`ConcurrentHashMap`缓存响应结果，根据本次通信的`key`进行隔离，为防止超时以及其他情况导致缓存的内存膨胀溢出，使用了**可配置的超时机制**，代理类获取响应时会借助线程池的`Future`返回对象进行**默认2秒**的超时等待获取，同时通信处理器中会做如下处理进行安全保证：
       1. 通信数据中带有毫秒值时间戳，客户端响应事件中会进行判断，若响应事件超过**配置时间**则丢弃响应。否则按**通信key存入缓存**。

       2. 代理类通过`Future`等待取值，给定时间(配置的超时时间加上50毫秒的传输时间)内获取成功后，移除缓存并返回响应。如果给定时间内没有响应成功，将本次`通信key`从缓存中删除并根据特殊情况存入**延时队列**，默认5秒后进行消费再次清除。

          > 通过上述处理，理论上缓存中不会存在无效数据

---

## 核心依赖

```XML
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.7.0</version>
</dependency>
```

---

## 核心代码说明

### 自动配置类

`cn.t.rpc.config.RpcAutoConfiguration`

```java
@Configuration
// @TRpcServiceScanner(basePackages = {"cn.t.rpc"})  // ※注意点1
public class RpcAutoConfiguration {

	/**
	 * 客户端配置类
	 * @return 客户端配置类
	 */
	@Bean
	@ConfigurationProperties(prefix = "rpc.service")
	public RpcConfigProperties rpcConfigProperties() {
		RpcConfigProperties properties = new RpcConfigProperties();
		properties.setSerializationStrategy(serializationStrategy());
		return properties;
	}
    
    /**
	 * 容器后处理
	 * @return
	 */
	@Bean
	public TRpcFrameworkAware tRpcFrameworkAware() {
		return new TRpcFrameworkAware();
	}
	
	/**
	 * 序列化方式工具类<br>
	 * 默认使用Jackson的Json工具类
	 * @return 序列化方式工具类
	 */
	@Bean
	@ConditionalOnMissingBean(SerializationStrategy.class)
	public SerializationStrategy serializationStrategy() {
		return new SerializationByJson();
	}
	
	/**
	 * 服务发现
	 * @return 服务发现Bean
	 */
	@Bean
	@ConditionalOnMissingBean(TRpcDiscoveryStrategy.class)
	public TRpcDiscoveryStrategy rpcDiscovery() {
		return new TRpcDiscoveryByProperties();
	}
}
```
自动配置类中默认装配了整个框架运行所必须的**配置信息对象**`TRpcConfigProperties`,同时默认配置了**Json方式序列化**以及从**内存中读取服务列表**的策略。

**需要获取框架定义的配置项时注入**`TRpcConfigProperties`即可。

需要自定义序列化方式时，实现`SerializationStrategy`接口并生成`Bean`即可。

需要自定义服务发现方式时，实现`TRpcDiscoveryStrategy`接口并生成`Bean`即可。

**注意点1:代码中注释掉的**`@TRpcServiceScanner(basePackages = {"cn.t.rpc"})`**需要在自己的配置类中添加并指定包路径，否则无法扫描到需要代理的接口!!!**

   > `resources\META-INF\spring.factories`路径下已经配置了自动装配入口
   >
   > ```
   > org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
   > cn.t.rpc.config.RpcAutoConfiguration
   > ```

### 约定的数据类型以及格式(编码器解码器)

传输约定的默认数据类型为`Json`，序列化的基类为`cn.t.rpc.core.TrafficData`，默认派生三个子类

1. `TRequest`   请求的数据类
2. `TResponse`   响应的数据类
3. `TIdle`   心跳检测用的数据类

> 用于数据传输的数据类中的字段名已经在保证语义的情况下尽量缩短了，以减少序列化传输时的数据量，提升通信性能。

传输约定的数据格式如下：

1. 序列化的数据类型(用于区分核心功能以及后续扩展功能)
2. 有效业务数据的字节长度
3. 当前传输的数据类型(请求还是响应，该字段目前没有实际业务意义)
4. 业务数据
5. 终止符

举例(以竖线为分隔符，0代表各个字节):

```java
0000000|0000|0000|00000...000000|000
```

编码器如下(只贴出核心部分):

```java
@Override
protected void encode(ChannelHandlerContext ctx, TrafficData msg, ByteBuf out) throws Exception {
    try {
        // do something
        byte[] data = this.strategy.serialize(msg);
        // do something
        int len = data.length;
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
```

解码器如下(只贴出核心部分):

```java
@Override
protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    try {
        // 1.判断长度
        if (in.readableBytes() <= TRpcConstants.RPC_TRAFFIC_VALID_LEN) {
            logger.warn("TrafficDecoder: 传输的数据不合法...数据长度不正确...移交后续解码器处理...");
            // 中止处理，触发后续管道操作
            ctx.fireChannelRead(in);
            return;
        }
        // 2.判断类型
        // 读取序列化类型字节数组
        byte[] types = new byte[TRpcConstants.RPC_TRAFFIC_TYPE_LEN];
        try {
            in.readBytes(types);
        } catch (IndexOutOfBoundsException e) {
            logger.debug("TrafficDecoder: 类型不正确...移交后续解码器处理...");
            // 重置读取的index避免影响后续解码器
            in.resetReaderIndex();
            ctx.fireChannelRead(in);
            return;
        }
        if (!TRpcConstants.RPC_TRAFFIC_TYPE.equals(new String(types, CharsetUtil.UTF_8))) {
            logger.debug("TrafficDecoder: 类型不正确...移交后续解码器处理...");
            in.resetReaderIndex();
            ctx.fireChannelRead(in);
            return;
        }
        // 读取报文的数据长度
        int len = in.readInt();
        if (len <= 0) {
            logger.error("TrafficDecoder: 传输的数据不合法...数据长度不正确...");
            // 直接关闭连接,不再接收,简单防止攻击
            ctx.close();
            return;
        }

        // 读取类型
        int type = in.readInt();
        // 剩余的数据量长度(数据长度加上终止符长度)
        int validLen = len + TRpcConstants.RPC_TRAFFIC_EOF_LEN;
        if (in.readableBytes() < validLen) {
            // 此时有可能数据量大，还未读完，等待下一次读取
            in.resetReaderIndex();
        } else if (in.readableBytes() > validLen) {
            // 进入这个case中有两种情况
            // 1: 业务设计ok的情况下,进入此分支则出现了沾包问题(两次消息利用同一个channel无间隔发送)
            // 2: 数据被篡改了
            // =======进行拆包======
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
                // 保存当前的readerIndex位置,读取下一个包的数据
                in.markReaderIndex();
            } else {
                logger.error("TrafficDecoder: 传输的数据不合法...数据被篡改...");
                // 直接关闭连接,不再接收
                ctx.close();
            }
        } else {
            logger.debug("TrafficDecoder: 数据流读取完毕...长度:" + len + " ...类型:" + TrafficType.findName(type));

            byte[] body = new byte[len];
            // 将剩余的缓冲区字节数据读入临时数组
            in.readBytes(body);
            // 将终止符读完(注意,netty中in.readBytes传入的参数为int类型时会生成新的ByteBuf,此时需要释放这个新的ByteBuf,否则会内存泄露)
			// in.readBytes(TRpcConstants.RPC_TRAFFIC_EOF_LEN);
			in.readBytes(new byte[TRpcConstants.RPC_TRAFFIC_EOF_LEN]);

            TrafficData traffic = this.strategy.deserialize(body, TrafficTypeHelper.getTrafficClassType(type));
            out.add(traffic);

            // 保存当前的readerIndex位置(本次有可能是沾包数据,但是刚好一半一半)
            in.markReaderIndex();
        }
    } catch (Exception e) {
        logger.error("TrafficDecoder: 解码异常...本次连接关闭...异常信息{}", e.getMessage());
        ctx.close();
    }
}
```

### 客户端服务发现策略

客户端的服务发现默认使用`properties`文件读取方式，文件在`/resources/rpc-server.properties`。

文件格式如下:

```properties
#serverId=host:port
#服务器id=IP地址:端口
#多实例的服务器id指定同一个,以逗号分隔
app=127.0.0.1:8081,127.0.0.1:18081
time=127.0.0.1:18081
```

正常启动时，配置类会默认生成一个基于文件的服务发现策略。

```java
/**
 * 服务发现
 * @return 服务发现Bean
 */
@Bean
@ConditionalOnMissingBean(TRpcDiscoveryStrategy.class)
public TRpcDiscoveryStrategy rpcDiscovery() {
return new TRpcDiscoveryByProperties();
}
```

同时，框架也提供了基于`zookeeper`为注册中心的服务发现策略。将`TRpcDiscoveryByZk`配置为`Bean`即可。

`TRpcDiscoveryByZk`中配合连接池工具类已经实现了**动态刷新服务**的功能。

```java
@Configuration
@TRpcServiceScanner(basePackages = { "aaa.bbb.ccc" })
public class XXXConfig {
	// 注入配置信息
	@Autowired
	private TRpcConfigProperties rpcConfigProperties;

	/**
	 * 基于zookeeper的服务发现
	 * @return 服务发现Bean
	 * @throws Exception 
	 */
	@Bean
	public TRpcDiscoveryStrategy rpcDiscoveryZk() throws Exception {
		return new TRpcDiscoveryByZk(rpcConfigProperties);
	}
}
```

### 客户端接口自动代理

接口的自动代理主要依赖于`TRpcServiceScanner`注解的实现，在该注解中`@Import`了`RpcServiceScanner.class`。

```java
@Retention(RUNTIME)
@Target(TYPE)
@Import(RpcServiceScanner.class)
public @interface TRpcServiceScanner {
    String[] basePackages();
}
```

在`RpcServiceScanner`中获取`TRpcServiceScanner`注解的`basePackages`值，然后进行扫描并生成`bean`定义。

```java
public class RpcServiceScanner implements ImportBeanDefinitionRegistrar {
    
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		
		// 获取@Import中指定类的注解元数据
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(TRpcServiceScanner.class.getName()));

		// 获取指定扫描的包路径
		String[] basePackages = annotationAttributes.getStringArray("basePackages");

		RpcClassPathBeanDefinitionScanner scanner = new RpcClassPathBeanDefinitionScanner(registry, false);
		try {
			scanner.scanRpc(registry, basePackages);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
			e.printStackTrace();
		}
	}
}
```

类中借助了`RpcClassPathBeanDefinitionScanner`来扫描`Class`文件。

```java
public class RpcClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
	
	public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters)
			 {
		super(registry, useDefaultFilters);
	}

	protected void scanRpc(BeanDefinitionRegistry registry, String... basePackages) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		// 添加一个扫描的类型过滤器
		super.addIncludeFilter(new AnnotationTypeFilter(TRpcRemoteService.class));
		// 扫描包获取元数据-由于spring默认的doScan会将顶级接口忽略,所以需要手动重写这块逻辑
		Set<BeanDefinition> candidates = new LinkedHashSet<>();
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		for (String basePackage: basePackages) {
			// 下面的逻辑直接复制spring的代码
			String searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + basePackage.replace(".", File.separator)  + File.separator + "/**/*.class";
			// 扫描包下所有的类
			Resource[] resources = resourcePatternResolver.getResources(searchPath);
			for (Resource resource : resources) {
				MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
				ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
				sbd.setSource(resource);
				// 添加到候选类中
				candidates.add(sbd);
			}
		}
		
		for (BeanDefinition s : candidates) {
			GenericBeanDefinition definition = (GenericBeanDefinition) s;
			Class<?> clazz = Class.forName(definition.getBeanClassName());
			Annotation[] annotations = clazz.getAnnotations();
			for (Annotation a : annotations) {
				// 判断注解类型和Class类型(只代理接口)
				if (TRpcRemoteService.class.isAssignableFrom(a.annotationType()) && clazz.isInterface()) {
					// 全限定类(接口)名
					String className = definition.getBeanClassName();
					String beanName = className.substring(className.lastIndexOf("."));
					// 按类型自动装配
					definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
					// 设置生成Bean实例的工厂
					definition.setBeanClass(RpcProxyBeanFactory.class);
					// 设置创建对象工厂的构造方法参数
					definition.getConstructorArgumentValues().addIndexedArgumentValue(0, className);
					// 注册bean
					registry.registerBeanDefinition(beanName, definition);
				}
			}
		}
	}
}
```

通过上述两个核心类的配置，在服务启动时就可以根据注解自动生成对应的代理类。

### 客户端连接池

在多服务器的情况下,客户端会根据每个服务器的`serverId`分配连接池，底层连接池使用`netty`的`FixedChannelPool`实现，同时利用`AbstractChannelPoolMap`建立`serverId`和连接池的映射关系，再聚合相同`serverId`的连接池到`TClientPoolWrapper`类中，达到高可用的目的。同时，连接池工具类中会开启定时任务，清除不可用的连接池。

核心逻辑(已删减一些`log`操作)如下:

```java
/**
 * 初始化连接池
 * 
 * @param addrList 服务地址
 */
public void init(List<TRpcRemoteAddress> addrList) {

    if (this.config == null) {
        this.failureMsg.append("1.未配置连接池...连接池初始化失败...需要调用config()方法进行配置...");
    }

    if (addrList == null || addrList.isEmpty()) {
        this.failureMsg.append("2.未设置远程地址...");
    }

    this.workerGroup = new NioEventLoopGroup();
    this.bootstrap = new Bootstrap();

    this.bootstrap.group(this.workerGroup).channel(NioSocketChannel.class) 
        .option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getConnectionTimeout());

    // 初始化poolMap
    poolMap = new AbstractChannelPoolMap<TRpcRemoteAddress, FixedChannelPool>() {

        @Override
        protected FixedChannelPool newPool(TRpcRemoteAddress key) {
            return new FixedChannelPool(bootstrap.remoteAddress(key.getRemoteAddress()),
                                        new TClientChannelPoolHandler(config), config.getMaxPoolConnection());
        }
    };

    // 预创建
    if (addrList != null && !addrList.isEmpty()) {
        // 构建连接池
        this.initPool(addrList);
        // 清空失败操作信息
        this.failureMsg.delete(0, this.failureMsg.length());
        // 开启定时刷新
        this.startTimer();
    } else {
        this.failureMsg.append("3.未获取到设置远程地址,连接池预创建未完成...");
    }
}
```

```java
/**
 * 构建连接池
 * 
 * @param addrList 服务列表
 */
private void initPool(List<TRpcRemoteAddress> addrList) {
    this.rpcMaps = (this.rpcMaps == null || this.rpcMaps.size() == 0) ? new HashMap<>(32) : this.rpcMaps;
    addrList.forEach(addr -> {
        // poolMap调用get获取时会默认创建连接池,如果已创建则直接返回
        String id = addr.getServerId();
        if (this.rpcMaps.containsKey(id)) {
            // 相同的连接将不会重复添加
            if (!this.poolMap.contains(addr)) {
                this.rpcMaps.get(id).add(new TClientPoolWrapper(addr, this.poolMap.get(addr)));
            }
        } else {
            List<TClientPoolWrapper> poolList = new ArrayList<>();
            poolList.add(new TClientPoolWrapper(addr, this.poolMap.get(addr)));
            this.rpcMaps.put(id, poolList);
        }
    });

    // 初始化成功
    this.status = true;
}
```

上面的`this.rpcMaps`是客户端服务端映射关系池，同id的远程服务会映射在一个list中。属性如下:

```java
private Map<String, List<TClientPoolWrapper>> rpcMaps;
```

远程调用时会从下面的方法获取通道，同时会进行简单的随机轮询，获取到通道后指定当前通道的从属，用于后续释放。

```java
/**
 * 获取指定服务的通道
 * 
 * @param key 服务id
 * @return 通道
 */
public Channel getPooledChannel(String key) {
    if (this.rpcMaps == null || this.rpcMaps.isEmpty()) {
        throw new TRpcOperationException("连接池未初始化...需要调用init()方法进行初始化...");
    }

    List<TClientPoolWrapper> poolList = rpcMaps.get(key);

    if (poolList == null || poolList.isEmpty()) {
        throw new TRpcConnectionException("远程连接丢失...无可用连接池...");
    }

    // 按数组长度随机取余
    int random = this.rand.nextInt(1024);
    int retryCnt = 0;
    // 保证每个池子都能尝试到
    int limit = poolList.size();
    int index = 0;
    Channel channel = null;
    do {
        index = random % limit;
        // 使用随机下标获取连接池
        channel = poolList.get(index).acquireChannel(this.config.getRetryCnt(), this.config.getReconnectionTime());
        // 获取失败时随机数自增，进行下一个池子的获取
        random++;
        retryCnt++;
    } while (channel == null && retryCnt < limit);

    if (channel == null) {
        throw new TRpcCoreException("无法获取连接通道...");
    }

    // 给当前的通道绑定属性,指定其从属于哪一个池子
    AttributeKey<Map<String, Integer>> attrKey = AttributeKey.valueOf(POOL_KEY);
    Map<String, Integer> metaMap = new HashMap<>();
    metaMap.put(key, index);
    channel.attr(attrKey).set(metaMap);
    return channel;
}
```

`TClientPoolWrapper`中会进行连接池的**健康检查**，**重连操作**。获取连接通道的代码如下:

```java
/**
 * 获取通道
 * 
 * @param retryCnt         获取通道的重试次数上限
 * @param reconnectionTime 连接池重连的时间上限
 * @return 通道
 */
public Channel acquireChannel(int retryCnt, int reconnectionTime) {

		// 池子不可用时,直接抛出异常
		if (!this.isHealthy()) {
			logger.warn("连接至{}的连接池不可用...请检查网络状态...", this.key.getAddressString());
			// 尝试下一个连接池
			return null;
		}

		Future<Channel> channeFuture = this.pool.acquire();
		Channel channel = null;
		try {
			channel = channeFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			// 健康检查,重试
			this.increaseFailureCnt(retryCnt, reconnectionTime);
		}
		return channel;
}
```

### 代理类的核心实现逻辑(线程数据同步，绑定)

代理类继承了实现`InvocationHandler`接口的基类，基类中会调用`action`方法实现核心业务。

```java
@Override
protected Object action(Method method, Object[] args) throws TRpcTimeoutException, TRpcConnectionException {

    if (!CHANNEL_POOL.isReady()) {
        throw new TRpcConnectionException("连接池未初始化成功...操作信息:" + CHANNEL_POOL.getFailureMsg());
    }

    // 获取指定的服务器id
    TRpcRemoteService a = method.getDeclaringClass().getAnnotation(TRpcRemoteService.class);
    String serverId = a.serverId();

    // 获取通信通道
    Channel ch = CHANNEL_POOL.getPooledChannel(serverId);

    TRequest req = new TRequest();
    // 方法名一致才可以调用
    req.setMethod(method.getName());
    req.setParams(args);
    // 调用的服务类名
    String className = a.className();
    if (className.isBlank()) {
        className = method.getDeclaringClass().getSimpleName();
    }
    req.setClsNm(className);

    // 预创建一个响应
    CompletableFuture<TrafficData> trafficFuture = TRpcTrafficDataContextHolder.newFuture(req.getId());

    ch.writeAndFlush(req).addListener((f) -> {
        if (f.isSuccess()) {
            logger.debug("发送成功...");
        } else {
            logger.warn("远程调用{}失败...", method.getName());
            // 发送失败清除缓存
            TRpcTrafficDataContextHolder.removeCache(req.getId());
        }
        // 使用完通道后立即释放
        CHANNEL_POOL.release(ch);
    }).syncUninterruptibly();

    try {
        TResponse res = trafficFuture.get(TRpcTrafficDataContextHolder.getTimeout() + 50, TimeUnit.MILLISECONDS).convert();
        if (res.getStatus() != 200) {
            throw new TRpcResponseException(res.getMsg());
        } else {
            return res.getBody();
        }

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
        logger.error("RPC调用异常...异常信息:{}", e.getMessage());
        // 出现异常移除本次请求的缓存
        TRpcTrafficDataContextHolder.removeCache(req.getId());
        throw new TRpcOperationException(e.getMessage(), e);
    }
}
```

代理类中借助了`TRpcTrafficDataContextHolder`来实现多线程间的数据绑定和同步，在请求发送前先创建一个`Future`响应值同时存入`TRpcTrafficDataContextHolder`中的缓存中，如果出现异常则移除这个响应。

> 注意: 在`get`时除了指定的超时间外还多增加了50毫秒，这50毫秒主要是为了网络中的数据来回传输考虑。

`TRpcTrafficDataContextHolder`完整代码如下:

```java
public final class TRpcTrafficDataContextHolder {

	private static final Logger logger = LoggerFactory.getLogger(TRpcTrafficDataContextHolder.class);

	/**
	 * 响应结果缓存,根据key隔离不同的响应<br>
	 * 初始化hashMap容量,减少扩容
	 */
	private static final ConcurrentHashMap<String, CompletableFuture<TrafficData>> resultMap = new ConcurrentHashMap<>(
			128);

	/**
	 * 延时无界队列,用于定时清除极端情况下的无效响应
	 */
	private static final DelayQueue<InvalidKey> INVALIDKEY_QUEUE = new DelayQueue<>();

	/**
	 * 缓存清除线程
	 */
	private static final Thread sweeper = new Thread(() -> {
		while (true) {
			try {
				logger.info("keep sweep the cache...");
				resultMap.remove(INVALIDKEY_QUEUE.take().getKey());
			} catch (InterruptedException e) {
				logger.warn("the sweeper has been interrupted...");
			}
		}
	}, "the sweeper");

	static {
		sweeper.start();
	}

	private TRpcTrafficDataContextHolder() throws IllegalAccessException {
		throw new IllegalAccessException("Illegal Operation...");
	}

	/**
	 * 超时时间
	 */
	private static int TIMEOUT = 2000;

	public static void setTimeout(int timeout) {
		TIMEOUT = timeout;
	}

	public static int getTimeout() {
		return TIMEOUT;
	}

	/**
	 * 创建新的结果CompletableFuture对象
	 * 
	 * @param trafficId 唯一ID
	 * @return 空CompletableFuture对象
	 */
	public static CompletableFuture<TrafficData> newFuture(String trafficId) {
		CompletableFuture<TrafficData> future = new CompletableFuture<TrafficData>();
		var old = resultMap.putIfAbsent(trafficId, future);
		if (old != null) {
			throw new TRpcDuplicateKeyException();
		}
		return future;
	}

	/**
	 * 完成指定的future,客户端收到响应后调用
	 * 
	 * @param data 响应数据
	 */
	public static void complete(TrafficData data) {
		CompletableFuture<TrafficData> future = resultMap.remove(data.getId());
		if (future != null) {
			future.complete(data);
		} else {
			throw new TRpcCoreException("future not found...");
		}
	}

	/**
	 * 移除缓存中的响应
	 * 
	 * @param trafficId 唯一ID
	 * @return 缓存中的响应
	 */
	public static CompletableFuture<TrafficData> removeCache(String trafficId) {
		CompletableFuture<TrafficData> invalidData = resultMap.remove(trafficId);
		// 延时再删除(保证极端情况下等待线程达到等待最大值同时客户端handler满足响应条件时缓存无法被正常消费的case)
		if (invalidData == null) {
			INVALIDKEY_QUEUE.add(new InvalidKey(trafficId));
		}
		return invalidData;
	}
}
```

在正常响应时，响应完成后会根据请求时创建的唯一`key`从缓存中找到对应的预创建`Future`进行完成处理。

### 客户端响应事件

客户端链式处理器中收到响应后会根据响应开始时间(服务器端生成响应的时间)和当前时间进行差值计算，与配置文件中指定的请求超时时间进行比较，用于控制超时处理。此处由于使用的是当前时间与服务器生成响应的时间的差值，没有很细致地考虑数据传输的时间(代理类中增加了50ms的增益)，所以会存在请求的`Future`先超时的情况，针对这种情况，在上文说明的`TRpcTrafficDataContextHolder`中，持有一个延时队列，在移除缓存时如果缓存中没有则会将这个响应的key扔到队列中，固定5秒后进行再次移除处理。

```java
@Override
protected void channelRead0(ChannelHandlerContext ctx, TrafficData msg) throws Exception {
    logger.debug("{}客户端{}读取响应信息...", TRpcConstants.LOG_CLIENT_MSG_PREFIX, ctx.channel().remoteAddress());
    try {
        // 响应开始时间
        long ts = msg.getTs();
        // 当前时间
        long curTs = System.currentTimeMillis();
        // 超过超时时间则丢弃，不放入缓存(极端情况下-刚好达到阈值时 将会进入缓存,然后在清扫线程中被移除)
        if (curTs - ts > this.requestTimeout) {
            logger.warn("{}响应时间过长...数据已被丢弃...", TRpcConstants.LOG_CLIENT_MSG_PREFIX);
        } else {
            // 响应值存入共享map中
            TRpcTrafficDataContextHolder.complete(msg);
        }
    } catch(Exception e) {
        // 移除缓存
        TRpcTrafficDataContextHolder.removeCache(msg.getId());
        logger.error("{}响应处理异常...异常信息:{}", TRpcConstants.LOG_CLIENT_MSG_PREFIX, e.getMessage());
    }
}
```

### 服务端服务扫描以及服务调用

当`rpc.service.exposedService`设置为`true`时，工程启动后就会另起一个线程开启`RPC服务器`，同时将扫描所有标识了`TRpcLocalService`注解的实现类并且将注解上指定的`className`作为`key`，实现类实例作为`value`进行缓存。核心操作在`TLocalServiceHolderContext`辅助类中。调用方法时使用了`cglib`代理优化性能。在服务端收到请求后根据请求信息调用服务。

代码如下:

```java
public class TLocalServiceHolderContext {
	
	/**
	 * 全限定类名-类信息映射
	 */
	private static final Map<String, ServiceMetaData> SERVICE_MAP = new HashMap<>(32);
	
	/**
	 * 添加服务
	 * @param name 类名
	 * @param metaData 类元数据
	 */
	public static void addService(String name, ServiceMetaData metaData) {
		SERVICE_MAP.put(name, metaData);
	}
	
	/**
	 * 本地服务数量
	 * @return 本地服务数量
	 */
	public static int size() {
		return SERVICE_MAP.size();
	}
	
	/**
	 * 调用服务方法
	 * @param serviceName 服务名
	 * @param methodName 方法名
	 * @param params 参数
	 * @return 服务方法返回值
	 */
	public static Object invoke(String serviceName,String methodName, Object[] params) {
		
		ServiceMetaData metaData = SERVICE_MAP.get(serviceName);
		
		if (metaData == null) {
			throw new TRpcCoreException("未找到相关服务...");
		}
		
		// 获取类型创建cglib代理并执行(性能上比直接使用反射快)
		FastClass fc = FastClass.create(metaData.getBean().getClass());
		FastMethod m = fc.getMethod(metaData.getMethod(methodName));
		try {
			return m.invoke(metaData.getBean(), params);
		} catch (InvocationTargetException e) {
			throw new TRpcCoreException("调用服务失败...失败信息:" + e.getMessage());
		}
	}
	
	/**
	 * 本地服务的元数据
	 * @author TMQ
	 *
	 */
	public static class ServiceMetaData {
		
		public ServiceMetaData(Object bean) {
			this.bean = bean;
		}
		
		/**
		 * 当前实际的服务对象
		 */
		private Object bean;
		
		/**
		 * 方法名-方法对象映射
		 */
		private Map<String, Method> serviceMethods = new HashMap<>();;

		public Object getBean() {
			return bean;
		}

		/**
		 * 添加服务方法
		 * @param name 方法名
		 * @param method 方法对象
		 */
		public void addServiceMethod(String name, Method method) {
			this.serviceMethods.put(name, method);
		}
		
		/**
		 * 获取服务方法
		 * @param name 方法名
		 * @return 方法对象
		 */
		public Method getMethod(String name) {
			return this.serviceMethods.get(name);
		}
	}
}
```





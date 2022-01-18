package cn.t.rpc.core.scanner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import cn.t.rpc.core.annotation.TRpcRemoteService;
import cn.t.rpc.core.proxy.RpcProxyBeanFactory;

/**
 * 自定义类扫描器,只扫描指定的类并生成bean
 * @author TMQ
 *
 */
public class RpcClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
	
	/**
	 * 构造方法
	 * 
	 * @param registry          注册器
	 * @param useDefaultFilters 是否包含默认的注解类型(@Controller等)
	 */
	public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters)
			 {
		super(registry, useDefaultFilters);
	}

	/**
	 * 扫描并生成代理类
	 * @param registry
	 * @param basePackages
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException 
	 */
	protected void scanRpc(BeanDefinitionRegistry registry, String... basePackages) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		// 添加一个扫描的类型过滤器
		super.addIncludeFilter(new AnnotationTypeFilter(TRpcRemoteService.class));
		// 扫描包获取元数据-由于spring默认的doScan会将顶级接口忽略,所以需要手动重写这块逻辑
		// 如果想用官方的,那么可以继承需要代理的类使之满足条件
		// Set<BeanDefinitionHolder> beanDefinitionHolderSet = super.doScan(basePackages);
		Set<BeanDefinition> candidates = new LinkedHashSet<>();
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		for (String basePackage: basePackages) {
			// 下面的逻辑直接复制spring的代码
			String searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + basePackage.replace(".", File.separator)  + File.separator + "/**/*.class";
			// 扫描包下所有的类
			Resource[] resources = resourcePatternResolver.getResources(searchPath);
			for (Resource resource : resources) {
				MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
				// MetadataReader metadataReader = metadata.getMetadataReader(resource);
				ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
				sbd.setSource(resource);
				// 添加到候选类中
				candidates.add(sbd);
			}
		}
		
		for (BeanDefinition s : candidates) {
			// 获取Bean定义
			GenericBeanDefinition definition = (GenericBeanDefinition) s;
			// 根据接口的全路径名称获取类型
			Class<?> clazz = Class.forName(definition.getBeanClassName());
			// 获取注解
			Annotation[] annotations = clazz.getAnnotations();
			for (Annotation a : annotations) {
				// 判断注解类型和Class类型(只代理接口)
				if (TRpcRemoteService.class.isAssignableFrom(a.annotationType()) && clazz.isInterface()) {
					
					// 全限定类(接口)名
					String className = definition.getBeanClassName();
					// 类名
					String beanName = className.substring(className.lastIndexOf("."));
					
					logger.debug("代理对象接口:" + className);
					
					// 按类型自动装配
					definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
					// 设置生成Bean实例的工厂--工厂调用getObject方法所返回最终对象
					definition.setBeanClass(RpcProxyBeanFactory.class);
					// 设置创建对象工厂的构造方法参数
					// 添加RpcProxyBeanFactory的构造参数1-全限定名称-实际编译中泛型会丢失,这里直接传入全限定名
					// definition.getConstructorArgumentValues().addGenericArgumentValue(className);
					definition.getConstructorArgumentValues().addIndexedArgumentValue(0, className);
					// definition.getConstructorArgumentValues().addIndexedArgumentValue(1, RpcDiscoveryStrategyHelper.initClients());
					// 注册bean
					registry.registerBeanDefinition(beanName, definition);
					logger.debug("代理对象Bean名称:" + beanName);
				}
			}
		}
	}
}

package cn.t.rpc.core.scanner;

import java.io.IOException;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import cn.t.rpc.core.annotation.TRpcServiceScanner;


/**
 * 自定义包扫描器<br>
 * 在注解中使用@Import，若指定的类是ImportBeanDefinitionRegistrar的实现类，则会调用接口方法，将其中要注册的类注册成bean<br>
 * ImportBeanDefinitionRegistrar中动态注册的bean是优先于依赖其的bean初始化，也能被aop、validator等机制处理<br>
 * spring的bean是通过BeanFactory创建的,可以在BeanFactoryAware中获取BeanFactory，但此处只需要获取注册器即可
 * @author TMQ
 *
 */
public class RpcServiceScanner implements ImportBeanDefinitionRegistrar {
	
	/**
	 * 重写ImportBeanDefinitionRegistrar的默认方法,动态生成Bean
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		
		// 获取@Import中指定类的注解元数据
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(TRpcServiceScanner.class.getName()));

		// 获取指定扫描的包路径
		String[] basePackages = annotationAttributes.getStringArray("basePackages");
		
		if (basePackages == null || basePackages.length == 0) {
			// springboot中使用下列方法获取了当前注解(@AutoConfigurationPackage)所在的包
			// ClassUtils.getPackageName(metadata.getClassName())
			
		}
		
		// 类路径下包扫描器
		// 第二个参数见官方注释,此处设为false,只扫描本类指定的注解类型,不扫描官方默认的类型,默认类型在官方配置中进行扫描
		RpcClassPathBeanDefinitionScanner scanner = new RpcClassPathBeanDefinitionScanner(registry, false);
		try {
			scanner.scanRpc(registry, basePackages);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
			e.printStackTrace();
		}
	}
}

package cn.t.rpc.core.proxy;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.FactoryBean;

/**
 * 自定义接口代理类生成工厂(将会被RpcServiceScanner中使用,生成代理类)
 * @author TMQ
 *
 * @param <T>
 */
public class RpcProxyBeanFactory<T> implements FactoryBean<T> {
	
	/**
	 * 接口类型
	 */
	private Class<T> interfaceType;
	
	public RpcProxyBeanFactory(Class<T> interfaceType) {
		this.interfaceType = interfaceType;
	}

	/**
	 * Spring Bean工厂实现,自动装配时需要这个工厂来生成Bean对象
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getObject() throws Exception {
		// 将泛型传递,生成代理类
		return (T) Proxy.newProxyInstance(RpcProxy.class.getClassLoader(),
				new Class[] { this.interfaceType }, new RpcClientMethodImpl<T>(this.interfaceType));
	}

	@Override
	public Class<T> getObjectType() {
		return this.interfaceType;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}

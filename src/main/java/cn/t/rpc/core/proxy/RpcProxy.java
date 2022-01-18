package cn.t.rpc.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.t.rpc.core.exception.TRpcOperationException;

/**
 * jdk动态代理-为接口生成自定义的实现
 * @author TMQ
 *
 * @param <T> 代理对象的接口类型
 */
public class RpcProxy<T> implements InvocationHandler {

	/**
	 * 代理类对象
	 */
	private T proxy;

	public RpcProxy(Class<T> clazz) {
		this.proxy = this.createProxy(clazz);
	}

	/**
	 * @param proxy  当前代理类对象
	 * @param Method 被代理的对象的方法
	 * @param args   参数
	 */
	@Override
	public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 没有实现类的情况下在此方法中直接实现逻辑
		// Object result = method.invoke(target, args);
		return this.action(method, args);
	}
	
	/**
	 * 重新此方法以完成RPC
	 * @param method 方法对象
	 * @param args 参数
	 * @return RPC结果
	 */
	protected Object action(Method method, Object[] args) {
		throw new TRpcOperationException("非法空操作...");
	}

	/**
	 * 获取代理类对象(不使用Spring框架时手动调用获取代理对象)
	 * @return 代理类对象
	 */
	public final T getProxy() {
		return proxy;
	};
	
	/**
	 * 创建代理对象
	 * @param clazz 代理对象接口类型
	 * @return 代理对象
	 */
	@SuppressWarnings("unchecked")
	protected T createProxy(Class<T> clazz) {
		return (T) Proxy.newProxyInstance(RpcProxy.class.getClassLoader(),
				new Class[] { clazz }, this);
	}
}

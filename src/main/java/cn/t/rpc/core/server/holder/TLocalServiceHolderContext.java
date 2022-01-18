package cn.t.rpc.core.server.holder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import cn.t.rpc.core.exception.TRpcCoreException;

/**
 * 存储本地服务信息的辅助类
 * @author TMQ
 *
 */
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

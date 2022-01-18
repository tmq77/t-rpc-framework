package cn.t.rpc.core.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * RP远程服务标记-被该注解标记的<b>接口</b>会被自动生成代理类，主动连接RPC服务器
 * @author TMQ
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TRpcRemoteService {
	
	/**
	 * 远程服务器的ID(多实例的ID必须要一致)
	 * @return 远程服务器的ID
	 */
	String serverId();
	
	/**
	 * 远程服务器上对应的服务ID,默认为空时,使用调用者的接口名
	 * @return 服务ID
	 */
	String className() default "";
}

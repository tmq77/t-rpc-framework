package cn.t.rpc.core.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * RPC本地服务标记-被该注解标记的类会成为RPC服务,可以被RPC调用
 * @author TMQ
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
@Component
public @interface TRpcLocalService {
	
	/**
	 * 服务ID,默认为空,为空时使用类名
	 * @return 服务ID
	 */
	String className() default "";

}

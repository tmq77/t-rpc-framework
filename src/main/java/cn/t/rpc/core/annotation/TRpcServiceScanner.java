package cn.t.rpc.core.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import cn.t.rpc.core.scanner.RpcServiceScanner;

/**
 * RPC服务标记-被该注解标记的接口会被自动生成代理类，主动连接RPC服务器
 * @author TMQ
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
@Import(RpcServiceScanner.class)
public @interface TRpcServiceScanner {
	
    String[] basePackages();
}

package cn.t.rpc.core.exception;

/**
 * 核心包异常
 * @author TMQ
 *
 */
public class TRpcCoreException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -566102024731041798L;
	
	public TRpcCoreException(String message) {
		super(message);
	}
	
	public TRpcCoreException(String message, Throwable cause){
		super(message, cause);
	}
	
}

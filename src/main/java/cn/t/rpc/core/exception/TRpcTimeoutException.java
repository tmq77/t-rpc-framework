package cn.t.rpc.core.exception;

/**
 * 超时异常
 * @author TMQ
 *
 */
public class TRpcTimeoutException extends TRpcCoreException {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 8058863813559503906L;


	public TRpcTimeoutException() {
		super("连接超时...");
	}
	
	public TRpcTimeoutException(Throwable cause) {
		super("连接超时...", cause);
	}
	
	public TRpcTimeoutException(String message) {
		super(message);
	}
	
	
	public TRpcTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}

package cn.t.rpc.core.exception;

/**
 * 连接相关异常
 * @author TMQ
 *
 */
public class TRpcConnectionException extends TRpcCoreException {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2082542989234914602L;

	public TRpcConnectionException() {
		super("连接失败...");
	}
	
	public TRpcConnectionException(String message) {
		super(message);
	}
	
	public TRpcConnectionException(Throwable cause) {
		super("连接失败...", cause);
	}
	
	
	public TRpcConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}

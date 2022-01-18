package cn.t.rpc.core.exception;

/**
 * 类型异常
 * @author TMQ
 *
 */
public class TRpcTypeException extends TRpcCoreException {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -8366658251749137462L;
	
	public TRpcTypeException() {
		super("未知类型异常...");
	}

	public TRpcTypeException(String message) {
		super(message);
	}
	
	public TRpcTypeException(String message, Throwable cause) {
		super(message, cause);
	}
	
}

package cn.t.rpc.core.exception;

/**
 * 请求ID重复异常
 * @author TMQ
 *
 */
public class TRpcDuplicateKeyException extends TRpcCoreException {
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1599628233665440795L;

	public TRpcDuplicateKeyException() {
		super("key duplicate...please check the key generation algorithm...");
	}

	public TRpcDuplicateKeyException(String message) {
		super(message);
	}
	
	public TRpcDuplicateKeyException(String message, Throwable cause) {
		super(message, cause);
	}
	
}

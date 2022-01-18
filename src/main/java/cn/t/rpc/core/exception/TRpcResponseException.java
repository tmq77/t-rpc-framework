package cn.t.rpc.core.exception;

/**
 * 业务响应异常
 * @author TMQ
 *
 */
public class TRpcResponseException extends TRpcCoreException {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -2352478659384927113L;

	public TRpcResponseException(String message) {
		super(message);
	}
	
	public TRpcResponseException(String message, Throwable cause) {
		super(message, cause);
	}
	
}

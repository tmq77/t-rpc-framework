package cn.t.rpc.core.exception;

/**
 * 操作异常
 * @author TMQ
 *
 */
public class TRpcOperationException extends TRpcCoreException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1370084353819657728L;

	public TRpcOperationException(String message) {
		super(message);
	}
	
	public TRpcOperationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}

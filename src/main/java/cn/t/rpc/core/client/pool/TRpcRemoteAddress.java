package cn.t.rpc.core.client.pool;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * 远程服务器地址信息类
 * 
 * @author TMQ
 *
 */
public class TRpcRemoteAddress implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -804456773008791829L;

	/**
	 * 服务器Id
	 */
	private String serverId;

	/**
	 * 远程连接地址
	 */
	private InetSocketAddress remoteAddress;
	
	public TRpcRemoteAddress() {
		this.setRemoteAddress(new InetSocketAddress("127.0.0.1", 18081));
		this.serverId = "localhost";
	}

	public TRpcRemoteAddress(String remoteHost, int remotePort) {
		super();
		this.remoteAddress = new InetSocketAddress(remoteHost, remotePort);
		this.serverId = "localhost";
	}

	public TRpcRemoteAddress(String serverId, InetSocketAddress remoteAddress) {
		super();
		this.serverId = serverId;
		this.remoteAddress = remoteAddress;
	}

	public TRpcRemoteAddress(String serverId, String remoteHost, int remotePort) {
		super();
		this.remoteAddress = new InetSocketAddress(remoteHost, remotePort);
		this.serverId = serverId;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	@Override
	public int hashCode() {
		int hash = this.serverId.hashCode();
		hash = 31 * hash + this.remoteAddress.hashCode();
		hash = 31 * hash + this.remoteAddress.getHostName().hashCode();
		hash = 31 * hash + this.remoteAddress.getPort();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		TRpcRemoteAddress compareObj = (TRpcRemoteAddress) obj;

		if (compareObj == null || !this.serverId.equals(compareObj.getServerId())) {
			return false;
		}

		InetSocketAddress cpAddr = compareObj.getRemoteAddress();
		if (cpAddr == null) {
			return false;
		}

		if (this.remoteAddress.getHostName().equals(cpAddr.getHostName()) && this.remoteAddress.getPort() == cpAddr.getPort()) {
			return true;
		}
		return false;
	}

	/**
	 * 获取ip和端口
	 * @return
	 */
	public String getAddressString() {
		return this.remoteAddress.getHostName() + ":" + this.remoteAddress.getPort();
	}

}

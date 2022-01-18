package cn.t.rpc.web.remote;

import cn.t.rpc.core.annotation.TRpcRemoteService;

@TRpcRemoteService(serverId = "app", className = "ServerTimeService")
public interface ServerTimeService {

	String getServerTime(String user);
}

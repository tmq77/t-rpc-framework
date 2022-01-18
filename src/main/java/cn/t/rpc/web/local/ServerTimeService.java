package cn.t.rpc.web.local;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.t.rpc.core.annotation.TRpcLocalService;

/**
 * 获取本机时间的服务
 * @author TMQ
 *
 */
@TRpcLocalService(className = "ServerTimeService")
public class ServerTimeService {
	
	private static final String FORMAT = "yyyy年MM月dd日 hh时mm分ss秒";
	
	/**
	 * 获取当前服务器的时间
	 * @param user
	 * @return 服务时间
	 */
	public String getServerTime(String user) {
		return "Hello!" + user + "现在是:" + new SimpleDateFormat(FORMAT).format(new Date());
	}

}

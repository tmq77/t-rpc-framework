package cn.t.rpc.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * 读取资源文件的工具类<br>
 * 资源文件都必须是ISO-8859-1编码
 * 
 * @author TMQ
 *
 */
public class PropertiesUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
	
	private Properties properties;
	
	public PropertiesUtil(String filePath) {
		this.properties = new Properties();
		ClassPathResource res = new ClassPathResource(filePath);
		try(var in = new InputStreamReader(res.getInputStream(), "UTF-8");){
			this.properties.load(in);
		} catch (IOException e) {
			logger.error("读取配置文件出错...{}", e.getMessage());
		}
	}
	
	public Set<Entry<Object, Object>>  getAllProperties() {
		return this.properties.entrySet();
	}
	
	
}

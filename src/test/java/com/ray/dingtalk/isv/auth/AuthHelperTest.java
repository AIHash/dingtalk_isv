package com.ray.dingtalk.isv.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.ray.dingtalk.isv.config.Env;

/**@desc  : 
 * 
 * @author: shirayner
 * @date  : 2017年11月14日 下午6:18:13
 */
public class AuthHelperTest {

	private static final Logger log = LogManager.getLogger(AuthHelperTest.class);

	@Test
	public void testGetAccessToken() {
		
		try {
			
			String accessToken=AuthHelper.getAccessToken(Env.AUTH_CORP_ID);
			log.info(accessToken);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Test
	public void testGetJsapiTicket() {
		
		try {
			
			/*String jsapiTicket=AuthHelper.getAccessToken(corpId);
			log.info(accessToken);*/
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
}

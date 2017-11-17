package com.ray.dingtalk.isv.auth.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ray.dingtalk.isv.auth.AuthHelper;
import com.ray.dingtalk.isv.config.Env;

/**
 * @desc  : 定时获取AccessToken和jsapi_ticket，并存入文件
 * 
 * @author: shirayner
 * @date  : 2017年11月14日 下午3:04:32
 */
public class TokenRunnable implements Runnable {
	private static final Logger log = LogManager.getLogger(TokenRunnable.class);


	public void run() {
		while (true) {
			try {
				log.info("定时获取accessToken和jsapiTicket------------------------");
				AuthHelper.getAccessToken(Env.AUTH_CORP_ID);

				Thread.sleep(AuthHelper.cacheTime);

			} catch (InterruptedException e) {

				try {
					// 如果出现异常，60秒后再获取
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e1) {
					log.error("{}", e1);
				}
				log.error("{}", e);


			} catch (Exception e) {

				try {
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e1) {
					log.error("{}", e1);
				}

				log.error("{}", e);
			}
		}
	}
}
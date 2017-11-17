package com.ray.dingtalk.isv.auth;

import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Timer;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ray.dingtalk.isv.config.Env;
import com.ray.dingtalk.isv.service.access.IsvAccess;
import com.ray.dingtalk.isv.util.FileUtils;
import com.ray.dingtalk.isv.util.HttpHelper;

/**
 * @desc : ISV Token相关
 *       ISV接入：https://open-doc.dingtalk.com/docs/doc.htm?spm=a219a.7629140.0.0.MIneAD&treeId=366&articleId=104945&docType=1#s8
 * 
 * 
 * @author: shirayner
 * @date : 2017年10月27日 下午5:09:59
 */
public class AuthHelper {
	private static final Logger log = LogManager.getLogger(AuthHelper.class);

	public static Timer timer = null;
	// 调整到1小时50分钟
	public static final long cacheTime = 1000 * 60 * 55 * 2; // 110分钟

	public static long currentTime = 0 + cacheTime + 1;
	public static long lastTime = 0;
	public static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/*
	 * 在此方法中，为了避免频繁获取access_token， 在距离上一次获取access_token时间在两个小时之内的情况，
	 * 将直接从持久化存储中读取access_token
	 * 
	 * 因为access_token和jsapi_ticket的过期时间都是7200秒 所以在获取access_token的同时也去获取了jsapi_ticket
	 * 注：jsapi_ticket是在前端页面JSAPI做权限验证配置的时候需要使用的 具体信息请查看开发者文档--权限验证配置
	 */
	/**
	 * 1. 获取企业授权的access_token
	 * 
	 * @desc ： 服务提供商在取得企业的永久授权码并完成对企业应用的设置之后，便可以开始通过调用企业接口来运营这些应用。
	 * @param corpId
	 *            授权方企业id
	 * @return
	 * @throws Exception
	 *             String
	 */
	public static String getAccessToken(String corpId) throws Exception {

		String accToken = "";
		String jsTicket = "";

		long curTime = System.currentTimeMillis();
		JSONObject accessTokenValue = (JSONObject) FileUtils.getValue("accesstoken", corpId);
		log.info("accessTokenValue:" + accessTokenValue);
		JSONObject jsontemp = new JSONObject();

		// 1.若过期，大于7200秒
		if (accessTokenValue == null || curTime - accessTokenValue.getLong("begin_time") >= cacheTime) {

			// 1.1.获取accessToken并缓存

			// 1.1.1 重新获取accessToken
			String suiteAccessToken=(String)FileUtils.getValue("ticket", "suiteToken");
			String permanentCode=(String) FileUtils.getValue("permanentcode", corpId);
			log.info(suiteAccessToken);
			log.info(permanentCode);
			String accessToken = IsvAccess.getCropAccessToken(suiteAccessToken,corpId,permanentCode );

			// 1.1.2 将accessToken缓存到文件中
			if (accessToken != null) {
				log.info("accessToken:" + accessToken);

				accToken = accessToken;

				JSONObject jsonAccess = new JSONObject();
				jsontemp.clear();
				jsontemp.put("access_token", accToken);
				jsontemp.put("begin_time", curTime);
				jsonAccess.put(corpId, jsontemp);

				FileUtils.write2File(jsonAccess, "accesstoken");
			} else {
				throw new Exception("access_token获取失败");
			}

			// 1.2.获取JsapiTicket并缓存
			if (accToken.length() > 0) {

				jsTicket = IsvAccess.getJsapiTicket(accessToken);
				log.info("jsTicket:" + jsTicket);

				JSONObject jsonTicket = new JSONObject();
				jsontemp.clear();
				jsontemp.put("ticket", jsTicket);
				jsontemp.put("begin_time", curTime);
				jsonTicket.put(corpId, jsontemp);
				FileUtils.write2File(jsonTicket, "jsticket");
			}

		} else {
			return accessTokenValue.getString("access_token");
		}

		return accToken;
	}

	/**
	 * @desc ：2. 获取jsapi_ticket 正常的情况下，jsapi_ticket的有效期为7200秒，
	 *       所以开发者需要在某个地方设计一个定时器，定期去更新jsapi_ticket
	 *
	 * @param accessToken
	 *            企业授权的access_token
	 * @param corpId
	 *            授权方企业id
	 * @return
	 * @throws Exception
	 *             String
	 */
	public static String getJsapiTicket(String accessToken, String corpId) throws Exception {
		JSONObject jsTicketValue = (JSONObject) FileUtils.getValue("jsticket", corpId);
		long curTime = System.currentTimeMillis();
		String jsTicket = "";

		// 1.第一次或者缓存时间大于7200秒
		if (jsTicketValue == null || curTime - jsTicketValue.getLong("begin_time") >= cacheTime) {
			// 1.1获取jsTicket
			jsTicket = IsvAccess.getJsapiTicket(accessToken);

			// 1.2将jsTicket写入文件缓存起来
			JSONObject jsonTicket = new JSONObject();
			JSONObject jsontemp = new JSONObject();
			jsontemp.clear();
			jsontemp.put("ticket", jsTicket);
			jsontemp.put("begin_time", curTime);
			jsonTicket.put(corpId, jsontemp);
			FileUtils.write2File(jsonTicket, "jsticket");

			return jsTicket;
		} else {
			return jsTicketValue.getString("ticket");
		}
	}

	public static String sign(String ticket, String nonceStr, long timeStamp, String url) {
		String plain = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr + "&timestamp=" + String.valueOf(timeStamp)
				+ "&url=" + url;
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.reset();
			sha1.update(plain.getBytes("UTF-8"));
			return bytesToHex(sha1.digest());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String bytesToHex(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}

	public static String getConfig(HttpServletRequest request) {
		String urlString = request.getRequestURL().toString();
		String queryString = request.getQueryString();

		String corpId = Env.AUTH_CORP_ID;
		String appId = Env.AUTH_AGENT_ID;
		log.info(df.format(new Date()) + " getconfig,url:" + urlString + " query:" + queryString + " corpid:" + corpId
				+ " appid:" + appId);

		String queryStringEncode = null;
		String url;
		if (queryString != null) {
			queryStringEncode = URLDecoder.decode(queryString);
			url = urlString + "?" + queryStringEncode;
		} else {
			url = urlString;
		}
		log.info(url);
		String nonceStr = "abcdefg";
		long timeStamp = System.currentTimeMillis() / 1000;
		String signedUrl = url;
		String accessToken = null;
		String ticket = null;
		String signature = null;
		String agentid = null;

		try {
			accessToken = AuthHelper.getAccessToken(corpId);
			ticket = AuthHelper.getJsapiTicket(accessToken, corpId);
			signature = AuthHelper.sign(ticket, nonceStr, timeStamp, signedUrl);
			agentid = AuthHelper.getAgentId(corpId, appId);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("accessToken:" + accessToken);
		log.info("ticket:" + ticket);
		log.info("signature:" + signature);
		log.info("nonceStr:" + nonceStr);
		log.info("timeStamp:" + timeStamp);
		log.info("agentid:" + agentid);
		log.info("corpId:" + corpId);
		log.info("appId:" + appId);

		return "{jsticket:'" + ticket + "',signature:'" + signature + "',nonceStr:'" + nonceStr + "',timeStamp:'"
				+ timeStamp + "',corpId:'" + corpId + "',agentId:'" + agentid + "',appId:'" + appId + "'}";
	}

	public static String getAgentId(String corpId, String appId) {
		String agentId = null;
		String accessToken = FileUtils.getValue("ticket", "suiteToken").toString();
		String url = "https://oapi.dingtalk.com/service/get_auth_info?suite_access_token=" + accessToken;
		JSONObject args = new JSONObject();
		args.put("suite_key", Env.SUITE_KEY);
		args.put("auth_corpid", corpId);
		args.put("permanent_code", FileUtils.getValue("permanentcode", corpId));

		try {
			JSONObject response = HttpHelper.doPost(url, args);

			if (response.containsKey("auth_info")) {
				JSONArray agents = (JSONArray) ((JSONObject) response.get("auth_info")).get("agent");

				for (int i = 0; i < agents.size(); i++) {

					if (((JSONObject) agents.get(i)).get("appid").toString().equals(appId)) {
						agentId = ((JSONObject) agents.get(i)).get("agentid").toString();
						break;
					}
				}
			} else {
				throw new Exception("agentid 获取失败");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return agentId;
	}

}

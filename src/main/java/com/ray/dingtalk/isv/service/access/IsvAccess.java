package com.ray.dingtalk.isv.service.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ray.dingtalk.isv.util.HttpHelper;

/**@desc  :  钉钉ISV接入
 * 
 * 
 * 
 * @author: shirayner
 * @date  : 2017年11月14日 上午11:08:53
 */
public class IsvAccess {
	
	private static final Logger log = LogManager.getLogger(IsvAccess.class);

	//1.获取应用套件令牌
	private static final String GET_SUITE_ACCESSTOKEN_URL="https://oapi.dingtalk.com/service/get_suite_token";
	//2.获取企业授权的永久授权码
	private static final String GET_PERMANENT_CODE_URL="https://oapi.dingtalk.com/service/get_permanent_code?suite_access_token=SUITE_ACCESS_TOKEN";
	//3.获取企业授权的凭证
	private static final String GET_CROP_ACCESSTOKEN_URL="https://oapi.dingtalk.com/service/get_corp_token?suite_access_token=SUITE_ACCESS_TOKEN";

	//5.获取企业授权信息
	private static final String GET_CORP_AUTHINFO_URL="https://oapi.dingtalk.com/service/get_auth_info?suite_access_token=SUITE_ACCESS_TOKEN";
	//6.获取企业授权的权限范围
	private static final String GET_AUTH_SCOPE_URL="https://oapi.dingtalk.com/auth/scopes?access_token=ACCESS_TOKEN";
	//7.获取授权方企业的应用信息
	private static final String GET_AUTHCROP_AGENTINFO_URL="https://oapi.dingtalk.com/service/get_agent?suite_access_token=SUITE_ACCESS_TOKEN";
	//8.激活授权套件
	private static final String ACTIVATE_SUITE_URL="https://oapi.dingtalk.com/service/activate_suite?suite_access_token=SUITE_ACCESS_TOKEN";
	//9.ISV为授权方的企业单独设置IP白名单
	private static final String SET_CORP_IPWHITELIST_URL="https://oapi.dingtalk.com/service/set_corp_ipwhitelist?suite_access_token=SUITE_ACCESS_TOKEN";

	//10.获取jsapi_ticket
	private static final String GET_JSAPITICKET_URL="https://oapi.dingtalk.com/get_jsapi_ticket?access_token=ACCESSTOKE"; 

	/**
	 * @desc ：  1、获取应用套件令牌（suite_access_token）
	 *   获取套件访问Token
	 * @param suiteKey     应用套件的key
	 * @param suiteSecret  应用套件secret
	 * @param suiteTicket  钉钉后台推送的ticket
	 * 
	 * @return 
	 *        suite_access_token  应用套件access_token
	 *        expires_in	有效期7200秒，过期之前要主动更新。（建议ISV服务端做定时器主动更新，而不是依赖钉钉的定时推送）
	 * @throws Exception String    
	 */
	public static String getSuiteAccessToken(String suiteKey,String suiteSecret,String suiteTicket) throws Exception {
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();

		requestParameterMap.put("suite_key", suiteKey);
		requestParameterMap.put("suite_secret", suiteSecret);
		requestParameterMap.put("suite_ticket", suiteTicket);


		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=GET_SUITE_ACCESSTOKEN_URL;

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doPost(url, data);

		log.info(jsonObject.toJSONString());
		//5.解析结果，获取accessToken
		String suiteAccessToken="";  
		if (null != jsonObject) {  
			suiteAccessToken=jsonObject.getString("suite_access_token");

			//5.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}  
		}  


		return suiteAccessToken;
	}


	/**
	 * @desc ：2.获取企业授权的永久授权码
	 *  
	 * @param suiteAccessToken  应用套件令牌
	 * @param tmpAuthCode       回调接口（tmp_auth_code）获取的临时授权码
	 * 
	 * @return  
	 *        permanent_code     永久授权码
	 *        auth_corp_info	授权方企业信息
	 *              corpid	                  授权方企业id
	 *              corp_name	                  授权方企业名称
	 * @throws Exception String
	 */
	public static JSONObject getPermanentCodeAndCropId(String suiteAccessToken,String tmpAuthCode){
		log.info("tmpAuthCode:"+tmpAuthCode);
		
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();
		requestParameterMap.put("tmp_auth_code", tmpAuthCode);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=GET_PERMANENT_CODE_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject=null;
		JSONObject returnJson=null;  
		try {

			jsonObject= HttpHelper.doPost(url, data);

			log.info(jsonObject.toJSONString());
			//5.解析结果，获取accessToken

			if (null != jsonObject) {  
				returnJson=jsonObject;

				//6.错误消息处理
				if (0 != jsonObject.getInteger("errcode")) {  
					int errCode = jsonObject.getInteger("errcode");
					String errMsg = jsonObject.getString("errmsg");

					throw new Exception("error code:"+errCode+", error message:"+errMsg);

				}  
			}  


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return returnJson;
	}


	/**
	 * @desc ：3.获取企业授权的access_token
	 *  服务提供商在取得企业的永久授权码并完成对企业应用的设置之后，便可以开始通过调用企业接口来运营这些应用。
	 * @param suiteAccessToken  应用套件令牌
	 * @param authCorpId        授权方corpid
	 * @param permanentCode     永久授权码，通过get_permanent_code获取
	 * 
	 * @return 
	 *        accessToken       授权方（企业）access_token
	 *        expires_in        授权方（企业）access_token超时时间
	 * @throws Exception String
	 */
	public static String getCropAccessToken(String suiteAccessToken,String authCorpId,String permanentCode) throws Exception {
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();
		requestParameterMap.put("auth_corpid", authCorpId);
		requestParameterMap.put("permanent_code", permanentCode);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=GET_CROP_ACCESSTOKEN_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doPost(url, data);

		log.info(jsonObject.toJSONString());
		//5.解析结果，获取accessToken
		String accessToken="";  
		if (null != jsonObject) {  
			accessToken=jsonObject.getString("access_token");

			//6.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}  
		}  


		return accessToken;
	}


	
	/**
	 * @desc ：5.获取企业授权信息
	 *  
	 * @param suiteAccessToken  应用套件令牌
	 * @param authCorpId        授权方corpid
	 * @param suiteKey          应用套件key
	 * 
	 * @return  
	 * 三个json元素
	 *        auth_corp_info	授权方企业信息
	 *        auth_user_info	授权方管理员信息
	 *        auth_info         授权信息
	 * 
	 * @throws Exception JSONObject
	 */
	public static JSONObject getCropAuthInfo(String suiteAccessToken,String authCorpId,String suiteKey) throws Exception {
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();
		requestParameterMap.put("auth_corpid", authCorpId);
		requestParameterMap.put("suite_key", suiteKey);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=GET_CORP_AUTHINFO_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doPost(url, data);

		log.info(jsonObject.toJSONString());
		//5.解析结果，获取accessToken
		if (null != jsonObject) {  	
			//6.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}else {
				return jsonObject;
			}  
		}  

		return null;
	}

	/**
	 * @desc ：6.获取企业授权的权限范围
	 *  
	 * @param accessToken    
	 * @return
	 * @throws Exception JSONObject
	 */
	public static JSONObject getAuthScope(String accessToken) throws Exception {

		//3.获取请求url
		String url=GET_AUTH_SCOPE_URL.replace("ACCESS_TOKEN", accessToken);

		//4.发起GET请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doGet(url);
		log.info(jsonObject.toJSONString());

		//5.解析结果，获取accessToken
		if (null != jsonObject) {  	
			//6.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}else {
				return jsonObject;
			}  
		}  

		return null;
	}

	/**
	 * @desc ：7. 获取授权方企业的应用信息
	 *  
	 * @param suiteAccessToken   应用套件令牌
	 * @param suiteKey           应用套件key
	 * @param authCorpid         授权方corpid
	 * @param permanentCode      永久授权码，从get_permanent_code接口中获取
	 * @param authAgentId        授权方应用id
	 * 
	 * @return
	 *        agentid	                   授权方企业应用id
	 *        name				 授权方企业应用名称
	 *        logo_url			 授权方企业应用头像
	 *        description		 授权方企业应用详情
	 *        close				 授权方企业应用是否被禁用（0:禁用 1:正常 2:待激活 ）
	 * 
	 * @throws Exception JSONObject
	 */
	public static JSONObject getAuthCropAgentInfo(String suiteAccessToken,String suiteKey,String authCorpId,String permanentCode,String authAgentId) throws Exception {
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();
		requestParameterMap.put("suite_key", suiteKey);
		requestParameterMap.put("auth_corpid", authCorpId);
		requestParameterMap.put("permanent_code", permanentCode);
		requestParameterMap.put("agentid", authAgentId);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=GET_AUTHCROP_AGENTINFO_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doPost(url, data);

		log.info(jsonObject.toJSONString());
		//5.解析结果，获取accessToken
		if (null != jsonObject) {  	
			//6.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}else {
				return jsonObject;
			}  
		}  

		return null;
	}
	/**
	 * @desc ： 8.激活授权套件
	 *  
	 * @param suiteAccessToken    应用套件令牌
	 * @param suiteKey            应用套件key
	 * @param authCorpId          授权方corpid
	 * @param permanentCode       永久授权码，从get_permanent_code接口中获取
	 * @throws Exception void
	 */
	public static void activateSuite(String suiteAccessToken,String suiteKey,String authCorpId,String permanentCode) {
		//1.封装请求参数
		Map<String ,String > requestParameterMap=new HashMap<String,String>();
		requestParameterMap.put("suite_key", suiteKey);
		requestParameterMap.put("auth_corpid", authCorpId);
		requestParameterMap.put("permanent_code", permanentCode);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=ACTIVATE_SUITE_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		try {

			JSONObject jsonObject = HttpHelper.doPost(url, data);
			log.info(jsonObject.toJSONString());

			//5.解析结果，获取
			if (null != jsonObject) {  	
				//6.错误消息处理
				if (0 != jsonObject.getInteger("errcode")) {  
					int errCode = jsonObject.getInteger("errcode");
					String errMsg = jsonObject.getString("errmsg");
					throw new Exception("error code:"+errCode+", error message:"+errMsg); 
				}  
			}  

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	/**
	 * @desc ： 9.ISV为授权方的企业单独设置IP白名单
	 *  
	 * @param suiteAccessToken  应用套件令牌
	 * @param authCorpId  授权方corpid
	 * @param ip_whitelist    要为其设置的IP白名单,格式支持IP段,用星号表示,
	 *                        如【5.6.*.*】,代表从【5.6.0.*】到【5.6.255.*】的任意IP,
	 *           在第三段设为星号时,将忽略第四段的值,注意:仅支持后两段设置为星号
	 * @throws Exception void
	 */
	public static void setCorpIPWhiteList(String suiteAccessToken,String authCorpId,List<String> ip_whitelist ) throws Exception {
		//1.封装请求参数
		Map<Object,Object> requestParameterMap=new HashMap<Object,Object>();
		requestParameterMap.put("auth_corpid", authCorpId);
		requestParameterMap.put("ip_whitelist", ip_whitelist);

		//2.将Map转为JSONObject
		Object data=JSON.toJSON(requestParameterMap);

		//3.获取请求url
		String url=SET_CORP_IPWHITELIST_URL.replace("SUITE_ACCESS_TOKEN", suiteAccessToken);

		//4.发起POST请求，获取返回结果
		JSONObject jsonObject = HttpHelper.doPost(url, data);
		log.info(jsonObject.toJSONString());

		//5.解析结果，获取
		if (null != jsonObject) {  	
			//6.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}  
		}  

	}

	/**
	 * 10、获取JSTicket, 用于js的签名计算
	 * 正常的情况下，jsapi_ticket的有效期为7200秒，所以开发者需要在某个地方设计一个定时器，定期去更新jsapi_ticket
	 * @throws Exception 
	 */
	/**
	 * @desc ：
	 *  
	 * @param accessToken
	 * @return
	 * @throws Exception String
	 */
	public static String getJsapiTicket(String accessToken) throws Exception  {
		//1.获取请求url
		String url=GET_JSAPITICKET_URL.replace("ACCESSTOKE", accessToken);

		//2.发起GET请求，获取返回结果
		JSONObject jsonObject=HttpHelper.doGet(url);

		//3.解析结果，获取ticket
		String ticket="";  
		if (null != jsonObject) {  
			ticket=jsonObject.getString("ticket");

			//4.错误消息处理
			if (0 != jsonObject.getInteger("errcode")) {  
				int errCode = jsonObject.getInteger("errcode");
				String errMsg = jsonObject.getString("errmsg");
				throw new Exception("error code:"+errCode+", error message:"+errMsg); 
			}  
		}  

		return ticket;
	}


}

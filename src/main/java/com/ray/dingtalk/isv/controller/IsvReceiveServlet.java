package com.ray.dingtalk.isv.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ray.dingtalk.isv.auth.AuthHelper;
import com.ray.dingtalk.isv.auth.thread.TokenRunnable;
import com.ray.dingtalk.isv.config.Env;
import com.ray.dingtalk.isv.service.access.IsvAccess;
import com.ray.dingtalk.isv.util.FileUtils;
import com.ray.dingtalk.isv.util.aes.DingTalkEncryptException;
import com.ray.dingtalk.isv.util.aes.DingTalkEncryptor;

/**
 * Servlet implementation class DingTalkServlet  这个servlet用来接收钉钉服务器回调接口的推送
 */
public class IsvReceiveServlet extends HttpServlet {
	
	private static final Logger log = LogManager.getLogger(IsvReceiveServlet.class);
	
	private static final long serialVersionUID = 1L;


	public IsvReceiveServlet() {

	}
	

	/**
	 * 1.接收回调url
	 * 
	 * 验证回调模式是使用GET方法的
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		//1.将请求、响应的编码均设置为UTF-8（防止中文乱码）  
		request.setCharacterEncoding("UTF-8");  
		response.setCharacterEncoding("UTF-8");  

		//2.获取请求参数

		//2.1 ISV加密签名  
		String msgSignature = request.getParameter("signature");  
		//2.2  时间戳  
		String timeStamp = request.getParameter("timestamp");  
		//2.3  随机数  
		String nonce = request.getParameter("nonce");  

		//2.4 post数据包数据中的加密数据
		ServletInputStream sis = request.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(sis));
		String line = null;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		JSONObject jsonEncrypt = JSONObject.parseObject(sb.toString());
		String encrypt = jsonEncrypt.getString("encrypt");


		//3.对回调的参数进行解密，确保请求合法
		//3.1 通过检验msg_signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败  
		String plainText = null;  
		DingTalkEncryptor dingTalkEncryptor=null;
		try {
			//3.2 创建加解密类
			dingTalkEncryptor = new DingTalkEncryptor(Env.TOKEN, Env.ENCODING_AES_KEY, Env.SUITE_KEY);  //第一次创建套件的时候要使用CREATE_SUITE_KEY
			//3.3 获取从encrypt解密出来的明文
			plainText = dingTalkEncryptor.getDecryptMsg(msgSignature, timeStamp, nonce, encrypt);
			log.info("plainText:"+plainText);

		} catch (DingTalkEncryptException e) {
			e.printStackTrace();
		} finally {

		}
		//4.处理明文
		//  对从encrypt解密出来的明文进行处理，不同的eventType的明文数据格式不同
		JSONObject plainTextJson = JSONObject.parseObject(plainText);
		String eventType = plainTextJson.getString("EventType");


		// res是需要返回给钉钉服务器的字符串，一般为success
		// "check_create_suite_url"和"check_update_suite_url"事件为random字段
		String res = "success";
		switch (eventType) {
		//4.1 创建套件
		case "check_create_suite_url":      //第一次创建套件时
			//此事件需要返回的"Random"字段
			res = plainTextJson.getString("Random");
			break;

			//4.2 修改套件
		case "check_update_suite_url":      //修改套件时
			//此事件需要返回的"Random"字段
			res = plainTextJson.getString("Random");
			break;

			//4.3 Ticket推送
		case "suite_ticket":
			/*"suite_ticket"事件每二十分钟推送一次,数据格式如下
			 * {
				  "SuiteKey": "suitexxxxxx",
				  "EventType": "suite_ticket ",
				  "TimeStamp": 1234456,
				  "SuiteTicket": "adsadsad"
				}
			 */
			log.info("22222222222");
			Env.suiteTicket = plainTextJson.getString("SuiteTicket");
			//获取到suiteTicket之后需要换取suiteToken，
			String suiteToken=null;
			try {

				suiteToken = IsvAccess.getSuiteAccessToken(Env.SUITE_KEY, Env.SUITE_SECRET, Env.suiteTicket);

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			/*
			 * ISV应当把最新推送的suiteTicket做持久化存储，
			 * 以防重启服务器之后丢失了当前的suiteTicket
			 * 
			 */
			JSONObject json = new JSONObject();
			json.put("suiteTicket", Env.suiteTicket);
			json.put("suiteToken", suiteToken);
			FileUtils.write2File(json, "ticket");
			break;

			//4.4 授权套件
		case "tmp_auth_code":
			/*"tmp_auth_code"事件将企业对套件发起授权的时候推送
			 * 数据格式如下
			{
			  "SuiteKey": "suitexxxxxx",
			  "EventType": " tmp_auth_code",
			  "TimeStamp": 1234456,
			  "AuthCode": "adads"
			}			 
			 */
			Env.authCode = plainTextJson.getString("AuthCode");
			log.info("Env.authCode:"+Env.authCode);
			log.info("plainTextJson:"+plainTextJson.toJSONString());
			
			Object value = FileUtils.getValue("ticket", "suiteToken");//获取当前的suiteToken
			if (value == null) {
				break;
			}
			String suiteAccessToken = value.toString();
			
			/*
			 * 拿到tmp_auth_code（临时授权码）后，需要向钉钉服务器获取企业的corpId（企业id）和permanent_code（永久授权码）
			 */
			
			JSONObject permanentCodeAndCorpId =IsvAccess.getPermanentCodeAndCropId(suiteAccessToken, Env.authCode);
			log.info("permanentCodeAndCorpId:"+permanentCodeAndCorpId.toJSONString());
			String permanent_code = permanentCodeAndCorpId.getString("permanent_code");
		
			JSONObject auth_corp_info=permanentCodeAndCorpId.getJSONObject("auth_corp_info");
			String authCorpId=auth_corp_info.getString("corpid");
		
			/*
			 * 将corpId（企业id）和permanent_code（永久授权码）做持久化存储
			 * 之后在获取企业的access_token时需要使用
			 */
			JSONObject jsonPerm = new JSONObject();
			jsonPerm.put(authCorpId, permanent_code);
			FileUtils.write2File(jsonPerm, "permanentcode");
			/*
			 * 对企业授权的套件发起激活，
			 */
			IsvAccess.activateSuite(suiteAccessToken, Env.SUITE_KEY, authCorpId, permanent_code);
		
			/*
			 * 获取对应企业的access_token，每一个企业都会有一个对应的access_token，访问对应企业的数据都将需要带上这个access_token
			 * access_token的过期时间为两个小时
			 */
			try {
				AuthHelper.getAccessToken(authCorpId);

				log.info("授权套件成功————————————————————————————————————————————");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				System.out.println(e1.toString());
				e1.printStackTrace();
			}
			break;
			
			
		default: // do something
			break;
		}

		// 5.对返回信息进行加密
		long timeStampLong = Long.parseLong(timeStamp);
		Map<String, String> jsonMap = null;
		try {
			// jsonMap是需要返回给钉钉服务器的加密数据包
			jsonMap = dingTalkEncryptor.getEncryptedMap(res, timeStampLong, nonce);
		} catch (DingTalkEncryptException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		JSONObject json = new JSONObject();
		json.putAll(jsonMap);
		response.getWriter().append(json.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);

	}


	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		super.init(config);
		log.info("init---------------");
	
		
		Runnable tokenRunnable=new TokenRunnable();
		Thread thread=new Thread(tokenRunnable);
		thread.start();
		
		
		
		
		
	}

	
	
	
}

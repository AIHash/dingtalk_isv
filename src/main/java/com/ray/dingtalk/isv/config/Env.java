package com.ray.dingtalk.isv.config;

/**@desc  : 企业应用接入时的常量定义
 * 
 * @author: shirayner
 * @date  : 2017年9月27日 下午4:57:36
 */

public class Env {

    /**
     * 1.企业通讯回调加密Token，注册事件回调接口时需要传递给钉钉服务器
     */
	public static final String TOKEN = "isv";
	public static final String ENCODING_AES_KEY = "ts5lymw16n6exkp7w1chwn4g55pj62xjumo3rv4zzx6";
	public static final String CREATE_SUITE_KEY = "suite4xxxxxxxxxxxxxxx";
	
	public static final String SUITE_KEY = "suitenqeusfld8dqkseqs";
	public static final String SUITE_SECRET = "FROBdyvEPcAAGVdBKMNVYl_UInH2nWzOqHHUw_kbWDnAQSNBazxWgKiVRcOOk9Ju";
	

	public static String suiteTicket; 
	public static String authCode; 
	public static String suiteToken; 
	
	
	
	/**
     * 2. 免登时，授权方企业（ISV测试企业的配置）
     */
	public static final String AUTH_CORP_ID="ding86a8a5b5a7b5872135c2f4657eb6378f";
	//public static final String AUTH_AGENT_ID="134008280";
	public static final String AUTH_AGENT_ID="4404";
	



}

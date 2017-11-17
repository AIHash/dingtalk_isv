package com.ray.dingtalk.isv.service.user;

import com.alibaba.fastjson.JSONObject;
import com.ray.dingtalk.isv.util.HttpHelper;

/**@desc  : 用户业务类
 * 
 * @author: shirayner
 * @date  : 2017年11月16日 下午4:37:03
 */
public class UserService {
	//1.通过CODE换取用户身份
	private static final String GET_USER_INFO="https://oapi.dingtalk.com/user/getuserinfo?access_token=ACCESS_TOKEN&code=CODE";

	/** 1.通过CODE换取用户身份
	 * @desc ：
	 *  
	 * @param accessToken   调用接口凭证
	 * @param code  通过Oauth认证会给URL带上CODE
	 * @return 
	 *   JSONObject    用户信息
	 *   userid	         员工在企业内的UserID
	 *   deviceId	手机设备号,由钉钉在安装时随机产生
	 *   is_sys	         是否是管理员
	 *   sys_level	级别，三种取值。0:非管理员 1：普通管理员 2：超级管理员
	 *   
	 */
	public static JSONObject getUserInfoByCode(String accessToken,String code) {
		//1.准备url
		String url=GET_USER_INFO.replace("ACCESS_TOKEN", accessToken).replace("CODE", code);


		JSONObject jsonObject;
		JSONObject returnJson=null;
		try {
			//2.发起GET请求，获取返回结果
			jsonObject = HttpHelper.doGet(url);

			//3.解析结果
			if (null != jsonObject) {  	
				//4.错误消息处理
				if (0 != jsonObject.getInteger("errcode")) {  
					int errCode = jsonObject.getInteger("errcode");
					String errMsg = jsonObject.getString("errmsg");
					throw new Exception("error code:"+errCode+", error message:"+errMsg); 
				}else {
					returnJson=jsonObject;
				}  
			} 

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return returnJson;
	}


}

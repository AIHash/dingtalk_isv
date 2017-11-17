package com.ray.dingtalk.isv.controller.idauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ray.dingtalk.isv.auth.AuthHelper;
import com.ray.dingtalk.isv.config.Env;
import com.ray.dingtalk.isv.service.user.UserService;

/**
 * Servlet implementation class UserInfoServlet
 */
public class UserInfoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UserInfoServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException  {
		//1.将请求、响应的编码均设置为UTF-8（防止中文乱码）  
		request.setCharacterEncoding("UTF-8");  
		response.setCharacterEncoding("UTF-8"); 

		//1.获取code
		String code = request.getParameter("code");
		System.out.println("code:"+code);

		Object result=null;
		String userId=null;
		try {
			//2.通过CODE换取身份userid
			String accessToken = AuthHelper.getAccessToken(Env.AUTH_CORP_ID);
			userId=UserService.getUserInfoByCode(accessToken, code).getString("userid");

			//3.通过userid换取用户信息
			//result=us.getUser(accessToken, userId);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		PrintWriter out = response.getWriter(); 
		out.print(userId);  
		out.close();  
		out = null;  
	}

}

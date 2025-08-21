package jp.co.kpscorp.rc6.service;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.apache.catalina.connector.Connector;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.method.annotation.ModelFactory;

import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BatchExcuter {

	public String goBatch(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		System.out.println("Batch Mode!");
		String key = (String) req.getAttribute("key");
		Map<String, String> pmap = null;
		pmap = AuthTokenService.getPmap(req, resp, key);
		if (pmap == null) {
			return makeRes("internal error. pmap not found", pmap);
		}
		String authParm = pmap.get(Util.authParmKey);
		// System.out.println("authParm:" + authParm);
		JSONObject jo = (JSONObject) JSONValue.parse(authParm);
		String uid = (String) jo.get(loginUidKey);
		String pwd = (String) jo.get(loginPwdKey);
		if (Util.isNouserMode(pmap)) {
			if ("true".equals(pmap.get(Downloder.DataCheckKey))) {
				return makeRes("UI無しモードでは、データの確認は使用できません。", pmap);
			}
		}
		String authEndPoint = SetterServlet.getLoginNm(pmap
				.get(AuthToken.hostSafixKey));
		// authEndPoint = authEndPoint + "/services/Soap/u/22.0/";
		// 22.0 -> 35.0 2022/04/28
		authEndPoint = authEndPoint + "/services/Soap/u/35.0/";
		ConnectorConfig config = new ConnectorConfig();
		config.setUsername(uid);
		config.setPassword(pwd);
		config.setAuthEndpoint(authEndPoint);
		System.out.println("soap login authEndPoint:" + authEndPoint);
		DownloadService ds = null;
		Connection con = null;
		LoginResult lr = null;
		PartnerConnection pc = null;
		try {
			// WebApplicationContext wac = WebApplicationContextUtils
			// .getRequiredWebApplicationContext(req.getSession()
			// .getServletContext());
			try {
				con = ModelFactory.getConnection(wac);
			} catch (Exception e) {
				throw new ServletException(e);
			}
			// if (AuthTokenService.checkIpfixOrg(pmap, con)) {
			// System.out.println("proxy mode login to "
			// + AuthTokenService.ipfixServer);
			// JSONObject jcfg = toJSONObj(config);
			// String parms = jcfg.toJSONString();
			// // parms暗号化
			// parms = AuthTokenService.encrypt(parms,
			// AuthTokenService.makeKey(AuthTokenService.proxpass));
			// //
			// String url = AuthTokenService.ipfixServer + "login?proxyParms="
			// + URLEncoder.encode(parms, "UTF-8");
			// String sres = AuthTokenService.doHttpsGet(url);
			// JSONObject jres = (JSONObject) JSONValue.parse(sres);
			// if (jres.get("exception") != null) {
			// throw new ServletException((String) jres.get("exception"));
			// }
			// jcfg = (JSONObject) jres.get("ConnectorConfig");
			// JSONObject jlr = (JSONObject) jres.get("LoginResult");
			// BeanUtils.populate(config, jcfg);
			// // config.setServiceEndpoint((String)
			// // jcfg.get("serviceEndpoint"));
			// // config.setSessionId((String) jcfg.get("sessionId"));
			// pc = new PartnerConnection(config);
			// lr = new LoginResult();
			// GetUserInfoResult uinf = new GetUserInfoResult();
			// JSONObject juinf = (JSONObject) jlr.get("userInfo");
			// jlr.remove("userInfo");
			// BeanUtils.populate(uinf, juinf);
			// BeanUtils.populate(lr, jlr);
			// lr.setUserInfo(uinf);
			// } else {
			int proxy = AuthTokenService.checkIpfixOrg(pmap, con);
			if (proxy == 1) {
				config.setAuthEndpoint(AuthTokenService.getProxyURL(config
						.getAuthEndpoint()));
				config.setRequestHeader(AuthTokenService.proxpassKey,
						AuthTokenService.proxpass);
				// config.setConnectionTimeout(20 * 1000);
			} else if (proxy == 2) {
				config.setProxy("54.201.152.47", 443);
				config.setRequestHeader(AuthTokenService.proxpassKey,
						AuthTokenService.proxpass);
			}
			pc = Connector.newConnection(config);
			System.out.println("Auth EndPoint: " + config.getAuthEndpoint());
			System.out.println("Service EndPoint: "
					+ config.getServiceEndpoint());
			System.out.println("Username: " + config.getUsername());
			// System.out.println("SessionId: " + config.getSessionId());
			System.out.println("LicenseUid: " + jo.get(licenseUidKey));
			System.out.println("LicenseOid: " + jo.get(licenseOidKey));
			System.out.println("noUI mode: " + jo.get(noUserModeKey));
			lr = pc.login(uid, pwd);
			// }
			String tk = lr.getSessionId();
			if (tk == null) {
				return makeRes("login failed!", pmap);
			}
			System.out.println("login uid: " + lr.getUserId());
			System.out.println("login oid: "
					+ lr.getUserInfo().getOrganizationId());
			pmap.put(Downloder.JSonKey, makeOAuthJsonStr(lr));
			// org 単位で処理をシリアライズする
			String orgid = lr.getUserInfo().getOrganizationId();
			synchronized (Downloder.runningBatchs) {
				ds = Downloder.runningBatchs.get(orgid);
				if (ds == null) {
					ds = new DownloadService();
					Downloder.runningBatchs.put(orgid, ds);
				}
			}
			System.out.println("Wait cnt:" + ds.addWaitCnt());
			ds.doGetSync(req, resp, orgid, pmap, con);
		} finally {
			if (ds != null) {
				int cnt = ds.subWaitCnt();
				if (cnt == 0) {
					System.out.println("remove DownloadService instance.");
					if (lr != null) {
						Downloder.runningBatchs.remove(lr.getUserId());
					}
				} else {
					System.out.println("still DownloadService waiting:" + cnt);
				}
			}
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
		if (!Util.isNouserMode(pmap)) {
			// Downloadへ
			String url = AuthTokenService.getBaseUrl(req) + "/dl?state=" + key;
			return url;
		}
		String ermsg = pmap.get(batchErrorMsgKey);
		if (ermsg != null) {
			return makeRes(ermsg, pmap);
		}
		return makeRes("ok", pmap);
	}

	public static String makeRes(String msg, Map<String, String> pmap) {
		JSONObject resobj = new JSONObject();
		resobj.put("message", msg);
		String id = pmap.get(attachIdKey);
		resobj.put("id", id);
		if (id == null) {
			resobj.put("success", false);
			System.out.println("MakeRes error:" + msg);

		} else {
			resobj.put("success", true);
		}
		resobj.put("filename", pmap.get(Downloder.FnKey) + ".pdf");
		resobj.put("parentid", pmap.get(Downloder.parentidKey));
		resobj.put("tag", kpsrcTagKey);
		return resobj.toJSONString();
	}

	/**
	 * LoginResultから、OAuthのJsonString互換のものを作成する
	 *
	 * @param s
	 * @return
	 */
	private String makeOAuthJsonStr(LoginResult res) {
		JSONObject resobj = new JSONObject();
		resobj.put("access_token", res.getSessionId());
		String sep = res.getServerUrl();
		sep = sep.substring(0, sep.indexOf("/services"));
		resobj.put("instance_url", sep);
		GetUserInfoResult ui = res.getUserInfo();
		String oid = ui.getOrganizationId();
		String uid = ui.getUserId();
		resobj.put("id", "https:login.salesforce.com" + "/id/" + oid + "/"
				+ uid);
		return resobj.toJSONString();
	}

	private JSONObject toJSONObj(Object o) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		JSONObject res = new JSONObject();
		Map<String, Object> mp = Utils.getValueMap(o);
		for (String key : mp.keySet()) {
			Object val = mp.get(key);
			if (val instanceof Number || val instanceof Boolean
					|| val instanceof String) {
				res.put(key, val);
			} else {
				// System.out.println("toJSONObj prop " + key + " droped! val:"
				// + val);
			}
		}
		return res;
	}

}

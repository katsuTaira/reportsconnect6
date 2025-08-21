package jp.co.kpscorp.rc6.cont;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.component.UploadChecker;
import jp.co.kpscorp.rc6.component.Utils;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.sfdc.SalesforceAuthenticator;

@Service
@Scope("prototype")
public class BatchExcuter {
	public static final String authParmKey = "kps_aParm";
	public static final String batchErrorMsgKey = "kps_batchErrorMsg";
	public static final String attachIdKey = "kps_attachId";
	public static final String batchLicenseKey = "batch";
	// authParm uinfo keys
	public static final String loginUidKey = "uid";
	public static final String loginPwdKey = "pwd";
	public static final String licenseUidKey = "luid";
	public static final String licenseOidKey = "loid";
	public static final String noUserModeKey = "nouser";
	public static final String feedDescKey = "desc";
	public static final String versionKey = "ver";
	public static final String kpsrcTagKey = "kps-reportsconnect";

	@Autowired
	private ApplicationContext con;

	@Autowired
	private LicenseRepository rLicense;

	public String goBatch(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		System.out.println("Batch Mode!");
		String key = (String) req.getAttribute("key");
		Map<String, String> pmap = null;
		pmap = Utils.getPmap(req, key);
		if (pmap == null) {
			return makeRes("internal error. pmap not found", pmap);
		}
		String authParm = pmap.get(authParmKey);
		// System.out.println("authParm:" + authParm);
		JSONObject jo = (JSONObject) JSONValue.parse(authParm);
		String uid = (String) jo.get(loginUidKey);
		String pwd = (String) jo.get(loginPwdKey);
		if (isNouserMode(pmap)) {
			if ("true".equals(pmap.get(Utils.DataCheckKey))) {
				return makeRes("UI無しモードでは、データの確認は使用できません。", pmap);
			}
		}
		String authEndPoint = SetterController.getLoginNm(pmap
				.get(Utils.hostSafixKey), rLicense);
		// authEndPoint = authEndPoint + "/services/Soap/u/22.0/";
		// 22.0 -> 35.0 2022/04/28
		// authEndPoint = authEndPoint + "/services/Soap/u/35.0/";
		// restに変更
		authEndPoint = authEndPoint + "/services/oauth2/token";
		// ConnectorConfig config = new ConnectorConfig();
		// config.setUsername(uid);
		// config.setPassword(pwd);
		// config.setAuthEndpoint(authEndPoint);
		System.out.println("soap login authEndPoint:" + authEndPoint);
		DownloadController ds = null;
		// LoginResult lr = null;
		// PartnerConnection pc = null;
		JSONObject loginJson = null;
		try {
			int proxy = Utils.checkIpfixOrg(pmap, con);
			if (proxy == 1) {
				authEndPoint = Utils.getProxyURL(authEndPoint);
			}
			// とりあえず、proxy 2 は保留
			System.out.println("Auth EndPoint: " + authEndPoint);
			System.out.println("Username: " + uid);
			// System.out.println("SessionId: " + config.getSessionId());
			System.out.println("LicenseUid: " + jo.get(licenseUidKey));
			System.out.println("LicenseOid: " + jo.get(licenseOidKey));
			System.out.println("noUI mode: " + jo.get(noUserModeKey));
			String jsonStr = SalesforceAuthenticator.getLoginRes(uid, pwd, pmap.get("client_id"),
					pmap.get(Utils.csecKey),
					authEndPoint);
			if (jsonStr == null) {
				// return makeRes("login failed!", pmap);
				// login失敗時は exception にする
				String msg = makeRes("login failed!", pmap);
				throw new Exception(msg);
			}
			JSONParser parser = new JSONParser();
			loginJson = (JSONObject) parser.parse(jsonStr);
			String tk = (String) loginJson.get("access_token");
			if (tk == null) {
				// login失敗時は exception にする
				// String msg = makeRes("login failed!", pmap);
				throw new Exception(jsonStr);
			}
			System.out.println("login id: " + loginJson.get("id"));
			pmap.put(Utils.JSonKey, jsonStr);
			// org 単位で処理をシリアライズする
			// String orgid = lr.getUserInfo().getOrganizationId();
			String orgid = Utils.exOrgId((String) loginJson.get("id"));
			synchronized (DownloadController.runningBatchs) {
				ds = DownloadController.runningBatchs.get(orgid);
				if (ds == null) {
					// ds = new DownloadService();
					ds = con.getBean(DownloadController.class);
					DownloadController.runningBatchs.put(orgid, ds);
				}
			}
			System.out.println("Wait cnt:" + ds.addWaitCnt());
			ds.doGetSync(req, resp, orgid, pmap);
		} finally {
			if (ds != null) {
				int cnt = ds.subWaitCnt();
				if (cnt == 0) {
					System.out.println("remove DownloadService instance.");
					if (loginJson != null) {
						String userId = Utils.exUsrId((String) loginJson.get("id"));
						DownloadController.runningBatchs.remove(userId);
					}
				} else {
					System.out.println("still DownloadService waiting:" + cnt);
				}
			}
			// if (con != null) {
			// try {
			// con.close();
			// } catch (SQLException e) {
			// }
			// }
		}
		if (!isNouserMode(pmap)) {
			// Downloadへ
			String url = Utils.getBaseUrl(req) + "/dl?state=" + key;
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
		resobj.put("filename", pmap.get(UploadChecker.FnKey) + ".pdf");
		resobj.put("parentid", pmap.get(Utils.parentidKey));
		resobj.put("tag", kpsrcTagKey);
		return resobj.toJSONString();
	}

	/**
	 * LoginResultから、OAuthのJsonString互換のものを作成する
	 *
	 * @param s
	 * @return
	 */
	// private String makeOAuthJsonStr(LoginResult res) {
	// JSONObject resobj = new JSONObject();
	// resobj.put("access_token", res.getSessionId());
	// String sep = res.getServerUrl();
	// sep = sep.substring(0, sep.indexOf("/services"));
	// resobj.put("instance_url", sep);
	// GetUserInfoResult ui = res.getUserInfo();
	// String oid = ui.getOrganizationId();
	// String uid = ui.getUserId();
	// resobj.put("id", "https:login.salesforce.com" + "/id/" + oid + "/"
	// + uid);
	// return resobj.toJSONString();
	// }

	public static boolean isNouserMode(Map<String, String> pmap) {
		if (pmap == null) {
			return false;
		}
		String authParm = pmap.get(authParmKey);
		if (authParm == null) {
			return false;
		}
		JSONObject jo = (JSONObject) JSONValue.parse(authParm);
		Boolean noUserMode = (Boolean) jo.get(noUserModeKey);
		if (noUserMode == null) {
			return false;
		}
		return noUserMode;
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

package jp.co.kpscorp.rc6.sfdc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import jp.co.kpscorp.rc6.service.DownloadService.RCException;
import jp.co.kpscorp.util.CaseFreeMap;
import jp.co.kpscorp.util.CaseFreeMapNotNull;
import net.arnx.jsonic.JSON;

@Service
@Scope("request")
public class SfdcConnectImpl implements SfdcConnect {

	private static Logger logger = Logger.getLogger(SfdcConnectImpl.class);
	// manyToOneの参照先オブジェクト名を文字列配列で指定（ｓｆｄｃ以外は１つしか指定しない）
	public static String fldreferenceTo = "referenceTo";

	private LoginResult loginResult;

	private String mailhost;

	public void setMailhost(String mailhost) {
		this.mailhost = mailhost;
	}

	/**
	 *
	 */
	public SfdcConnectImpl() {
		super();

	}

	private String token;

	public void setToken(String token) {
		this.token = token;
	}

	private String serverUrl;

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public LoginResult getLoginResult() throws ConnectionException, IOException {
		if (this.uid == null) {
			setupUid();
		}
		if (this.loginResult == null) {
			this.loginResult = doLogin(uid, pwd);
		}
		return loginResult;
	}

	@Override
	public void logout() {
		this.loginResult = null;
	}

	private String uid;

	private String pwd;

	protected boolean sesExpired = false;

	public static String authEndPoint = "https://login.salesforce.com";

	public String getUid() {
		return uid;
	}

	public String getPwd() {
		return pwd;
	}

	@Override
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jp.co.kpscorp.demo.model.sfdc.SfdcConnect#getDesc(java.lang.String)
	 */
	@Override
	public ObjMeta getDesc(String obj) throws Exception {
		Map<String, Map<String, Object>> mp = new CaseFreeMapNotNull(); // CaseFreeMap -> CaseFreeMapNotNull 2023/08/22
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_Describe(surl, convertKey4Bean(obj));
		Map<String, Object> wmp = getMapres(new HttpGet(url));
		// logger.debug(JSON.encode(wmp));
		if (wmp.get("errorCode") != null) {
			logger.error(wmp);
			String msg = wmp.get("message") + "";
			String er = wmp.get("errorCode") + "";
			if (er.contains("INVALID_SESSION_ID") && !this.sesExpired) {
				logger.info("Session expired!");
				this.sesExpired = true;
				this.loginResult = null;
				return getDesc(obj);
			}
			this.sesExpired = false;
			throw new SfdcConnectException(er + "\n" + msg);
		}
		this.sesExpired = false;
		if (wmp.get("httpstatus") == null || !wmp.get("httpstatus").toString().equals("200")) {
			throw new SfdcConnectException(wmp.get("errorCode") + "\n" + wmp.get("message"));
		}
		List<Map<String, Object>> fldjs = (List<Map<String, Object>>) wmp.get("fields");
		if (fldjs == null) {
			logger.error("fields is null on obj:" + obj);

		}
		// logger.debug(JSON.encode(fldjs));
		for (Object fldo : fldjs) {
			Map<String, Object> wfldj = (Map<String, Object>) fldo;
			Map<String, Object> fldj = new CaseFreeMap<>();
			fldj.putAll(wfldj);
			// test
			// String nm = (String) fldj.get("name");
			// if (nm.length() > 2 && nm.endsWith("Id") &&
			// CommonData.ftypereference.equals(fldj.get("type"))) {
			// nm = nm.substring(0, nm.length() - "Id".length());
			// fldj.put("name", nm);
			// }
			// referenceToがgroupとuserの場合はとりあえずgroup外す
			List<String> refto = (List<String>) fldj.get(fldreferenceTo);
			if (refto.contains("Group") && refto.size() > 1) {
				refto.remove("Group");
			}
			mp.put((String) fldj.get("name"), fldj);
		}
		wmp.remove("fields");
		// API Version統一
		url = SfdcApiUrlUtil.getRestURL_BasicSObject(surl, convertKey4Bean(obj));
		Map<String, Object> wmp2 = getMapres(new HttpGet(url));
		// API Version統一
		url = SfdcApiUrlUtil.getRestURL_ListViews(surl, convertKey4Bean(obj));
		Map<String, Object> wmp3 = getMapres(new HttpGet(url));
		ObjMeta om = new ObjMeta(mp, wmp, wmp2, wmp3);
		return om;
	}

	@Override
	public Map<String, Object> getViewDesc(String obj, String vid) throws Exception {
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_ListViewDescribe(surl, convertKey4Bean(obj), vid);
		return getMapres(new HttpGet(url));
	}

	/**
	 * SFDCアクセス用PartnerConnectionをUID,PWDから作成
	 *
	 * @param uid
	 * @param pwd
	 * @return
	 * @throws ConnectionException
	 */
	private static PartnerConnection makePartnerConnection(String uid, String pwd) throws ConnectionException {
		authEndPoint = SfdcApiUrlUtil.getSoapURL(authEndPoint);
		return makePartnerConnection(uid, pwd, authEndPoint);
	}

	/**
	 * SFDCアクセス用PartnerConnectionをUID,PWD,authEndPointから作成
	 *
	 * @param uid
	 * @param pwd
	 * @return
	 * @throws ConnectionException
	 */
	private static PartnerConnection makePartnerConnection(String uid, String pwd, String authEndPoint)
			throws ConnectionException {
		ConnectorConfig config = new ConnectorConfig();
		config.setUsername(uid);
		config.setPassword(pwd);
		config.setAuthEndpoint(authEndPoint);
		PartnerConnection pc = null;
		pc = Connector.newConnection(config);
		return pc;
	}

	/**
	 * SFDCへログイン
	 *
	 * @param dir
	 * @param sprop
	 * @param uid
	 * @param pwd
	 * @return
	 * @throws ConnectionException
	 */
	private static LoginResult doLogin(String uid, String pwd) throws ConnectionException {
		LoginResult lr;
		PartnerConnection pc;
		// 暫定 uid .partialがあればtest.salesforce.com
		if (uid.endsWith(".partial")) {
			pc = makePartnerConnection(uid, pwd, SfdcApiUrlUtil.getSoapURL("https://test.salesforce.com"));
		} else {
			pc = makePartnerConnection(uid, pwd);
		}
		lr = pc.login(uid, pwd);
		return lr;
	}

	@Override
	public List<Map<String, ?>> doQuery(String ql) throws Exception {
		return doQuery(ql, false);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jp.co.kpscorp.demo.model.sfdc.SfdcConnect#doQuery(java.lang.String)
	 */
	@Override
	public List<Map<String, ?>> doQuery(String ql, boolean simpleResponse) throws Exception {
		logger.debug("SOQL:" + ql);
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_Query(surl) + "?q=" + URLEncoder.encode(ql, "UTF-8");
		// Eventは過去のものはQueryAllでないと読めない
		if (ql.toLowerCase().contains(" from event ")) {
			url = SfdcApiUrlUtil.getRestURL_QueryAll(surl) + "?q=" + URLEncoder.encode(ql, "UTF-8");
		}
		Map<String, Object> mp = getMapres(new HttpGet(url));
		List<Map<String, ?>> l = new ArrayList<Map<String, ?>>();
		if (simpleResponse) {
			l.add(mp);
			return l;
		}
		if (mp.get("records") == null) {
			String msg = mp.get("message") + "";
			String er = mp.get("errorCode") + "";
			if (er.contains("INVALID_SESSION_ID") && !this.sesExpired) {
				logger.info("Session expired!");
				this.sesExpired = true;
				this.loginResult = null;
				return doQuery(ql, simpleResponse);
			}
			this.sesExpired = false;
			throw new SfdcConnectException(er + "\n" + msg);
		}
		this.sesExpired = false;
		boolean queryMore = false;
		l.addAll((List<Map<String, Object>>) mp.get("records"));
		do {
			// 検索結果がまだあるかどうかを検査
			if (mp.get("done") != null && !(Boolean) mp.get("done")) {
				queryMore = true;
				mp = getMapres(new HttpGet(surl + mp.get("nextRecordsUrl")));
				l.addAll((List<Map<String, Object>>) mp.get("records"));
			} else {
				queryMore = false;
			}
		} while (queryMore);
		return l;
	}

	@Override
	public String getServerUrl() throws ConnectionException, IOException {
		if (this.serverUrl != null) {
			return this.serverUrl;
		}
		LoginResult res = getLoginResult();
		String sep = res.getServerUrl();
		sep = sep.substring(0, sep.indexOf("/services"));
		return sep;
	}

	@Override
	public Map<String, Object> doUpdate(String objectName, String id, Map<String, Object> obj)
			throws IOException, SfdcConnectException, Exception {
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_BasicSObject(surl, objectName);
		HttpEntityEnclosingRequestBase req;
		if (id == null) {
			req = new HttpPost(url);
		} else {
			url += id;
			req = new HttpPatch(url);
		}
		req.setHeader("Content-type", "application/json; charset=UTF-8");
		String js = JSON.encode(obj);
		req.setEntity(new StringEntity(js, "UTF-8"));
		Map<String, Object> res = getMapres(req);
		if (res.get("httpstatus") != null && (res.get("httpstatus").equals(201) || res.get("httpstatus").equals(204))) {
			if (res.get("id") == null) {
				obj.put("id", id);
			} else {
				obj.put("id", res.get("id"));
			}
			res.putAll(obj);
		}
		return res;
	}

	@Override
	public Map<String, Object> doSearch(Map<String, Object> obj) throws Exception {
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_RESTfulSelect(surl);
		HttpPost req = new HttpPost(url);
		req.setHeader("Content-type", "application/json; charset=UTF-8");
		String js = JSON.encode(obj);
		req.setEntity(new StringEntity(js, "UTF-8"));
		Map<String, Object> mp = getMapres(req);
		// if (mp.get("records") == null) {
		if (mp.get("searchRecords") == null) {
			String msg = mp.get("message") + "";
			String er = mp.get("errorCode") + "";
			if (er.contains("INVALID_SESSION_ID") && !this.sesExpired) {
				logger.info("Session expired!");
				this.sesExpired = true;
				this.loginResult = null;
				return doSearch(obj);
			}
			this.sesExpired = false;
			throw new SfdcConnectException(er + "\n" + msg);
		}
		this.sesExpired = false;

		return mp;
	}

	@Override
	public Map<String, Object> doInsert(String objectName, Map<String, Object> obj)
			throws IOException, SfdcConnectException, Exception {
		return doUpdate(objectName, null, obj);
	}

	@Override
	public Map<String, Object> doDelete(String objectName, String id)
			throws IOException, SfdcConnectException, Exception {
		String surl = getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_Rows(surl, objectName, id);
		HttpDelete req = new HttpDelete(url);
		return getMapres(req);
	}

	public static JSONObject responseTojson(HttpResponse response)
			throws IOException, UnsupportedEncodingException, RCException {
		HttpEntity entity = response.getEntity();
		JSONObject rjs = null;
		if (entity != null) {
			InputStream is = entity.getContent();
			try {
				Object resobj = JSONValue.parse(new InputStreamReader(is,
						"UTF-8"));
				if (resobj == null) {
					throw new RCException(response.toString());
				}
				if (resobj instanceof JSONObject) {
					rjs = (JSONObject) resobj;
				} else {
					JSONArray ja = (JSONArray) resobj;
					throw new RCException(makeErrorMsg(ja));
				}
				// System.out.println(rjs.toJSONString());
			} finally {
				is.close();
			}
			// System.out.println(rjs.toJSONString());
		}
		return rjs;
	}

	private static String makeErrorMsg(JSONArray ja) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ja.size(); i++) {
			JSONObject jo = (JSONObject) ja.get(i);
			sb.append("Error Code: ").append(jo.get("errorCode")).append("\n")
					.append(jo.get("message")).append("\n");
		}
		return sb.toString();
	}

	@Override
	public Map<String, Object> getMapres(HttpRequestBase req) throws IOException, Exception, SfdcConnectException {
		// 2023/03/06 cache 対応
		Map<String, Object> mp = null;

		HttpResponse response = getResponse(req);
		HttpEntity entity = response.getEntity();
		Object o = null;
		if (entity != null) {
			o = JSON.decode(entity.getContent());
		}
		if (o instanceof List) {
			List<Map<String, Object>> wl = (List<Map<String, Object>>) o;
			mp = new HashMap<String, Object>();
			if (!wl.isEmpty() && wl.get(0).get("errorCode") != null) {
				mp = wl.get(0);
			} else {
				mp.put("records", wl);
			}
		} else {
			mp = (Map<String, Object>) o;
		}
		if (mp == null) {
			mp = new HashMap<String, Object>();
		}
		mp.put("httpstatus", response.getStatusLine().getStatusCode());
		logger.trace(req + " > " + mp);

		return mp;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jp.co.kpscorp.demo.model.sfdc.SfdcConnect#getJsonRes(java.lang.String)
	 */
	@Override
	public HttpResponse getResponse(HttpRequestBase req) throws Exception {
		// LoginResult lres = getLoginResult();
		// String tk = lres.getSessionId();
		// TLS V1.1対応2016/4/14
		SSLContext sslContext = SSLContexts.custom().useTLS().build();
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.1", "TLSv1.2" },
				null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		// time out 延長設定
		// RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 *
		// 1000).setSocketTimeout(30 * 1000)
		// .build();
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).setSocketTimeout(120 * 1000)
				.build();

		HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(f).setDefaultRequestConfig(requestConfig)
				.build();

		// HttpClient httpclient = new DefaultHttpClient();
		// HttpGet get = new HttpGet(url);
		// set the token in the header
		req.setHeader("Authorization", "OAuth " + token);
		// get.setHeader("Authorization", "Bearer " + tk);
		HttpResponse response = httpclient.execute(req);
		return response;
	}

	public static class ObjMeta {
		/**
		 * @param flds
		 * @param etcMtta
		 * @param objMtta
		 */
		public ObjMeta(Map<String, Map<String, Object>> flds, Map<String, Object> etcMtta, Map<String, Object> objMtta,
				Map<String, Object> viewMtta) {
			super();
			this.flds = flds;
			this.etcMtta = etcMtta;
			this.objMtta = objMtta;
			this.viewMtta = viewMtta;
		}

		public Map<String, Map<String, Object>> flds;
		public Map<String, Object> etcMtta;
		/**
		 * カスタマイズ項目 upuserField:最終更新ユーザーフィールド名 uptimeField:最終更新時間フィールド名
		 * upuserField:作成ユーザーフィールド
		 */
		public Map<String, Object> objMtta;
		public Map<String, Object> viewMtta;

	}

	private void setupUid() throws IOException {
		if (this.uid != null && this.pwd != null) {
			return;
		}
		// List<SfdcauthVo> sas = null;
		// try {
		// sas = sr.findAllByOrderByPriorityAsc();
		// for (SfdcauthVo sa : sas) {
		// this.uid = sa.getUsername();
		// this.pwd = sa.getPassword();
		// LoginResult lr = null;
		// try {
		// lr = doLogin(uid, pwd);
		// } catch (ConnectionException e) {
		// }
		// if (lr != null) {
		// this.loginResult = lr;
		// break;
		// } else {
		// // login失敗
		// logger.error("login faild by " + sa.getUsername());
		// if (sa.getEmail() != null) {
		// // sendGrid廃止
		// // SendGridSender sd = new SendGridSender("info@kpscorp.co.jp");
		// // sd.sendMail(sa.getEmail(), "SFDC接続システムからのお知らせ", "ユーザー名:" +
		// sa.getUsername() +
		// // "でログインが失敗しました。");
		// CommonData.sendMail("ユーザー名:" + sa.getUsername() + "でログインが失敗しました。",
		// sa.getEmail(),
		// "info@kpscorp.co.jp", "SFDC接続システムからのお知らせ", mailhost);
		// }

		// }
		// }
		// } catch (Exception e) {
		// logger.warn(e.toString() + "/" + e.getMessage());
		// }
		// test用
		// if (sas == null || sas.isEmpty()) {
		// this.uid = "yamada@rc.com";
		// this.pwd = "sedoedo8";
		// }
	}

	/**
	 * Beanのしきたりにあわせてフィールドの名前の１文字目を小文字にする ただし１文字目2文字目が大文字ならそのまま
	 */
	/*
	 * private void setupFld4Bean() { Map<String, Map<String, Object>> nflds = new
	 * CaseFreeMap<>(); for (String key : this.fields.keySet()) { Map<String,
	 * Object> fld = this.fields.get(key); String nkey; nkey = convertKey4Bean(key);
	 * fld.put("name", nkey); nflds.put(nkey, fld); } this.fields = nflds; }
	 */
	public static String convertKey4Bean(String key) {
		if (key == null) {
			return null;
		}
		if (!key.contains(".")) {
			return convertKey4BeanSub(key);
		}
		String[] ss = key.split("\\.");
		for (int i = 0; i < ss.length; i++) {
			ss[i] = convertKey4BeanSub(ss[i]);
		}
		return String.join(".", ss);
	}

	public static String convertKey4BeanSub(String key) {
		if (key == null) {
			return null;
		}
		String nkey;
		if (key.length() > 1 && key.substring(0, 2).toUpperCase().equals(key.substring(0, 2))) {
			nkey = key;
		} else {
			nkey = key.substring(0, 1).toLowerCase();
			if (key.length() > 1) {
				nkey += key.substring(1);
			}
		}
		return nkey;
	}
}

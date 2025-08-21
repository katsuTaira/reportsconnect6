package jp.co.kpscorp.rc6.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.MainController;
import jp.co.kpscorp.rc6.model.Accesslog;
import jp.co.kpscorp.rc6.model.Organization;
import jp.co.kpscorp.rc6.model.RcPmap;
import jp.co.kpscorp.rc6.repo.AccesslogRepository;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.OrganizationRepository;

@Controller
@Scope("prototype")
public class AuthTokenService {

	// public static final String ipfixServer =
	// "https://ec2-54-201-104-230.us-west-2.compute.amazonaws.com:8443/loginproxy/";

	// public static String ipfixServer = "https://localhost:8443/loginproxy/";
	public static String ipfixServer = "https://ec2-54-201-152-47.us-west-2.compute.amazonaws.com:8443//loginproxy/";
	// DB使用不可の場合のipfixserver
	public static final String defipfixServer = "https://ec2-54-201-152-47.us-west-2.compute.amazonaws.com:8443//loginproxy/";
	public static final String proxpass = "sedoedo9";
	public static final String proxpassKey = "kps-op";
	@Autowired
	private LicenseRepository lcrep;
	@Autowired
	private ApplicationContext con;

	@RequestMapping("/dl")
	public String doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		String key = req.getParameter("state");
		ServletContext context = req.getSession().getServletContext();
		Map<String, String> pmap = getPmap(req, resp, key);
		try {
			if (pmap == null) {
				String res = dispachError((String) req.getAttribute("error"),
						(String) req.getAttribute("error_description"), req, resp,
						pmap, key);
				cleanUp(key, context);
				return "redirect:" + res;
			}
			String ermsg = pmap.get(Util.batchErrorMsgKey);
			if (ermsg != null) {
				// Batchモードですでにエラー
				String res = dispachError(ermsg, "", req, resp, pmap, key);
				cleanUp(key, context);
				return "redirect:" + res;
			}
			String code = req.getParameter("code");
			String client_id = pmap.get("client_id");
			if (code != null) {
				int ipfix = checkIpfixOrg(pmap, con);
				String jstr = getToken(client_id, pmap.get(AuthToken.csecKey),
						code, makeReUrl(req), pmap.get(AuthToken.hostSafixKey),
						ipfix, pmap);
				JSONObject jo = (JSONObject) JSONValue.parse(jstr);
				if (jo.get("exception") != null) {
					String res = dispachError("ログインできません！", (String) jo.get("exception"),
							req, resp, pmap, key);
					System.out.println("can't login! " + jo.get("exception"));
					cleanUp(key, context);
					return "redirect:" + res;
				}
				if (jstr == null) {
					String res = dispachError("request token eoor", "can't get Token!", req,
							resp, pmap, key);
					System.out.println("can't get Token!");
					cleanUp(key, context);
					return "redirect:" + res;
				}

				// for test json上書き
				if (req.getParameter(Downloder.JSonKey) != null) {
					jstr = URLDecoder.decode(
							req.getParameter(Downloder.JSonKey), "UTF-8");
				}

				pmap.put(Downloder.JSonKey, jstr);
			}
			if (pmap.get(Downloder.JSonKey) != null) {
				req.setAttribute("key", key);
				req.setAttribute("baseUrl", getBaseUrl(req));
				// リダイレクト
				String res = dispachJsp(req, resp, pmap, key, Downloder.dljspKey,
						"views/dl");
				return res;
			}
			req.getSession().getServletContext().removeAttribute(key);
			String res = dispachError(req.getParameter("error"),
					req.getParameter("error_description"), req, resp, pmap, key);
			cleanUp(key, context);
			return "redirect:" + res;
		} catch (Exception e) {
			cleanUp(key, context);
			String emsg = e.toString() + "/" + e.getMessage();
			if (emsg.contains("response code: 400")) {
				emsg = "アクセスが拒否されました。IPアドレス制限を行っている場合は<a href='http://www.reportsconnect.com/ip.html'>IPアドレス固定化オプション</a>を使用してください。";
			}
			String res = "サーバーエラー";
			try {
				e.printStackTrace();
				res = dispachError("処理できません", emsg, req, resp, pmap, key);
			} catch (IOException e1) {
				throw new ServletException(e);
			}
			return res;
		}

	}

	@RequestMapping("/views/{key}")
	public String goToViews(@PathVariable String key) {
		return "views/" + key;
	}

	public static Map<String, String> getPmap(HttpServletRequest req,
			HttpServletResponse resp, String key) throws ServletException {
		Map<String, String> pmap = null;
		if (key != null) {
			pmap = (Map<String, String>) req.getSession().getServletContext()
					.getAttribute(key);
			ApplicationContext ctx = Util.getApplicationContext();
			if (ctx != null) {
				RcPmap rcPmap = ctx.getBean(RcPmap.class);
				rcPmap.pmap = pmap;
			}
		}
		if (pmap != null) {
			// getServletContext().removeAttribute(key);
			// セキュリティ上ログに出すとまずい
			// System.out.println("download current pmap:" + pmap);
		} else {
			String res = null;
			try {
				res = dispachError("サーバーエラー", "印刷処理が行えませんでした。再度処理を行なってください。" + URLEncoder.encode(key, "UTF-8"),
						req, resp, null, key);
			} catch (IOException e) {
				throw new ServletException(e);
			}
			System.out.println("pmap is null for key:" + key);
			return null;
		}
		return pmap;
	}

	public static String dispachError(String error, String description,
			HttpServletRequest req, HttpServletResponse resp,
			Map<String, String> pmap, String key) throws UnsupportedEncodingException {
		// nouser modeの場合はpmapにメッセージを保管
		if (Util.isNouserMode(pmap)) {
			pmap.put(Util.batchErrorMsgKey, error + "/" + description);
			// とりあえずnullを返す
			return null;
		}
		req.setAttribute("error", error);
		req.setAttribute("error_description", description);
		req.setAttribute("baseUrl", getBaseUrl(req));
		String rurl;
		if (req.getParameter("signed_request") != null) {
			rurl = "views/dlErrorCanvas";
		} else {
			rurl = "views/dlError";
		}
		// dispachJsp(req, resp, pmap, key, Downloder.dlerrorjspKey, rurl);
		System.out.println("Dispach error:" + error + "/" + description);
		return rurl;
	}

	public static String dispachJsp(HttpServletRequest req,
			HttpServletResponse resp, Map<String, String> pmap, String key,
			String jspkey, String defaultjsp) {
		String jspname = null;
		if (pmap != null) {
			jspname = pmap.get(jspkey);
		}
		// RequestDispatcher dispatcher;
		// ServletContext context = req.getSession().getServletContext();
		if (jspname != null && key != null) {
			String path = "/jasper/" + key + "/" + jspname;
			replaceVariable(path, key, req);
			// dispatcher = context.getRequestDispatcher(path);
			return path;
		} else {
			// dispatcher = context.getRequestDispatcher(defaultjsp);
			return defaultjsp;
		}
		// dispatcher.forward(req, resp);
	}

	private static void replaceVariable(String path, String key,
			HttpServletRequest req) {
		ServletContext context = req.getSession().getServletContext();
		path = context.getRealPath(path);
		File file = new File(path);
		BufferedReader br = null;
		FileWriter fw = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
			StringBuffer sb = new StringBuffer();
			int c;
			while ((c = br.read()) != -1) {
				sb.append((char) c);
			}
			String res = sb.toString();
			String[] repstrs = { "error", "error_description", "baseUrl", "key" };
			req.setAttribute("key", key);
			for (String s : repstrs) {
				String val = (String) req.getAttribute(s);
				res = res.replaceAll("\\$\\{" + s + "\\}", val);
			}
			br.close();
			fw = new FileWriter(file);
			fw.write(res);
		} catch (Exception e) {
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
				}
			}
		}

	}

	public static String getBaseUrl(HttpServletRequest req)
			throws UnsupportedEncodingException {
		String rurl = makeReUrl(req);
		rurl = URLDecoder.decode(rurl, "UTF-8");
		return rurl.substring(0, rurl.length() - 3);
		// return rurl;
	}

	public static String makeReUrl(HttpServletRequest req)
			throws UnsupportedEncodingException {
		String url = req.getRequestURL().toString();
		if (url.indexOf("localhost") == -1) {
			url = url.replaceFirst("http:", "https:");
			// コンテキストパスがない場合は テスト環境と考え /rc6 を追加する
			String wurl = url.replaceFirst("https://", "");
			String[] urls = wurl.split("/");
			if (urls.length < 3) {
				url = "https://" + urls[0] + "/rc6/" + urls[1];
			}
		}
		return URLEncoder.encode(url, "UTF-8");
	}

	private String getToken(String clid, String client_secret, String code,
			String redirect_uri, String hostSfx, int proxy, Map<String, String> pmap)
			throws IOException,
			KeyManagementException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		// 2020/09/10 con pmap 追加
		String url = MainController.getLoginNm(hostSfx, lcrep)
				+ "/services/oauth2/token";
		String parms = "";
		parms += "code=" + code + "&";
		parms += "grant_type=authorization_code" + "&";
		parms += AuthToken.cidKey + "=" + clid + "&";
		parms += AuthToken.csecKey + "=" + client_secret + "&";
		parms += "redirect_uri=" + redirect_uri + "&";
		parms += "code_verifier=" + pmap.get(MainController.codeVerifierKey);
		// セキュリティ上ログに出すとまずい
		// System.out.println("getToken:" + parms);
		HttpURLConnection connection;
		// if (proxy) {
		// // String server = "http://localhost:8080/loginproxy/";
		// System.out.println("proxy mode to " + ipfixServer);
		// // parms暗号化
		// parms = encrypt(parms, makeKey(proxpass));
		// //
		// url = ipfixServer + "token?proxyUrl="
		// + URLEncoder.encode(url, "UTF-8") + "&proxyParms="
		// + URLEncoder.encode(parms, "UTF-8");
		// return doHttpsGet(url);
		// } else {
		if (proxy == 1) {
			url = getProxyURL(url);
		}
		URL uurl = new URL(url);
		if (proxy == 2) {
			Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
					"54.201.152.47", 443));
			connection = (HttpURLConnection) uurl.openConnection(p);
			// connection.setRequestProperty(proxpassKey, proxpass);
			connection.setRequestProperty("kps-op", "kps");
		} else {
			connection = (HttpURLConnection) uurl.openConnection();
		}
		if (proxy == 1) {
			ignoreValidateCertification((HttpsURLConnection) connection);
			connection.setRequestProperty(proxpassKey, proxpass);
			connection.setRequestProperty("Referer", "kps");
		}
		connection.setReadTimeout(20000);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		PrintWriter writer = new PrintWriter(connection.getOutputStream());
		writer.print(parms);
		writer.flush();
		writer.close();
		return readPage(connection);
		// }
	}

	/**
	 * 
	 * @param ogs
	 *            豆のリスト
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @returnマップのリスト。各マップのキーは、Beanのプロパティ名で、
	 * 値はプロパティ値です。
	 */
	static <T> List<Map<String, String>> makeMaps(List<T> ogs)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		List<Map<String, String>> mps = new ArrayList<Map<String, String>>();
		for (Object bean : ogs) {
			Map<String, String> mp = BeanUtils.describe(bean);
			mps.add(mp);
		}
		return mps;
	}

	/**
	 * このJavaメソッドは、組織ID (oid) とフラグ (forUser) に基づいて、データベースから組織の一覧を取得します。
	 * 
	 * forUser が true の場合、numberof が 0 より大きい組織のみをフィルタリングします。
	 * 
	 * 結果は、各マップが組織を表し、その属性がキー値のペアとして表現されるマップのリストとして返されます。
	 * 
	 * このメソッドは、開始日が現在の日付以下で、終了日が現在の日付以上の組織のみを考慮する日付フィルタも適用します。
	 * 
	 * @param con     OrganizationRepository
	 * @param oid     orgid
	 * @param forUser if true, add numberof > 0 condition
	 * @return List<Map<String, String>>
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public static List<Map<String, String>> getOrgsById(ApplicationContext con,
			String oid, boolean forUser)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		java.util.Date d = new java.util.Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
		List<Organization> ogs;
		OrganizationRepository rOrganization = (OrganizationRepository) con
				.getBean(OrganizationRepository.class);
		if (forUser) {
			ogs = rOrganization
					.findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqualAndNumberofGreaterThan(
							oid + "%", d2, d2, 0);
			// sql += " and numberof > 0";
		} else {
			ogs = rOrganization.findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqual(
					oid + "%", d2, d2);
		}
		List<Map<String, String>> res = makeMaps(ogs);
		return res;
	}

	public static String getProxyURL(String path) throws MalformedURLException {
		return ipfixServer + "proxy" + path.substring("https:/".length());
	}

	// public static String doHttpsGet(String url) throws MalformedURLException,
	// IOException, NoSuchAlgorithmException, KeyManagementException,
	// ProtocolException {
	// HttpURLConnection connection;
	// URL uurl = new URL(url);
	// connection = (HttpURLConnection) uurl.openConnection();
	// connection.setReadTimeout(20000);
	// HttpsURLConnection scon = (HttpsURLConnection) connection;
	// ignoreValidateCertification(scon);
	// connection.setRequestMethod("GET");
	// return readPage(connection);
	// }

	private static String readPage(HttpURLConnection connection)
			throws IOException {
		String html = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			String line;
			while ((line = reader.readLine()) != null) {
				html += line + "\n";
			}
			reader.close();
		} else {
			html = "ResponseCode:" + connection.getResponseCode() + "/"
					+ connection.getResponseMessage();
			String line;
			while ((line = reader.readLine()) != null) {
				html += line + "\n";
			}
			reader.close();
		}
		// セキュリティ上ログに出すとまずい
		// System.out.println("readPage:" + html);
		return html;
	}

	public static void ignoreValidateCertification(
			HttpsURLConnection httpsconnection)
			throws NoSuchAlgorithmException, KeyManagementException {
		KeyManager[] km = null;
		TrustManager[] tm = { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] arg0, String arg1)
					throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] arg0, String arg1)
					throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };
		SSLContext sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(km, tm, new SecureRandom());
		httpsconnection.setSSLSocketFactory(sslcontext.getSocketFactory());
	}

	public static void cleanUp(String key, ServletContext context) {
		// リソースの掃除
		context.removeAttribute(key);
		File f = new File(context.getRealPath("/jasper/" + key));
		if (f.exists()) {
			delete(f);
			System.out.println(f.getName() + " deleted.");
		}
		ThreadMap.get().clear();
	}

	private static void delete(File f) {
		if (f.exists() == false) {
			return;
		}

		if (f.isFile()) {
			f.delete();
		}

		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				delete(files[i]);
			}
			f.delete();
		}
	}

	/**
	 * @param pmap
	 * @param con
	 * @return 0:ipfixでない 1:オリジナルproxy 2:Apacheプロキシ
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public static int checkIpfixOrg(Map<String, String> pmap, ApplicationContext con) {
		try {
			// authOlnyKeyは、便宜上ipfixから行く
			if (pmap.get(Downloder.authOlnyKey) != null) {
				setupProxyUrl(con);
				return 1;
			}
			JSONObject ajo = getUifo(pmap);
			if (ajo == null) {
				return 0;
			}
			String orgid = (String) ajo.get(Util.licenseOidKey);
			List<Map<String, String>> mps = AuthTokenService.getOrgsById(con,
					orgid.substring(0, 15), false);
			for (Map<String, String> mp : mps) {
				if (AuthToken.ipfixKey.equals(mp.get("licensename"))) {
					setupProxyUrl(con);
					System.out.println("use proxy 1");
					return 1;
				}
				if (AuthToken.ipfixApacheKey.equals(mp.get("licensename"))) {
					System.out.println("use proxy 2");
					return 2;
				}
			}
			return 0;
		} catch (Exception e) {
			ipfixServer = defipfixServer;
			System.out.println("DB Error use proxy 1");
			return 1;
		}
	}

	private static void setupProxyUrl(ApplicationContext con) {
		// String sql = "select * from accesslog where orgid = ? and userid = ?";
		// PreparedStatement pstmts = con.prepareStatement(sql);
		// pstmts.setString(1, "servers");
		// pstmts.setString(2, "proxy");
		// ResultSet result = pstmts.executeQuery();
		// List<Accesslog> acs = ModelFactory.getByObjs(result,
		// Accesslog.class);
		AccesslogRepository rAccesslog = con.getBean(AccesslogRepository.class);
		List<Accesslog> acs = rAccesslog.findByOrgidAndUserid("servers", "proxy");
		if (acs.isEmpty() || acs.get(0).getMemo() == null) {
			return;
		}
		String url = null;
		try {
			JSONObject jo = (JSONObject) JSONValue.parse(acs.get(0).getMemo());
			url = (String) jo.get("url");
		} catch (Exception e) {
		}
		if (url != null) {
			ipfixServer = url;
		}
	}

	public static JSONObject getUifo(Map<String, String> pmap) {
		String jstr = pmap.get(AuthToken.uinfoKey);
		System.out.println("uinfo:" + jstr);
		if (jstr == null) {
			return null;
		}
		JSONObject ajo = (JSONObject) JSONValue.parse(jstr);
		return ajo;
	}

	/**
	 * 文字列を16文字の秘密鍵でAES暗号化してBase64した文字列で返す
	 *
	 * @param originalString
	 *                       String
	 * @param secretKey
	 *                       String
	 * @return String
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static String encrypt(String originalString, byte[] secretKeyBytes)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

		byte[] originalBytes = originalString.getBytes();
		// byte[] secretKeyBytes = secretKey.getBytes();

		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, AES);
		Cipher cipher = Cipher.getInstance(AES);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		byte[] encryptBytes = cipher.doFinal(originalBytes);
		byte[] encryptBytesBase64 = Base64.encodeBase64(encryptBytes, false);
		return new String(encryptBytesBase64);
	}

	private static final String AES = "AES";

	public static byte[] makeKey(String pwd) {
		byte[] res;
		if (pwd.length() >= 16) {
			res = pwd.substring(0, 15).getBytes();
		} else {
			String wk = pwd;
			for (Integer i = 0; i < 16 - pwd.length(); i++) {
				wk = wk + 'a';
			}
			res = wk.getBytes();
		}
		return res;
	}

}

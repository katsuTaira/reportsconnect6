package jp.co.kpscorp.rc6.cont;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.component.Utils;
import jp.co.kpscorp.rc6.repo.LicenseRepository;

@Controller
@Scope("prototype")
public class AuthTokenCotroller {

	// public static final String ipfixServer =
	// "https://ec2-54-201-104-230.us-west-2.compute.amazonaws.com:8443/loginproxy/";

	@Autowired
	private LicenseRepository lcrep;
	@Autowired
	private ApplicationContext con;

	@RequestMapping("/dl")
	public String dl(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		String key = req.getParameter("state");
		// ServletContext context = req.getSession().getServletContext();
		Map<String, String> pmap = Utils.getPmap(req, key);
		try {
			if (pmap == null) {
				String res = Utils.dispachError((String) req.getAttribute("error"),
						(String) req.getAttribute("error_description"), req,
						pmap, key);
				// cleanUp(key, context);
				// return "redirect:" + res;
				return res;
			}
			String ermsg = pmap.get(Utils.batchErrorMsgKey);
			if (ermsg != null) {
				// Batchモードですでにエラー
				if (ermsg.contains("invalid_grant") && ermsg.contains("authentication failure")) {
					// ユーザ名パスワードフローを許可していない場合
					ermsg = "ログインできません。ユーザ名パスワードが正しくない可能性があります。また、 ログインユーザー固定化オプションを使用する場合、設定→OAuth および OpenID Connect 設定を開き、[OAuth ユーザ名パスワードフローを許可] をオンに変更する必要があります。";
				}
				String res = Utils.dispachError(ermsg, "", req, pmap, key);
				// cleanUp(key, context);
				// return "redirect:" + res;
				return res;

			}
			String code = req.getParameter("code");
			String client_id = pmap.get("client_id");
			if (code != null) {
				int ipfix = Utils.checkIpfixOrg(pmap, con);
				String jstr = getToken(client_id, pmap.get(Utils.csecKey),
						code, Utils.makeReUrl(req), pmap.get(Utils.hostSafixKey),
						ipfix, pmap);
				JSONObject jo = (JSONObject) JSONValue.parse(jstr);
				if (jo.get("exception") != null) {
					String res = Utils.dispachError("ログインできません！", (String) jo.get("exception"),
							req, pmap, key);
					System.out.println("can't login! " + jo.get("exception"));
					// cleanUp(key, context);
					// return "redirect:" + res;
					return res;
				}
				if (jstr == null) {
					String res = Utils.dispachError("request token error", "can't get Token!", req,
							pmap, key);
					System.out.println("can't get Token!");
					// cleanUp(key, context);
					// return "redirect:" + res;
					return res;
				}

				// for test json上書き
				if (req.getParameter(Utils.JSonKey) != null) {
					jstr = URLDecoder.decode(
							req.getParameter(Utils.JSonKey), "UTF-8");
				}

				pmap.put(Utils.JSonKey, jstr);
			}
			if (pmap.get(Utils.JSonKey) != null) {
				req.setAttribute("key", key);
				req.setAttribute("baseUrl", Utils.getBaseUrl(req));
				// リダイレクト
				String res = Utils.dispachJsp(req, pmap, key, Utils.dljspKey,
						"views/dl");
				return res;
			}
			req.getSession().getServletContext().removeAttribute(key);
			String res = Utils.dispachError(req.getParameter("error"),
					req.getParameter("error_description"), req, pmap, key);
			// cleanUp(key, context);
			// return "redirect:" + res;
			return res;
		} catch (Exception e) {
			String emsg = e.toString() + "/" + e.getMessage();
			if (emsg.contains("response code: 400")) {
				emsg = "アクセスが拒否されました。IPアドレス制限を行っている場合は<a href='http://www.reportsconnect.com/ip.html'>IPアドレス固定化オプション</a>を使用してください。";
			}
			String res = "サーバーエラー";
			try {
				e.printStackTrace();
				res = Utils.dispachError("処理できません", emsg, req, pmap, key);
			} catch (IOException e1) {
				throw new ServletException(e);
			}
			// cleanUp(key, context);
			return res;
		}

	}

	private String getToken(String clid, String client_secret, String code,
			String redirect_uri, String hostSfx, int proxy, Map<String, String> pmap)
			throws IOException,
			KeyManagementException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		// 2020/09/10 con pmap 追加
		String url = SetterController.getLoginNm(hostSfx, lcrep)
				+ "/services/oauth2/token";
		String parms = "";
		parms += "code=" + code + "&";
		parms += "grant_type=authorization_code" + "&";
		parms += Utils.cidKey + "=" + clid + "&";
		parms += Utils.csecKey + "=" + client_secret + "&";
		parms += "redirect_uri=" + redirect_uri + "&";
		parms += "code_verifier=" + pmap.get(Utils.codeVerifierKey);
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
		// if (proxy == 1) {
		// url = Utils.getProxyURL(url);
		// }
		URL uurl = new URL(url);
		if (proxy == 2) {
			Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
					"54.201.152.47", 443));
			connection = (HttpURLConnection) uurl.openConnection(p);
			// connection.setRequestProperty(proxpassKey, proxpass);
			connection.setRequestProperty("kps-op", "kps");
		} else if (proxy == 1) {
			// Utils.ignoreValidateCertification((HttpsURLConnection) connection);
			// connection.setRequestProperty(Utils.proxpassKey, Utils.proxpass);
			// connection.setRequestProperty("Referer", "kps");
			// appache proxy 標準機能 8888で設定済み
			Proxy proxyObj = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Utils.ipfixServerExternal, 8888));
			connection = (HttpURLConnection) uurl.openConnection(proxyObj);

		} else {
			connection = (HttpURLConnection) uurl.openConnection();
		}
		connection.setReadTimeout(20000);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		PrintWriter writer = new PrintWriter(connection.getOutputStream());
		writer.print(parms);
		writer.flush();
		writer.close();
		return Utils.readPage(connection);
		// }
	}

}

package jp.co.kpscorp.rc6;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64FormatException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

//import com.lowagie.text.pdf.EudcFontRegistry;

import jp.co.kpscorp.rc6.model.Accesslog;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Organization;
import jp.co.kpscorp.rc6.model.Usertbl;
import jp.co.kpscorp.rc6.repo.AccesslogRepository;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.OrganizationRepository;
import jp.co.kpscorp.rc6.repo.UsertblRepository;
import jp.co.kpscorp.rc6.service.AuthTokenService;
import net.arnx.jsonic.JSON;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

@Controller
@Scope("prototype")
public class MainController {

	private Logger logger = Logger.getLogger(MainController.class);

	public static final String defurl = "http://localhost:8080";
	public static final String exattKey = "exatt";
	public static final String orgidKey = "kps_orgid";
	public static final String jsonUrlKey = "kps_jsonurl";
	public static final String useridKey = "kps_userid";
	public static final String pkeyKey = "kps_parmkey";
	public static final String jrxmlFnKey = "kps_jrxml";
	public static final String dljspKey = "kps_dljsp";
	public static final String dlerrorjspKey = "kps_dlerrorjsp";
	public static final String dlnodatajspKey = "kps_dlnodatajsp";
	public static final String csecKey = "client_secret";
	public static final String cvsecKey = "canvas_secret";
	public static final String cidKey = "client_id";
	public static final String authParmKey = "kps_aParm";
	public static final String hostSafixKey = "Kps_hostSafix";
	public static final String codeVerifierKey = "Kps_codeVerifier";

	@Autowired
	private LicenseRepository lcrep;
	@Autowired
	private UsertblRepository usrep;
	@Autowired
	private AccesslogRepository alrep;
	@Autowired
	private OrganizationRepository ogrep;
	@Autowired
	ServletContext context;

	/**
	 * @param pmap
	 * @return
	 *         sfdc側のReportsConnectControllerのコンストラクタからのリクエストに応えてユーザー情報/serverUrl等を返す
	 *         パラメーターにjsonUrlKeyがあればjsonでユーザー情報を返す、なければserverurlのみを返す
	 *         該当ユーザーがあれば、そのライセンスのserverurlを、なければライセンスfreeのserverurlをライセンスから読んで返す
	 *         そのユーザーのアクセスログがあれば、hasaccesslogをtrueとする
	 *         そのユーザーか、そのユーザーの組織にライセンスが一ついじょうあれば、haslicenseをtrueとする
	 *         組織にexattKeyがある場合は、licensenameにそれを返す
	 *
	 */
	@RequestMapping("/su")
	@ResponseBody
	public String su(@RequestParam Map<String, String> pmap) {
		String uid = pmap.get(useridKey);
		String isjson = pmap.get(jsonUrlKey);
		String res;
		Map<String, String> wmp;
		try {
			wmp = getServerUrl(uid);
		} catch (Exception e) {
			// DB使用不可の場合 serverurlはnull
			wmp = new HashMap<String, String>();
			wmp.put("serverurl", null);
			wmp.put("licensename", "pro");
		}
		// 2017/06/22
		checkOrg(pmap, wmp);
		if ("true".equals(isjson)) {
			res = JSON.encode(wmp);
			logger.info("Setup User:" + res);
		} else {
			res = wmp.get("serverurl");
		}
		return res;
	}

	private String getDefaultServerUrl() {
		List<License> ls = lcrep.findByLicensename("free");
		if (!ls.isEmpty()) {
			return ls.get(0).getServerurl();
		}
		return defurl;
	}

	private Map<String, String> getServerUrl(String uid) throws SQLException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Map<String, String> ump = new HashMap<String, String>();
		if (uid != null && uid.length() > 15) {
			uid = uid.substring(0, 15);
		}
		List<Usertbl> us = usrep.findByUseridLike(uid + "%");
		if (!us.isEmpty()) {
			Usertbl u = us.get(0);
			ump.put("serverurl", u.getLicenseBean().getServerurl());
			ump.put("licensename", u.getLicenseBean().getLicensename());
			ump.put("haslicense", "true");
		} else {
			ump.put("serverurl", getDefaultServerUrl());
			ump.put("licensename", "free");
		}
		List<Accesslog> acs = alrep.findByUseridLike(uid + "%");
		if (!acs.isEmpty()) {
			ump.put("hasaccesslog", "true");
			if (!"true".equals(ump.get("haslicense"))) {
				for (Accesslog ac : acs) {
					String oid = ac.getOrgid();
					if (oid != null && oid.length() > 15) {
						oid = oid.substring(0, 15);
					}
					Date dt = new Date();
					List<Organization> orgs = ogrep
							.findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqualAndNumberofGreaterThan(
									oid + "%", dt, dt, 0);
					if (!orgs.isEmpty()) {
						ump.put("haslicense", "true");
						break;
					}
				}
			}
		}
		return ump;
	}

	/**
	 * 2017/06/22 orgidより添付拡大オプション対応
	 *
	 * @param wmp
	 */
	private void checkOrg(Map<String, String> pmap, Map<String, String> wmp) {
		String orgid = pmap.get(orgidKey);
		if (orgid == null || orgid.length() < 15) {
			return;
		}
		List<Organization> mps;
		try {
			Date dt = new Date();
			mps = ogrep.findByOrgidLikeAndStartdateLessThanEqualAndEnddateGreaterThanEqual(orgid.substring(0, 15), dt,
					dt);
			for (Organization mp : mps) {
				if (exattKey.equals(mp.getLicenseBean().getLicensename())) {
					wmp.put("licensename", exattKey);
				}
			}
		} catch (Exception e) {
			wmp.put("licensename", exattKey);
		}
	}

	/**
	 * クライアントから必要なファイルのアップロードを処理して保存する
	 * 
	 * @param req
	 * @return
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws Base64FormatException
	 * @throws JRException
	 *
	 *                                  ①ファイルおよびパラメータの受信
	 *                                  クライアント側（ReportsConnectController)から該当の帳票オブジェクトに添付されているファイル(base64で内容をエンコード）
	 *                                  およびパラメータをmultipart/form-dataとしてPostで送信する。
	 *                                  PDF連結の場合は、filenameは帳票オブジェクトのID/ファイル名
	 *                                  として送る。（これにより、サーバー側のファイル領域は、帳票
	 *                                  オブジェクトのIDのサブフォルダー以下に自動的に配置される）
	 *
	 *                                  ②keyの生成
	 *                                  x + 乱数 のkeyを生成する
	 *
	 *                                  ③ファイルおよびパラメータの保存
	 *                                  Apache commons
	 *                                  FileUploadライブラリを使用して受信したmultipart/form-dataを処理する
	 *                                  ファイルは、"/jasper/" +
	 *                                  keyのフォルダーに保存する（連結の場合はファイル名の前に帳票オブジェクトのID+"/"が入っているため、
	 *                                  階層構造になる）
	 *                                  パラメータは、pmapというMapにkey/valueで保存して、keyをattribute名としてアプリケーションスコープに保管する
	 *                                  サブリポートがある場合、サブリポートのjrxmlファイルをコンパイルしておく（メインのjrxmlはダウンロード時にコンパイルするが
	 *                                  それ以外はしないため）
	 *                                  Springコンポーネント
	 *                                  Settings内に設定されているconsumerKeyおよびconsumerSeacretを、処理するサーバーのURLから取り出すして
	 *                                  pmapに設定（heroku環境とlocalhost（開発時）では異なる）
	 *                                  OAuth2.0のリクエスト用URLを組み立てる
	 *                                  (https://login.salesforce.com/services/oauth2/authorize?response_type=code&client_id=<your_client_id>&redirect_uri=<your_redirect_uri>)
	 *                                  redirect_uriは次のAuthTokenサーブレットを呼び出すURLとなる。また、次の処理に引き渡すkeyの値をURLのstateに設定する。
	 *                                  OAuth2.0のリクエスト用URLをクライアントへ返して終了
	 *
	 *                                  通常のフォルダー構成
	 *
	 *                                  -jasper-key--xxx.jrxml
	 *                                  |-xxx.jpg
	 *                                  :
	 *
	 *                                  連結の場合
	 *
	 *                                  -jasper-key
	 *                                  |-子帳票オブジェクトのid1--yyy.jrxml (子）
	 *                                  |-yyy.jpg
	 *                                  |-子帳票オブジェクトのid2--zzz.jrxml (子）
	 *                                  |-zzz.jpg
	 *
	 *                                  なお、外字ファイル（EudcFontRegistry)の扱いに関しては保留
	 * @throws ServletException
	 * @throws NoSuchAlgorithmException
	 */
	@RequestMapping("/st")
	@ResponseBody
	public String st(HttpServletRequest req)
			throws FileUploadException, IOException, Base64FormatException, JRException, ServletException,
			NoSuchAlgorithmException {
		String res = null;

		String key = null;
		// String key = makeKey(req, resp);
		// Check that we have a file upload request
		boolean isMultipart = JakartaServletFileUpload.isMultipartContent(req);
		if (!isMultipart) {
			return null;
		}

		// Create a factory for disk-based file items
		FileItemFactory<?> factory = DiskFileItemFactory.builder().get();

		// Create a new file upload handler
		JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);

		// Parse the request
		// Connection con = null;
		// add 2017/04/07
		// 最初にkeyを抽出
		List<FileItem<?>> items = upload.parseRequest(req);
		Map<String, String> pmap = new HashMap<String, String>();
		for (FileItem item : items) {
			System.out.println(item);
			if (item.isFormField()) {
				processFormField(item, pmap);
			}
		}
		key = pmap.get(pkeyKey);
		if (key == null) {
			key = makeKey();
		}
		// add end 2017/04/07
		// Map<String, String> pmap = new HashMap<String, String>();
		// ipを保存
		// printHeader(req);
		// List<FileItem> items = upload.parseRequest(req);
		List<File> savedFiles = new ArrayList<File>();
		for (FileItem item : items) {
			System.out.println(item);
			if (item.isFormField()) {
				// processFormField(item, pmap);
			} else {
				File f = processUploadedFile(item, key, pmap);
				if (f != null) {
					savedFiles.add(f);
				}
			}

		}
		// サブリポートはここでコンパイルしておく必要あり
		for (File f : savedFiles) {
			String fileName = f.getName();
			if (fileName.toLowerCase().endsWith("jrxml")) {
				if (pmap.get(jrxmlFnKey) == null) {
					pmap.put(jrxmlFnKey, fileName);
				} else if (!pmap.get(jrxmlFnKey).contains(fileName)) {
					// サブリポートのコンパイル
					JasperReport jasperReport = JasperCompileManager.compileReport(f.getAbsolutePath());
					String ofnm = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf('.'))
							+ ".jasper";
					FileOutputStream of = new FileOutputStream(ofnm);
					ObjectOutputStream oos = new ObjectOutputStream(of);
					oos.writeObject(jasperReport);
					oos.close();
					System.out.println("compiled file path:" + ofnm);
				}
			}

		}

		Properties props = readAppProps();
		if (!pmap.containsKey(cidKey)) {
			lookupMap(req, pmap, props);
		}
		context.setAttribute(key, pmap);
		if (pmap.get(authParmKey) != null) {
			// batch mode
			req.setAttribute("key", key);
			/*
			 * String res;
			 * try {
			 * res = new BatchExcuter().goBatch(req, resp);
			 * } catch (Exception e) {
			 * e.printStackTrace();
			 * String emsg = e + ":" + e.getMessage();
			 * if (emsg.contains("LOGIN_DURING_RESTRICTED_DOMAIN")) {
			 * emsg =
			 * "アクセスが拒否されました。IPアドレス制限を行っている場合は<a href='http://www.reportsconnect.com/ip.html'>IPアドレス固定化オプション</a>を使用してください。"
			 * ;
			 * }
			 * pmap.put(BatchExcuter.batchErrorMsgKey, emsg);
			 * if (!BatchExcuter.isNouserMode(pmap)) {
			 * // Downloadへ
			 * res = AuthTokenService.getBaseUrl(req) + "/dl?state=" + key;
			 * } else {
			 * res = BatchExcuter.makeRes(emsg, pmap);
			 * AuthTokenService.cleanUp(key, req.getSession().getServletContext());
			 * }
			 * }
			 * write(res, resp);
			 */
		} else {
			String rurl = AuthTokenService.getBaseUrl(req) + "/dl";
			String url = makeUrl(pmap.get(cidKey), rurl, key, pmap.get(hostSafixKey), pmap);
			res = url;
		}

		return res;
	}

	private void processFormField(FileItem item, Map<String, String> pmap) throws UnsupportedEncodingException {
		String name = URLDecoder.decode(item.getFieldName(), "UTF-8");
		String value = item.getString();
		if (value != null) {
			value = URLDecoder.decode(value, "UTF-8");
		}
		pmap.put(name, value);
	}

	private File processUploadedFile(FileItem item, String key, Map<String, String> pmap)
			throws IOException, Base64FormatException, JRException {
		String fileName = URLDecoder.decode(item.getName(), "UTF-8");
		// jspは保存させない
		if (fileName.toLowerCase().endsWith("jsp")) {
			System.out.println("Warnig! can't upload jsp:" + fileName);
			return null;
		}
		File bdir = new File(context.getRealPath("/jasper"));
		if (!bdir.exists()) {
			bdir.mkdir();
		}
		String pt = context.getRealPath("/jasper/" + key);
		File newdir = new File(pt);
		newdir.mkdir();
		makeSubDir(newdir.getAbsolutePath(), fileName);
		String fpath = newdir.getAbsolutePath() + "/" + fileName;
		System.out.println("file path:" + fpath);
		File file = new File(fpath);
		FileOutputStream fos = new FileOutputStream(file);
		Base64Decoder bd = new Base64Decoder(item.getInputStream(), fos);
		bd.process();
		item.getInputStream().close();
		fos.close();
		/*
		 * if (fileName.toLowerCase().endsWith("ttf")) {
		 * // 外字ファイルPathをPMAPへ保管
		 * pmap.put(EudcFontRegistry.TH_EUDCFILE, file.getAbsolutePath());
		 * }
		 */
		// 以下、カスタマイズHTML
		if (fileName.toLowerCase().startsWith("dl.") && (
		// fileName.toLowerCase().endsWith("jsp") ||
		fileName.toLowerCase().endsWith("html") || fileName.toLowerCase().endsWith("htm"))) {
			pmap.put(dljspKey, fileName);
		}
		if (fileName.toLowerCase().startsWith("dlerror.") && (
		// fileName.toLowerCase().endsWith("jsp")||
		fileName.toLowerCase().endsWith("html") || fileName.toLowerCase().endsWith("htm"))) {
			pmap.put(dlerrorjspKey, fileName);
		}
		if (fileName.toLowerCase().startsWith("dlnodata.") && (
		// fileName.toLowerCase().endsWith("jsp") ||
		fileName.toLowerCase().endsWith("html") || fileName.toLowerCase().endsWith("htm"))) {
			pmap.put(dlnodatajspKey, fileName);
		}
		return file;
	}

	private void makeSubDir(String path, String fname) {
		if (path == null || fname == null) {
			return;
		}
		String[] ds = fname.split("/");
		for (int i = 0; i < ds.length; i++) {
			if (i < (ds.length - 1)) {
				String d = ds[i];
				path += "/" + d;
				File dir = new File(path);
				if (!dir.exists()) {
					dir.mkdir();
				}
			}
		}

	}

	private String makeKey() {
		double d = Math.random();
		String key = d + "";
		key = key.replaceFirst("0\\.", "x");
		return key;
	}

	private Properties readAppProps() {
		Properties prop = new Properties();
		try {
			String loc = context.getInitParameter("spring.config.location");
			if (loc != null) {
				prop.load(new InputStreamReader(new FileInputStream(loc), "UTF-8"));
			} else {
				prop.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}

	public static void lookupMap(HttpServletRequest req, Map<String, String> pmap, Properties props)
			throws ServletException {
		String url = req.getRequestURL().toString();
		boolean ok = false;
		for (Object key : props.keySet()) {
			String s = (String) key;
			String[] ss = s.split("\\.");
			if (ss.length > 2 && url.toLowerCase().contains(ss[2].toLowerCase())) {
				if (s.startsWith("rc6.consumerKey")) {
					pmap.put(cidKey, (String) props.get(key));
					ok = true;
				}
				if (s.startsWith("rc6.consumerSeacret")) {
					pmap.put(csecKey, (String) props.get(key));
				}
				if (s.startsWith("rc6.canvasSeacret")) {
					pmap.put(cvsecKey, (String) props.get(key));
				}
			}
		}
		if (!ok) {
			throw new ServletException("このサーバー環境が登録されていません！");
		}
	}

	private String makeUrl(String clid, String redirect_uri, String key, String hostSfx,
			Map<String, String> pmap) throws IOException, NoSuchAlgorithmException {
		String url = getLoginNm(hostSfx, lcrep) + "/services/oauth2/authorize?response_type=code&"
				+ cidKey + "=";
		String uurl = url + clid + "&redirect_uri=" + redirect_uri + "&state=" + key;
		// code_challenge の生成
		String code_verifier = UUID.randomUUID().toString();
		pmap.put(codeVerifierKey, code_verifier);
		// 2. SHA-256ハッシュしてbase64urlエンコード
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(code_verifier.getBytes(StandardCharsets.US_ASCII));
		String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		uurl += "&code_challenge=" + codeChallenge + "&code_challenge_method=S256";
		System.out.println("requestCode:" + uurl);
		return uurl;
	}

	public static String getLoginNm(String hostSfx, LicenseRepository lcrep) {
		if (hostSfx == null || hostSfx.indexOf('.') == -1) {
			return "https://login.salesforce.com";
		}
		// 2020/09/10 Licenseを見る
		String res = null;
		List<License> lvs = lcrep.findByLicensename("authlogin");
		if (lvs != null && lvs.size() > 0 && !StringUtils.isEmpty(lvs.get(0).getServerurl())) {
			String[] ss = lvs.get(0).getServerurl().split(",");
			for (String s : ss) {
				if (hostSfx.contains(s)) {
					res = "login";
				}
			}
		}
		if (res == null) {
			lvs = lcrep.findByLicensename("authlogin");
			if (lvs != null && lvs.size() > 0 && !StringUtils.isEmpty(lvs.get(0).getServerurl())) {
				String[] ss = lvs.get(0).getServerurl().split(",");
				for (String s : ss) {
					if (hostSfx.contains(s)) {
						res = "test";
					}
				}
			}
		}
		if (res != null) {
			System.out.println("getLoginNm hit:" + res);
			return "https://" + res + ".salesforce.com";
		}
		// acaric例外対応 2017/04/17
		if (hostSfx.indexOf("acaric--c") != -1) {
			return "https://login.salesforce.com";
		}
		// .分割して1個目と2個めを比較
		String[] sfxs = hostSfx.split("\\.");
		if (isSandBox(sfxs[0]) || isSandBox(sfxs[1])) {
			return "https://test.salesforce.com";
		} else {
			// test scratch用urlを返す
			// return "https://momentum-ruby-4236-dev-ed.scratch.my.salesforce.com";
			return "https://login.salesforce.com";
		}

	}

	private static boolean isSandBox(String server) {
		if (server == null) {
			return false;
		}
		// It's easiest to check for 'my domain' sandboxes first
		// even though that will be rare
		if (server.contains("--"))
			return true;

		// tapp0 is a unique "non-cs" server so we check it now
		if (server == "tapp0")
			return true;

		// If server is "cs" followed by a number it"s a sandbox
		if (server.length() > 2) {
			if (server.substring(0, 2).equals("cs")) {
				try {
					Integer.valueOf(server.substring(2, server.length()));
				} catch (Exception e) {
					// started with cs, but not followed by a number
					return false;
				}

				// cs followed by a number, that's a hit
				return true;
			}
		}

		// If we made it here it's a production box
		return false;

	}
}

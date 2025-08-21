package jp.co.kpscorp.rc6.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.component.PrintService;
import jp.co.kpscorp.rc6.component.PrintServiceMapSuper;
import jp.co.kpscorp.rc6.model.Accesslog;
import jp.co.kpscorp.rc6.model.Hint;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Usertbl;
import jp.co.kpscorp.rc6.repo.AccesslogRepository;
import jp.co.kpscorp.rc6.repo.HintRepository;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.UsertblRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRuntimeException;

@Controller
@Scope("prototype")
public class DownloadService {

	private final PrintSource<Map<String, ?>> printSource;

	private int waitCnt = 0;

	private Map<String, String> pmap = null;
	public static final String pageEx = "Report pages larger than ";
	public static final String memEx = "Report size too large:";

	// @Autowired
	// private SfdcConnectImpl sfdcConnectImpl;
	// @Autowired
	// private SfdcRepositoryService sfdcRepositoryService;
	@Autowired
	private MapServiceRest mapServiceRest;
	@Autowired
	private HintRepository rHint;

	DownloadService(PrintSource<Map<String, ?>> printSource) {
		this.printSource = printSource;
	}

	public void doGetSync(HttpServletRequest req, HttpServletResponse resp,
			String orgid, Map<String, String> pmap) throws ServletException {
		synchronized (this) {
			System.out.println("Org id:" + orgid + " batch download Start! by "
					+ this);
			// Batch Option Check
			if (!checkOrg(orgid, pmap, req.getSession().getServletContext(),
					Util.getApplicationContext()) && Util.isNouserMode(pmap)) {
				return;
			}
			if (!Util.isNouserMode(pmap)) {
				// dl.jspへ遷移しDownloadへ
				return;
			}
			doGet(req, resp);
			System.out.println("Org id:" + orgid + " batch download End! by "
					+ this);
		}
	}

	private boolean checkOrg(String orgid, Map<String, String> pmap,
			ServletContext context, ApplicationContext con)
			throws ServletException {
		try {
			// 実行ユーザーとバッチユーザーの組織が一致しているか？
			String authParm = pmap.get(Util.authParmKey);
			if (authParm != null) {
				JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
				String loid = (String) ajo.get(Util.licenseOidKey);
				if (!orgid.substring(0, 15).equals(loid.substring(0, 15))) {
					pmap.put(Util.batchErrorMsgKey,
							"実行組織と固定化ユーザーの組織が一致しません！");
					return false;
				}
			}
			List<Map<String, String>> mps = AuthTokenService.getOrgsById(con,
					orgid.substring(0, 15), false);
			for (Map<String, String> mp : mps) {
				if (Util.batchLicenseKey.equals(mp.get("licensename"))) {
					return true;
				}
			}
			QueryResult qr = null;
			try {
				String ql = "Select Id,name,OrganizationType From Organization where id='"
						+ orgid + "'";
				List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
				// null, ql);
				// sfdcRepositoryServiceから読む
				// List<Map<String, ?>> maps = sfdcConnectImpl.doQuery(ql);
				// List<Map<String, ?>> maps = sfdcRepositoryService.doSoql(ql);
				if (maps.size() > 0) {
					Map<String, ?> org = maps.get(0);
					String orgType = (String) org.get("OrganizationType");
					if ("Developer Edition".equals(orgType)) {
						// Developer Editionは通す
						return true;
					}
				}
				/*
				 * qr = pcon
				 * .queryAll("Select Id,name,OrganizationType From Organization where id='"
				 * + orgid + "'");
				 * if (qr.getRecords().length > 0) {
				 * SObject org = qr.getRecords()[0];
				 * String orgType = (String) org.getChild("OrganizationType")
				 * .getValue();
				 * if ("Developer Edition".equals(orgType)) {
				 * // Developer Editionは通す
				 * return true;
				 * }
				 * }
				 */
			} catch (ConnectionException e) {
				System.out.println(e.getMessage() + "\n" + e);
			}
		} catch (Exception e) {
			// throw new ServletException(e);
			// DB使用不可の場合 true
			return true;
		}
		pmap.put(Util.batchErrorMsgKey,
				"この組織ではログインユーザー固定化は使用できません！ReportsConnectのホームページからログインユーザー固定化オプションをお申込みください");
		return false;
	}

	public synchronized int addWaitCnt() {
		waitCnt++;
		return waitCnt;
	}

	public synchronized int subWaitCnt() {
		waitCnt--;
		return waitCnt;
	}

	public int getWaitCnt() {
		return waitCnt;
	}

	@RequestMapping("/dl2")
	public String doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		ApplicationContext con = Util.getApplicationContext();
		String key = req.getParameter("state");
		if (key == null) {
			key = (String) req.getAttribute("key");
		}
		// Map<String, String> pmap = null;
		pmap = AuthTokenService.getPmap(req, resp, key);
		if (pmap == null) {
			return "views/dlError";
		}
		ServletContext context = req.getSession().getServletContext();
		String s = pmap.get(Downloder.JSonKey);
		System.out.println("JSonKey:" + s);
		if (s == null) {
			AuthTokenService.cleanUp(key, context);
			throw new ServletException("no JSON Key!");
		}
		printHeader(req);
		if (pmap.get(Downloder.ipKey) == null
				&& !Util.isNouserMode(pmap)) {
			if (req.getHeader("X-Forwarded-For") != null) {
				pmap.put(Downloder.ipKey, req.getHeader("X-Forwarded-For"));
			} else {
				pmap.put(Downloder.ipKey, req.getRemoteAddr());
			}
		}
		String ip = pmap.get(Downloder.ipKey);
		JSONObject jo = (JSONObject) JSONValue.parse(s);
		// sfdcConnectImpl.setToken((String) jo.get("access_token"));
		// sfdcConnectImpl.setServerUrl((String) jo.get("instance_url"));
		String[] louid = getLicenseOuid(pmap, jo);
		String rnnningid = louid[1];
		Settings.Userprop up = null;
		try {
			// WebApplicationContext wac = WebApplicationContextUtils
			// .getRequiredWebApplicationContext(context);
			try {
				// String tk = (String) jo.get("access_token");
				// ConnectorConfig config = new ConnectorConfig();
				// config.setSessionId(tk);
				// config.setServiceEndpoint(jo.get("instance_url")
				// + "/services/Soap/u/22.0");
				// 22.0 -> 40.0 2022/04/05 <-これだとhttp timeoutになる
				// config.setAuthEndpoint(
				// "https://login.salesforce.com/services/Soap/u/54.0");
				// api 39以下廃止のためPartnerConnectionは使用しないrestに統一
				// 以下のPartnerConnectionは実際には使用されない
				// System.out.println("ServiceEndpoint:" + config.getServiceEndpoint());
				// PartnerConnection connection = Connector.newConnection(config);
				// connection.setSessionHeader(tk);
				// UserのCheck
				up = checkUsr(pmap, jo, con);
				System.out.println("User:" + up.getLicense() + "/"
						+ up.getOrgid() + "/" + up.getUserid());
				if (pmap.get(Downloder.authOlnyKey) != null) {
					// 認証
					putLog(ip, con, louid[0], louid[1], 6, up, s);
					// lastJsonはDBからアプリケーションスコープへ変更
					String svkey = Downloder.lastJsonKey + "@" + up.getUserid();
					System.out.println("Set AppScope key:" + svkey + " val" + s);
					req.getSession().getServletContext().setAttribute(svkey, s);
					req.setAttribute("baseUrl",
							AuthTokenService.getBaseUrl(req));
					String go = AuthTokenService
							.dispachJsp(req, resp, pmap, key,
									Downloder.dlnodatajspKey,
									"views/auth");
					return go;
				}
				// ユーザー並行処理を制御
				if (!checkPara(rnnningid)) {
					String go = AuthTokenService.dispachError("処理できません", "同一ユーザーが印刷中です",
							req, resp, pmap, key);
					putLogSync(ip, con, louid[0], louid[1], 1, up, s);
					return go;
				}

				String compName = pmap.get("compName");
				if (compName == null) {
					compName = "MapList";
				}
				// PrintService<Map<String, Object>> ps = (PrintService<Map<String, Object>>)
				// wac
				// .getBean(compName);
				MapListFactory factory = (MapListFactory) con.getBean(MapListFactory.class);
				PrintService ps = factory.create(compName);
				String fname = pmap.get(Downloder.FnKey);
				if (fname != null) {
					ps.setFileName(fname);
				} else {
					// fname無い場合はPrintSourceのdefaultを使用
					fname = printSource.getFileName();
					pmap.put(Downloder.FnKey, fname);
				}
				ps.setRequest(req);
				ps.setRespons(resp);
				// User情報をThreadlocalへ登録
				ThreadMap.get().put(Settings.TH_USRPROP, up);
				// データ確認モードCheck
				if ("true".equals(pmap.get(Downloder.DataCheckKey))) {
					String ql = pmap.get(Downloder.QlKey);
					if (ql != null && ps instanceof PrintServiceMapSuper) {
						PrintServiceMapSuper psms = (PrintServiceMapSuper) ps;
						psms.setQl(makeQl(ql, pmap));
					}
					goDataCheck(pmap, req, resp, ps, con);
					putLog(ip, con, louid[0], louid[1], 5, up, s);
					return "views/dcheck";
				}
				// ps.setConnection(connection);
				// 外字ファイルをThreadlocalへ登録
				// TODO とりあえず保留
				// String eudcfp = pmap.get(EudcFontRegistry.TH_EUDCFILE);
				// if (eudcfp != null) {
				// ThreadMap.get().put(EudcFontRegistry.TH_EUDCFILE,
				// new File(eudcfp));
				// }
				String[] jfns = {};
				if (pmap.get(Downloder.jrxmlFnKey) != null
						&& !pmap.get(Downloder.jrxmlFnKey).trim().equals("")) {
					jfns = pmap.get(Downloder.jrxmlFnKey).split(",");
				} else {
					throw new PrintServiceException(
							PrintServiceMapSuper.nojrexlMsg);
				}
				long mapsize = 0;
				ThreadMap.get().put(Settings.TH_PAGECNT, new Integer(0));
				List<ByteArrayOutputStream> byteOuts = new ArrayList<ByteArrayOutputStream>();
				// PrintSource<Map<String, ?>> source = null;
				for (String jfn : jfns) {
					resetPmap(pmap, jfn);
					String ql = pmap.get(Downloder.QlKey);
					if (ql != null && ps instanceof PrintServiceMapSuper) {
						PrintServiceMapSuper psms = (PrintServiceMapSuper) ps;
						psms.setQl(makeQl(ql, pmap));
					}
					// source = new PrintSource<Map<String, ?>>(pmap);
					ps.setXmlPath("/jasper/" + key + "/" + jfn);
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					printSource.setMapsize(mapsize);
					byteOut = ps.documentEdit(byteOut, printSource);
					if (mapsize < printSource.getMapsize()) {
						// dataない場合は足さない
						mapsize = printSource.getMapsize();
						byteOuts.add(byteOut);
					}
				}
				if (mapsize == 0) {
					// 0件画面へ
					throw new RCException(Downloder.dlnodatajspKey);
				}
				ByteArrayOutputStream byteOut = margePdf(byteOuts);
				if (byteOut != null) {
					downloadOrSavePDF(req, resp, key, pmap, jo, ps,
							fname, printSource, byteOut);
				}
				putLog(ip, con, louid[0], louid[1], 0, up, s);
			} finally {
				// synchronized (Downloder.runningUsers) {
				Downloder.runningUsers.remove(rnnningid);
				// }
			}
		} catch (Exception e) {

			if (e instanceof JRRuntimeException
					&& e.getCause() instanceof JRException) {
				e = (Exception) e.getCause();
			}
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(Downloder.dlnodatajspKey)) {
				String go;
				try {
					if (Util.isNouserMode(pmap)) {
						String msg = "印刷対象データがありません！";
						go = AuthTokenService.dispachError("印刷データ無し", msg, req,
								resp, pmap, key);
					} else {
						req.setAttribute("baseUrl",
								AuthTokenService.getBaseUrl(req));
						go = AuthTokenService.dispachJsp(req, resp, pmap, key,
								Downloder.dlnodatajspKey,
								"views/dlNodata");
					}
				} catch (IOException e1) {
					throw new ServletException(e);
				}
				return go;
			}
			if (e instanceof JRException && e.getMessage() != null
					&& e.getMessage().contains(pageEx)) {
				String go;
				String msg = "印刷ページ数が制限を超えています。制限値："
						+ e.getMessage().replaceFirst(pageEx, "")
						+ "<br/>制限値については、<a href='http://www.reportsconnect.com/rc4.html'>こちら</a>を参照してください。";
				try {
					putLog(ip, con, louid[0], louid[1], 3, up, s);
					go = AuthTokenService.dispachError("制限オーバー", msg, req, resp,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(memEx)) {
				String msg = "読み込みデータ量が制限を超えています。制限値："
						+ e.getMessage().replaceFirst(memEx, "")
						+ "<br/>制限値については、<a href='http://www.reportsconnect.com/rc4.html'>こちら</a>を参照してください。";
				String go;
				try {
					putLog(ip, con, louid[0], louid[1], 2, up, s);
					go = AuthTokenService.dispachError("制限オーバー", msg, req, resp,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			if (e instanceof PrintServiceException && e.getMessage() != null
					&& e.getMessage().contains(PrintServiceMapSuper.nojrexlMsg)) {
				String msg = "この帳票にはjrxmlファイルが設定されていません！";
				String go;
				try {
					putLog(ip, con, louid[0], louid[1], 4, up, s);
					go = AuthTokenService.dispachError("設定エラー", msg, req, resp,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			// SFからエラーのケース
			// if (e instanceof RCException && e.getMessage() != null
			if (e.getMessage() != null // 2025/06/16 exceptionなら拾う
					&& !"true".equals(pmap.get(Downloder.DataCheckKey))) {
				String msg = e.getMessage();
				String go;
				try {
					if (rnnningid != null) {
						putLog(ip, con, exOrgId(rnnningid),
								exUsrId(rnnningid), 2, up, s);
					}
					go = AuthTokenService.dispachError("エラーにより印刷できません！", msg, req,
							resp, pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			try {
				putLog(ip, con, louid[0], louid[1], 4, up, s);
			} catch (Exception e1) {
				throw new ServletException(e);
			}
			throw new ServletException(e);
		} finally {
			AuthTokenService.cleanUp(key, context);
		}
		return null;
	}

	private void downloadOrSavePDF(HttpServletRequest req,
			HttpServletResponse resp, String key, Map<String, String> pmap,
			JSONObject jo,
			PrintService ps, String fname,
			PrintSource<Map<String, ?>> source, ByteArrayOutputStream byteOut)
			throws UnsupportedEncodingException, ConnectionException,
			IOException, ServletException, KeyManagementException, NoSuchAlgorithmException, RCException {
		String pid = pmap.get(Downloder.parentidKey);
		if (pid != null) {
			// Attachementをinsertする
			String rurl = pmap.get(Downloder.returnUrlKey);
			if (rurl == null) {
				rurl = jo.get("instance_url") + "/" + pid;
			} else if (rurl.startsWith("/")) {
				rurl = jo.get("instance_url") + rurl;
			}
			JSONObject res = insertPdf(fname + ".pdf", byteOut.toByteArray(),
					pid, pmap);
			if (rurl.contains("?")) {
				rurl += "&file=" + (String) res.get("id");
			} else {
				rurl += "?file=" + (String) res.get("id");
			}
			if ((Boolean) res.get("success")) {
				// nouser modeの場合はpmapにidを保管
				if (Util.isNouserMode(pmap)) {
					pmap.put(Util.attachIdKey, (String) res.get("id"));
				} else {
					resp.sendRedirect(rurl);
				}
			} else {
				AuthTokenService.dispachError("PDFファイルの保存に失敗しました!",
						res.get("Errors").toString(), req, resp, pmap, key);
			}
		} else if (Util.isNouserMode(pmap)) {
			String authParm = pmap.get(Util.authParmKey);
			JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
			String luid = (String) ajo.get(Util.licenseUidKey);
			String desc = (String) ajo.get(Util.feedDescKey);
			JSONObject res = insertFeedData(fname + ".pdf",
					byteOut.toByteArray(), luid, desc, pmap);
			if ((Boolean) res.get("success")) {
				pmap.put(Util.attachIdKey, (String) res.get("id"));
			} else {
				AuthTokenService.dispachError("PDFファイルの保存に失敗しました!",
						res.get("Errors").toString(), req, resp, pmap, key);
			}
		} else {
			// pdfをdownloadする
			ps.prepareResponse(byteOut, source);
		}
	}

	private void resetPmap(Map<String, String> pmap, String jfn) {
		// リポート単位のパラメーターを初期化して再設定
		int i = jfn.lastIndexOf("/");
		if (i < 1) {
			return;
		}
		String[] parmByRepstrs = Downloder.parmByReps.split(",");
		String prefix = jfn.substring(0, i + 1);
		for (String parmByRep : parmByRepstrs) {
			pmap.put(parmByRep, pmap.get(prefix + parmByRep));
		}
	}

	private ByteArrayOutputStream margePdf(List<ByteArrayOutputStream> byteOuts)
			throws IOException/* , DocumentException */, DocumentException {
		if (byteOuts.size() == 1) {
			return byteOuts.get(0);
		}
		ByteArrayOutputStream res = new ByteArrayOutputStream();
		PdfCopyFields cp = new PdfCopyFields(res);
		for (ByteArrayOutputStream byteOut : byteOuts) {
			PdfReader rd = new PdfReader(byteOut.toByteArray());
			cp.addDocument(rd);
		}
		cp.close();
		return res;

	}

	private boolean checkPara(String uid) {
		synchronized (Downloder.runningUsers) {
			if (Downloder.runningUsers.contains(uid)) {
				return false;
			}
			Downloder.runningUsers.add(uid);
		}
		return true;
	}

	private void goDataCheck(Map<String, String> pmap,
			HttpServletRequest req,
			HttpServletResponse resp, PrintService ps,
			ApplicationContext con) throws Exception {
		try {
			// 隠すもの
			String[] hidestrs = {
					// EudcFontRegistry.TH_EUDCFILE,
					Downloder.JSonKey, Downloder.cidKey, Downloder.csecKey,
					Downloder.cvsecKey, Downloder.ipKey,
					Util.authParmKey, Util.batchErrorMsgKey };
			String pmapStr = makePmapString(pmap, hidestrs);
			String dataStr = "";
			req.setAttribute("pmapStr", formatJson(pmapStr));
			Csvwrap csvw = new Csvwrap();
			String ql = makeQl(pmap.get(Downloder.QlKey), pmap);
			req.setAttribute("ql", ql);
			if (ql != null) {
				ql = ql.replaceFirst(" limit .*", "") + " limit 10";
				List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
				// sfdcConnectImplから読む <-やめ
				// List<Map<String, ?>> maps = sfdcConnectImpl.doQuery(ql);
				// // List<Map<String, ?>> maps = sfdcRepositoryService.doSoql(ql);
				// maps = sfdcRepositoryService.makeVoList(maps, CaseFreeMap.class);
				dataStr = JSONValue.toJSONString(maps);
				csvw = makeCSV(maps, null);
				// req.setAttribute("xml", makeXml(maps));
			}
			req.setAttribute("dataStr", formatJson(dataStr));
			req.setAttribute("cw", csvw);
			// req.setAttribute("csvs", makeCsvBox(csvw));

			// 実際に出力してみる
			if (pmap.get(Downloder.jrxmlFnKey) != null) {
				String key = req.getParameter("state");
				ps.setXmlPath("/jasper/" + key + "/"
						+ pmap.get(Downloder.jrxmlFnKey));
			} else {
				req.setAttribute("hint", getHint(con, "please download"));
				return;
			}
			// ps.setConnection(connection);
			// PrintSource<Map<String, ?>> source = new PrintSource<Map<String, ?>>(
			// pmap);
			// 外字ファイルをThreadlocalへ登録
			// TODO 外字ファイル
			// String eudcfp = pmap.get(EudcFontRegistry.TH_EUDCFILE);
			// if (eudcfp != null) {
			// ThreadMap.get().put(EudcFontRegistry.TH_EUDCFILE,
			// new File(eudcfp));
			// }
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ps.documentEdit(byteOut, printSource);
		} catch (Throwable e) {
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(Downloder.dlnodatajspKey)) {
				req.setAttribute("hint", getHint(con, e.getMessage()));
				return;
			}
			if (!(e instanceof Exception) && e.getCause() instanceof Exception) {
				e = e.getCause();
			}
			Exwrap ew = new Exwrap(e);
			req.setAttribute("ex", ew);

			req.setAttribute("hint", getHint(con, ew.getMsgOrStr()));
		} finally {
			req.setAttribute("baseUrl", AuthTokenService.getBaseUrl(req));
			// リダイレクト
			// RequestDispatcher dispatcher = req.getSession().getServletContext()
			// .getRequestDispatcher("/WEB-INF/views/dcheck.jsp");
			// dispatcher.forward(req, resp);

		}
	}

	public static String makePmapString(Map<String, String> pmap,
			String[] hidestrs) {
		Map<String, String> wmap = new HashMap<String, String>();
		wmap.putAll(pmap);
		for (String hidestr : hidestrs) {
			wmap.remove(hidestr);
		}
		String pmapStr = JSONValue.toJSONString(wmap);
		return pmapStr;
	}

	// Hitの出力
	private String getHint(ApplicationContext con, String msg) {
		List<Hint> l = rHint.findByKindIsNullOrKind("sfdc");
		for (Hint h : l) {
			if (msg.contains(h.getMsgstring())) {
				return h.getHintmessage();
			}
		}
		return null;
	}

	private Csvwrap makeCSV(List<Map<String, ?>> maps, String name) {
		Csvwrap mycsv = new Csvwrap();
		mycsv.name = name;
		StringBuilder sb = new StringBuilder();
		StringBuilder hd = null;
		List<String> dubchk = new ArrayList<String>();
		for (Map<String, ?> map : maps) {
			StringBuilder line = new StringBuilder();
			if (hd == null
					|| hd.toString().split(",").length < map.keySet().size()) {
				hd = new StringBuilder();
				for (String tit : map.keySet()) {
					hd.append(tit).append(",");
				}
			}
			for (String tit : map.keySet()) {
				if (map.get(tit) instanceof List) {
					Csvwrap chcsv = makeCSV(
							(List<Map<String, ?>>) map.get(tit), tit);
					if (!dubchk.contains(tit)) {
						// 同じ名前のCSVデータソースは作らない
						mycsv.csvws.add(chcsv);
						dubchk.add(tit);
					}
					line.append("(List)" + tit).append(",");
				} else {
					line.append(dvqu(map.get(tit))).append(",");
				}
			}
			sb.append(line.substring(0, line.length() - 1)).append("\n");
		}
		// headerをつける
		String csv = hd.substring(0, hd.length() - 1) + "\n" + sb.toString();

		mycsv.csv = csv;
		return mycsv;
	}

	private String dvqu(Object object) {
		if (object == null) {
			return "";
		}
		if (object instanceof String) {
			String s = (String) object;
			if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
				return "\"" + s.replaceAll("\"", "\"\"") + "\"";
			}
		}
		return object.toString();
	}

	public static class Csvwrap {
		private String name;

		public String getName() {
			return name;
		}

		public String getCsv() {
			return csv;
		}

		public List<Csvwrap> getCsvws() {
			return csvws;
		}

		private String csv;
		private List<Csvwrap> csvws = new ArrayList<Csvwrap>();
	}

	public static class Exwrap {
		private Throwable e;

		public Exwrap(Throwable e) {
			super();
			this.e = e;
		}

		public String getMessage() {
			return e.getMessage();
		}

		public String getString() {
			return e.toString();
		}

		public String getMsgOrStr() {
			if (getMessage() == null) {
				return getString();
			}
			return getMessage();
		}

		public StackTraceElement[] getStackTrace() {
			return e.getStackTrace();
		}

		public String getStackTraceStr() {
			// themleaf3 で stackTrace element は表示しないので
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return sw.toString();
		}

	}

	private String formatJson(String s) {
		StringBuilder sb = new StringBuilder();
		boolean ignore = false;
		int indent = 0;
		for (int i = 0; i < s.length(); i++) {
			String a = null;
			if (i > 0) {
				a = s.substring(i - 1, i);
			}
			String b = s.substring(i, i + 1);
			if ("\"".equals(b) && !"\\".equals(a)) {
				ignore = !ignore;
			}
			if (ignore) {
				sb.append(b);
			} else {
				if ("[".equals(b) || "{".equals(b)) {
					sb.append(b);
					indent++;
					sb.append(addIndent(indent));
				} else if (",".equals(b)) {
					sb.append(b);
					sb.append(addIndent(indent));
				} else if ("]".equals(b) || "}".equals(b)) {
					indent--;
					sb.append(addIndent(indent));
					sb.append(b);
				} else {
					sb.append(b);
				}
			}
		}
		return sb.toString();
	}

	private String addIndent(int cnt) {
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < cnt; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}

	private String makeQl(String ql, Map<String, String> pmap) {
		if (ql == null) {
			return ql;
		}
		// 文字列長い方から変換
		String[] keys = pmap.keySet().toArray(new String[1]);
		Arrays.sort(keys, new LengComparator());
		for (String key : keys) {
			ql = ql.replaceAll(":" + key, pmap.get(key));
		}
		// :xxxが残っていたら消す（文字列に埋まっているものはこれでは消せない)
		ql = ql.replaceAll("^:[^ ]*\n", " ").replaceAll("^:[^ ]*\r\n", " ")
				.replaceAll(" :[^ ]*\n", " ").replaceAll(" :[^ ]*\r\n", " ")
				.replaceAll(" :[^ ]* ", " ").replaceFirst("^:[^ ]* ", " ")
				.replaceFirst(" :[^ ]*$", " ");
		return ql;
	}

	class LengComparator implements Comparator<String> {
		public int compare(String o1, String o2) {
			return o2.length() - o1.length();
		}
	}

	private Settings.Userprop checkUsr(Map<String, String> pmap, JSONObject jo,
			ApplicationContext con) {
		String oid;
		String uid;
		String[] ouid = getLicenseOuid(pmap, jo);
		oid = ouid[0];
		uid = ouid[1];
		Map<String, License> lmap = getLmap(con);
		try {
			String[] cks = Downloder.noneOrgCheckKey.split(",");
			if (pmap.get(cks[0]) != null && pmap.get(cks[0]).equals(cks[1])) {
				// check 迂回
				System.out.println("Didn't Org check!");
				return new Settings.Userprop(oid, uid, lmap.get("pro"),
						"Org for Test");
			}
			Usertbl usr = getUser(con, uid);
			if (usr != null) {
				return new Settings.Userprop(oid, uid, lmap.get(usr
						.getLicensename()), "License user");
			}
			try {
				System.out.println("into rest query:");
				// con.queryAllを使用しない
				String ql = "Select Id,name,OrganizationType From Organization where id='"
						+ oid + "'";
				// sfdcConnectImplから読む <-やめ
				List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
				// List<Map<String, ?>> maps = getMapService(pmap,
				// new PrintSource<Map<String, ?>>()).queryToMap(
				// null, ql);
				if (maps.size() > 0) {
					Map<String, ?> org = maps.get(0);
					String orgType = (String) org.get("OrganizationType");
					System.out.println("Check orgType:" + orgType);
					if (lmap.get(orgType) != null) {
						return new Settings.Userprop(oid, uid,
								lmap.get(orgType), lmap.get(orgType)
										.getLicensename());
					}
				}
				/*
				 * QueryResult qr = null;
				 * System.out.println("into queryAll:");
				 * qr =
				 * con.queryAll("Select Id,name,OrganizationType From Organization where id='"
				 * + oid + "'");
				 * if (qr.getRecords().length > 0) {
				 * SObject org = qr.getRecords()[0];
				 * String orgType = (String) org.getChild("OrganizationType")
				 * .getValue();
				 * System.out.println("Check orgType:" + orgType);
				 * if (lmap.get(orgType) != null) {
				 * return new Settings.Userprop(oid, uid,
				 * lmap.get(orgType), lmap.get(orgType)
				 * .getLicensename());
				 * }
				 * }
				 * } catch (ConnectionException e) {
				 * System.out.println("con.queryAll failed:" + e.getMessage() + "\n" + e);
				 * }
				 */
			} catch (Exception e) {
				System.out.println("rest query failed:" + e.getMessage() + "\n" + e);
				e.printStackTrace();
			}
			System.out.println("Org check free!");
			return new Settings.Userprop(oid, uid, lmap.get("free"), "free");
		} catch (Exception e) {
			// DB使用不可の場合
			System.out.println("DB Error! license pro" + e.getMessage() + "\n" + e);
			return new Settings.Userprop(oid, uid, lmap.get("pro"), "DB Error!");
		}
	}

	public static String[] getLicenseOuid(Map<String, String> pmap,
			JSONObject jo) {
		String[] ouid = new String[2];
		String authParm = pmap.get(Util.authParmKey);
		if (authParm != null) {
			JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
			ouid[0] = (String) ajo.get(Util.licenseOidKey);
			ouid[1] = (String) ajo.get(Util.licenseUidKey);
		} else {
			ouid[0] = exOrgId((String) jo.get("id"));
			ouid[1] = exUsrId((String) jo.get("id"));
		}
		return ouid;
	}

	public static Map<String, License> getLmap(ApplicationContext con) {
		try {
			// String sql = "SELECT * FROM license";
			// PreparedStatement pstmts = con.prepareStatement(sql);
			// ResultSet result = pstmts.executeQuery();
			// List<License> l = getByObjs(result, License.class);
			LicenseRepository rLicense = con.getBean(LicenseRepository.class);
			List<License> l = rLicense.findAll();
			Map<String, License> mp = new HashMap<String, License>();
			for (License lv : l) {
				mp.put(lv.getLicensename(), lv);
			}
			// result.close();
			return mp;
		} catch (Exception e) {
			// DB使用不可の場合
			String[][] ltbl = { { "basic", "200", "1000000" },
					{ "Developer Edition", "20", "300000" },
					{ "free", "3", "200000" }, { "light", "30", "500000" },
					{ "pro", "200", "1000000" } };
			Map<String, License> mp = new HashMap<String, License>();
			for (String[] ss : ltbl) {
				License lv = new License();
				lv.setLicensename(ss[0]);
				lv.setMaxpage(new Integer(ss[1]));
				lv.setMaxmen(new Integer(ss[2]));
				mp.put(ss[0], lv);
			}
			return mp;
		}
	}

	private Usertbl getUser(ApplicationContext con, String uid) throws SQLException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		// String sql = "SELECT * FROM usertbl where userid like ?";
		// PreparedStatement pstmts = con.prepareStatement(sql);
		// if (uid.length() > 15) {
		// uid = uid.substring(0, 15);
		// }
		// pstmts.setString(1, uid + "%");
		// ResultSet result = pstmts.executeQuery();
		// List<Usertbl> l = getByObjs(result, Usertbl.class);
		List<Usertbl> l = con.getBean(UsertblRepository.class).findByUserid(uid);
		if (l.isEmpty()) {
			return null;
		}
		// result.close();
		return l.get(0);
	}

	private synchronized void putLogSync(String ip, ApplicationContext con, String oid,
			String uid, int err, Settings.Userprop up, String tk)
			throws IllegalArgumentException, SecurityException, SQLException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchFieldException,
			NoSuchMethodException {
		putLog(ip, con, oid, uid, err, up, tk);
	}

	/**
	 * @param ip
	 * @param con
	 * @param oid
	 * @param uid
	 * @param err
	 *            0:通常印刷 1:並行チェック 2:メモリーオーバー 3:ページオーバー 4:その他エラー 5:データの確認 6:認証
	 * @param up
	 * @param tk
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws NoSuchMethodException
	 */
	private void putLog(String ip, ApplicationContext con, String oid, String uid,
			int err, Settings.Userprop up, String tk) {
		System.out.println("put log/oid:" + oid + " uid:" + uid + " errcode:"
				+ err);
		try {
			// String sql = "select * from accesslog where orgid = ? and userid = ?";
			// PreparedStatement pstmts = con.prepareStatement(sql);
			// pstmts.setString(1, oid);
			// pstmts.setString(2, uid);
			// ResultSet result = pstmts.executeQuery();
			// List<Accesslog> acs = getByObjs(result,
			// Accesslog.class);
			AccesslogRepository rAccesslog = con.getBean(AccesslogRepository.class);
			List<Accesslog> acs = rAccesslog.findByOrgidAndUserid(oid, uid);
			if (!acs.isEmpty()) {
				Accesslog ac = acs.get(0);
				ac.setAccesscnt(ac.getAccesscnt() + 1);
				// V5をカウント ReportsConnect5のみ
				ac.setV5cnt(ac.getV5cnt() + 1);
				switch (err) {
					case 1:
						ac.setIdruningcnt(ac.getIdruningcnt() + 1);
						break;
					case 2:
						ac.setMemovercnt(ac.getMemovercnt() + 1);
						break;

					case 3:
						ac.setPageovercnt(ac.getPageovercnt() + 1);
						break;
					case 4:
						ac.setOtherercnt(ac.getOtherercnt() + 1);
						break;
					case 5:
						ac.setDatacheckcnt(ac.getDatacheckcnt() + 1);
						break;
					case 6:
						// lastJsonはDBからアプリケーションスコープへ変更
						// ac.setLastjson(tk);
						break;
				}
				setOther(up, ac, tk, ip);
				// ModelFactory.upadte(con, ac, "orgid,userid".split(","));
				rAccesslog.saveAndFlush(ac);
			} else {
				Accesslog ac = new Accesslog(oid, uid);
				ac.setAccesscnt(1);
				ac.setV5cnt(1);
				ac.setIp(ip);
				switch (err) {
					case 1:
						ac.setIdruningcnt(1);
						break;
					case 2:
						ac.setMemovercnt(1);
						break;
					case 3:
						ac.setPageovercnt(1);
						break;
					case 4:
						ac.setOtherercnt(1);
						break;
					case 5:
						ac.setDatacheckcnt(1);
						break;
					case 6:
						// lastJsonはDBからアプリケーションスコープへ変更
						// ac.setLastjson(tk);
						break;
				}
				setOther(up, ac, tk, ip);
				// ModelFactory.insert(con, ac);
				rAccesslog.saveAndFlush(ac);
			}
			// result.close();
			// pstmts.close();
		} catch (Exception e) {
			// DB使用不可の場合
			System.out.println("Can't put log " + e);
		}
	}

	private void setOther(Settings.Userprop up, Accesslog ac, String tk,
			String ip) {
		ac.setLastdate(new Timestamp(System.currentTimeMillis()));
		if (up != null) {
			ac.setLicensename(up.getLicense().getLicensename());
		}
		// jsonはauthOnly時のみ
		// ac.setLastjson(tk);
		// その他の統計資料はJSONで
		JSONObject jo = null;
		if (ac.getMemo() != null) {
			try {
				jo = (JSONObject) JSONValue.parse(ac.getMemo());
			} catch (Exception e) {
			}
		}
		if (jo == null) {
			jo = new JSONObject();
		}
		if (pmap.get(Util.authParmKey) != null) {
			// batch mode
			addCnt(jo, "batch");
		}
		if (Util.isNouserMode(pmap)) {
			// no UI
			addCnt(jo, "noui");
		}
		JSONObject uif = AuthTokenService.getUifo(pmap);
		if (uif != null) {
			jo.put("ver", uif.get(Util.versionKey));
		}
		String s = jo.toJSONString();
		if (s.length() > 1024) {
			s = s.substring(0, 1024);
		}
		ac.setMemo(s);
		ac.setIp(ip);
	}

	private void addCnt(JSONObject jo, String prop) {
		long cnt;
		if (jo.get(prop) == null) {
			cnt = 1;
		} else {
			Long ocnt = (Long) jo.get(prop);
			cnt = ocnt + 1;
		}
		jo.put(prop, cnt);
	}

	public static String exOrgId(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceFirst(".*/id/", "").replaceFirst("/.*", "");
	}

	public static String exUsrId(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceFirst(".*/id/", "").replaceFirst(".*/", "");
	}

	private JSONObject insertPdf(String name, byte[] bs, String pid,
			Map<String, String> pmap)
			throws ConnectionException, KeyManagementException, NoSuchAlgorithmException, IOException, RCException {
		Map<String, String> data = new HashMap<String, String>();
		data.put("name", name);
		data.put("body", new String(new Base64().encode(bs)));
		data.put("parentId", pid);
		return insertSobj(pmap, "Attachment", data);
	}

	private JSONObject insertFeedData(String name, byte[] bs, String pid,
			String desc, Map<String, String> pmap)
			throws ConnectionException, KeyManagementException, NoSuchAlgorithmException, IOException, RCException {
		Map<String, String> data = new HashMap<String, String>();
		data.put("ContentFileName", name);
		data.put("ContentData", new String(new Base64().encode(bs)));
		data.put("ContentDescription", desc);
		data.put("parentId", pid);
		return insertSobj(pmap, "FeedItem", data);
	}

	// PartnerConnection廃止
	private JSONObject insertSobj(Map<String, String> pmap, String type,
			Map<String, String> data) throws KeyManagementException, NoSuchAlgorithmException,
			UnsupportedEncodingException, IOException, RCException {
		String s = pmap.get(Downloder.JSonKey);
		JSONObject jo = (JSONObject) JSONValue.parse(s);
		String tk = (String) jo.get("access_token");
		String surl = (String) jo.get("instance_url");
		String apiv = pmap.get(Downloder.apivKey);
		// TLS V1.1対応2016/3/30
		SSLContext sslContext = SSLContexts.custom().useTLS().build();
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(
				sslContext, new String[] { "TLSv1.1", "TLSv1.2" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(f)
				.build();
		String url;
		// if (apiv != null) {
		// System.out.println("API Version changed to " + apiv);
		// url = surl + "/services/data/" + apiv + "/sobjects/" + type;
		// } else {
		url = surl + "/services/data/v35.0/sobjects/" + type;
		// }
		HttpPost post = new HttpPost(url);
		// set the token in the header
		post.addHeader("Authorization", "OAuth " + tk);
		post.addHeader("Content-type", "application/json; charset=UTF-8");
		String json = JSONObject.toJSONString(data);
		StringEntity entity = new StringEntity(json, "UTF-8");
		post.setEntity(entity);
		HttpResponse response = httpclient.execute(post);
		JSONObject rjs = Util.responseTojson(response);
		return rjs;
	}
	/*
	 * private SaveResult insertSobj(PartnerConnection connection, String type,
	 * Map<String, Object> data) throws UnsupportedEncodingException,
	 * ConnectionException {
	 * SObject att = new SObject();
	 * att.setType(type);
	 * for (String key : data.keySet()) {
	 * att.setField(key, data.get(key));
	 * }
	 * //TODO
	 * SaveResult[] srs = connection.create(new SObject[] { att });
	 * System.out.println("pdf saved:\n" + srs[0].toString());
	 * return srs[0];
	 * }
	 */

	// public void doPost(HttpServletRequest req, HttpServletResponse resp)
	// throws ServletException, IOException {
	// ApplicationContext con = null;
	// String key = null;
	// try {
	// // Canvasプリケーションのsigned request対応
	// // Pull the signed request out of the request body and verify/decode
	// // it.
	// Map<String, String[]> parameters = req.getParameterMap();
	// String[] signedRequest = parameters.get("signed_request");
	// if (signedRequest == null) {
	// AuthTokenService.dispachError("エラーにより印刷できません！",
	// "Canvasアプリとして起動されていません！", req, resp, null, null);
	// return;
	// }
	// WebApplicationContext wac = WebApplicationContextUtils
	// .getRequiredWebApplicationContext(req.getSession()
	// .getServletContext());
	// Settings st = (Settings) wac.getBean("Settings");
	// Map<String, String> dmmymap = new HashMap<String, String>();
	// SetterServlet.lookupMap(req, dmmymap, st.getKmap());
	// String yourConsumerSecret = dmmymap.get(Downloder.cvsecKey);
	// String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(
	// signedRequest[0], yourConsumerSecret);
	// JSONObject jo = (JSONObject) JSONValue.parse(signedRequestJson);
	// JSONObject wkjo = (JSONObject) jo.get("context");
	// wkjo = (JSONObject) wkjo.get("environment");
	// wkjo = (JSONObject) wkjo.get("parameters");
	// key = (String) wkjo.get("state");
	// // Map<String, String> pmap = null;
	// try {
	// pmap = AuthTokenService.getPmap(req, resp, key);
	// } catch (Exception e2) {
	// }
	// if (pmap == null) {
	// return;
	// }
	// JSONObject jo2 = makeOAuthJsonStr(jo);
	// pmap.put(Downloder.JSonKey, jo2.toJSONString());
	// for (Object k : wkjo.keySet()) {
	// pmap.put((String) k, wkjo.get(k) + "");
	// }
	// req.setAttribute("key", key);
	// req.setAttribute("baseUrl", AuthTokenService.getBaseUrl(req));
	// // Canvasの場合はnoUserModeで動かす
	// String authParm = pmap.get(Util.authParmKey);
	// if (authParm == null) {
	// authParm = "{}";
	// }
	// JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
	// ajo.put(Util.noUserModeKey, true);
	// ajo.put(Util.licenseUidKey, exUsrId((String) jo2.get("id")));
	// ajo.put(Util.licenseOidKey, exOrgId((String) jo2.get("id")));
	// pmap.put(Util.authParmKey, ajo.toJSONString());
	// // リダイレクト
	// // AuthTokenService.dispachJsp(req, resp, pmap, key,
	// // Downloder.dljspKey,
	// // "/WEB-INF/views/dlCanvas.jsp");
	// con = getConnection(wac);
	// new DownloadService().doGet(req, resp, con);
	// } catch (Exception e) {
	// if (pmap == null) {
	// pmap = new HashMap<String, String>();
	// }
	// pmap.put(Util.batchErrorMsgKey, e.toString());
	// } finally {
	// if (con != null) {
	// try {
	// con.close();
	// } catch (SQLException e) {
	// }
	// }
	// }
	// // TODO JSPを呼ぶ
	// String ermsg = pmap.get(Util.batchErrorMsgKey);
	// String jsonstr;
	// if (ermsg != null) {
	// jsonstr = Util.makeRes(ermsg, pmap);
	// } else {
	// jsonstr = Util.makeRes("ok", pmap);
	// }
	// req.setAttribute("jsonres", jsonstr);
	// AuthTokenService.dispachJsp(req, resp, pmap, key, Downloder.dljspKey,
	// "/WEB-INF/views/dlCanvasRes.jsp");
	// }

	/**
	 * signedRequestJsonから、OAuthのJsonString互換のものを作成する
	 *
	 * @param s
	 * @return
	 */
	private JSONObject makeOAuthJsonStr(JSONObject jo) {
		JSONObject resobj = new JSONObject();
		JSONObject wkjo = (JSONObject) jo.get("client");
		resobj.put("access_token", wkjo.get("oauthToken"));
		resobj.put("instance_url", wkjo.get("instanceUrl"));
		wkjo = (JSONObject) jo.get("context");
		JSONObject wkjo2 = (JSONObject) wkjo.get("links");
		String lurl = (String) wkjo2.get("loginUrl");
		wkjo2 = (JSONObject) wkjo.get("organization");
		String oid = (String) wkjo2.get("organizationId");
		wkjo2 = (JSONObject) wkjo.get("user");
		String uid = (String) wkjo2.get("userId");
		resobj.put("id", lurl + "/id/" + oid + "/" + uid);
		return resobj;
	}

	public static void printHeader(HttpServletRequest req) {
		Enumeration<String> en = req.getHeaderNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			System.out.println(key + ":" + req.getHeader(key));
			// ここのX-Forwarded-ForはsalesforceのIPなので意味が無い
			// if (req.getParameter(Downloder.ipKey) == null
			// && "X-Forwarded-For".equals(key)) {
			// pmap.put(Downloder.ipKey, req.getHeader(key));
			// }
		}
	}

	public static class RCException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public RCException(String message) {
			super(message);
		}
	}

}

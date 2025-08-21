package jp.co.kpscorp.rc6.cont;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.component.MapListFactory;
import jp.co.kpscorp.rc6.component.MapService;
import jp.co.kpscorp.rc6.component.PdfExportReporter;
import jp.co.kpscorp.rc6.component.PrintService;
import jp.co.kpscorp.rc6.component.PrintServiceException;
import jp.co.kpscorp.rc6.component.PrintServiceMapSuper;
import jp.co.kpscorp.rc6.component.PrintSource;
import jp.co.kpscorp.rc6.component.RCException;
import jp.co.kpscorp.rc6.component.Settings;
import jp.co.kpscorp.rc6.component.ThreadMap;
import jp.co.kpscorp.rc6.component.UploadChecker;
import jp.co.kpscorp.rc6.component.Utils;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Usertbl;
import jp.co.kpscorp.rc6.repo.HintRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRuntimeException;

@Controller
@Scope("prototype")
public class DownloadController {

	// 処理実行中のBatch
	public static Map<String, DownloadController> runningBatchs = new HashMap<String, DownloadController>();

	private final PrintSource<Map<String, ?>> printSource;

	private int waitCnt = 0;

	private Map<String, String> pmap = null;
	// @Autowired
	// private SfdcConnectImpl sfdcConnectImpl;
	// @Autowired
	// private SfdcRepositoryService sfdcRepositoryService;
	@Autowired
	private MapService mapServiceRest;
	@Autowired
	private HintRepository rHint;

	DownloadController(PrintSource<Map<String, ?>> printSource) {
		this.printSource = printSource;
	}

	public void doGetSync(HttpServletRequest req, HttpServletResponse resp,
			String orgid, Map<String, String> pmap) throws ServletException {
		synchronized (this) {
			System.out.println("Org id:" + orgid + " batch download Start! by "
					+ this);
			// Batch Option Check
			if (!checkOrg(orgid, pmap, req.getSession().getServletContext(),
					Utils.getApplicationContext()) && Utils.isNouserMode(pmap)) {
				return;
			}
			if (!Utils.isNouserMode(pmap)) {
				// dl.jspへ遷移しDownloadへ
				return;
			}
			dl2(req, resp);
			System.out.println("Org id:" + orgid + " batch download End! by "
					+ this);
		}
	}

	private boolean checkOrg(String orgid, Map<String, String> pmap,
			ServletContext context, ApplicationContext con)
			throws ServletException {
		try {
			// 実行ユーザーとバッチユーザーの組織が一致しているか？
			String authParm = pmap.get(Utils.authParmKey);
			if (authParm != null) {
				JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
				String loid = (String) ajo.get(Utils.licenseOidKey);
				if (!orgid.substring(0, 15).equals(loid.substring(0, 15))) {
					pmap.put(Utils.batchErrorMsgKey,
							"実行組織と固定化ユーザーの組織が一致しません！");
					return false;
				}
			}
			List<Map<String, String>> mps = Utils.getOrgsById(con,
					orgid.substring(0, 15), false);
			for (Map<String, String> mp : mps) {
				if (Utils.batchLicenseKey.equals(mp.get("licensename"))) {
					return true;
				}
			}
			// QueryResult qr = null;
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
		} catch (Exception e) {
			// throw new ServletException(e);
			// DB使用不可の場合 true
			return true;
		}
		pmap.put(Utils.batchErrorMsgKey,
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
	public String dl2(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		ApplicationContext con = Utils.getApplicationContext();
		String key = req.getParameter("state");
		if (key == null) {
			key = (String) req.getAttribute("key");
		}
		// Map<String, String> pmap = null;
		pmap = Utils.getPmap(req, key);
		if (pmap == null) {
			return "views/dlError";
		}
		ServletContext context = req.getSession().getServletContext();
		String s = pmap.get(Utils.JSonKey);
		System.out.println("JSonKey:" + s);
		if (s == null) {
			Utils.cleanUp(key, context);
			throw new ServletException("no JSON Key!");
		}
		printHeader(req);
		if (pmap.get(Utils.ipKey) == null
				&& !Utils.isNouserMode(pmap)) {
			if (req.getHeader("X-Forwarded-For") != null) {
				pmap.put(Utils.ipKey, req.getHeader("X-Forwarded-For"));
			} else {
				pmap.put(Utils.ipKey, req.getRemoteAddr());
			}
		}
		String ip = pmap.get(Utils.ipKey);
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
				if (pmap.get(Utils.authOlnyKey) != null) {
					// 認証
					Utils.putLog(ip, con, louid[0], louid[1], 6, up, s, pmap);
					// lastJsonはDBからアプリケーションスコープへ変更
					String svkey = Utils.lastJsonKey + "@" + up.getUserid();
					System.out.println("Set AppScope key:" + svkey + " val" + s);
					req.getSession().getServletContext().setAttribute(svkey, s);
					req.setAttribute("baseUrl",
							Utils.getBaseUrl(req));
					String go = Utils
							.dispachJsp(req, pmap, key,
									Utils.dlnodatajspKey,
									"views/auth");
					Utils.cleanUp(key, context); // 認証後はクリーンアップ
					return go;
				}
				// ユーザー並行処理を制御
				if (!Utils.checkPara(rnnningid)) {
					String go = Utils.dispachError("処理できません", "同一ユーザーが印刷中です",
							req, pmap, key);
					Utils.putLogSync(ip, con, louid[0], louid[1], 1, up, s, pmap);
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
				String fname = pmap.get(UploadChecker.FnKey);
				if (fname != null) {
					ps.setFileName(fname);
				} else {
					// fname無い場合はPrintSourceのdefaultを使用
					fname = printSource.getFileName();
					pmap.put(UploadChecker.FnKey, fname);
				}
				ps.setRequest(req);
				ps.setRespons(resp);
				// User情報をThreadlocalへ登録
				ThreadMap.get().put(Settings.TH_USRPROP, up);
				// データ確認モードCheck
				if ("true".equals(pmap.get(Utils.DataCheckKey))) {
					String ql = pmap.get(Utils.QlKey);
					if (ql != null && ps instanceof PrintServiceMapSuper) {
						PrintServiceMapSuper psms = (PrintServiceMapSuper) ps;
						psms.setQl(Utils.makeQl(ql, pmap));
					}
					goDataCheck(pmap, req, ps, con);
					Utils.putLog(ip, con, louid[0], louid[1], 5, up, s, pmap);
					Utils.cleanUp(key, context); // データ確認モードreturnするのでここでクリーンアップ
					return "views/dcheck";
				}
				// ps.setConnection(connection);
				// 外字ファイルをThreadlocalへ登録
				String eudcfp = pmap.get(PdfExportReporter.TH_EUDCFILE);
				if (eudcfp != null) {
					ThreadMap.get().put(PdfExportReporter.TH_EUDCFILE,
							new File(eudcfp));
				}
				String[] jfns = {};
				if (pmap.get(Utils.jrxmlFnKey) != null
						&& !pmap.get(Utils.jrxmlFnKey).trim().equals("")) {
					jfns = pmap.get(Utils.jrxmlFnKey).split(",");
				} else {
					throw new PrintServiceException(
							PrintServiceMapSuper.nojrexlMsg);
				}
				long totMapsize = 0;
				ThreadMap.get().put(Settings.TH_PAGECNT, new Integer(0));
				List<ByteArrayOutputStream> byteOuts = new ArrayList<ByteArrayOutputStream>();
				// PrintSource<Map<String, ?>> source = null;
				for (String jfn : jfns) {
					Utils.resetPmap(pmap, jfn);
					String ql = pmap.get(Utils.QlKey);
					if (ql != null && ps instanceof PrintServiceMapSuper) {
						PrintServiceMapSuper psms = (PrintServiceMapSuper) ps;
						psms.setQl(Utils.makeQl(ql, pmap));
					}
					// source = new PrintSource<Map<String, ?>>(pmap);
					ps.setXmlPath("/jasper/" + key + "/" + jfn);
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					printSource.setMapsize(totMapsize);
					byteOut = ps.documentEdit(byteOut, printSource);
					if (totMapsize < printSource.getMapsize()) {
						// dataない場合は足さない
						totMapsize = printSource.getMapsize();
						byteOuts.add(byteOut);
					}
				}
				if (totMapsize == 0) {
					// 0件画面へ
					throw new RCException(Utils.dlnodatajspKey);
				}
				ByteArrayOutputStream byteOut = Utils.margePdf(byteOuts);
				if (byteOut != null) {
					downloadOrSavePDF(req, resp, key, pmap, jo, ps,
							fname, printSource, byteOut);
				}
				Utils.putLog(ip, con, louid[0], louid[1], 0, up, s, pmap);
			} finally {
				synchronized (Utils.runningUsers) {
					Utils.runningUsers.remove(rnnningid);
				}
			}
		} catch (Exception e) {

			if (e instanceof JRRuntimeException
					&& e.getCause() instanceof JRException) {
				e = (Exception) e.getCause();
			}
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(Utils.dlnodatajspKey)) {
				String go;
				try {
					if (Utils.isNouserMode(pmap)) {
						String msg = "印刷対象データがありません！";
						go = Utils.dispachError("印刷データ無し", msg, req,
								pmap, key);
					} else {
						req.setAttribute("baseUrl",
								Utils.getBaseUrl(req));
						go = Utils.dispachJsp(req, pmap, key,
								Utils.dlnodatajspKey,
								"views/dlNodata");
						if (go != null && !go.startsWith("redirect:")) {
							Utils.cleanUp(key, context); // dlNodatajspを使用する都合上 dispatcErrorを使わないためここでクリーンアップ
						}
					}
				} catch (IOException e1) {
					throw new ServletException(e);
				}
				return go;
			}
			if (e instanceof JRException && e.getMessage() != null
					&& e.getMessage().contains(PrintServiceMapSuper.pageEx)) {
				String go;
				String msg = "印刷ページ数が制限を超えています。制限値："
						+ e.getMessage().replaceFirst(PrintServiceMapSuper.pageEx, "")
						+ "<br/>制限値については、<a href='http://www.reportsconnect.com/rc4.html'>こちら</a>を参照してください。";
				try {
					Utils.putLog(ip, con, louid[0], louid[1], 3, up, s, pmap);
					go = Utils.dispachError("制限オーバー", msg, req,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(PrintServiceMapSuper.memEx)) {
				String msg = "読み込みデータ量が制限を超えています。制限値："
						+ e.getMessage().replaceFirst(PrintServiceMapSuper.memEx, "")
						+ "<br/>制限値については、<a href='http://www.reportsconnect.com/rc4.html'>こちら</a>を参照してください。";
				String go;
				try {
					Utils.putLog(ip, con, louid[0], louid[1], 2, up, s, pmap);
					go = Utils.dispachError("制限オーバー", msg, req,
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
					Utils.putLog(ip, con, louid[0], louid[1], 4, up, s, pmap);
					go = Utils.dispachError("設定エラー", msg, req,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			// SFからエラーのケース
			// if (e instanceof RCException && e.getMessage() != null
			if (e.getMessage() != null // 2025/06/16 exceptionなら拾う
					&& !"true".equals(pmap.get(Utils.DataCheckKey))) {
				e.printStackTrace();
				String msg = e.getMessage();
				String go;
				try {
					if (rnnningid != null) {
						Utils.putLog(ip, con, Utils.exOrgId(rnnningid),
								Utils.exUsrId(rnnningid), 2, up, s, pmap);
					}
					go = Utils.dispachError("エラーにより印刷できません！", msg, req,
							pmap, key);
				} catch (Exception e1) {
					throw new ServletException(e);
				}
				return go;
			}
			try {
				Utils.putLog(ip, con, louid[0], louid[1], 4, up, s, pmap);
			} catch (Exception e1) {
				throw new ServletException(e);
			}
			throw new ServletException(e);
		} finally {
			// dispacheErrorのケースはまだcleaupしない
			// AuthTokenService.cleanUp(key, context);
		}
		// 正常終了持 cleanUpする
		Utils.cleanUp(key, context);
		return null;
	}

	private void downloadOrSavePDF(HttpServletRequest req, HttpServletResponse resp,
			String key, Map<String, String> pmap,
			JSONObject jo,
			PrintService ps, String fname,
			PrintSource<Map<String, ?>> source, ByteArrayOutputStream byteOut)
			throws UnsupportedEncodingException,
			IOException, ServletException, KeyManagementException, NoSuchAlgorithmException, RCException {
		String pid = pmap.get(Utils.parentidKey);
		if (pid != null) {
			// Attachementをinsertする
			String rurl = pmap.get(Utils.returnUrlKey);
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
				if (Utils.isNouserMode(pmap)) {
					pmap.put(Utils.attachIdKey, (String) res.get("id"));
				} else {
					resp.sendRedirect(rurl);
				}
			} else {
				// AuthTokenService.dispachError("PDFファイルの保存に失敗しました!",
				// res.get("Errors") + "", req, pmap, key);
				throw new RCException(
						"PDFファイルの保存に失敗しました!" + (res.get("Errors") == null ? "" : "/" + res.get("Errors")));
			}
		} else if (Utils.isNouserMode(pmap)) {
			String authParm = pmap.get(Utils.authParmKey);
			JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
			String luid = (String) ajo.get(Utils.licenseUidKey);
			String desc = (String) ajo.get(Utils.feedDescKey);
			JSONObject res = insertFeedData(fname + ".pdf",
					byteOut.toByteArray(), luid, desc, pmap);
			if ((Boolean) res.get("success")) {
				pmap.put(Utils.attachIdKey, (String) res.get("id"));
			} else {
				Utils.dispachError("PDFファイルの保存に失敗しました!",
						res.get("Errors").toString(), req, pmap, key);
			}
		} else {
			// pdfをdownloadする
			ps.prepareResponse(byteOut, source);
		}
	}

	private void goDataCheck(Map<String, String> pmap,
			HttpServletRequest req,
			PrintService ps,
			ApplicationContext con) throws Exception {
		try {
			// 隠すもの
			String[] hidestrs = {
					// EudcFontRegistry.TH_EUDCFILE,
					Utils.JSonKey, Utils.cidKey, Utils.csecKey,
					Utils.cvsecKey, Utils.ipKey,
					Utils.authParmKey, Utils.batchErrorMsgKey };
			String pmapStr = Utils.makePmapString(pmap, hidestrs);
			String dataStr = "";
			req.setAttribute("pmapStr", Utils.formatJson(pmapStr));
			Utils.Csvwrap csvw = new Utils.Csvwrap();
			String ql = Utils.makeQl(pmap.get(Utils.QlKey), pmap);
			req.setAttribute("ql", ql);
			if (ql != null) {
				ql = ql.replaceFirst(" limit .*", "") + " limit 10";
				List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
				// sfdcConnectImplから読む <-やめ
				// List<Map<String, ?>> maps = sfdcConnectImpl.doQuery(ql);
				// // List<Map<String, ?>> maps = sfdcRepositoryService.doSoql(ql);
				// maps = sfdcRepositoryService.makeVoList(maps, CaseFreeMap.class);
				dataStr = JSONValue.toJSONString(maps);
				csvw = Utils.makeCSV(maps, null);
				// req.setAttribute("xml", makeXml(maps));
			}
			req.setAttribute("dataStr", Utils.formatJson(dataStr));
			req.setAttribute("cw", csvw);
			// req.setAttribute("csvs", makeCsvBox(csvw));

			// 実際に出力してみる
			if (pmap.get(Utils.jrxmlFnKey) != null) {
				String key = req.getParameter("state");
				ps.setXmlPath("/jasper/" + key + "/"
						+ pmap.get(Utils.jrxmlFnKey));
			} else {
				req.setAttribute("hint", Utils.getHint(con, "please download", rHint));
				return;
			}
			// ps.setConnection(connection);
			// PrintSource<Map<String, ?>> source = new PrintSource<Map<String, ?>>(
			// pmap);
			// 外字ファイルをThreadlocalへ登録
			String eudcfp = pmap.get(PdfExportReporter.TH_EUDCFILE);
			if (eudcfp != null) {
				ThreadMap.get().put(PdfExportReporter.TH_EUDCFILE,
						new File(eudcfp));
			}
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ps.documentEdit(byteOut, printSource);
		} catch (Throwable e) {
			if (e instanceof RCException && e.getMessage() != null
					&& e.getMessage().contains(Utils.dlnodatajspKey)) {
				req.setAttribute("hint", Utils.getHint(con, e.getMessage(), rHint));
				return;
			}
			if (!(e instanceof Exception) && e.getCause() instanceof Exception) {
				e = e.getCause();
			}
			Utils.Exwrap ew = new Utils.Exwrap(e);
			req.setAttribute("ex", ew);

			req.setAttribute("hint", Utils.getHint(con, ew.getMsgOrStr(), rHint));
		} finally {
			req.setAttribute("baseUrl", Utils.getBaseUrl(req));
			// リダイレクト
			// RequestDispatcher dispatcher = req.getSession().getServletContext()
			// .getRequestDispatcher("/WEB-INF/views/dcheck.jsp");
			// dispatcher.forward(req, resp);

		}
	}

	private Settings.Userprop checkUsr(Map<String, String> pmap, JSONObject jo,
			ApplicationContext con) {
		String oid;
		String uid;
		String[] ouid = getLicenseOuid(pmap, jo);
		oid = ouid[0];
		uid = ouid[1];
		Map<String, License> lmap = Utils.getLmap(con);
		try {
			String[] cks = Utils.noneOrgCheckKey.split(",");
			if (pmap.get(cks[0]) != null && pmap.get(cks[0]).equals(cks[1])) {
				// check 迂回
				System.out.println("Didn't Org check!");
				return new Settings.Userprop(oid, uid, lmap.get("pro"),
						"Org for Test");
			}
			Usertbl usr = Utils.getUser(con, uid);
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
		String authParm = pmap.get(Utils.authParmKey);
		if (authParm != null) {
			JSONObject ajo = (JSONObject) JSONValue.parse(authParm);
			ouid[0] = (String) ajo.get(Utils.licenseOidKey);
			ouid[1] = (String) ajo.get(Utils.licenseUidKey);
		} else {
			ouid[0] = Utils.exOrgId((String) jo.get("id"));
			ouid[1] = Utils.exUsrId((String) jo.get("id"));
		}
		return ouid;
	}

	private JSONObject insertPdf(String name, byte[] bs, String pid,
			Map<String, String> pmap)
			throws KeyManagementException, NoSuchAlgorithmException, IOException, RCException {
		Map<String, String> data = new HashMap<String, String>();
		data.put("name", name);
		data.put("body", new String(new Base64().encode(bs)));
		data.put("parentId", pid);
		return insertSobj(pmap, "Attachment", data);
	}

	private JSONObject insertFeedData(String name, byte[] bs, String pid,
			String desc, Map<String, String> pmap)
			throws KeyManagementException, NoSuchAlgorithmException, IOException, RCException {
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
		String s = pmap.get(Utils.JSonKey);
		JSONObject jo = (JSONObject) JSONValue.parse(s);
		String tk = (String) jo.get("access_token");
		String surl = (String) jo.get("instance_url");
		String apiv = pmap.get(Utils.apivKey);
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
		JSONObject rjs = Utils.responseTojson(response);
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
	// String yourConsumerSecret = dmmymap.get(Utils.cvsecKey);
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
	// pmap.put(Utils.JSonKey, jo2.toJSONString());
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
	// // Utils.dljspKey,
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
	// AuthTokenService.dispachJsp(req, resp, pmap, key, Utils.dljspKey,
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
			// if (req.getParameter(Utils.ipKey) == null
			// && "X-Forwarded-For".equals(key)) {
			// pmap.put(Utils.ipKey, req.getHeader(key));
			// }
		}
	}

}

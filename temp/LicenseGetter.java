package jp.co.kpscorp.rc6.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.method.annotation.ModelFactory;

import com.sforce.ws.ConnectionException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.model.Usertbl;

public class LicenseGetter extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String noAuthMsg = "Auth required!";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// header check
		Enumeration<String> he = req.getHeaderNames();
		StringBuilder sb = new StringBuilder();
		while (he.hasMoreElements()) {
			String key = (String) he.nextElement();
			sb.append(key).append("=").append(req.getHeader(key)).append("\n");
		}
		System.out.println(sb);
		//
		String uid = req.getParameter(Downloder.useridKey);
		if (uid == null || uid.length() < 15) {
			// SetterServlet.write("id error!", resp);
			return;
		}
		// WebApplicationContext wac = WebApplicationContextUtils
		// .getRequiredWebApplicationContext(req.getSession()
		// .getServletContext());
		Connection con = null;
		try {
			// con = ModelFactory.getConnection(wac);
			// accesslog 読まないに変更
			// uid = uid.substring(0, 15);
			// String sql = "select * from accesslog where userid like ?";
			// PreparedStatement pstmts = con.prepareStatement(sql);
			// pstmts.setString(1, uid + "%");
			// ResultSet result = pstmts.executeQuery();
			// List<Accesslog> acs = ModelFactory.getByObjs(result,
			// Accesslog.class);
			// if (!acs.isEmpty()) {
			// Accesslog ac = acs.get(0);
			// String oid = userCheck(req, ac);
			String oid = userCheck(req, uid);
			if (oid == null) {
				// SetterServlet.write(noAuthMsg, resp);
				return;
			}
			String utblstr = req.getParameter(Downloder.usertblsKey);
			if (utblstr == null) {
				getLicense(resp, con, oid);
			} else {
				commitLicense(resp, con, oid, utblstr);
			}
			// } else {
			// SetterServlet.write(noAuthMsg, resp);
			// }

		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}

	}

	// doPost追加 2016/2/1
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	private void commitLicense(HttpServletResponse resp, Connection con,
			String oid, String utblstr) throws SQLException, IOException {
		try {
			con.setAutoCommit(false);
			String sql;
			PreparedStatement pstmts;
			JSONArray ja = (JSONArray) JSONValue.parse(utblstr);
			List<Usertbl> l = new ArrayList<Usertbl>();
			for (int i = 0; i < ja.size(); i++) {
				JSONObject jo = (JSONObject) ja.get(i);
				Usertbl us = new Usertbl((String) jo.get("userid"));
				us.setLicensename((String) jo.get("licensename"));
				us.setOrgid((String) jo.get("orgid"));
				l.add(us);
			}
			oid = oid.substring(0, 15);
			// ライセンスの整合性チェック
			List<Map<String, String>> orgs = AuthTokenService.getOrgsById(con, oid, true);
			if (!checkLicense(l, orgs)) {
				throw new Exception("License check NG!");
			}
			sql = "delete from usertbl where  orgid like ?";
			pstmts = con.prepareStatement(sql);
			pstmts.setString(1, oid + "%");
			pstmts.executeUpdate();
			pstmts.close();
			for (Usertbl us : l) {
				ModelFactory.insert(con, us);
			}
			// SetterServlet.write("ok", resp);
			System.out.println("Licens Getter commit:" + utblstr);
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			con.rollback();
			// SetterServlet.write("ng", resp);
		}

	}

	private void getLicense(HttpServletResponse resp, Connection con, String oid)
			throws SQLException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		oid = oid.substring(0, 15);
		String sql;
		PreparedStatement pstmts;
		List<Map<String, String>> ogs = AuthTokenService.getOrgsById(con, oid, true);
		sql = "select * from usertbl where orgid like ? ";
		pstmts = con.prepareStatement(sql);
		oid = oid.substring(0, 15);
		pstmts.setString(1, oid + "%");
		Class<Usertbl> clazz2 = Usertbl.class;
		List<Map<String, String>> uss = AuthTokenService.makeMaps(pstmts, clazz2);
		Map<String, List<Map<String, String>>> res = new HashMap<String, List<Map<String, String>>>();
		res.put("organizations", ogs);
		res.put("usertbls", uss);
		String jstr = JSONObject.toJSONString(res);
		System.out.println("Licens Getter res:" + jstr);
		SetterServlet.write(jstr, resp);
	}

	private boolean checkLicense(List<Usertbl> l,
			List<Map<String, String>> orgs) {
		Map<String, Integer> ump = new HashMap<String, Integer>();
		Map<String, Integer> omp = new HashMap<String, Integer>();
		for (Usertbl us : l) {
			Integer n = ump.get(us.getLicensename());
			if (n == null) {
				n = new Integer(1);
			} else {
				n++;
			}
			ump.put(us.getLicensename(), n);
		}
		for (Map<String, String> org : orgs) {
			Integer n = omp.get(org.get("licensename"));
			if (n == null) {
				n = new Integer(org.get("numberof"));
			} else {
				n += new Integer(org.get("numberof"));
			}
			omp.put(org.get("licensename"), n);
		}
		for (String lc : ump.keySet()) {
			Integer orgn = omp.get(lc);
			Integer usn = ump.get(lc);
			if (orgn == null || orgn < usn) {
				return false;
			}
		}
		return true;
	}

	// private String userCheck(HttpServletRequest req, Accesslog ac)
	// 引数をuidに変更 2016/1/26
	private String userCheck(HttpServletRequest req, String uid)
			throws ServletException, ConnectionException {
		// lastJsonはDBからアプリケーションスコープへ変更
		String svkey = Downloder.lastJsonKey + "@" + uid;
		String js = (String) req.getSession().getServletContext()
				.getAttribute(svkey);
		System.out.println("Get AppScope key:" + svkey + " val" + js);

		// if (js == null || ac.getIp() == null) {
		if (js == null) {
			return null;
		}
		String url = req.getRequestURL().toString();
		if (url.indexOf("localhost") == -1) {
			String agent = req.getHeader("User-Agent");
			if (agent == null || !agent.contains("SFDC-Callout")) {
				return null;
			}
			// String ra = req.getHeader("Kps_xip");
			// System.out.println("IP Check:" + ra + " vs " + ac.getIp());
			// if (ra == null || !ra.equals(ac.getIp())) {
			// return null;
			// }
		}
		JSONObject jo = (JSONObject) JSONValue.parse(js);
		// String tk = (String) jo.get("access_token");
		String oid = DownloadService.exOrgId((String) jo.get("id"));
		Map<String, String> pmap = new HashMap<String, String>();
		pmap.put(Downloder.JSonKey, js);
		System.out.println("into rest query:");
		// ConnectorConfig config = new ConnectorConfig();
		// config.setSessionId(tk);
		// config.setServiceEndpoint(jo.get("instance_url")
		// + "/services/Soap/u/22.0");
		// PartnerConnection pcon = Connector.newConnection(config);
		// con.queryAllを使用しない
		String ql = "select id from Organization where id ='" + oid + "'";
		List<Map<String, ?>> maps = null;
		try {
			maps = ModelFactory.getMapService(pmap,
					new PrintSource<Map<String, ?>>()).queryToMap(
							null, ql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (maps == null || maps.size() > 0) {
			return oid;
		}

		// pcon.setSessionHeader(tk);
		// QueryResult qr = pcon.query("select id from Organization where id ='"
		// + oid + "'");
		// if (qr.getRecords().length > 0) {
		// return oid;
		// }
		System.out.println("Licens Getter User Check NG:" + uid);
		return null;
	}

}

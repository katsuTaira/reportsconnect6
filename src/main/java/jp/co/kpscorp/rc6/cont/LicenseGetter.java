package jp.co.kpscorp.rc6.cont;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jp.co.kpscorp.rc6.component.MapService;
import jp.co.kpscorp.rc6.component.Utils;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Organization;
import jp.co.kpscorp.rc6.model.RcPmap;
import jp.co.kpscorp.rc6.model.Usertbl;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.OrganizationRepository;
import jp.co.kpscorp.rc6.repo.UsertblRepository;

@Controller
@Scope("prototype")
public class LicenseGetter {

	/**
	 *
	 */
	// private static final long serialVersionUID = 1L;

	public static final String noAuthMsg = "Auth required!";
	@Autowired
	private ApplicationContext con;
	@Autowired
	private UsertblRepository rUsertbl;
	@Autowired
	private LicenseRepository rLicense;
	@Autowired
	private OrganizationRepository rOrganization;
	@Autowired
	private MapService mapServiceRest;
	@Autowired
	private RcPmap rcPmap;

	@RequestMapping("/lg")
	@ResponseBody
	protected String doLg(HttpServletRequest req)
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
		String uid = req.getParameter(Utils.useridKey);
		if (uid == null || uid.length() < 15) {
			return "id error!";
		}
		// Connection con = null;
		String res = null;
		try {
			// con = ModelFactory.getConnection(wac);
			String oid = userCheck(req, uid);
			if (oid == null) {
				return noAuthMsg;
			}
			String utblstr = req.getParameter(Utils.usertblsKey);
			if (utblstr == null) {
				res = getLicense(oid);
			} else {
				res = commitLicense(oid, utblstr);
			}
			// } else {
			// write(noAuthMsg, resp);
			// }

		} catch (Exception e) {
			throw new ServletException(e);
		}
		return res;
	}

	// doPost追加 2016/2/1
	// @Override
	// protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	// throws ServletException, IOException {
	// doGet(req, resp);
	// }

	private String commitLicense(String oid, String utblstr) throws Exception {
		// con.setAutoCommit(false);
		// String sql;
		// PreparedStatement pstmts;
		JSONArray ja = (JSONArray) JSONValue.parse(utblstr);
		List<Usertbl> l = new ArrayList<Usertbl>();
		for (int i = 0; i < ja.size(); i++) {
			JSONObject jo = (JSONObject) ja.get(i);
			Usertbl us = new Usertbl();
			us.setUserid((String) jo.get("userid"));
			// us.setLicensename((String) jo.get("licensename"));
			List<License> lics = rLicense.findByLicensename((String) jo.get("licensename"));
			if (lics == null || lics.size() == 0) {
				throw new Exception("License not found:" + jo.get("licensename"));
			}
			us.setLicenseBean(lics.get(0));
			// us.setOrgid((String) jo.get("orgid"));
			// List<Organization> orgs = rOrganization.findByOrgidLike((String)
			// jo.get("orgid") + "%");
			List<Organization> orgs = Utils.getOrgBeans(con, (String) jo.get("orgid"), true);
			Organization org = null;
			for (Organization o : orgs) {
				if (o.getLicenseBean().getId().equals(us.getLicenseBean().getId())) {
					org = o;
					break;
				}
			}
			if (org == null) {
				throw new Exception("Organization not found:" + jo.get("orgid"));
			}
			us.setOrganizationBean(org);
			l.add(us);
		}
		oid = oid.substring(0, 15);
		// ライセンスの整合性チェック
		List<Map<String, String>> orgs = Utils.getOrgsById(con, oid, true);
		if (!checkLicense(l, orgs)) {
			throw new Exception("License check NG!");
		}
		// sql = "delete from usertbl where orgid like ?";
		// pstmts = con.prepareStatement(sql);
		// pstmts.setString(1, oid + "%");
		// pstmts.executeUpdate();
		// pstmts.close();
		rUsertbl.deleteByOrganizationBeanOrgidLike(oid + "%");
		// for (Usertbl us : l) {
		// ModelFactory.insert(con, us);
		// }
		rUsertbl.saveAll(l);
		System.out.println("Licens Getter commit:" + utblstr);
		// con.commit();
		return "ok";
	}

	private String getLicense(String oid)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		oid = oid.substring(0, 15);
		// String sql;
		// PreparedStatement pstmts;
		List<Map<String, String>> ogs = Utils.getOrgsById(con, oid, true);
		// sql = "select * from usertbl where orgid like ? ";
		// pstmts = con.prepareStatement(sql);
		// oid = oid.substring(0, 15);
		// pstmts.setString(1, oid + "%");
		// Class<Usertbl> clazz2 = Usertbl.class;
		List<Usertbl> utbls = rUsertbl.findByOrganizationBeanOrgidLike(oid + "%");
		// UsertblのリストをList<Map<String, String>>に変換
		List<Map<String, String>> uss = Utils.makeMaps(utbls);
		Map<String, List<Map<String, String>>> res = new HashMap<String, List<Map<String, String>>>();
		res.put("organizations", ogs);
		res.put("usertbls", uss);
		String jstr = JSONObject.toJSONString(res);
		System.out.println("Licens Getter res:" + jstr);
		return jstr;
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

	// private String userCheck(HttpServletRequest req, AccesslogVo ac)
	// 引数をuidに変更 2016/1/26
	private String userCheck(HttpServletRequest req, String uid) throws Exception {
		// lastJsonはDBからアプリケーションスコープへ変更
		String svkey = Utils.lastJsonKey + "@" + uid;
		String js = (String) req.getSession().getServletContext()
				.getAttribute(svkey);
		System.out.println("Get AppScope key:" + svkey + " val" + js);

		if (js == null) {
			return null;
		}
		String url = req.getRequestURL().toString();
		if (url.indexOf("localhost") == -1) {
			String agent = req.getHeader("User-Agent");
			if (agent == null || !agent.contains("SFDC-Callout")) {
				return null;
			}
		}
		JSONObject jo = (JSONObject) JSONValue.parse(js);
		// String tk = (String) jo.get("access_token");
		String oid = Utils.exOrgId((String) jo.get("id"));
		Map<String, String> pmap = new HashMap<String, String>();
		pmap.put(Utils.JSonKey, js);
		rcPmap.setPmap(pmap);
		System.out.println("into rest query:");
		String ql = "select id from Organization where id ='" + oid + "'";

		List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
		if (maps == null || maps.size() > 0) {
			return oid;
		}

		System.out.println("Licens Getter User Check NG:" + uid);
		return null;
	}

}

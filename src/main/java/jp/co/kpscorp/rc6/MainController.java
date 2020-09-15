package jp.co.kpscorp.rc6;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.co.kpscorp.rc6.model.Accesslog;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Organization;
import jp.co.kpscorp.rc6.model.User;
import jp.co.kpscorp.rc6.repo.AccesslogRepository;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.OrganizationRepository;
import jp.co.kpscorp.rc6.repo.UserRepository;
import net.arnx.jsonic.JSON;

@Controller
@Scope("prototype")
public class MainController {

	private Logger logger = Logger.getLogger(MainController.class);

	public static final String defurl = "http://localhost:8080";
	public static final String exattKey = "exatt";
	public static final String orgidKey = "kps_orgid";
	public static final String jsonUrlKey = "kps_jsonurl";
	public static final String useridKey = "kps_userid";

	@Autowired
	private LicenseRepository lcrep;
	@Autowired
	private UserRepository usrep;
	@Autowired
	private AccesslogRepository alrep;
	@Autowired
	private OrganizationRepository ogrep;

	/**
	 * @param req
	 * @param pmap
	 * @return
	 * sfdc側のReportsConnectControllerのコンストラクタからのリクエストに応えてユーザー情報/serverUrl等を返す
	 * パラメーターにjsonUrlKeyがあればjsonでユーザー情報を返す、なければserverurlのみを返す
	 * 該当ユーザーがあれば、そのライセンスのserverurlを、なければライセンスfreeのserverurlをライセンスから読んで返す
	 * そのユーザーのアクセスログがあれば、hasaccesslogをtrueとする
	 * そのユーザーか、そのユーザーの組織にライセンスが一ついじょうあれば、haslicenseをtrueとする
	 * 組織にexattKeyがある場合は、licensenameにそれを返す
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
		List<User> us = usrep.findByUseridLike(uid + "%");
		if (!us.isEmpty()) {
			User u = us.get(0);
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

}

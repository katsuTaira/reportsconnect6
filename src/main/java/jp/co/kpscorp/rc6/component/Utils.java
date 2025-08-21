package jp.co.kpscorp.rc6.component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jp.co.kpscorp.rc6.model.Accesslog;
import jp.co.kpscorp.rc6.model.Hint;
import jp.co.kpscorp.rc6.model.License;
import jp.co.kpscorp.rc6.model.Organization;
import jp.co.kpscorp.rc6.model.RcPmap;
import jp.co.kpscorp.rc6.model.Usertbl;
import jp.co.kpscorp.rc6.repo.AccesslogRepository;
import jp.co.kpscorp.rc6.repo.HintRepository;
import jp.co.kpscorp.rc6.repo.LicenseRepository;
import jp.co.kpscorp.rc6.repo.OrganizationRepository;
import jp.co.kpscorp.rc6.repo.UsertblRepository;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;

public class Utils {
    private static Logger logger = Logger.getLogger(Utils.class);
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
    public static final String csecKey = "client_secret";
    public static final String cvsecKey = "canvas_secret";
    public static final String cidKey = "client_id";
    public static final String labelsKey = "kps_labels";
    public static final String useridKey = "kps_userid";
    public static final String dljspKey = "kps_dljsp";
    public static final String dlerrorjspKey = "kps_dlerrorjsp";
    public static final String dlnodatajspKey = "kps_dlnodatajsp";
    public static final String null2blankKey = "ksp_null2blank";
    public static final String authOlnyKey = "ksp_authOnly";
    public static final String usertblsKey = "kps_usertbls";
    public static final String ipKey = "Kps_xip";
    public static final String jsonUrlKey = "kps_jsonurl";
    public static final String lastJsonKey = "kps_lastJson";
    // attchemnt対応
    public static final String parentidKey = "kps_parentid";
    public static final String returnUrlKey = "kps_returnUrl";
    // 連結用 Report単位パラメーター
    public static final String parmByReps = "ksp_ql,ksp_null2blank";
    public static final int noerror = 0;
    public static final int idrunning = 1;
    public static final int memover = 2;
    public static final int pageover = 3;
    // API Version key
    public static final String apivKey = "kps_apiv";

    public static final String hostSafixKey = "Kps_hostSafix";
    public static final String ipfixKey = "ipfix";
    public static final String ipfixApacheKey = "ipfixApache";
    public static final String uinfoKey = "kps_uinfo";
    // public static String ipfixServer = "https://localhost:8443/loginproxy/";
    public static String ipfixServer = "https://ec2-54-201-152-47.us-west-2.compute.amazonaws.com:8443//loginproxy/";
    // internal address
    public static String ipfixServerInternal = "ip-172-31-43-89.us-west-2.compute.internal";
    public static String ipfixServerExternal = "ec2-54-201-152-47.us-west-2.compute.amazonaws.com";

    // public static String ipfixServer = "http://54.201.152.47:8443//loginproxy/";
    // DB使用不可の場合のipfixserver
    public static final String defipfixServer = "https://ec2-54-201-152-47.us-west-2.compute.amazonaws.com:8443//loginproxy/";
    // public static final String defipfixServer =
    // "http://54.201.152.47:8443//loginproxy/";
    public static final String proxpass = "sedoedo9";
    public static final String proxpassKey = "kps-op";
    public static final String noneOrgCheckKey = "nc,tk";
    public static final String jrxmlFnKey = "kps_jrxml";
    public static final String QlKey = "ksp_ql";
    public static final String DataCheckKey = "ksp_datacheck";
    public static final String JSonKey = "ksp_jsonstr";

    public static final String defurl = "http://localhost:8080";
    public static final String exattKey = "exatt";
    public static final String orgidKey = "kps_orgid";
    public static final String pkeyKey = "kps_parmkey";
    public static final String codeVerifierKey = "Kps_codeVerifier";

    // 処理実行中のユーザー
    public static List<String> runningUsers = new ArrayList<String>();

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

    public static ApplicationContext getApplicationContext() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            ServletContext sctx = sra.getRequest().getServletContext();
            return WebApplicationContextUtils.getRequiredWebApplicationContext(sctx);
        }
        return null;
    }

    /**
     * オブジェクトのgetterを動かした結果の値を保持するMapを得る
     *
     * @param o
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Map getValueMap(Object o) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Map map = getPropMethodMap(o);
        Iterator it = map.keySet().iterator();
        Map rmap = new HashMap();
        while (it.hasNext()) {
            String key = (String) it.next();
            Method mt = (Method) map.get(key);
            rmap.put(key, mt.invoke(o, null));
        }
        return rmap;
    }

    /**
     * beanのプロパティをKeyにしたgetterのMapを返す
     *
     * @param bean
     * @return
     */
    public static Map getPropMethodMap(Object bean) {
        Map map = new HashMap();
        List setterNames = null;
        if (bean != null) {
            Method[] ms = bean.getClass().getMethods();
            for (int i = 0; i < ms.length; i++) {
                String mname = ms[i].getName();
                if (mname.startsWith("get")
                        && ms[i].getParameterTypes().length == 0
                        && mname.length() > 3) {
                    String p = mname.substring(3, 4).toLowerCase()
                            + mname.substring(4);
                    map.put(p, ms[i]);
                }
            }
        }
        return map;
    }

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

    public static String dispachError(String error, String description,
            HttpServletRequest req, Map<String, String> pmap, String key) throws ServletException, IOException {
        // nouser modeの場合はpmapにメッセージを保管
        if (isNouserMode(pmap)) {
            pmap.put(batchErrorMsgKey, error + "/" + description);
            // とりあえずnullを返す
            return null;
        }
        req.setAttribute("error", error);
        req.setAttribute("error_description", description);
        req.setAttribute("baseUrl", Utils.getBaseUrl(req));
        String rurl;
        if (req.getParameter("signed_request") != null) {
            rurl = "views/dlErrorCanvas";
        } else {
            rurl = "views/dlError";
        }
        rurl = Utils.dispachJsp(req, pmap, key, dlerrorjspKey, rurl);
        System.out.println("Dispach error:" + error + "/" + description);
        // エラー時はここで、クリーンアップを行う 通常時は dl2メソッド側で行う
        // redirectの場合はcleanUpしない
        if (rurl != null && !rurl.startsWith("redirect:")) {
            cleanUp(key, req.getSession().getServletContext());
        }
        return rurl;
    }

    public static String dispachJsp(HttpServletRequest req, Map<String, String> pmap,
            String key,
            String jspkey, String defaultjsp) throws ServletException, IOException {
        String jspname = null;
        if (pmap != null) {
            jspname = pmap.get(jspkey);
        }
        // RequestDispatcher dispatcher;
        // ServletContext context = req.getSession().getServletContext();
        if (jspname != null && key != null) {
            // String path = req.getContextPath() + "/jasper/" + key + "/" + jspname;
            String path = "/jasper/" + key + "/" + jspname;
            Utils.replaceVariable(path, key, req);
            // debug環境のcontextPathを考慮
            // /custom というpathにredirect
            // String rePath = Utils.getContextPathWithinDebug(req) + "/custom?key=" + key +
            // "&jspname="
            // + jspname;
            String rePath = Utils.getContextPathWithinDebug(req) + "/custom?key=" + key + "&jspname="
                    + jspname;
            return "redirect:" + rePath;
        } else {
            // dispatcher = context.getRequestDispatcher(defaultjsp);
            return defaultjsp;
        }
        // dispatcher.forward(req, resp);
    }

    public static String replaceVariable(String path, String key,
            HttpServletRequest req) {
        ServletContext context = req.getSession().getServletContext();
        path = context.getRealPath(path);
        File file = new File(path);
        BufferedReader br = null;
        // FileWriter fw = null;
        OutputStreamWriter osw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(
                    file), "UTF-8"));
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = br.read()) != -1) {
                sb.append((char) c);
            }
            String res = URLDecoder.decode(sb.toString(), "UTF-8");
            String[] repstrs = { "error", "error_description", "baseUrl", "key" };
            req.setAttribute("key", key);
            for (String s : repstrs) {
                String val = (String) req.getAttribute(s);
                res = res.replaceAll("\\$\\{" + s + "\\}", val);
            }
            br.close();

            // fw = new FileWriter(file);
            // fw.write(res);
            FileOutputStream fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(res);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                }
            }
        }

    }

    public static Map<String, String> getPmap(HttpServletRequest req, String key) throws ServletException {
        Map<String, String> pmap = null;
        if (key != null) {
            pmap = (Map<String, String>) req.getSession().getServletContext()
                    .getAttribute(key);
            ApplicationContext ctx = getApplicationContext();
            if (ctx != null) {
                RcPmap rcPmap = ctx.getBean(RcPmap.class);
                rcPmap.setPmap(pmap);
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
                        req, null, key);
                // dispachErrorのresは使用しないが、メッセージを保管するために呼んでいる
            } catch (IOException e) {
                throw new ServletException(e);
            }
            System.out.println("pmap is null for key:" + key);
            return null;
        }
        return pmap;
    }

    public static String getBaseUrl(HttpServletRequest req)
            throws UnsupportedEncodingException {
        String rurl = Utils.makeReUrl(req);
        rurl = URLDecoder.decode(rurl, "UTF-8");
        // return rurl.substring(0, rurl.length() - 3);
        // 最後の/以降を除く
        int idx = rurl.lastIndexOf('/');
        if (idx != -1) {
            return rurl.substring(0, idx);
        }
        return rurl;
        // return rurl.substring(0, rurl.length() - 3);
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

    public static String getContextPathWithinDebug(HttpServletRequest req) {
        logger.debug("getContextPathWithinDebug cpath:" + req.getContextPath() + " servletPath:" + req.getServletPath()
                + " RequestURL:"
                + req.getRequestURL());
        String cPath = req.getContextPath();
        if (cPath.length() == 0) {
            // コンテキストパスがない場合は テスト環境と考え /rc6 を返す
            return "/rc6";
        }
        return "";
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
    public static <T> List<Map<String, String>> makeMaps(List<T> ogs)
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
        List<Organization> ogs = Utils.getOrgBeans(con, oid, forUser);
        List<Map<String, String>> res = makeMaps(ogs);
        return res;
    }

    public static List<Organization> getOrgBeans(ApplicationContext con, String oid, boolean forUser) {
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
        return ogs;
    }

    public static String getProxyURL(String path) throws MalformedURLException {
        return ipfixServer + "proxy" + path.substring("https:/".length());
    }

    public static String readPage(HttpURLConnection connection)
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
        // 証明書に書かれているCommon NameとURLのホスト名が一致していることの検証をスキップ 2025/06/30
        httpsconnection.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession sslSession) {
                return true;
            }
        });
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
            Utils.delete(f);
            // System.out.println(f.getName() + " deleted.");
        }
        ThreadMap.get().clear();
        ExtensionsEnvironment.setThreadExtensionsRegistry(null);
        logger.info("cleanUp key:" + key);
    }

    public static void delete(File f) {
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
            if (pmap.get(authOlnyKey) != null) {
                Utils.setupProxyUrl(con);
                return 1;
            }
            JSONObject ajo = Utils.getUifo(pmap);
            if (ajo == null) {
                return 0;
            }
            String orgid = (String) ajo.get(licenseOidKey);
            List<Map<String, String>> mps = getOrgsById(con,
                    orgid.substring(0, 15), false);
            for (Map<String, String> mp : mps) {
                if (Utils.ipfixKey.equals(mp.get("licensename"))) {
                    Utils.setupProxyUrl(con);
                    System.out.println("use proxy 1");
                    return 1;
                }
                if (Utils.ipfixApacheKey.equals(mp.get("licensename"))) {
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

    public static void setupProxyUrl(ApplicationContext con) {
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
        String jstr = pmap.get(Utils.uinfoKey);
        System.out.println("uinfo:" + jstr);
        if (jstr == null) {
            return null;
        }
        JSONObject ajo = (JSONObject) JSONValue.parse(jstr);
        return ajo;
    }

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

    public static Map<String, String> getServerUrl(String uid, LicenseRepository lcrep, UsertblRepository usrep,
            AccesslogRepository alrep, OrganizationRepository ogrep)
            throws SQLException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String, String> ump = new HashMap<String, String>();
        if (uid != null && uid.length() > 15) {
            uid = uid.substring(0, 15);
        }
        List<Usertbl> us = usrep.findByUseridLike(uid + "%");
        if (!us.isEmpty() && us.get(0).getLicenseBean() != null) {
            Usertbl u = us.get(0);
            ump.put("serverurl", u.getLicenseBean().getServerurl());
            ump.put("licensename", u.getLicenseBean().getLicensename());
            ump.put("haslicense", "true");
        } else {
            ump.put("serverurl", Utils.getDefaultServerUrl(lcrep));
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

    public static String getDefaultServerUrl(LicenseRepository lcrep) {
        List<License> ls = lcrep.findByLicensename("free");
        if (!ls.isEmpty()) {
            return ls.get(0).getServerurl();
        }
        return defurl;
    }

    /**
     * 2017/06/22 orgidより添付拡大オプション対応
     *
     * @param wmp
     */
    public static void checkOrg(Map<String, String> pmap, Map<String, String> wmp, OrganizationRepository ogrep) {
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

    public static void makeSubDir(String path, String fname) {
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

    public static String makeKey() {
        double d = Math.random();
        String key = d + "";
        key = key.replaceFirst("0\\.", "x");
        return key;
    }

    public static ByteArrayOutputStream margePdf(List<ByteArrayOutputStream> byteOuts)
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

    public static boolean checkPara(String uid) {
        synchronized (runningUsers) {
            if (runningUsers.contains(uid)) {
                return false;
            }
            runningUsers.add(uid);
        }
        return true;
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
    public static String getHint(ApplicationContext con, String msg, HintRepository rHint) {
        List<Hint> l = rHint.findByKindIsNullOrKind("sfdc");
        for (Hint h : l) {
            if (msg.contains(h.getMsgstring())) {
                return h.getHintmessage();
            }
        }
        return null;
    }

    public static long safeParse(String s) {
        long res = -1;
        try {
            res = Long.parseLong(s);
        } catch (NumberFormatException e) {
        }
        return res;
    }

    /**
     * beanのcollectionをソートする
     *
     * @param c
     *                     beanのcollection
     * @param sortPropList
     *                     ソートキーとなるプロパティのリスト
     * @param asc
     *                     昇順かどうか
     * @return
     */
    public static List sortBeanCollection(Collection c, List sortPropList,
            boolean asc, boolean leftPad) {
        // propertyの連結を100桁までサポート
        int keta = 100;
        Map map = new TreeMap();
        Iterator it1 = c.iterator();
        int cnt = 0;
        while (it1.hasNext()) {
            Object bean = (Object) it1.next();
            Iterator it2 = sortPropList.iterator();
            StringBuffer sortKey = new StringBuffer();
            while (it2.hasNext()) {
                String k = (String) it2.next();
                Object value = null;
                try {
                    value = Utils.getMulti(bean, k);
                } catch (InvocationTargetException e) {
                }
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < keta; i++) {
                    sb.append("0");
                }
                String svalue = sb.toString();
                if (value != null) {
                    if (leftPad) {
                        svalue = svalue + value.toString();
                        svalue = svalue.substring(svalue.length() - keta);
                    } else {
                        svalue = value.toString() + svalue;
                        svalue = svalue.substring(0, keta);
                    }
                }
                sortKey.append(svalue).append(";");
            }
            // cntを追加してkeyをユニークにする
            sortKey.append("kpsc_" + cnt);
            map.put(sortKey.toString(), bean);
            cnt++;
        }
        List l = new ArrayList(map.values());
        if (!asc) {
            Collections.reverse(l);
        }
        return l;
    }

    /**
     * オブジェクトのプロパティを得る<br>
     * 配列形式、ピリオド連結をサポートする
     *
     * @param o
     * @param prop
     * @return
     * @throws InvocationTargetException
     * @throws Exception
     */
    public static Object getMulti(Object o, String prop)
            throws InvocationTargetException {
        if (o == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(prop, ".");
        Object p = o;
        while (st.hasMoreTokens()) {
            if (p == null) {
                return getSingleProp(o, prop);
            }
            String tk = st.nextToken();
            // 配列形式か
            int ix1 = -1;
            int ix2 = -1;
            ix1 = tk.indexOf("[");
            if (ix1 > 0) {
                ix2 = tk.indexOf("]");
            }
            if (ix1 > 0 && ix2 > 0 && ix2 > ix1) {
                String lName = tk.substring(0, ix1);
                int ix;
                try {
                    ix = Integer.parseInt(tk.substring(ix1 + 1, ix2));
                } catch (NumberFormatException e) {
                    return getSingleProp(o, prop);
                }
                p = getSingleProp(p, lName);
                if (p instanceof Collection) {
                    Collection col = (Collection) p;
                    try {
                        p = col.toArray()[ix];
                    } catch (Exception e) {
                        return getSingleProp(p, prop);
                    }
                }
            } else {
                p = getSingleProp(p, tk);
            }
        }
        return p;
    }

    private static Object getSingleProp(Object p, String tk)
            throws InvocationTargetException {
        Object o = null;
        try {
            o = tryGeeter(p, tk);
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        }
        if (o == null) {
            try {
                o = tryBoolGeeter(p, tk);
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            }
        }
        if (o == null && p instanceof Map) {
            Map map = (Map) p;
            o = map.get(tk);
        }

        return o;
    }

    /**
     * 引数propからオブジェクトのgetPropを呼び出す
     *
     * @param o
     * @param prop
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object tryGeeter(Object o, String prop)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        String methodName = "get" + prop.substring(0, 1).toUpperCase()
                + prop.substring(1);
        Method m = o.getClass().getMethod(methodName, null);
        return m.invoke(o, null);
    }

    /**
     * 引数propからオブジェクトのisPropを呼び出す
     *
     * @param o
     * @param prop
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object tryBoolGeeter(Object o, String prop)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        String methodName = "is" + prop.substring(0, 1).toUpperCase()
                + prop.substring(1);
        Method m = o.getClass().getMethod(methodName, null);
        return m.invoke(o, null);
    }

    /**
     * Order by 文字列解釈用クラス
     *
     * @author katsusuke
     *
     */
    public static class OrderByRef {
        public OrderByRef(String str) throws Exception {
            // tableとorder byを読み取る
            String[] tbstrs = str.trim().split("[ ]+");
            table = tbstrs[0];
            if (tbstrs.length > 3) {
                if (!tbstrs[1].toLowerCase().equals("order")) {
                    throw new Exception(str + " は無効です！");
                }
                sortPropList = new ArrayList<String>();
                String spstr = "";
                for (int ix = 3; ix < tbstrs.length; ix++) {
                    if (ix == (tbstrs.length - 1)) {
                        // 最後の文字列
                        String lst = tbstrs[ix];
                        if (lst.trim().toLowerCase().equals("asc")) {
                            break;
                        }
                        if (lst.trim().toLowerCase().equals("desc")) {
                            asc = false;
                            break;
                        }
                    }
                    spstr += tbstrs[ix];
                }
                for (String sp : spstr.split(",")) {
                    sortPropList.add(sp.trim());
                }
            }

        }

        public String table;
        public List<String> sortPropList = null;
        public boolean asc = true;
    }

    public static void resetPmap(Map<String, String> pmap, String jfn) {
        // リポート単位のパラメーターを初期化して再設定
        int i = jfn.lastIndexOf("/");
        if (i < 1) {
            return;
        }
        String[] parmByRepstrs = parmByReps.split(",");
        String prefix = jfn.substring(0, i + 1);

        // public class LengComparator {
        // }
        for (String parmByRep : parmByRepstrs) {
            pmap.put(parmByRep, pmap.get(prefix + parmByRep));
        }
    }

    public static Utils.Csvwrap makeCSV(List<Map<String, ?>> maps, String name) {
        Utils.Csvwrap mycsv = new Utils.Csvwrap();
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
                    Utils.Csvwrap chcsv = makeCSV(
                            (List<Map<String, ?>>) map.get(tit), tit);
                    if (!dubchk.contains(tit)) {
                        // 同じ名前のCSVデータソースは作らない
                        mycsv.csvws.add(chcsv);
                        dubchk.add(tit);
                    }
                    line.append("(List)" + tit).append(",");
                } else {
                    line.append(Utils.dvqu(map.get(tit))).append(",");
                }
            }
            sb.append(line.substring(0, line.length() - 1)).append("\n");
        }
        // headerをつける
        String csv = hd.substring(0, hd.length() - 1) + "\n" + sb.toString();

        mycsv.csv = csv;
        return mycsv;
    }

    public static String formatJson(String s) {
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

    private static String addIndent(int cnt) {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < cnt; i++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    public static String makeQl(String ql, Map<String, String> pmap) {
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

    public static class LengComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            return o2.length() - o1.length();
        }
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
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }

    }

    public static class Csvwrap {
        public String name;

        public String getName() {
            return name;
        }

        public String getCsv() {
            return csv;
        }

        public List<Csvwrap> getCsvws() {
            return csvws;
        }

        public String csv;
        public List<Csvwrap> csvws = new ArrayList<Csvwrap>();
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

    public static Usertbl getUser(ApplicationContext con, String uid) throws SQLException,
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
    public static void putLog(String ip, ApplicationContext con, String oid, String uid,
            int err, Settings.Userprop up, String tk, Map<String, String> pmap) {
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
                Utils.setOther(up, ac, tk, ip, pmap);
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
                Utils.setOther(up, ac, tk, ip, pmap);
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

    public static String dvqu(Object object) {
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

    public static void setOther(Settings.Userprop up, Accesslog ac, String tk,
            String ip, Map<String, String> pmap) {
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
        if (pmap.get(authParmKey) != null) {
            // batch mode
            addCnt(jo, "batch");
        }
        if (isNouserMode(pmap)) {
            // no UI
            addCnt(jo, "noui");
        }
        JSONObject uif = getUifo(pmap);
        if (uif != null) {
            jo.put("ver", uif.get(versionKey));
        }
        String s = jo.toJSONString();
        if (s.length() > 1024) {
            s = s.substring(0, 1024);
        }
        ac.setMemo(s);
        ac.setIp(ip);
    }

    public static void addCnt(JSONObject jo, String prop) {
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

    public static synchronized void putLogSync(String ip, ApplicationContext con, String oid,
            String uid, int err, Settings.Userprop up, String tk, Map<String, String> pmap)
            throws IllegalArgumentException, SecurityException, SQLException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException,
            NoSuchMethodException {
        putLog(ip, con, oid, uid, err, up, tk, pmap);
    }

}

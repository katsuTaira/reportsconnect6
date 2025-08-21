package jp.co.kpscorp.rc6.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletContext;
import jp.co.kpscorp.rc6.service.DownloadService.RCException;

public class Util {
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

}

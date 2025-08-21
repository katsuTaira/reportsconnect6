package jp.co.kpscorp.rc6.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;

public class Downloder extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String noneOrgCheckKey = "nc,tk";

	public static final String jrxmlFnKey = "kps_jrxml";
	public static final String FnKey = "ksp_filename";
	public static final String QlKey = "ksp_ql";
	public static final String DataCheckKey = "ksp_datacheck";
	public static final String JSonKey = "ksp_jsonstr";
	public static final String csecKey = "client_secret";
	public static final String cvsecKey = "canvas_secret";
	public static final String cidKey = "client_id";
	public static final String textAreaKey = "kps_textarea";
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

	// 処理実行中のユーザー
	public static List<String> runningUsers = new ArrayList<String>();

	// 処理実行中のBatch
	public static Map<String, DownloadService> runningBatchs = new HashMap<String, DownloadService>();

}

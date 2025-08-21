package jp.co.kpscorp.rc6.sfdc;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import com.sforce.soap.partner.LoginResult;
import com.sforce.ws.ConnectionException;

import jp.co.kpscorp.rc6.sfdc.SfdcConnectImpl.ObjMeta;
import net.arnx.jsonic.JSONException;

public interface SfdcConnect {

	/**
	 * SObjectのフィールドメタ情報の入手
	 *
	 * @param res
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public ObjMeta getDesc(String obj) throws Exception;

	public List<Map<String, ?>> doQuery(String ql) throws JSONException, IOException, Exception;

	public String getServerUrl() throws ConnectionException, IOException;

	public HttpResponse getResponse(HttpRequestBase req) throws Exception;

	public Map<String, Object> doUpdate(String objectName, String id, Map<String, Object> obj)
			throws IOException, SfdcConnectException, Exception;

	public Map<String, Object> doDelete(String objectName, String id)
			throws IOException, SfdcConnectException, Exception;

	public Map<String, Object> doInsert(String objectName, Map<String, Object> obj)
			throws IOException, SfdcConnectException, Exception;

	void setPwd(String pwd);

	void setUid(String uid);

	List<Map<String, ?>> doQuery(String ql, boolean simpleResponse) throws Exception;

	Map<String, Object> getMapres(HttpRequestBase req) throws IOException, Exception, SfdcConnectException;

	Map<String, Object> doSearch(Map<String, Object> obj) throws Exception;

	Map<String, Object> getViewDesc(String obj, String vid) throws Exception;

	LoginResult getLoginResult() throws ConnectionException, IOException;

	void logout();

}
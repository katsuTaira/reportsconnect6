package jp.co.kpscorp.rc6.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jp.co.kpscorp.rc6.model.RcPmap;
import jp.co.kpscorp.rc6.service.DownloadService.RCException;
import jp.co.kpscorp.rc6.service.Settings.Userprop;

@Service
@Scope("prototype")
public class MapServiceRest {

	protected RcPmap rcPmap;

	protected long mapsize;
	protected PrintSource<Map<String, ?>> printSource;
	protected Settings.Userprop up = (Userprop) ThreadMap.get().get(
			Settings.TH_USRPROP);

	protected MapServiceRest(RcPmap rcPmap,
			PrintSource<Map<String, ?>> printSource) {
		super();
		this.rcPmap = rcPmap;
		this.printSource = printSource;
		this.mapsize = printSource.getMapsize();

	}

	public List<Map<String, ?>> queryToMap(String ql) throws Exception {
		List<Map<String, ?>> l = new ArrayList<Map<String, ?>>();
		String s = rcPmap.pmap.get(Downloder.JSonKey);
		// API Ver 設定可能に 2015/7/7
		String apiv = rcPmap.pmap.get(Downloder.apivKey);
		JSONObject jo = (JSONObject) JSONValue.parse(s);
		String tk = (String) jo.get("access_token");
		String surl = (String) jo.get("instance_url");
		// API Ver 設定可能に 2015/7/7
		// v28.0に変更 2013/09/02
		// v35.0に変更 2022/04/28
		String url;
		if (apiv != null) {
			System.out.println("API Version changed to " + apiv);
			url = surl + "/services/data/" + apiv + "/query?q="
					+ URLEncoder.encode(ql, "UTF-8");
		} else {
			url = surl + "/services/data/v35.0/query?q="
					+ URLEncoder.encode(ql, "UTF-8");
		}
		JSONObject rjs = getJsonRes(tk, url);
		JSONArray jrcs = (JSONArray) rjs.get("records");
		boolean queryMore = false;
		l.addAll(makeMaps(tk, jrcs, surl));
		do {
			// 検索結果がまだあるかどうかを検査
			if (!(Boolean) rjs.get("done")) {
				queryMore = true;
				rjs = getJsonRes(tk, surl + rjs.get("nextRecordsUrl"));
				jrcs = (JSONArray) rjs.get("records");
				l.addAll(makeMaps(tk, jrcs, surl));
			} else {
				queryMore = false;
			}
		} while (queryMore);
		return l;

	}

	private JSONObject getJsonRes(String tk, String url) throws IOException,
			ClientProtocolException, UnsupportedEncodingException, RCException,
			KeyManagementException, NoSuchAlgorithmException {
		// TLS V1.1対応2016/3/30
		SSLContext sslContext = SSLContexts.custom().useTLS().build();
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(
				sslContext, new String[] { "TLSv1.1", "TLSv1.2" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(f)
				.build();
		// HttpClient httpclient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		// set the token in the header
		get.setHeader("Authorization", "OAuth " + tk);
		HttpResponse response = httpclient.execute(get);
		JSONObject rjs = responseTojson(response);
		return rjs;
	}

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

	private List<Map<String, Object>> makeMaps(String tk, JSONArray sos,
			String surl) throws Exception {
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < sos.size(); i++) {
			Map<String, Object> o = makeMap(tk, (JSONObject) sos.get(i), null,
					surl);
			res.add(o);
		}
		// printSource.setMapsize(mapsize);
		printSource.setMapsize(printSource.getMapsize() + mapsize); //
		System.out.println("Map size:" + mapsize / 1000 + "kb");
		return res;

	}

	private Map<String, Object> makeMap(String tk, JSONObject jo,
			String parentProp, String surl) throws Exception {
		Map<String, Object> o = new LinkedHashMap<String, Object>();
		for (Object key : jo.keySet()) {
			// System.out.println(key + ":" + jo.get(key));
			// QName tp = xo.getXmlType();
			String prop = key.toString();
			if (parentProp != null) {
				prop = parentProp + "." + prop;
			}
			if (jo.get(key) instanceof JSONObject) {
				JSONObject subjo = (JSONObject) jo.get(key);
				if (subjo.get("records") instanceof JSONArray) {
					// サブクエリー部分対応
					List<Map<String, Object>> mps = new ArrayList<Map<String, Object>>();
					JSONArray ja = (JSONArray) subjo.get("records");
					for (int i = 0; i < ja.size(); i++) {
						JSONObject record = (JSONObject) ja.get(i);
						Map<String, Object> mp = makeMap(tk, record, null, surl);
						mps.add(mp);
					}
					if (!(Boolean) subjo.get("done")) {
						mps.addAll(queryMoreToMap(tk, subjo, surl));
					}
					o.put(prop, mps);
				} else {
					if (prop.equals("attributes")
							|| prop.endsWith(".attributes")) {
						// attributesは出さない
						continue;
					}
					// ピリオド連結対応分
					o.putAll(makeMap(tk, subjo, prop, surl));
				}
			} else {
				// Fieldに値をセットする
				if (prop.equals("attributes") || prop.endsWith(".attributes")) {
					// attributesは出さない
					continue;
				}
				if ((prop.equals("Id") || prop.endsWith(".Id"))
						&& jo.get(key) == null) {
					// nullのIdは出さない
					continue;
				}
				if (jo.get(key) != null) {
					String s = jo.get(key).toString();
					mapsize += s.length();
					if (up != null && mapsize > up.getLicense().getMaxmen()) {
						throw new RCException(DownloadService.memEx
								+ up.getLicense().getMaxmen() / 1000 + "kb");
					}
				}
				if (jo.get(key) == null
						&& "true".equals(rcPmap.pmap.get(Downloder.null2blankKey))
						&& !prop.endsWith("__r") && !prop.endsWith("s")) {
					// Nullを空文字に置き換え(ただし、関連(__r,s)は除く
					o.put(prop, "");
				} else {
					// CSVDataSourceとの互換性維持のため、Stringにする
					o.put(prop, jo.get(key) + "");
				}
			}
		}
		return o;
	}

	private List<Map<String, Object>> queryMoreToMap(String tk, JSONObject rjs,
			String surl) throws Exception {
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		boolean queryMore = false;
		do {
			// 検索結果がまだあるかどうかを検査
			if (!(Boolean) rjs.get("done")) {
				queryMore = true;
				rjs = getJsonRes(tk, surl + rjs.get("nextRecordsUrl"));
				JSONArray jrcs = (JSONArray) rjs.get("records");
				l.addAll(makeMaps(tk, jrcs, surl));
			} else {
				queryMore = false;
			}
		} while (queryMore);
		return l;
	}

}

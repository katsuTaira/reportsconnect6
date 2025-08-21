package jp.co.kpscorp.rc6.sfdc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SFDCのAPIバージョン一括管理。 使いそうなAPIのみ設定しているため、必要があれば適宜追加すること。
 *
 * @author Miura Jumpei
 * @since 2021/04/01
 *
 */
public class SfdcApiUrlUtil {

	private static final String ENC_CODE = "UTF-8";
	private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	/**
	 * ドメインの末尾/があれば削除
	 * 
	 * @param domain
	 * @return
	 */
	private static String formatDomain(String domain) {
		String d = domain;
		if (d.endsWith("/")) {
			d = d.substring(0, d.length() - 1);
		}
		return d;
	}

	/**
	 * 日付をSFのDatetimeフォーマットJPロケーションにして返却
	 * 
	 * @param d
	 * @return
	 */
	private static String formatDateToURLEncode(Date d) {
		Date tgt = d;
		try {
			return URLEncoder.encode(SDF_DATETIME.format(tgt).replace(" ", "T") + "+09:00", ENC_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/* SOAP系 */
	private static String SFDC_SOAP_API_VERSION = "50.0";

	/**
	 * SOAPのバージョンを設定。
	 * 
	 * @param v バージョン番号。フォーマットは 0.0
	 */
	public static void setSoapVersion(String v) {
		SFDC_SOAP_API_VERSION = v;
	}

	/**
	 * SOAP用URLの取得。
	 * 
	 * @param domain http://XXXの文字列
	 * @return SOAPアクセスURL
	 */
	public static String getSoapURL(String domain) {
		return formatDomain(domain) + "/services/Soap/u/" + SFDC_SOAP_API_VERSION;
	}

	/* REST系 */
	// https://developer.salesforce.com/docs/atlas.ja-jp.api_rest.meta/api_rest/resources_sobject_basic_info.htm
	// 参照
	private static String SFDC_REST_API_VERSION = "50.0";

	/**
	 * RESTのバージョンを設定。
	 * 
	 * @param v バージョン番号。フォーマットは 0.0
	 */
	public static void setRestVersion(String v) {
		SFDC_REST_API_VERSION = v;
	}

	/**
	 * REST用のURLベース<br>
	 * /services/data/vXX.X/
	 * 
	 * @param domain http://XXXの文字列
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_Base(String domain) {
		return formatDomain(domain) + "/services/data/v" + SFDC_REST_API_VERSION + "/";
	}

	/**
	 * REST用、オブジェクトデータ取得のベース<br>
	 * /services/data/vXX.X/sobjects/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_BasicInfomation(String domain) {
		return getRestURL_Base(domain) + "sobjects/";
	}

	/**
	 * REST用、オブジェクトデータを取得<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_BasicSObject(String domain, String objectName) {
		return getRestURL_BasicInfomation(domain) + objectName + "/";
	}

	/**
	 * REST用、オブジェクトのDescribe、つまりメタデータを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/describe/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_Describe(String domain, String objectName) {
		return getRestURL_BasicSObject(domain, objectName) + "describe/";
	}

	/**
	 * オブジェクトの検索結果レイアウト情報取得<br>
	 * /services/data/vXX.X/search/layout/?q=カンマで区切られたオブジェクトのリスト
	 * 
	 * @param domain
	 * @param objectNames
	 * @return
	 */
	public static String getRestURL_SearchResultLayouts(String domain, String objectNames) {
		return getRestURL_Base(domain) + "search/layout/?q=" + objectNames;
	}

	/**
	 * REST用、オブジェクトのLayout（複数）を取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/describe/approvalLayouts/
	 * 
	 * @param domain
	 * @param objectName
	 * @return
	 */
	public static String getRestURL_ApprovalLayouts(String domain, String objectName) {
		return getRestURL_BasicSObject(domain, objectName) + "describe/approvalLayouts/";
	}

	/**
	 * REST用、オブジェクトのLayoutのRecordTypeId等（複数）を取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/describe/Layouts/
	 * 
	 * @param domain
	 * @param objectName
	 * @return
	 */
	public static String getRestURL_Layouts(String domain, String objectName) {
		return getRestURL_BasicSObject(domain, objectName) + "describe/layouts/";
	}

	/**
	 * REST用、オブジェクトのレイアウトを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/describe/Layouts/{recordTypeId}
	 * 
	 * @param domain
	 * @param objectName
	 * @param recordTypeId
	 * @return
	 */
	public static String getRestURL_Layouts(String domain, String objectName, String recordTypeId) {
		return getRestURL_BasicSObject(domain, objectName) + "describe/layouts/" + recordTypeId;
	}

	/**
	 * REST用、オブジェクトのLayoutを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/describe/namedLayout/{LAYOUT_NAME}
	 * 
	 * これ使えない does not have any named layouts になる /layout/{recordTypeId}を使うべき
	 * 
	 * @param domain
	 * @param objectName
	 * @param layoutName
	 * @return
	 */
	public static String getRestURL_NamedLayouts(String domain, String objectName, String layoutName) {
		return getRestURL_BasicSObject(domain, objectName) + "describe/namedLayouts/" + layoutName;
	}

	/**
	 * REST用、削除したデータ一覧を取得<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/deleted/?start={START}&end={END}
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param start      データを取得する期間の開始日時
	 * @param end        データを取得する期間の終了日時
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 29.0 以降
	 */
	public static String getRestURL_GetDeleted(String domain, String objectName, Date start, Date end) {
		String st = formatDateToURLEncode(start);
		String ed = formatDateToURLEncode(end);
		return getRestURL_BasicSObject(domain, objectName) + "deleted/?start=" + st + "&end=" + ed;
	}

	/**
	 * REST用、更新したデータ一覧を取得。Max600,000 件<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/updated/?start={START}&end={END}
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param start      データを取得する期間の開始日時
	 * @param end        データを取得する期間の終了日時
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 29.0 以降
	 */
	public static String getRestURL_Updated(String domain, String objectName, Date start, Date end) {
		String st = formatDateToURLEncode(start);
		String ed = formatDateToURLEncode(end);
		return getRestURL_BasicSObject(domain, objectName) + "updated/?start=" + st + "&end=" + ed;
	}

	/**
	 * REST用、データを取得。<br>
	 * この形式ではファイル取得できないので注意。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/{SALESFORCE_ID}/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param id         SalesforceID
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_Rows(String domain, String objectName, String id) {
		return getRestURL_BasicSObject(domain, objectName) + id + "/";
	}

	/**
	 * REST用、外部IDでデータを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/{FIELD_NAME}/{FIELD_VALUE}/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param fieldName  外部ID項目の物理名
	 * @param fieldValue 検索値
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_RowsByExternalID(String domain, String objectName, String fieldName,
			String fieldValue) {
		return getRestURL_BasicSObject(domain, objectName) + fieldName + "/" + fieldValue + "/";
	}

	/**
	 * REST用、BLOBデータ、つまりファイルを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/{SALESFORCE_ID}/{FIELD_NAME}/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param id         SalesforceID
	 * @param fieldName  BLOBフィールドの物理名
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_BlobRetrieve(String domain, String objectName, String id, String fieldName) {
		return getRestURL_Rows(domain, objectName, id) + fieldName + "/";
	}

	/**
	 * REST用、リッチテキスト中の画像を取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/{SALESFORCE_ID}/richTextImageFields/{FIELD_NAME}/{REFERENCE_ID}
	 * 
	 * @param domain             http://XXXの文字列
	 * @param objectName         オブジェクトの物理名
	 * @param id                 SalesforceID
	 * @param fieldName          リッチテキストフィールドの物理名
	 * @param contentReferenceId リッチテキスト内の一意になるコンテンツID。オブジェクトのフィールド情報で取得する。
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_RichTextImageRetrieve(String domain, String objectName, String id, String fieldName,
			String contentReferenceId) {
		return getRestURL_Rows(domain, objectName, id) + "richTextImageFields/" + fieldName + "/" + contentReferenceId
				+ "/";
	}

	/**
	 * REST用、リッチテキスト中の画像を取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/{SALESFORCE_ID}/RELATIONSHIP_FIELD_NAME}
	 * 
	 * @param domain                http://XXXの文字列
	 * @param objectName            オブジェクトの物理名
	 * @param id                    SalesforceID
	 * @param relationshipFieldName 参照フィールドの物理名。__cではなく、__rで指定
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 36.0 以降
	 */
	public static String getRestURL_Relationships(String domain, String objectName, String id,
			String relationshipFieldName) {
		return getRestURL_Rows(domain, objectName, id) + relationshipFieldName + "/";
	}

	/**
	 * REST用、ユーザパスワードの変更に関するAPI。<br>
	 * /services/data/vXX.X/sobjects/User/{SALESFORCE_ID}/password/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param id         ユーザID
	 * @return RESTアクセスURL
	 */
	public static String getRestURL_UserPassword(String domain, String id) {
		return getRestURL_BasicInfomation(domain) + "User/" + id + "/password/";
	}

	/**
	 * REST用、オブジェクトのリストビュー一覧を取得。データではなく、リストビュー<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/listviews/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 32.0 以降
	 */
	public static String getRestURL_ListViews(String domain, String objectName) {
		return getRestURL_BasicSObject(domain, objectName) + "listviews/";
	}

	/**
	 * REST用、オブジェクトのリストビューのDescribe、つまりメタデータを取得。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/listviews/{SALESFORCE_ID}/describe/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param id         リストビューのID
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 32.0 以降
	 */
	public static String getRestURL_ListViewDescribe(String domain, String objectName, String id) {
		return getRestURL_ListViews(domain, objectName) + id + "/" + "describe/";
	}

	/**
	 * REST用、リストビューの結果データと表示情報を返します。<br>
	 * /services/data/vXX.X/sobjects/{OBJECT_NAME}/listviews/{SALESFORCE_ID}/results/
	 * 
	 * @param domain     http://XXXの文字列
	 * @param objectName オブジェクトの物理名
	 * @param id         リストビューのID
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 32.0 以降
	 */
	public static String getRestURL_ListViewResults(String domain, String objectName, String id) {
		return getRestURL_ListViews(domain, objectName) + id + "/" + "results/";
	}

	/**
	 * REST用、Restfulな検索API.<br>
	 * 『?q=search string』で検索条件を設定する。この文字列はこのメソッド取得後に付与すること。<br>
	 * /services/data/vXX.X/parameterizedSearch/
	 * 
	 * @param domain http://XXXの文字列
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 36.0 以降
	 * @see https://developer.salesforce.com/docs/atlas.ja-jp.228.0.api_rest.meta/api_rest/resources_search_parameterized.htm
	 */
	public static String getRestURL_RESTfulSelect(String domain) {
		return getRestURL_Base(domain) + "parameterizedSearch/";
	}

	/**
	 * REST用、SOQLクエリ実行。<br>
	 * 『?q=SOQLクエリ』で検索条件を設定する。この文字列はこのメソッド取得後に付与すること。<br>
	 * /services/data/vXX.X/query/
	 * 
	 * @param domain http://XXXの文字列
	 * @return RESTアクセスURL
	 * @see https://developer.salesforce.com/docs/atlas.ja-jp.228.0.api_rest.meta/api_rest/resources_query.htm
	 */
	public static String getRestURL_Query(String domain) {
		return getRestURL_Base(domain) + "query/";
	}

	/**
	 * REST用、SOQLクエリ実行。<br>
	 * 『?q=SOQLクエリ』で検索条件を設定する。この文字列はこのメソッド取得後に付与すること。<br>
	 * /services/data/vXX.X/queryAll/
	 * 
	 * @param domain http://XXXの文字列
	 * @return RESTアクセスURL
	 * @see https://developer.salesforce.com/docs/atlas.ja-jp.228.0.api_rest.meta/api_rest/resources_queryall.htm
	 */
	public static String getRestURL_QueryAll(String domain) {
		return getRestURL_Base(domain) + "queryAll/";
	}

	/**
	 * REST用、組織内のオブジェクトレコード件数に関する情報を取得。<br>
	 * /services/data/vXX.X/limits/recordCount/?sObjects={OBJECT_NAME}
	 * 
	 * @param domain  http://XXXの文字列
	 * @param objects カンマ区切りのオブジェクト物理名
	 * @return RESTアクセスURL
	 * @apiNote API バージョン 40.0 以降
	 */
	public static String getRestURL_RecordCount(String domain, String objects) {
		return getRestURL_Base(domain) + "limits/recordCount/?sObjects=" + objects;
	}

	/**
	 * REST用、SOSLクエリ実行。<br>
	 * 『?q=SOSLクエリ』で検索条件を設定する。この文字列はこのメソッド取得後に付与すること。<br>
	 * /services/data/vXX.X/search/
	 * 
	 * @param domain http://XXXの文字列
	 * @return RESTアクセスURL
	 * @see https://developer.salesforce.com/docs/atlas.ja-jp.228.0.api_rest.meta/api_rest/resources_search.htm
	 */
	public static String getRestURL_Search(String domain) {
		return getRestURL_Base(domain) + "search/";
	}

}

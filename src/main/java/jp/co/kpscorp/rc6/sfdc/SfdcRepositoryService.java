package jp.co.kpscorp.rc6.sfdc;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import com.sforce.ws.ConnectionException;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import jp.co.kpscorp.util.CaseFreeMap;

@Scope("prototype")
@Repository
public class SfdcRepositoryService<T> {
	private Logger logger = Logger.getLogger(SfdcRepositoryService.class);

	private Map<String, String> labelMap = new CaseFreeMap<>();

	private String label;
	public static String fldcolumnname = "columnname";

	// OpenActivity等のサブクエリーを呼ぶ場合の親のオブジェクト名
	private String oyaObjectName;

	// OpenActivity等のサブクエリーを呼ぶ場合のtotal件数
	private Integer koTotal;

	// public void putLabel(String key, String value) {
	// labelMap.put(key, value);
	// setupColumns();
	// }
	//
	// public void removeLabel(Object key) {
	// labelMap.remove(key);
	// setupColumns();
	// }

	private String objectName;

	private Map<String, Map<String, Object>> fields;

	public enum Ftype {
		date, datetime, number, multiselect, etc
	}

	private String columns;

	// private Class<T> voClass;

	private BeanUtilsBean bub;

	@Autowired
	private EntityManager em;

	// private Map<String, Options<T>> viewsMap = new HashMap<>();

	private boolean isPaging = true;

	public boolean isPaging() {
		return isPaging;
	}

	public void setPaging(boolean isPaging) {
		this.isPaging = isPaging;
	}

	public EntityManager getEm() {
		return em;
	}

	/*
	 * public void setFields(Map<String, Map<String, Object>> fields) { this.fields
	 * = fields; setupColumns(); }
	 *
	 * public void addField(String name, String label, String alias, Ftype ftype) {
	 * Map<String, Object> fld = new CaseFreeMap<>(); String ftypes; switch (ftype)
	 * { case date: ftypes = "date"; break; case datetime: ftypes = "datetime";
	 * break; case number: ftypes = "double"; break; case multiselect: ftypes =
	 * "multiselect"; break; default: ftypes = "string"; break; } fld.put("type",
	 * ftypes); fld.put("name", name); fld.put("label", label); fld.put("sortable",
	 * true); if (alias != null) { fld.put("alias", alias); } this.fields.put(name,
	 * fld); this.fields.put(alias, fld); setupColumns();
	 *
	 * }
	 */

	private boolean isYourField(String fn, Class claz) {
		for (Field f : claz.getDeclaredFields()) {
			if (f.getName().toLowerCase().equals(fn.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	public void setObjectName(String objectName) throws Exception {
		this.objectName = objectName;
		// this.fields = sfdcCdata.getFlds(objectName);
		// Map<String, Object> od = (Map<String, Object>)
		// sfdcCdata.getObjMeta(objectName).objMtta.get("objectDescribe");
		// this.label = (String) od.get("label");
		// findCls(objectName);
		// if (!(boolean) od.get("queryable")) {
		// // queryable出ない場合は は親のクラスのサブクエリーで呼ぶ、ここでは親オブジェクト名がわからないのでとりあえずdummyを入れる
		// setOyaObjectName("dummy");
		// }

		// とりあえずsetupColumnsしない
		// setupColumns();
	}

	/**
	 * Beanのしきたりにあわせてフィールドの名前の１文字目を小文字にする ただし１文字目2文字目が大文字ならそのまま
	 */
	/*
	 * private void setupFld4Bean() { Map<String, Map<String, Object>> nflds = new
	 * CaseFreeMap<>(); for (String key : this.fields.keySet()) { Map<String,
	 * Object> fld = this.fields.get(key); String nkey; nkey = convertKey4Bean(key);
	 * fld.put("name", nkey); nflds.put(nkey, fld); } this.fields = nflds; }
	 */
	public static String convertKey4Bean(String key) {
		if (key == null) {
			return null;
		}
		if (!key.contains(".")) {
			return convertKey4BeanSub(key);
		}
		String[] ss = key.split("\\.");
		for (int i = 0; i < ss.length; i++) {
			ss[i] = convertKey4BeanSub(ss[i]);
		}
		return String.join(".", ss);
	}

	public static String convertKey4BeanSub(String key) {
		if (key == null) {
			return null;
		}
		String nkey;
		if (key.length() > 1 && key.substring(0, 2).toUpperCase().equals(key.substring(0, 2))) {
			nkey = key;
		} else {
			nkey = key.substring(0, 1).toLowerCase();
			if (key.length() > 1) {
				nkey += key.substring(1);
			}
		}
		return nkey;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	@Autowired
	private SfdcConnect con;

	public SfdcConnect getCon() {
		return con;
	}

	// test用
	// public void setCon(SfdcConnectImpl con) {
	// this.con = con;
	// }

	public String getColumns() {
		return columns;
	}

	public String getObjectName() {
		return objectName;
	}

	public SfdcRepositoryService() throws Exception {
		super();
	}

	private boolean updateMeta = false;

	public List<T> makeVoList(List<Map<String, Object>> mps, Class<T> voClass)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, IllegalArgumentException, NoSuchMethodException, SecurityException {
		// BeanUtilsBean bb = getBeanUtilBean();

		List<T> res = new ArrayList<>();

		for (Map<String, Object> mp : mps) {
			T bean = voClass.getDeclaredConstructor().newInstance();
			set2Entity(bean, mp);
			// bb.populate(bean, mp);
			res.add(bean);
		}
		return res;
	}

	public T set2Entity(T ent, Map<String, Object> mp) {
		for (String fn : mp.keySet()) {
			try {
				Object val = mp.get(fn);
				if (val != null) {
					if (val instanceof String[]) {
						// for multipicklist
						String[] ss = (String[]) val;
						StringBuilder sb = new StringBuilder();
						for (String s : ss) {
							sb.append(s).append(";");
						}
						val = sb.toString();
					} else if (val instanceof Map) {
						Map<String, Object> valMap = (Map<String, Object>) val;
						if ("attributes".equals(fn)) {
							getIdFromUrl(ent, mp, fn);
							continue;
						}
						// Object対応
						Object cobj = null;
						for (String s : valMap.keySet()) {
							String prop = convertKey4Bean(s);
							if ("attributes".equals(prop)) {
								cobj = getIdFromUrl(new CaseFreeMap<>(), valMap, prop);
							} else {
								BeanUtils.setProperty(cobj, prop, valMap.get(s));
							}
						}
						val = cobj;
					}
					// entがMapの場合対応
					if (ent instanceof Map) {
						((Map<String, Object>) ent).put(fn, val);
					} else {
						Method mth = null;
						try {
							mth = ent.getClass().getMethod(makeSetterName(fn), val.getClass());
						} catch (NoSuchMethodException e) {
						}
						if (mth == null) {
							// setterの型がSerializableを試す
							try {
								mth = ent.getClass().getMethod(makeSetterName(fn), Serializable.class);
							} catch (NoSuchMethodException e) {
							}
						}
						if (mth == null && fn.indexOf('.') >= 0) {
							fn = serachFn(fn);
							try {
								mth = ent.getClass().getMethod(makeSetterName(fn), val.getClass());
							} catch (NoSuchMethodException e) {
							}
						}
						if (mth == null && val instanceof BigDecimal) {
							try {
								mth = ent.getClass().getMethod(makeSetterName(fn), Double.class);
								val = ((BigDecimal) val).doubleValue();
							} catch (NoSuchMethodException e) {
							}
							if (mth == null) {
								try {
									mth = ent.getClass().getMethod(makeSetterName(fn), Integer.class);
									val = ((BigDecimal) val).intValue();
								} catch (NoSuchMethodException e) {
								}
							}
							if (mth == null) {
								try {
									mth = ent.getClass().getMethod(makeSetterName(fn), Long.class);
									val = ((BigDecimal) val).longValue();
								} catch (NoSuchMethodException e) {
								}
							}
							if (mth == null) {
								try {
									mth = ent.getClass().getMethod(makeSetterName(fn), String.class);
									val = ((BigDecimal) val).toString();
								} catch (NoSuchMethodException e) {
								}
							}
						}
						if (mth != null) {
							mth.invoke(ent, val);
						}
					}
				}
			} catch (InvocationTargetException e) {
			} catch (Exception e) {
				logger.error(e + "/" + e.getMessage());
			}
		}
		return ent;
	}

	private Object getIdFromUrl(Object cobj, Map<String, Object> valMap, String prop)
			throws IllegalAccessException, InvocationTargetException, InstantiationException, IllegalArgumentException,
			NoSuchMethodException, SecurityException {
		// idをurlから抽出
		Map<String, Object> atrMap = (Map) valMap.get(prop);
		String url = (String) atrMap.get("url");
		String id = url.substring(url.lastIndexOf("/") + 1);
		// if (cobj == null) {
		// url = url.substring(0, url.lastIndexOf("/"));
		// String type = url.substring(url.lastIndexOf("/") + 1);
		// type = type.substring(0, 1).toUpperCase() + type.substring(1);
		// cobj = dbcdata.findCls(type).getDeclaredConstructor().newInstance();
		// }
		BeanUtils.setProperty(cobj, "id", id);
		return cobj;
	}

	private String serachFn(String s) {
		for (String key : this.fields.keySet()) {
			Map<String, Object> fld = fields.get(key);
			String cn = (String) fld.get("columnname");
			if (cn != null && cn.toLowerCase().equals(s.toLowerCase())) {
				return key;
			}
		}
		return null;
	}

	private String makeSetterName(String key) {
		if (key.length() > 1 && key.substring(0, 2).toUpperCase().equals(key.substring(0, 2))) {
			return "set" + key;
		}
		String nkey = key.substring(0, 1).toUpperCase();
		if (key.length() > 1) {
			nkey += key.substring(1);
		}
		return "set" + nkey;
	}

	/**
	 * null safeなBeanUtilsBeanを返す
	 *
	 * @return
	 */
	public BeanUtilsBean getBeanUtilBean() {
		if (this.bub != null) {
			return this.bub;
		}
		return this.bub = getBub();
	}

	public static BeanUtilsBean getBub() {
		ConvertUtilsBean convertUtilsBean = new ConvertUtilsBean();
		convertUtilsBean.deregister(Date.class);
		convertUtilsBean.register(new DateConverter(null), Date.class);
		convertUtilsBean.deregister(Double.class);
		convertUtilsBean.register(new DoubleConverter(null), Double.class);
		return new BeanUtilsBean(convertUtilsBean, new PropertyUtilsBean());
	}

	private void setNestedVal(Map<String, Object> res, String key, Map<String, Object> mp) {
		for (String subkey : mp.keySet()) {
			Object val = mp.get(subkey);
			if (val instanceof Map) {
				Map<String, Object> mval = (Map<String, Object>) val;
				setNestedVal(res, key + "." + subkey, mval);
			} else {
				res.put(key + "." + subkey, val);
			}
		}
	}

	public List<Map<String, Object>> doSoql(String soql) throws IOException, Exception {
		if (this.oyaObjectName == null) {
			soql = addLimitOffset(soql);
		} else {
			// 親がある場合親のlimitは必ず1
			soql += " limit 1";
		}
		List<Map<String, ?>> wmps = con.doQuery(soql);
		List<Map<String, Object>> res = conver4Bean(wmps);
		return res;
	}

	private String addLimitOffset(String soql) {
		// if (this.pageNation.limit >= 0 && isPaging) {
		// soql += " limit " + this.pageNation.limit;
		// }
		// if (this.pageNation.offset >= 0 && isPaging) {
		// soql += " offset " + this.pageNation.offset;
		// }
		return soql;
	}

	public List<Map<String, Object>> conver4Bean(List<Map<String, ?>> wmps) throws ParseException {
		List<Map<String, Object>> res = new ArrayList<>();
		for (Map<String, ?> wmp : wmps) {
			CaseFreeMap<String, Object> cmp = new CaseFreeMap<String, Object>();
			for (String key : wmp.keySet()) {
				Object val = wmp.get(key);
				if (val instanceof Map) {
					// 単純にMapをそのkeyでputするのも必要 2023/02/16
					cmp.put(convertKey4Bean(key), val);
					Map<String, Object> rval = new HashMap<>();
					Map<String, Object> mval = (Map<String, Object>) val;
					setNestedVal(rval, key, mval);
					for (String rvkey : rval.keySet()) {
						if (this.fields != null) {
							Map<String, Object> fld = this.fields.get(rvkey);
							if (fld != null) {
								String al = (String) fld.get("alias");
								if (al != null) {
									cmp.put(convertKey4Bean(al), convert4Map(rvkey, rval.get(rvkey)));
								}
								String vofnm = (String) fld.get("vofieldname");
								if (vofnm != null) {
									cmp.put(convertKey4Bean(vofnm), convert4Map(rvkey, rval.get(rvkey)));
								}
							}
						}
					}
				} else {
					cmp.put(convertKey4Bean(key), convert4Map(key, val));
				}
			}
			res.add(cmp);
		}
		return res;
	}

	public Map<String, Object> saveAttachment(File file, String fileName, String parentId)
			throws SfdcConnectException, Exception {
		byte[] data = Files.readAllBytes(file.toPath());
		String base64str = DatatypeConverter.printBase64Binary(data);
		Map<String, Object> entity = new HashMap<>();
		entity.put("Name", fileName);
		entity.put("parentId", parentId);
		entity.put("Body", base64str);
		return con.doInsert("Attachment", entity);
	}

	public Map<String, Object> saveNote(String text, String title, String parentId)
			throws SfdcConnectException, Exception {
		Map<String, Object> entity = new HashMap<>();
		entity.put("Title", title);
		entity.put("parentId", parentId);
		entity.put("Body", text);
		return con.doInsert("Note", entity);
	}

	public Map<String, Object> saveAndFlush(Map<String, Object> entity) throws Exception {
		// 該当のoptios消す
		// omap.getOptionsMap().remove(getObjectName());
		String id = (String) entity.get("id");
		entity = makeObj4Send(entity);
		// LastModifiedDate等の設定
		// Map<String, Object> om = (Map<String, Object>)
		// sfdcCdata.getObjMeta(objectName).objMtta;
		// String cuf = (String) om.get("cruserField");
		// String uuf = (String) om.get("upuserField");
		// String utf = (String) om.get("uptimeField");
		// if (cuf == null) {
		// cuf = "CreatedByApi__c";
		// }
		// if (uuf == null) {
		// uuf = "LastModifiedByApi__c";
		// }
		// if (utf == null) {
		// utf = "LastModifiedDateApi__c";
		// }
		// if (this.fields.get(uuf) != null && this.fields.get(uuf).get("updateable") !=
		// null
		// && (boolean) this.fields.get(uuf).get("updateable")) {
		// SfdcUserDetail sud = (SfdcUserDetail)
		// SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// entity.put(uuf, sud.getId());
		// }
		// if (this.fields.get(utf) != null && this.fields.get(utf).get("updateable") !=
		// null
		// && (boolean) this.fields.get(utf).get("updateable")) {
		// entity.put(utf, new Date());
		// }
		if (id == null || id.length() == 0) {
			// if (this.fields.get(cuf) != null && this.fields.get(cuf).get("updateable") !=
			// null
			// && (boolean) this.fields.get(cuf).get("updateable")) {
			// SfdcUserDetail sud = (SfdcUserDetail)
			// SecurityContextHolder.getContext().getAuthentication()
			// .getPrincipal();
			// entity.put(cuf, sud.getId());
			// }
			return con.doInsert(objectName, entity);
		}
		return con.doUpdate(objectName, id, entity);
	}

	public Map<String, Object> delete(String id) throws Exception {
		// 該当のoptios消す
		// omap.getOptionsMap().remove(getObjectName());
		return con.doDelete(objectName, id);
	}

	/**
	 * 送信用に不必要なエントリーをMapから削除/型をSFDC用に変更 受け側が日付型の場合は９時間足した日時にする
	 * https://help.salesforce.com/articleView?id=000047804&language=ja&type=1
	 *
	 * @param mp
	 * @return
	 * @throws SfdcConnectException
	 */
	private Map<String, Object> makeObj4Send(Map<String, Object> mp) throws SfdcConnectException {
		Map<String, Object> res = new HashMap<>();
		for (String key : mp.keySet()) {
			if (this.fields.get(key) != null) {
				if (!key.toLowerCase().equals("id")) {
					Boolean upok = (Boolean) this.fields.get(key).get("updateable");
					Object val = convert4Sfdc(key, mp.get(key));
					if (upok != null && upok) {
						if (!isSfdcField(this.fields.get(key))) {
							// column指定がある場合は除く
							if (val != null && this.fields.get(key).get(fldcolumnname) == null) {
								// null以外をセットしたいがSFDCに無いFieldはexception
								throw new SfdcConnectException("filed " + key + " is not in this Object!");
							} else {
								// このKeyは無視
								continue;
							}
						}
						// 受け側が日付型の場合は９時間足した日時にする
						if ("date".equals(this.fields.get(key).get("type")) && mp.get(key) instanceof Date) {
							Calendar calendar = Calendar.getInstance();
							calendar.setTime((Date) mp.get(key));
							calendar.add(Calendar.HOUR, 9);
							val = calendar.getTime();
						}
						// checkbox nullはエラーになる
						if ("boolean".equals(this.fields.get(key).get("type"))
								&& (Boolean) this.fields.get(key).get("nillable") == false && val == null) {
							val = false;
						}
						res.put(key, val);
					}
					/*
					 * 参照のみの項目もあるので、この部分があるとまずい else if (val != null) {
					 * //null以外をセットしたいがupdateableでないのでexception throw new
					 * SfdcConnectException("filed " + key + " is not updateable!"); }
					 */
				}
			}
		}
		return res;
	}

	private Object convert4Sfdc(String key, Object val) {
		if (val == null) {
			return null;
		}
		Object res = val;
		if (val instanceof String[]) {
			String[] ss = (String[]) val;
			res = StringUtils.join(ss, ';');
		}
		return res;
	}

	/*
	 * private Object getNestVal(String key, Map<String, Object> val) { int i =
	 * key.indexOf('.'); // String firstKey = key.substring(0, i); String secondKey
	 * = key.substring(i + 1); Object res = val.get(secondKey); if
	 * (secondKey.indexOf('.') > 0 && res instanceof Map) { return
	 * getNestVal(secondKey, (Map<String, Object>) res); } return res; }
	 */
	private Object convert4Map(String key, Object val) throws ParseException {
		Map<String, Object> fld = this.fields.get(key);
		if (fld == null) {
			logger.debug("Can't Find field for " + key);
			return val;
		}
		if (val == null) {
			if ("multipicklist".equals(fld.get("type"))) {
				return new String[0];
			}
			return null;
		}
		Object res = val;
		if (fld == null) {
			return res;
		}
		if ("multipicklist".equals(fld.get("type"))) {
			String s = (String) val;
			res = s.split(";");
		} else if ("date".equals(fld.get("type"))) {
			String s = (String) val;
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			res = df.parse(s);
		} else if ("datetime".equals(fld.get("type"))) {
			String s = (String) val;
			// SimpleDateFormat df = new
			// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss");
			TimeZone tz = TimeZone.getTimeZone("GMT");
			df.setTimeZone(tz);
			res = df.parse(s);
		}
		return res;
	}

	/**
	 * BeanからSaveAndFlash用のMapに変換
	 *
	 * @param bean
	 * @return
	 */
	public Map<String, Object> bean2Map(Object bean) {
		Map<String, Object> res = new HashMap<>();
		Field[] fs = bean.getClass().getDeclaredFields();
		for (Field f : fs) {
			try {
				PropertyDescriptor properties = new PropertyDescriptor(f.getName(), bean.getClass());
				Method getter = properties.getReadMethod();
				Object val = getter.invoke(bean, (Object[]) null);
				// Booleanでnullは更新対象から外す
				if (!properties.getPropertyType().equals(Boolean.class) || val != null) {
					res.put(f.getName(), val);
				}
			} catch (IntrospectionException | InvocationTargetException | IllegalAccessException
					| IllegalArgumentException e) {
				// e.printStackTrace();
			}
		}
		return res;
	}

	public Map<String, Object> getObjInfo(String id) throws Exception {
		String surl = this.con.getServerUrl();
		// API Version統一
		String url = SfdcApiUrlUtil.getRestURL_Rows(surl, objectName, id);
		return con.getMapres(new HttpGet(url));
	}

	public void downloadBinary(String id, HttpServletRequest req, HttpServletResponse sres) throws Exception {
		InputStream is = getIs4Download(id);
		Map<String, Object> mp = getObjInfo(id);
		String fileName = (String) mp.get("Name");
		if (req.getHeader("User-Agent").indexOf("MSIE") == -1) {
			// Firefox, Opera 11
			sres.setHeader("Content-Disposition", String.format(Locale.JAPAN, "attachment; filename*=utf-8'jp'%s",
					URLEncoder.encode(fileName, "utf-8")));
		} else {
			// IE7, 8, 9
			sres.setHeader("Content-Disposition", String.format(Locale.JAPAN, "attachment; filename=\"%s\"",
					new String(fileName.getBytes("MS932"), "ISO8859_1")));
		}
		sres.addHeader("Content-Type", (String) mp.get("ContentType"));
		IOUtils.copy(is, sres.getOutputStream());
		sres.flushBuffer();
	}

	public ResponseEntity<byte[]> makeResponsEntity(String id) throws Exception {
		InputStream is = getIs4Download(id);
		Map<String, Object> mp = getObjInfo(id);
		String fileName = (String) mp.get("Name");
		if (!fileName.endsWith(".pdf")) {
			fileName += ".pdf";
		}
		HttpHeaders h = new HttpHeaders();
		h.add("Content-Type", "application/pdf");
		h.add("Content-Disposition",
				"inline; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));
		return new ResponseEntity<>(IOUtils.toByteArray(is), h, HttpStatus.OK);
	}

	public String downloadBinary(String id, String dirpath, String fileName) throws Exception {
		if (fileName == null) {
			Map<String, Object> mp = getObjInfo(id);
			fileName = (String) mp.get("Name");
		}
		InputStream is = getIs4Download(id);
		FileOutputStream fos = new FileOutputStream(dirpath + File.separator + fileName);
		IOUtils.copy(is, fos);
		fos.flush();
		fos.close();
		return dirpath + File.separator + fileName;
	}

	public String downloadBinary(String id, String dirpath) throws Exception {
		return downloadBinary(id, dirpath, null);
	}

	private InputStream getIs4Download(String id) throws ConnectionException, IOException, Exception {
		String surl = this.con.getServerUrl();
		// ApiVersion統一
		String url = SfdcApiUrlUtil.getRestURL_BlobRetrieve(surl, objectName, id, "body");
		HttpResponse res = con.getResponse(new HttpGet(url));
		InputStream is = res.getEntity().getContent();
		return is;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getOyaObjectName() {
		return oyaObjectName;
	}

	public void setOyaObjectName(String oyaObjectName) {
		this.oyaObjectName = oyaObjectName;
	}

	public T saveAndFlush(T t, Class<T> cls) {
		try {
			return set2Entity(cls.getDeclaredConstructor().newInstance(),
					saveAndFlush(PropertyUtils.describe(t)));
		} catch (Exception e) {
			logger.warn(e.getMessage() + "/" + e);
		}
		return null;
	}

	public static boolean isSfdcField(Map<String, Object> field) {
		if (field == null) {
			return false;
		}
		return field.get("autoNumber") != null;
	}

}

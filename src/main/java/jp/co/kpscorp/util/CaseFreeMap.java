package jp.co.kpscorp.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class CaseFreeMap<String, V> extends LinkedHashMap<String, V> {

	private Logger logger = Logger.getLogger(CaseFreeMap.class);
	private Map<String, String> orgKeyMap = new HashMap<>();

	@Override
	public V get(Object key) {

		V res;
		if (key != null) {
			String lkey = (String) key.toString().toLowerCase();
			res = super.get(orgKeyMap.get(lkey));
			if (res == null && !((java.lang.String) lkey).startsWith("attributes.") && !"format".equals(key)) {
				logger.trace(
						"Map return null! on key:" + key + " of " + super.get("name") + " keySet:" + super.keySet());
				if ("nosearchbutton".equals(lkey) || "livesearch".equals(lkey)) {
					logger.debug("Map return null! on key:" + key + " of " + super.get("name"));
				}
			} else {
				// debug
				// if ("insert".equals(key)) {
				// System.out.println("insert:" + res);
				// }

				return res;
			}
		}
		res = super.get(key);
		String keystr = (String) "dummy";
		if (key != null) {
			keystr = (String) key.toString();
		}
		if (res == null && !((java.lang.String) keystr).startsWith("attributes.") && !"format".equals(key)) {
			logger.trace("Map return null! on key:" + key + " of field name " + super.get("name") + " keySet:"
					+ super.keySet());
		}
		// debug
		// if ("insert".equals(key)) {
		// System.out.println("insert:" + res);
		// }
		return res;
	}

	@Override
	public V put(String key, V value) {
		// debug
		// if ("hide".equals(key) && get(CommonData.fldmytype) != null) {
		// System.out.println("hide:" + value + " type:" + get(CommonData.fldmytype));
		// }

		if (key != null) {
			String lkey = (String) key.toString().toLowerCase();
			orgKeyMap.put(lkey, key);
		}
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m) {
		for (String key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public V remove(Object key) {
		if (key != null) {
			String lkey = (String) key.toString().toLowerCase();
			return super.remove(orgKeyMap.get(lkey));
		}
		return super.remove(key);
	}

}
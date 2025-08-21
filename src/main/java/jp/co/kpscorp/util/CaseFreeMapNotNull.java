package jp.co.kpscorp.util;

import java.util.Map;

public class CaseFreeMapNotNull extends CaseFreeMap<String, Map<String, Object>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Map<String, Object> get(Object key) {
		Map<String, Object> res = super.get(key);
		if (res == null) {
			res = new CaseFreeMap<>();
			put((String) key, res);
			// fieldにmyType追加 (主にdebug用)
			// if (get(CommonData.fldmytype) != null) {
			// res.put(CommonData.fldmytype, get(CommonData.fldmytype).get("type"));
			// }
		}
		// debug
		// if ("name_table".equals(key)) {
		// System.out.println(res.get("type"));
		// }

		return res;
	}

	@Override
	public Map<String, Object> put(String key, Map<String, Object> value) {
		// jsonBのfield定義を持つform classがっせ呈された場合
		return super.put(key, value);
	}

}

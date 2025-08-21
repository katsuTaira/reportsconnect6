package jp.co.kpscorp.rc6.service;

import java.util.HashMap;
import java.util.Map;

public class ThreadMap {
	private static ThreadLocal<Map<String, Object>> tl = new ThreadLocal<Map<String, Object>>() {
		protected synchronized Map<String, Object> initialValue() {
			return new HashMap<String, Object>();
		}
	};

	public static Map<String, Object> get() {
		return tl.get();
	}

}

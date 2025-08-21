package jp.co.kpscorp.rc6.service;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

public interface PrepareResponse {
	public void prepare(HttpServletResponse resp, PrintSource<Map<String, ?>> source);
}

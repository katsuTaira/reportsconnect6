package jp.co.kpscorp.rc6.component;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

public interface PrepareResponse {
	public void prepare(HttpServletResponse resp, PrintSource<Map<String, ?>> source);
}

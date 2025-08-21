package jp.co.kpscorp.rc6.component;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

public class TxtResponse implements PrepareResponse {

	public void prepare(HttpServletResponse resp, PrintSource<Map<String, ?>> source) {
		resp.setContentType("application/octet-stream; charset=Shift_JIS");
		resp.setHeader("Content-Disposition",
				"attachment;filename=" + source.getFileName() + ".txt");
		resp.setContentType("application/txt");
	}

}

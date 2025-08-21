package jp.co.kpscorp.rc6.service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class PdfResponse implements PrepareResponse {

	public void prepare(HttpServletResponse resp, PrintSource<Map<String, ?>> source) {
		resp.setContentType("application/octet-stream; charset=Shift_JIS");
		try {
			String value = source.getFileName() + ".pdf";
			String fileName = new String(value.getBytes("SHIFT_JIS"),
					"ISO-8859-1");
			resp.setHeader("Content-Disposition", "attachment;filename="
					+ fileName);
		} catch (UnsupportedEncodingException e) {
		}
		resp.setContentType("application/pdf");
	}
}

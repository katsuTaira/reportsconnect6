package jp.co.kpscorp.rc6.component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class PdfInlineResponse implements PrepareResponse {

	public void prepare(HttpServletResponse resp,
			PrintSource<Map<String, ?>> source) {
		// ContentDisposition を Spring が RFC 準拠で作ってくれる
		ContentDisposition disposition = ContentDisposition
				.inline()
				.filename(source.getFileName() + ".pdf", StandardCharsets.UTF_8)
				.build();

		resp.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

		resp.setContentType("application/pdf");
	}
}

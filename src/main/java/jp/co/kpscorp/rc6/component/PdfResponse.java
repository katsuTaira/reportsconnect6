package jp.co.kpscorp.rc6.component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class PdfResponse implements PrepareResponse {

	public void prepare(HttpServletResponse resp, PrintSource<Map<String, ?>> source) {
		ContentDisposition disposition = ContentDisposition
				.attachment()
				.filename(source.getFileName() + ".pdf", StandardCharsets.UTF_8)
				.build();

		resp.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
		resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
	}
}

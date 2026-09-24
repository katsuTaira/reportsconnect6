package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UploadChecker {
	public static final String FnKey = "ksp_filename";
	public static final String textAreaKey = "kps_textarea";

	@Autowired
	private ServletContext context;

	/*
	 * アップロードしたファイルの確認用
	 *
	 * @see
	 * jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.
	 * HttpServletRequest
	 * , jakarta.servlet.http.HttpServletResponse)
	 */
	@RequestMapping(path = "/uc", method = RequestMethod.GET)
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String path = req.getParameter("path");
		System.out.println("UploadChecker path:" + path);
		if (path == null) {
			return;
		}
		String fnm = path.substring(path.lastIndexOf("/") + 1);
		// ServletContext context = getServletContext();
		path = context.getRealPath(path);
		resp.setContentType("application/octet-stream");
		download(req, resp, fnm, new FileInputStream(path));
	}

	/*
	 * CSVデータソースのダウンロード用
	 *
	 * @see
	 * jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.
	 * HttpServletRequest
	 * , jakarta.servlet.http.HttpServletResponse)
	 */
	@RequestMapping(path = "/uc", method = RequestMethod.POST)
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		// req.setCharacterEncoding("MS932");
		String s = req.getParameter(textAreaKey);
		String fnm = req.getParameter(FnKey);
		resp.setContentType("text/comma-separated-values");
		// download(req, resp, fnm, new ByteArrayInputStream(s.getBytes("utf-8")));
		download(req, resp, fnm, new ByteArrayInputStream(s.getBytes("MS932")));
	}

	private void download(HttpServletRequest request,
			HttpServletResponse response, String fnm, InputStream in)
			throws ServletException, IOException {
		OutputStream out = null;
		try {
			// response.setHeader("Content-Disposition", "filename=\"" + fnm
			// + "\"");
			ContentDisposition disposition = ContentDisposition
					.attachment()
					.filename(fnm, StandardCharsets.UTF_8)
					.build();

			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

			out = response.getOutputStream();
			byte[] buff = new byte[1024];
			int len = 0;
			while ((len = in.read(buff, 0, buff.length)) != -1) {
				out.write(buff, 0, len);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}

}

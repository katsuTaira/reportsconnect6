package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.component.Settings.Userprop;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;

public class PrintServiceMapSuper implements PrintService {

	// protected PartnerConnection connection;

	protected ExportReporter exportReporter;

	protected PrepareResponse prepareResponse;

	protected String xmlPath;

	protected String fileName;

	protected String message;

	protected @Autowired ServletContext servletContext;

	protected @Autowired ApplicationContext applicationContext;

	protected HttpServletRequest request;

	protected HttpServletResponse respons;

	protected Locale locale = new Locale("ja", "JP");

	protected String ql;

	public static final String memEx = "Report size too large:";

	public static final String pageEx = "Report pages larger than ";

	public static final String nojrexlMsg = "xmlPath path can't be null!";

	public HttpServletRequest getRequest() {
		return request;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seejp.co.kpscorp.component.PrintService#setRequest(javax.servlet.http.
	 * HttpServletRequest)
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public String getQl() {
		return ql;
	}

	public void setQl(String ql) {
		this.ql = ql;
	}

	public HttpServletResponse getRespons() {
		return respons;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seejp.co.kpscorp.component.PrintService#setRespons(javax.servlet.http.
	 * HttpServletResponse)
	 */
	public void setRespons(HttpServletResponse respons) {
		this.respons = respons;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jp.co.kpscorp.component.PrintService#setConnection(com.sforce.soap.partner
	 * .PartnerConnection)
	 */
	// public void setConnection(PartnerConnection connection) {
	// this.connection = connection;
	// }

	/*
	 * (non-Javadoc)
	 *
	 * @see jp.co.kpscorp.component.PrintService#setFileName(java.lang.String)
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getXmlPath() {
		return xmlPath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jp.co.kpscorp.component.PrintService#setXmlPath(java.lang.String)
	 */
	public void setXmlPath(String xmlPath) {
		this.xmlPath = xmlPath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jp.co.kpscorp.component.PrintService#setExportReporter(jp.co.kpscorp.
	 * service.ExportReporter)
	 */
	public void setExportReporter(ExportReporter exportReporter) {
		this.exportReporter = exportReporter;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jp.co.kpscorp.component.PrintService#setPrepareResponse(jp.co.kpscorp
	 * .service.PrepareResponse)
	 */
	public void setPrepareResponse(
			PrepareResponse prepareResponse) {
		this.prepareResponse = prepareResponse;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seejp.co.kpscorp.component.PrintService#documentEdit(java.io.
	 * ByteArrayOutputStream, jp.co.kpscorp.service.PrintSource)
	 */
	public ByteArrayOutputStream documentEdit(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws Exception {
		if (source.getXmlPath() != null) {
			this.xmlPath = source.getXmlPath();
		}
		if (this.xmlPath == null) {
			throw new PrintServiceException(nojrexlMsg);
		}
		// テンプレートXMLファイルのパス
		String templatePath = servletContext.getRealPath(xmlPath);
		final String basePath = templatePath.substring(0,
				templatePath.lastIndexOf(File.separator) + 1);

		JasperReport jasperReport;
		// テンプレートXMLのコンパイル
		System.out.println("templatePath:" + templatePath);
		// 外字の設定 2025/07/01
		File f = (File) ThreadMap.get().get(PdfExportReporter.TH_EUDCFILE);
		String str1 = null;
		if (f != null && f.exists()) {
			str1 = f.getAbsolutePath();
		} else {
			String eudcFilename = "EUDC.TTF";
			str1 = ".fonts/EUDC/" + eudcFilename;
		}
		SimpleJasperReportsContext context = new SimpleJasperReportsContext();
		File eudcFile = new File(str1);
		if (eudcFile.exists()) {
			String xmlFilePath = eudcFile.getParentFile().getAbsolutePath() +
					File.separator + "fonts.xml";
			DynamicFontXmlGenerator.generateFontXml(
					eudcFile.getAbsolutePath(), new File(xmlFilePath).toPath());
			// JRPropertiesUtil.getInstance(context).setProperty(
			// "net.sf.jasperreports.extension.registry.factory.fonts",
			// "net.sf.jasperreports.engine.fonts.SimpleFontExtensionsRegistryFactory");
			// JRPropertiesUtil.getInstance(context).setProperty(
			// "net.sf.jasperreports.extension.simple.font.families.eudc",
			// xmlFilePath);
			// ExtensionsEnvironment.setThreadExtensionsRegistry(fontExtensionsRegistry);
			MyExtensionRegistory myExtensionsRegistry = new MyExtensionRegistory(
					xmlFilePath);
			ExtensionsEnvironment.setThreadExtensionsRegistry(myExtensionsRegistry);
			jasperReport = JasperCompileManager.compileReport(templatePath);
		} else {
			jasperReport = JasperCompileManager.compileReport(templatePath);
		}

		// JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(
		// source.getBeanList());

		JRMapCollectionDataSource ds = new JRMapCollectionDataSource(
				source.getBeanList());

		// JasperPrint print = JasperFillManager.fillReport(jasperReport,
		// source.getParmMap(), ds);
		Map<String, Object> omap = makeObjMap(source.getParmMap());
		// locale設定
		if (locale != null) {
			omap.put(JRParameter.REPORT_LOCALE, locale);
		}
		// リソースをjrxmlと同じpathからとる 2025/06/11 FileResolver はdeprecated
		// FileResolver fileResolver = new FileResolver() {
		// @Override
		// public File resolveFile(String fileName) {
		// return new File(basePath + fileName);
		// }
		// };
		// omap.put("REPORT_FILE_RESOLVER", fileResolver);

		JasperPrint print = Utils.getPrint(basePath, jasperReport, context, ds, omap);
		// pageSize check 2025/06/26
		Settings.Userprop up = (Userprop) ThreadMap.get().get(
				Settings.TH_USRPROP);
		if (print.getPages().size() > up.getLicense().getMaxpage()) {
			throw new JRException(PrintServiceMapSuper.pageEx + up.getLicense().getMaxpage()
					+ "page");
		}
		exportReporter.exportReport(print, byteOut);
		return byteOut;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seejp.co.kpscorp.component.PrintService#prepareResponse(java.io.
	 * ByteArrayOutputStream, jp.co.kpscorp.service.PrintSource)
	 */
	public void prepareResponse(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws IOException {
		if (prepareResponse == null) {
			prepareResponse = new TxtResponse();
		}
		source.setFileName(fileName);
		prepareResponse.prepare(respons, source);
		respons.setContentLength(byteOut.size());
		ServletOutputStream out = respons.getOutputStream();
		out.write(byteOut.toByteArray());
		out.close();

	}

	private Map<String, Object> makeObjMap(Map<String, String> mp) {
		Map<String, Object> res = new HashMap<String, Object>();
		for (String key : mp.keySet()) {
			res.put(key, mp.get(key));
		}
		return res;
	}

	protected ByteArrayOutputStream dispachError(Exception e)
			throws ServletException, IOException {
		request.setAttribute("error", e.toString());
		request.setAttribute("error_description", e.getMessage());
		RequestDispatcher dispatcher = servletContext
				.getRequestDispatcher("/WEB-INF/views/dlError.jsp");
		dispatcher.forward(request, respons);
		return null;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}

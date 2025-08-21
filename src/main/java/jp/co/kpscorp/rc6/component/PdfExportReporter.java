package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

@Component
public class PdfExportReporter implements ExportReporter {
	public static final String TH_EUDCFILE = "kps_eudcfile";

	public void exportReport(JasperPrint print, ByteArrayOutputStream byteOut)
			throws JRException {
		JasperExportManager.exportReportToPdfStream(print, byteOut);
	}

}

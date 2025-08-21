package jp.co.kpscorp.rc6.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

@Component
public class PdfExportReporter implements ExportReporter {

	public void exportReport(JasperPrint print, ByteArrayOutputStream byteOut)
			throws JRException {
		JasperExportManager.exportReportToPdfStream(print, byteOut);
	}

}

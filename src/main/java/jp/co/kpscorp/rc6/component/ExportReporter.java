package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayOutputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

public interface ExportReporter {
	public void exportReport(JasperPrint print, ByteArrayOutputStream byteOut)
			throws JRException;
}

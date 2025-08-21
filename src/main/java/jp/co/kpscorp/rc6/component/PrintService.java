package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import com.sforce.soap.partner.PartnerConnection;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kpscorp.rc6.service.ExportReporter;
import jp.co.kpscorp.rc6.service.PrepareResponse;
import jp.co.kpscorp.rc6.service.PrintSource;

public interface PrintService {

	public ByteArrayOutputStream documentEdit(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws Exception;

	public void prepareResponse(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws IOException;

	public void setConnection(PartnerConnection connection);

	public void setRequest(HttpServletRequest request);

	public void setRespons(HttpServletResponse respons);

	public void setFileName(String fileName);

	public void setXmlPath(String xmlPath);

	public void setExportReporter(ExportReporter exportReporter);

	public void setPrepareResponse(PrepareResponse prepareResponse);

	public void setLocale(Locale locale);

}
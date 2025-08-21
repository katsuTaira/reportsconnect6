package jp.co.kpscorp.rc6.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jp.co.kpscorp.rc6.model.RcPmap;

@Component
@Scope("request")
public class PrintSource<T> {
	// test

	// public PrintSource() {
	// super();
	// }

	private RcPmap rcPmap;

	public PrintSource(RcPmap rcPmap) {
		super();
		this.rcPmap = rcPmap;
		this.parmMap = rcPmap.pmap;
	}

	private Map<String, String> parmMap;

	private List<T> beanList;

	private String fileName = "print";

	private String xmlPath;

	private String subMessage;

	private long mapsize = 0;

	public long getMapsize() {
		return mapsize;
	}

	public void setMapsize(long mapsize) {
		this.mapsize = mapsize;
	}

	public String getXmlPath() {
		return xmlPath;
	}

	public void setXmlPath(String xmlPath) {
		this.xmlPath = xmlPath;
		System.out.println(xmlPath + "xyx");
	}

	public List<T> getBeanList() {
		return beanList;
	}

	public void setBeanList(List<T> beanList) {
		this.beanList = beanList;
	}

	public void setData(String key, String value) {
		getParmMap().put(key, value);
	}

	public String getData(String key) {
		return getParmMap().get(key);
	}

	public Map<String, String> getParmMap() {
		if (this.parmMap == null) {
			this.parmMap = this.rcPmap.pmap;
		}
		return parmMap;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSubMessage() {
		return subMessage;
	}

	public void setSubMessage(String subMessage) {
		this.subMessage = subMessage;
	}
}

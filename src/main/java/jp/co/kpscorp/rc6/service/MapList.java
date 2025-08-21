package jp.co.kpscorp.rc6.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jp.co.kpscorp.rc6.component.PrintServiceMapSuper;

@Service
@Scope("prototype")
public class MapList extends PrintServiceMapSuper {
	// @Autowired
	// private SfdcConnectImpl sfdcConnectImpl;
	@Autowired
	private MapServiceRest mapServiceRest;

	@Override
	public ByteArrayOutputStream documentEdit(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws Exception {
		System.out.println("ql:" + ql);
		List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
		// ModelFactory(source.getParmMap())
		// .queryToMap(connection, ql);
		// List<Map<String, ?>> maps = ModelFactory.getMapService(
		// source.getParmMap(), source).queryToMap(connection, ql);
		// List<Map<String, ?>> maps = sfdcConnectImpl.doQuery(ql);
		System.out.println("record count:" + maps.size());
		// System.out.println("Model Maps:" + maps);
		source.setBeanList(maps);
		return super.documentEdit(byteOut, source);
	}
}

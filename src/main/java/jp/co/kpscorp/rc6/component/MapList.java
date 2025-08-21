package jp.co.kpscorp.rc6.component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class MapList extends PrintServiceMapSuper {
	// @Autowired
	// private SfdcConnectImpl sfdcConnectImpl;
	@Autowired
	private MapService mapServiceRest;

	@Override
	public ByteArrayOutputStream documentEdit(ByteArrayOutputStream byteOut,
			PrintSource<Map<String, ?>> source) throws Exception {
		System.out.println("ql:" + ql);
		List<Map<String, ?>> maps = mapServiceRest.queryToMap(ql);
		long mapsize = mapServiceRest.getMapsize();
		source.setMapsize(source.getMapsize() + mapsize); //
		if (mapsize > 1000) {
			System.out.println("Map size added:" + mapsize / 1000 + "kb");
		} else {
			System.out.println("Map size added:" + mapsize + "byte");
		}
		// ModelFactory(source.getParmMap())
		// .queryToMap(connection, ql);
		// List<Map<String, ?>> maps = ModelFactory.getMapService(
		// source.getParmMap(), source).queryToMap(connection, ql);
		// List<Map<String, ?>> maps = sfdcConnectImpl.doQuery(ql);
		System.out.println("record count:" + maps.size());
		// System.out.println("Model Maps:" + maps);
		source.setBeanList(maps);
		// sourceにmapServiceRest
		return super.documentEdit(byteOut, source);
	}
}

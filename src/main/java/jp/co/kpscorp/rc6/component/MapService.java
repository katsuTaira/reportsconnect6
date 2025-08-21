package jp.co.kpscorp.rc6.component;

import java.util.List;
import java.util.Map;

public interface MapService {

    List<Map<String, ?>> queryToMap(String ql) throws Exception;

    long getMapsize();

    public void setMapsize(long mapsize);

}

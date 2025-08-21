package jp.co.kpscorp.rc6.service;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jp.co.kpscorp.rc6.model.RcPmap;

@Component
@Scope("request")
public class RcPmapImpl implements RcPmap {
    private Map<String, String> pmap;

    @Override
    public Map<String, String> getPmap() {
        return pmap;
    }

    @Override
    public void setPmap(Map<String, String> pmap) {
        this.pmap = pmap;
    }

}

package jp.co.kpscorp.rc6.model;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class RcPmap {
    public Map<String, String> pmap;

}

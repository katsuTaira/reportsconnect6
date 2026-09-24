package jp.co.kpscorp.rc6.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MapListFactory {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PdfExportReporter pdfExport;

    @Autowired
    private PdfResponse pdfRes;

    @Autowired
    private PdfInlineResponse pdfInlineRes;

    private final Map<String, Supplier<PrintService>> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        // ここで名前と構築ロジックを登録
        registry.put("MapList", () -> {
            PrintService m = context.getBean(PrintService.class);
            m.setFileName("print");
            m.setExportReporter(pdfExport);
            m.setPrepareResponse(pdfRes);
            return m;
        });

        registry.put("MapListInline", () -> {
            PrintService m = context.getBean(MapList.class);
            m.setFileName("print");
            m.setExportReporter(pdfExport);
            m.setPrepareResponse(pdfInlineRes);
            return m;
        });
    }

    public PrintService create(String name) {
        Supplier<PrintService> creator = registry.get(name);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown PrintService config: " + name);
        }
        return creator.get();
    }

    // メニュー追加用
    public void register(String name, Supplier<PrintService> builder) {
        registry.put(name, builder);
    }

    public Set<String> listAvailableConfigs() {
        return registry.keySet();
    }
}

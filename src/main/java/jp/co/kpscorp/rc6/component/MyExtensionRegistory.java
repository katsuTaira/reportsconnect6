package jp.co.kpscorp.rc6.component;

import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.fonts.FontExtensionsRegistry;
import net.sf.jasperreports.extensions.DefaultExtensionsRegistry;
import net.sf.jasperreports.extensions.ExtensionsRegistry;

public class MyExtensionRegistory extends DefaultExtensionsRegistry {
    private FontExtensionsRegistry fontExtensionsRegistry;

    public MyExtensionRegistory(String xmlFilePath) {
        super();
        List<String> fontFamiliesLocations = new ArrayList<>();
        fontFamiliesLocations.add(xmlFilePath);
        fontExtensionsRegistry = new FontExtensionsRegistry(
                fontFamiliesLocations);
    }

    /**
     * ExtensionsRegistryの初期化
     * 
     * FontExtensionsRegistryをxmlFilePathを読むものに置き換える
     * 
     * @return ExtensionsRegistryのリスト
     */
    @Override
    protected List<ExtensionsRegistry> loadRegistries() {
        List<ExtensionsRegistry> registries = super.loadRegistries();
        // FontExtensionsRegistryを入れ替える
        FontExtensionsRegistry oldFontRegistry = null;
        for (ExtensionsRegistry registry : registries) {
            if (registry instanceof FontExtensionsRegistry) {
                oldFontRegistry = (FontExtensionsRegistry) registry;
                break;
            }
        }
        if (oldFontRegistry != null) {
            registries.remove(oldFontRegistry);
        }
        registries.add(fontExtensionsRegistry);
        return registries;
    }

}

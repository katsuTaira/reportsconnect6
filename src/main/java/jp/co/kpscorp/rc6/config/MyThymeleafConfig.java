package jp.co.kpscorp.rc6.config;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * 開発環境で使用するテンプレートを読み込むTemplateResolverを設定する
 * 複数設定する場合はConfigで設定する必要がある
 * templateのソースの修正が即時に反映されるようにするにはソースのパスをfile:でアクセスするようにする
 * 
 */
@Profile("dev") // devプロファイルでのみ有効 launch.configで"vmArgs":
                // "-Dspring.profiles.active=dev"によりdevプロファイルを有効にする
@Configuration
public class MyThymeleafConfig {
    static Logger logger = Logger.getLogger(MyThymeleafConfig.class);

    @Autowired
    private Environment env;

    private String devTemplatePath;
    private String scon2TemplatePath;

    /**
     * 開発環境で使用するテンプレートを読み込むTemplateResolver
     * <ul>
     * <li>prefix: src/main/resources/templates/</li>
     * <li>suffix: .html</li>
     * <li>templateMode: HTML</li>
     * <li>characterEncoding: UTF-8</li>
     * <li>order: 1 (優先度)</li>
     * <li>checkExistence: true (存在しないファイルはスキップ)</li>
     * <li>cacheable: false (キャッシュを使用しない)</li>
     * </ul>
     */
    @Bean
    public SpringResourceTemplateResolver appTemplateResolver() {
        // application.propertiesから取得
        devTemplatePath = env.getProperty("dev.template.path");
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix(devTemplatePath);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1); // 優先度を指定
        resolver.setCheckExistence(true); // 存在しないファイルはスキップ
        resolver.setCacheable(false); // これが必要！
        logger.info("✔ DevTemplateResolver1 registered:" + devTemplatePath);
        return resolver;
    }

    /**
     * SCON2用のテンプレートを読み込むTemplateResolver
     * <ul>
     * <li>prefix: zscon2resorces/templates/</li>
     * <li>suffix: .html</li>
     * <li>templateMode: HTML</li>
     * <li>characterEncoding: UTF-8</li>
     * <li>order: 2 (優先度)</li>
     * <li>checkExistence: true (存在しないファイルはスキップ)</li>
     * <li>cacheable: false (キャッシュを使用しない)</li>
     * </ul>
     */
    @Bean
    public SpringResourceTemplateResolver sconTemplateResolver() {
        // application.propertiesから取得
        scon2TemplatePath = env.getProperty("scon2.template.path");
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix(scon2TemplatePath);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(2); // 優先度を低く
        resolver.setCheckExistence(true);
        resolver.setCacheable(false); // これが必要！
        logger.info("✔ SCON2TemplateResolver registered:" + scon2TemplatePath);
        return resolver;
    }

    /**
     * SCON2用のテンプレートエンジンを生成
     * <ul>
     * <li>appTemplateResolver: src/main/resources/templates/</li>
     * <li>sconTemplateResolver: zscon2resorces/templates/</li>
     * <li>sconDialect: SCON2用のdialect</li>
     * </ul>
     */
    @Bean
    public SpringTemplateEngine templateEngine(
            SpringResourceTemplateResolver appTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(appTemplateResolver);
        // engine.addTemplateResolver(sconTemplateResolver);
        // engine.addDialect(sconDialect);
        return engine;
    }
}

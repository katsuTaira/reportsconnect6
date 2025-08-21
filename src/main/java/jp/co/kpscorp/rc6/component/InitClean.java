package jp.co.kpscorp.rc6.component;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;

@Component
public class InitClean implements ApplicationRunner {
    @Autowired
    private ServletContext servletContext;
    Logger logger = Logger.getLogger(InitClean.class);

    /**
     * Spring Bootのアプリケーション起動直後 (ApplicationRunner) に実行される.
     * /jasper/ 以下のファイル・ディレクトリを削除する.
     * 
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        File f = new File(servletContext.getRealPath("/jasper/"));
        if (f.exists() && f.isDirectory()) {
            // ディレクトリが存在する場合は中身を再帰的に削除
            for (File c : f.listFiles()) {
                if (c.isDirectory()) {
                    org.apache.commons.io.FileUtils.deleteDirectory(c);
                } else {
                    c.delete();
                }
                logger.info(c.getName() + " deleted.");

            }
        }
    }

}

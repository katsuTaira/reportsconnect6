package jp.co.kpscorp.rc6.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

@Scope("prototype")
@Controller
public class CommonController {
    private Logger logger = Logger.getLogger(CommonController.class);

    /**
     * カスタム画面を表示
     * 
     * @param req
     *                HTTPリクエスト
     * @param key
     *                orgid
     * @param jspname
     *                jspファイル名
     * @return HTMLファイル
     * @throws IOException
     *                     IOエラー
     */
    @RequestMapping("/custom")
    @ResponseBody
    public ResponseEntity<Resource> showHtml(HttpServletRequest req) throws IOException {
        System.out.println("custom called");
        String key = req.getParameter("key");
        String jspname = req.getParameter("jspname");

        ServletContext context = req.getSession().getServletContext();
        String path = "/jasper/" + key + "/" + jspname;
        String fPath = context.getRealPath(path);

        File file = new File(fPath);
        if (file.exists() == false) {
            logger.error("404 File not found: " + fPath);
            // ファイルが存在しない場合は404エラーを返す
            return ResponseEntity.notFound().build();
        }
        if (file.isDirectory()) {
            logger.error("Requested path is a directory: " + fPath);
            // ディレクトリが指定された場合は404エラーを返す
            return ResponseEntity.notFound().build();
        }
        Resource resource;
        if (jspname.toLowerCase().startsWith("dlerror") || jspname.toLowerCase().startsWith("dlnodata")) {
            // エラー画面の場合 cleanUpも行う
            // String folderPath = context.getRealPath("/jasper/" + key);
            // 自動削除用に InputStream にラップ
            InputStream inputStream = new FileInputStream(file) {
                @Override
                public void close() throws IOException {
                    super.close();
                    Utils.cleanUp(key, context); // ストリームが閉じられたら削除
                }
            };
            resource = new InputStreamResource(inputStream);
        } else {
            resource = new FileSystemResource(file);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    @RequestMapping("/views/{key}")
    public String goToViews(@PathVariable String key) {
        return "views/" + key;
    }

}

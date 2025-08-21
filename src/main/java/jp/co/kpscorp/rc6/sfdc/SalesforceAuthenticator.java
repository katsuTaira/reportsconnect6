package jp.co.kpscorp.rc6.sfdc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import jp.co.kpscorp.rc6.component.Utils;

public class SalesforceAuthenticator {

    /**
     * Salesforce にログインし、結果のjsonを取得します。
     *
     * @param username     Salesforce のユーザーID（メールアドレス）
     * @param password     パスワード + セキュリティトークンを結合した文字列
     * @param clientId     Connected App のクライアントID
     * @param clientSecret Connected App のクライアントシークレット
     * @param tokenUrl     トークン取得エンドポイント（例:
     *                     https://login.salesforce.com/services/oauth2/token）
     * @return 結果のjson
     */
    public static String getLoginRes(String username, String password,
            String clientId, String clientSecret,
            String tokenUrl) {

        try {
            // パラメータを URL エンコードしてフォーム形式に
            String params = "grant_type=password"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

            URL url = new URL(tokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            if (tokenUrl.contains("loginproxy")) {
                Utils.ignoreValidateCertification((HttpsURLConnection) conn);
                conn.setRequestProperty(Utils.proxpassKey, Utils.proxpass);
                conn.setRequestProperty("Referer", "kps");
            }

            // パラメータ送信
            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes(params);
                out.flush();
            }

            int status = conn.getResponseCode();
            BufferedReader reader;
            if (status == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            // レスポンス読み取り
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (status == 200) {
                // JSONParser parser = new JSONParser();
                // JSONObject json = (JSONObject) parser.parse(response.toString());
                // return (String) json.get("access_token");
                return response.toString();
            } else {
                System.err.println("Login failed: " + status);
                System.err.println("Response: " + response);
                return response.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

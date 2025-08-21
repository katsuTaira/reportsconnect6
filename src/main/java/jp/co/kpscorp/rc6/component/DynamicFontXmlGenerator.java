package jp.co.kpscorp.rc6.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DynamicFontXmlGenerator {
  // 元テンプレート（ここに%eudcPath を差し込む）
  private static final String FONT_XML_TEMPLATE = """
      <fontFamilies>
        <fontFamily name="_IPAmj明朝">
          <normal><![CDATA[fonts_local/IPAmjMincho.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
        </fontFamily>
        <fontFamily name="yubinbcd">
          <normal><![CDATA[fonts_local/yubinbcd.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
        </fontFamily>
        <fontFamily name="_IPAexゴシック">
          <normal><![CDATA[fonts_local/IPAexGothic.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
        </fontFamily>
        <fontFamily name="_IPAex明朝">
          <normal><![CDATA[fonts_local/IPAexMincho.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
        </fontFamily>
        <fontFamily name="_IPAゴシック">
          <normal><![CDATA[fonts_local/IPAGothic.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
        </fontFamily>
        <fontFamily name="_IPA明朝">
          <normal><![CDATA[fonts_local/IPAMincho.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
        </fontFamily>
        <fontFamily name="OCRB">
          <normal><![CDATA[fonts_local/OCRB.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
        </fontFamily>
        <fontFamily name="Times-Roman">
          <normal><![CDATA[fonts_local/times-roman.ttf]]></normal>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
        </fontFamily>
        <fontFamily name="_EUDC">
          <normal><![CDATA[%eudcPath]]></normal>
          <pdfEncoding><![CDATA[Identity-H]]></pdfEncoding>
          <pdfEmbedded><![CDATA[true]]></pdfEmbedded>
        </fontFamily>
        <fontSet name="IPAmj明朝">
          <family familyName="_EUDC" primary="true" />
          <family familyName="_IPAmj明朝" />
        </fontSet>
        <fontSet name="IPAexゴシック">
          <family familyName="_EUDC" primary="true" />
          <family familyName="_IPAexゴシック" />
        </fontSet>
        <fontSet name="IPAex明朝">
          <family familyName="_EUDC" primary="true" />
          <family familyName="_IPAex明朝" />
        </fontSet>
        <fontSet name="IPAゴシック">
          <family familyName="_EUDC" primary="true" />
          <family familyName="_IPAゴシック" />
        </fontSet>
        <fontSet name="IPA明朝">
          <family familyName="_EUDC" primary="true" />
          <family familyName="_IPA明朝" />
        </fontSet>
      </fontFamilies>
            """;

  /**
   * fonts.xml を生成する
   * 
   * @param eudcPath   EUDC.ttf の絶対パス
   * @param outputPath 出力先 fonts.xml のパス
   */
  public static void generateFontXml(String eudcPath, Path outputPath) throws IOException {
    String xml = FONT_XML_TEMPLATE
        .replace("%eudcPath", eudcPath);

    Files.writeString(outputPath, xml, StandardCharsets.UTF_8);
    System.out.println("fonts.xml を生成しました: " + outputPath);
  }
}

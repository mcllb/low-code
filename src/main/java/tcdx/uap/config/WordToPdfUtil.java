package tcdx.uap.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.WordUtils;
import org.docx4j.Docx4J;
import org.docx4j.fonts.FontUtils;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public class WordToPdfUtil {

    /**
     * DOCX转PDF工具
     */
    public static void convertDocxToPdf(InputStream inputStream, HttpServletResponse response, String fileName) throws Exception {
        response.setContentType("application/pdf");
        String fullName = new String(fileName.getBytes(), StandardCharsets.ISO_8859_1);
        response.setHeader("Content-Disposition", "attachment; filename=" + fullName + ".pdf");
        WordprocessingMLPackage wmlPackage = WordprocessingMLPackage.load(inputStream);
        setFontMapper(wmlPackage);
        Docx4J.toPDF(wmlPackage, response.getOutputStream());
    }

    private static void setFontMapper(WordprocessingMLPackage mlPackage) throws Exception {
        URL fontUrl = WordUtils.class.getClassLoader().getResource("fonts/SimSun.ttf");
        if (fontUrl == null) {
            throw new FileNotFoundException("字体文件未找到，请检查 resources/fonts/SimSun.ttf 是否存在");
        }
        PhysicalFonts.addPhysicalFonts("SimSun", fontUrl.toURI());
        // 设置字体映射
        Mapper fontMapper = new IdentityPlusMapper();
        log.info("获取到字体:{}", PhysicalFonts.get("SimSun"));
        fontMapper.put("宋体", PhysicalFonts.get("SimSun"));  // Word文档中的字体名
        fontMapper.put("微软雅黑", PhysicalFonts.get("SimSun"));
        fontMapper.put("黑体", PhysicalFonts.get("SimSun"));
        fontMapper.put("楷体", PhysicalFonts.get("SimSun"));
        fontMapper.put("仿宋", PhysicalFonts.get("SimSun"));
        fontMapper.put("幼圆", PhysicalFonts.get("SimSun"));
        fontMapper.put("Arial", PhysicalFonts.get("SimSun"));
        mlPackage.setFontMapper(fontMapper);
    }
}

package tcdx.uap.config;

import java.io.File;

public class LibreOfficeUtil {
    // 获取LibreOffice的soffice路径
    private static String getOfficePath() {
        // Windows下路径示例
         return "C:\\Program Files\\LibreOffice\\program\\soffice.exe";
    }

    // 转换文档为PDF
    public static void convertToPDF(String inputPath, String outputPath) throws Exception {
        String officePath = getOfficePath();

        // 启动LibreOffice服务
        String[] cmd = {officePath, "--headless", "--convert-to", "pdf", inputPath, "--outdir", outputPath};
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();

        // 检查转换是否成功
        File outputFile = new File(outputPath, new File(inputPath).getName().replaceAll("\\.\\w+$", ".pdf"));
        if (!outputFile.exists()) {
            throw new RuntimeException("PDF转换失败");
        }
    }
}

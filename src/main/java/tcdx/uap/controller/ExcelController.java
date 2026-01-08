package tcdx.uap.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.JpegOptions;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.val;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.github.pagehelper.PageInfo;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.entity.page.TableDataInfo;
import tcdx.uap.common.utils.*;
import tcdx.uap.config.UEditorConfig;
import tcdx.uap.config.WordToPdfUtil;
import tcdx.uap.mapper.BusinessMapper;
import tcdx.uap.service.*;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.Modules;
import tcdx.uap.service.vo.QRCodeReq;


import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/uap/excel")
public class ExcelController {
    private static final Logger logger = LoggerFactory.getLogger(ExcelController.class);

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private ServiceConfigService serviceConfigService;

    private static final String ROOT_PATH = "C:" + File.separator + "files";
    //    private static final String ROOT_PATH = "/home/qwq/uploads";
    @Autowired
    private BaseDBService baseDBService;
    @Autowired
    private ExcelService excelService;
    @Autowired
    private CommonDBService commonDBService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private WebDavService webDavService;
    private CompDataSourceField col;

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String originFilename = file.getOriginalFilename();
        if (originFilename == null || originFilename.trim().isEmpty()) {
            originFilename = "file.bin";
        }
        String mainName = cn.hutool.core.io.FileUtil.mainName(originFilename);
        String extName = cn.hutool.core.io.FileUtil.extName(originFilename);
        String extLower = extName == null ? "" : extName.toLowerCase(Locale.ROOT);

        // 远端目录：/home/cesc/upload/yyyy-MM-dd
        String yyyyMMdd = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String remoteDir = webDavService.getBasePath() + "/upload/" + yyyyMMdd;
        webDavService.ensureDirs(remoteDir);  // 逐级 MKCOL，存在会忽略

        // 为避免重名覆盖，统一加时间戳
        String ts = new java.text.SimpleDateFormat("HHmmssSSS").format(new java.util.Date());
        String baseName = mainName + "_" + ts;

        // 先把文件读入内存（便于复用/生成缩略图）
        byte[] originalBytes = file.getBytes();

        // 1) 原图直传
        String originalContentType = file.getContentType();
        if (originalContentType == null) {
            originalContentType = contentTypeOf(extLower);
        }
        String originalRemotePath = remoteDir + "/" + baseName + "." + (extLower.isEmpty() ? "bin" : extLower);
        String originalUrl = webDavService.put(
                originalRemotePath,
                new java.io.ByteArrayInputStream(originalBytes),
                originalBytes.length,
                originalContentType
        );

        String midUrl = null, minUrl = null;

        // 2) 图片则额外生成中/小图
        if (isImageFile(extLower)) {
            // 中图 500x500（保持比例）
            byte[] midBytes = resizeImage(originalBytes, 500, 500, extLower);
            String midRemotePath = remoteDir + "/" + baseName + "_medium." + normalizeExtForThumb(extLower);
            midUrl = webDavService.put(
                    midRemotePath,
                    new java.io.ByteArrayInputStream(midBytes),
                    midBytes.length,
                    contentTypeOf(extLower)
            );

            // 小图 100x100（保持比例）
            byte[] minBytes = resizeImage(originalBytes, 100, 100, extLower);
            String minRemotePath = remoteDir + "/" + baseName + "_min." + normalizeExtForThumb(extLower);
            minUrl = webDavService.put(
                    minRemotePath,
                    new java.io.ByteArrayInputStream(minBytes),
                    minBytes.length,
                    contentTypeOf(extLower)
            );

            // 入库：三份
            baseDBService.insertMap("v_file", Lutils.genMap(
                    "display_name", originFilename,
                    "size", file.getSize(),
                    "max_file_code", originalUrl,
                    "mid_file_code", midUrl,
                    "min_file_code", minUrl,
                    "upload_time", new java.util.Date()
            ));
        } else {
            // 非图片只存原图
            baseDBService.insertMap("v_file", Lutils.genMap(
                    "display_name", originFilename,
                    "size", file.getSize(),
                    "max_file_code", originalUrl,
                    "upload_time", new java.util.Date()
            ));
        }

        // 返回给前端
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("display_name", originFilename);
        map.put("url", originalUrl);
        if (midUrl != null) map.put("mid_url", midUrl);
        if (minUrl != null) map.put("min_url", minUrl);
        return map;
    }

    private static byte[] resizeImage(byte[] src, int w, int h, String ext) throws IOException {
        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            net.coobird.thumbnailator.Thumbnails.of(new java.io.ByteArrayInputStream(src))
                    .size(w, h)                                   // 按最长边等比缩放到不超过 w/h
                    .outputFormat(normalizeExtForThumb(ext))      // 与原扩展匹配（jpeg 归一到 jpg）
                    .toOutputStream(bos);
            return bos.toByteArray();
        }
    }


    @PostMapping("/exportMoBan")
    @ResponseBody
    public void exportMoBan(@RequestBody Map map, HttpServletResponse response) throws Exception {
        String table_id = map.get("table_id").toString();

        Table table = (Table) Modules.getInstance().get(table_id, false);
        List<TableCol> cols = table.cols;

        List<List<String>> headList = new ArrayList();
        List<List<String>> dataList = new ArrayList();

        List<String> items = new ArrayList<>();
        List<String> items2 = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            TableCol col = cols.get(i);
            headList.add(new ArrayList() {{
                add(col.name);
            }});
            items.add(col.field);
            items2.add(col.data_type);

        }
        dataList.add(items);
        dataList.add(items2);


        //List<List<String>> list = dataList();
        // 头的策略
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // 背景设置为蓝色
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        // 这个策略是 头是头的样式 内容是内容的样式 其他的策略可以自己实现
        HorizontalCellStyleStrategy horizontalCellStyleStrategy =
                new HorizontalCellStyleStrategy(headWriteCellStyle, new WriteCellStyle());

        WriteFont headWriteFont = new WriteFont();
        //headWriteFont.setFontHeightInPoints((short)16);
        headWriteCellStyle.setWriteFont(headWriteFont);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("模板文件", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream())
                //加载样式
                .registerWriteHandler(horizontalCellStyleStrategy)
                //自动列宽
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 这里放入动态头
                .head(headList).sheet("demo")
                // 当然这里数据也可以用 List<List<String>> 去传入
                .doWrite(dataList);
    }


    @PostMapping("/upload1")
    @ResponseBody
    public Map upload1(MultipartFile file) throws IOException {
        String originFilename = file.getOriginalFilename();
        String mainName = FileUtil.mainName(originFilename);
        String extName = FileUtil.extName(originFilename);
        if (!FileUtil.exist(ROOT_PATH)) {
            FileUtil.mkdir(ROOT_PATH);
        }
        if (FileUtil.exist(ROOT_PATH + File.separator + originFilename)) {
            originFilename = System.currentTimeMillis() + "_" + mainName + "." + extName;
        }
        File saveFile = new File(ROOT_PATH + File.separator + File.separator + originFilename);
        file.transferTo(saveFile);
        String url = ROOT_PATH + originFilename;
        Map<String, Object> map =
                Lutils.genMap("display_name",
                        originFilename, "url", url);
        return map;
    }

    @PostMapping("/upload2")
    @ResponseBody
    public Map<String, Object> upload2(MultipartFile file) throws IOException {
        String originFilename = file.getOriginalFilename();
        String mainName = FileUtil.mainName(originFilename);  // 获取文件主名称
        String extName = FileUtil.extName(originFilename);  // 获取文件扩展名

        long fileSize = file.getSize();  // 获取文件的大小（单位：字节）

        // 创建目录
        if (!FileUtil.exist(ROOT_PATH)) {
            FileUtil.mkdir(ROOT_PATH);
        }

        // 判断文件是否已存在，若存在则加上当前时间戳
        if (FileUtil.exist(ROOT_PATH + File.separator + originFilename)) {
            originFilename = System.currentTimeMillis() + "_" + mainName + "." + extName;
        }

        // 保存原始文件
        File originalFile = new File(ROOT_PATH + File.separator + originFilename);
        file.transferTo(originalFile);

        // 如果是图片格式，进行压缩处理
        if (isImageFile(extName)) {
            //拉取原始像素，按比例变更
            // 保存最小文件（例如缩小到100x100像素）
            File minFile = new File(ROOT_PATH + File.separator + mainName + "_min." + extName);
            Thumbnails.of(originalFile)
                    .size(100, 100)  // 设置压缩后的尺寸
                    .toFile(minFile);

            // 保存中等文件（例如缩小到500x500像素）
            File mediumFile = new File(ROOT_PATH + File.separator + mainName + "_medium." + extName);
            Thumbnails.of(originalFile)
                    .size(500, 500)  // 设置压缩后的尺寸
                    .toFile(mediumFile);

            // 插入数据库
            baseDBService.insertMap("v_file", Lutils.genMap(
                    "display_name", originFilename,
                    "size", fileSize,
                    "min_file_code", minFile.getAbsolutePath(),
                    "max_file_code", originalFile.getAbsolutePath(),
                    "mid_file_code", mediumFile.getAbsolutePath(),
                    "upload_time", new Date()
            ));
        } else {
            // 如果不是图片格式，直接插入数据库
            baseDBService.insertMap("v_file", Lutils.genMap(
                    "display_name", originFilename,
                    "size", fileSize,
                    "max_file_code", originalFile.getAbsolutePath(),
                    "upload_time", new Date()
            ));
        }

        // 返回文件路径和信息
        String url = originalFile.getAbsolutePath();
        Map<String, Object> map = Lutils.genMap("display_name", originFilename, "url", url);
        return map;
    }

    // 判断文件是否为图片格式
    private boolean isImageFile(String extName) {
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "tiff"};
        for (String ext : imageExtensions) {
            if (ext.equalsIgnoreCase(extName)) {
                return true;
            }
        }
        return false;
    }

    @PostMapping("/downloadByUrl")
    public void downloadByUrl(@RequestBody DownloadRequest req, HttpServletResponse response) throws IOException {
        String urlOrPath = req.getUrl();
        String targetPath;

        // 处理URL或路径
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            targetPath = urlOrPath.replaceFirst("^https?://[^/]+", "");
        } else {
            targetPath = urlOrPath.startsWith("/") ? urlOrPath : ("/" + urlOrPath);
        }

        String pathPart = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        String filename = decodeFileName(pathPart);

        response.setHeader("Content-Disposition",
                (req.isDownload() ? "attachment" : "inline") + "; filename=" + URLEncoder.encode(filename, "UTF-8"));
        response.setContentType(determineContentType(filename));

        try (ServletOutputStream out = response.getOutputStream()) {
            webDavService.download(targetPath, out);
            out.flush();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/downloadByUrlPreview")
    public void downloadByUrlPreview(@RequestParam String url,
                                 @RequestParam(defaultValue = "false") boolean download,
                                 HttpServletResponse response) throws IOException {
        DownloadRequest req = new DownloadRequest();
        req.setUrl(url);
        req.setDownload(download);
        downloadByUrl(req, response);
    }

    // 解码文件名的辅助方法
    private String decodeFileName(String encodedName) {
        try {
            String decoded = encodedName;
            String previous;
            do {
                previous = decoded;
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
            } while (!previous.equals(decoded) && decoded.contains("%"));
            return decoded;
        } catch (Exception e) {
            return encodedName;
        }
    }

    @PostMapping("/downloadByUrl2") // 改为 POST 请求
    public void downloadByUrl2(@RequestBody DownloadRequest request, HttpServletResponse response) throws IOException {
        String url1 = request.getUrl(); // 从请求体中获取 URL
        // 处理本地路径：补充file://协议（支持本地文件）
        String url = processLocalPath(url1);
        boolean isDownload = request.isDownload(); // 可选：区分预览/下载模式

        try {
            // 验证 URL 格式
            new URL(url);

            // 设置响应头（根据需求选择预览或下载）
            String disposition = isDownload ? "attachment" : "inline";
            String filename = url.substring(url.lastIndexOf("/") + 1);
            response.addHeader("Content-Disposition",
                    String.format("%s;filename=%s", disposition, URLEncoder.encode(filename, "UTF-8")));

            // 从 URL 读取文件流并写入响应
            try (InputStream inputStream = new URL(url).openStream();
                 ServletOutputStream outputStream = response.getOutputStream()) {

                response.setContentType("application/octet-stream");
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (MalformedURLException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的 URL 格式");
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件下载失败");
        }
    }

    // 请求体参数类
    // 添加 static 关键字
    static class DownloadRequest {
        private String url;
        private boolean download;

        // 无参构造函数（Jackson必需）
        public DownloadRequest() {
        }

        // Getter 和 Setter
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isDownload() {
            return download;
        }

        public void setDownload(boolean download) {
            this.download = download;
        }
    }

    //导入功能
    @PostMapping(value = "/importExcel", produces = {"text/html;charset=UTF-8;", "application/json;"})
    @ResponseBody
    public String importExcel(MultipartFile file, String table_id, HttpSession session) throws IOException {
        try {
            excelService.importExcel(file, table_id, session);
            return "导入成功";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return e.getMessage();
        }
    }

    //getImportInfoList 获取导入但并未提交的数据清单
    @PostMapping(value = "/getImportInfoList", produces = {"text/html;charset=UTF-8;", "application/json;"})
    @ResponseBody
    public TableDataInfo getImportInfoList(@RequestBody Map map) throws IOException {
        String table_id = map.get("table_id").toString();

        String table_name = "z_" + table_id;
        List<Map> list = baseDBService.selectSql("select distinct a.create_time_,b.staff_nm as create_user_ \n" +
                "from " + table_name + " a " +
                "left join v_user b on a.create_user_ = b.id " +
                "where a.create_time_ is not null and a.time_if_delete_ is null order by a.create_time_ desc");

        for (int i = 0; i < list.size(); i++) {
            Map map1 = list.get(i);
            String create_time_ = map1.get("create_time_").toString();
            map1.put("create_time_", create_time_);
        }

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }


    @PostMapping("/select_tableview_collist")
    @ResponseBody
    public AjaxResult select_tableview_collist(@RequestBody Map<String, Object> map) {
        Map re = new HashMap<String, Object>();
        String table_id = map.get("table_id").toString();
        Table table = (Table) Modules.getInstance().get(table_id, false);
        List<TableCol> cols = table.cols;
        re.put("tableview_columns", cols);
        return AjaxResult.success("suc", re);
    }


    //getImportInfoList 获取导入但并未提交的数据清单
    @PostMapping(value = "/getImportDetailListByComoleteTime", produces = {"text/html;charset=UTF-8;", "application/json;"})
    @ResponseBody
    public TableDataInfo getImportDetailListByComoleteTime(@RequestBody Map map) throws Exception {
        String table_id = map.get("table_id").toString();
        Object create_time_ = map.get("create_time_");
        Object pageNum = map.get("pageNum");
        Object pageSize = map.get("pageSize");
        Table table = (Table) Modules.getInstance().get(table_id, false);
        String table_name = table.table_name;
        SimpleDateFormat time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        if (map.get("pageNum") != null && map.get("pageSize") != null)
            PageUtils.startPage(map);
        List<Map> list = baseDBService.selectSql("" +
                "select * from " + table_name + " where \n" +
                "create_time_ =  to_timestamp('" + create_time_ + "','YYYY-MM-DD HH24:MI:SS.MS')");
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    @PostMapping("/ueditor-config")
    @ResponseBody
    public ResponseEntity<?> handleUEditorRequest(
            @RequestParam("action") String action,
            @RequestParam(value = "upfile", required = false) MultipartFile file) {

        switch (action) {
            case "uploadimage":
                return uploadImage(file);
            case "config":
                return getConfig();
            default:
                return ResponseEntity.badRequest().body("Unsupported action");
        }
    }

    private ResponseEntity<?> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.error("上传失败，文件为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file uploaded");
        }
        try {
            // 文件处理逻辑
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File targetFile = new File(ROOT_PATH, fileName);
            file.transferTo(targetFile);

            Map<String, Object> result = new HashMap<>();
            result.put("state", "SUCCESS");
            result.put("url", "/uap/excel/path/to/image/" + fileName);
            result.put("title", fileName);
            result.put("original", file.getOriginalFilename());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("上传图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
        }
    }

    private ResponseEntity<?> getConfig() {
        // 返回UEditor的配置
        String jsonResponse = UEditorConfig.UEDITOR_CONFIG;
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(jsonResponse);
    }

    @GetMapping("/path/to/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(ROOT_PATH, filename);
            Resource file = new UrlResource(filePath.toUri());
            if (file.exists() || file.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                        .contentType(MediaType.IMAGE_JPEG)  // 或者根据实际文件类型设置
                        .body(file);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    /**
     * 获取ueditor参数
     */
    @RequestMapping(value = "/ueditor-config", method = RequestMethod.GET)
    public ResponseEntity<String> ueditorGet(@RequestParam(required = false) String callback) {
        String jsonResponse = UEditorConfig.UEDITOR_CONFIG;
        if (callback != null && !callback.isEmpty()) {
            jsonResponse = callback + "(" + jsonResponse + ");";
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/javascript"))
                    .body(jsonResponse);
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(jsonResponse);
        }
    }

    private boolean isFileField(List<Map<String, Object>> dataList, String fieldName) {
        if (dataList == null || dataList.isEmpty()) {
            return false;
        }

        // 检查前几条数据即可
        int checkCount = Math.min(dataList.size(), 5);
        for (int i = 0; i < checkCount; i++) {
            Map<String, Object> data = dataList.get(i);
            Object fieldValue = data.get(fieldName);

            if (fieldValue instanceof String) {
                String strValue = (String) fieldValue;
                // 判断是否符合文件JSON数组格式：以[开头，以]结尾，包含name和url字段
                if (strValue.startsWith("[") && strValue.endsWith("]")) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        List<Map<String, Object>> fileList = objectMapper.readValue(strValue, List.class);

                        if (!fileList.isEmpty()) {
                            Map<String, Object> firstFile = fileList.get(0);
                            if (firstFile.containsKey("name") && firstFile.containsKey("url")) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // 解析失败，说明不是有效的文件JSON格式
                        continue;
                    }
                }
            }
        }
        return false;
    }

    @RequestMapping("/getTableAllData")
    @ResponseBody
    public List<?> getTableAllData(@RequestBody Map map, HttpSession httpSession) throws Exception {
        String ds_id = map.get("ds_id").toString();
        CompDataSource ds = (CompDataSource) Modules.getInstance().get(ds_id, true);
        map.put("pageSize",3000);
        map.put("pageNum",1);
        ExecContext execContext = new ExecContext();
        execContext.addContexts( (List<Map>) map.get("session") );
        UserAction userAction = new UserAction();
        userAction.setUserInfo(httpSession);
        TableDataInfo tableInfo = CompUtils.getInstance().get_ds_data(ds, map, execContext, userAction, true );
        return tableInfo.getRows();
    }
    @RequestMapping("/exportExcel")
    @ResponseBody
    public void exportExcel(@RequestBody Map map, HttpServletResponse response, HttpSession httpSession) throws Exception {

        String ds_id = map.get("ds_id").toString();
        /**获取数据源配置*/
        CompDataSource compDataSource = (CompDataSource) Modules.getInstance().get(ds_id, true);
        // 创建两个Map：一个按field，一个按id，方便查找
        Map<String, TableCol> colFieldMap = new HashMap<>();
        Map<String, TableCol> colIdMap = new HashMap<>();
        // 加入主表的字段
        Table mainTable = (Table) Modules.getInstance().get(compDataSource.table_id, false);
        if (Objects.nonNull(mainTable)) {
            colFieldMap = mainTable.cols.stream().filter(filed -> {
                return StringUtils.isNotEmpty(filed.field) && !filed.field.contains("_id");
            }).collect(Collectors.toMap(TableCol::getField, v -> v));
            colIdMap = mainTable.cols.stream().filter(filed -> {
                return StringUtils.isNotEmpty(filed.field) && !filed.field.contains("_id");
            }).collect(Collectors.toMap(TableCol::getId, v -> v));
        }
        // 暂时只处理自定义sql的表格
        Set<String> priTableIds = new HashSet<>(mainTable.priTableIds);

        if (Objects.equals("defined", compDataSource.data_type)) {
            Set<String> collect = compDataSource.fields.stream()
                    .map(f -> {
                        if (f == null || StringUtils.isEmpty(f.field)) return null;

                        String keyword = "table";
                        int startIndex = f.field.indexOf(keyword);
                        if (startIndex == -1) return null;

                        String remaining = f.field.substring(startIndex);

                        // 优先截到 "_id"，否则截到第一个 "_"，都没有就全取
                        int endIndex = remaining.indexOf("_id");
                        if (endIndex == -1) endIndex = remaining.indexOf('_');

                        return (endIndex != -1) ? remaining.substring(0, endIndex) : remaining;
                    })
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());

            priTableIds.addAll(collect);
        }

        // 加入外联表字段
        for (String priTableId : priTableIds) {
            Table priTable = (Table) Modules.getInstance().get(priTableId, false);
            colFieldMap.putAll(priTable.cols.stream().filter(filed -> {
                return StringUtils.isNotEmpty(filed.field) && !filed.field.contains("_id");
            }).collect(Collectors.toMap(TableCol::getField, v -> v)));
            colIdMap.putAll(priTable.cols.stream().filter(filed -> {
                return StringUtils.isNotEmpty(filed.field) && !filed.field.contains("_id");
            }).collect(Collectors.toMap(TableCol::getId, v -> v)));
        }

        // 数据源的列
        List<CompDataSourceField> dataSourceFields = compDataSource.fields;

        // 处理分页查询
        map.put("pageSize",3000);
        map.put("pageNum",1);
        ExecContext execContext = new ExecContext();
        UserAction userAction = new UserAction();
        userAction.setUserInfo(httpSession);
        TableDataInfo tableInfo = CompUtils.getInstance().get_ds_data(compDataSource, map, execContext , userAction,false);
        List<?> list = tableInfo.getRows();
        // 构建表头 - 只显示有name的字段
        List<List<String>> headList = new ArrayList();
        // 记录需要显示的字段索引
        List<Integer> displayFieldIndexes = new ArrayList<>();

        for (int i = 0; i < dataSourceFields.size(); i++) {
            CompDataSourceField col = dataSourceFields.get(i);

            if (col.field_type == "ds_total" || col.field_type == "ds_rows_length") continue;

            String headerName = null;

            // 如果有table_col_id，尝试从TableCol中获取name
            if (col.table_col_id != null) {
                TableCol tableCol = colIdMap.get(col.table_col_id);
                if (tableCol != null && tableCol.getName() != null && !tableCol.getName().trim().isEmpty()) {
                    headerName = tableCol.getName();
                }
            }
            // 如果没有table_col_id，但field不为空，尝试通过field名匹配
            else if (col.field != null) {
                TableCol tableCol = colFieldMap.get(col.field);
                if (tableCol != null && tableCol.getName() != null && !tableCol.getName().trim().isEmpty()) {
                    headerName = tableCol.getName();
                }
            }

            // 只有当找到中文name时才添加到表头
            if (headerName != null) {
                String finalHeaderName = headerName;
                headList.add(new ArrayList() {{
                    add(finalHeaderName);
                }});
                displayFieldIndexes.add(i); // 记录需要显示的字段索引
            }
        }

        // 构建数据行 - 只显示有name的字段对应的数据
        List<List<Object>> dataList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Map oneData = (Map) list.get(i);
            List<Object> items = new ArrayList<>();

            // 只遍历需要显示的字段索引
            for (Integer fieldIndex : displayFieldIndexes) {
                CompDataSourceField col = dataSourceFields.get(fieldIndex);
                if (col.field == null) {
                    continue;
                }
                String colname = col.field;
                if (oneData.get(colname) != null) {
                    String column_type = col.data_type != null ? col.data_type : "varchar";
                    if (column_type.equals("numeric")) {
                        try {
                            items.add(new BigDecimal(oneData.get(colname).toString()));
                        } catch (NumberFormatException e) {
                            items.add(oneData.get(colname).toString());
                        }
                    } else {
                        items.add(oneData.get(colname).toString());
                    }
                } else {
                    items.add(" ");
                }
            }
            dataList.add(items);
        }

        // 导出Excel（保持不变）
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        HorizontalCellStyleStrategy horizontalCellStyleStrategy =
                new HorizontalCellStyleStrategy(headWriteCellStyle, new WriteCellStyle());

        WriteFont headWriteFont = new WriteFont();
        headWriteCellStyle.setWriteFont(headWriteFont);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("模板文件", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(horizontalCellStyleStrategy)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(headList).sheet("sheet1")
                .doWrite(dataList);
    }

//    @RequestMapping("/exportExcel")
//    @ResponseBody
//    public void exportExcel(@RequestBody Map map, HttpServletResponse response, HttpSession session) throws Exception {
//
//        String ds_id = map.get("ds_id").toString();
//        /**获取数据源配置*/
//        CompDataSource compDataSource = (CompDataSource) Modules.getInstance().get(ds_id, true);
//        Table mainTable = (Table) Modules.getInstance().get(compDataSource.table_id, false);
//        Map<String, TableCol> colMap = new HashMap<>();
//        if (Objects.nonNull(mainTable)) {
//            colMap = mainTable.cols.stream().collect(Collectors.toMap(TableCol::getField, v -> v));
//        }
//        for (Map.Entry<String, TableCol> entry : colMap.entrySet()) {
//            String fieldName = entry.getKey();
//            TableCol tableCol = entry.getValue();
//            String name = tableCol.getName();
//
//            // 在这里处理每个字段
//            System.out.println("字段名: " + fieldName);
//            System.out.println("字段对象: " + tableCol);
//            System.out.println("字段对象: " + name);
//
//            // 示例：将字段名转换为中文
//            // String chineseName = convertToChinese(fieldName);
//            // tableCol.setField(chineseName);
//        }
//
////        Map<String,Object> dataSource = DSStore.getInstance().get(ds_id);
//        //数据源的列
//        List<CompDataSourceField> dataSourceFields = compDataSource.fields;
//        //数据表的列，即table_col_id不为空的列
//        /**根据会话、用户、数据源字段，处理拼接的sql语句及查询范围。*/
//        map.put("pageSize",3000);
//        map.put("pageNum",1);
//        Map pageRows = CompUtils.getInstance().get_ds_data(compDataSource, map, session, true);
//        TableDataInfo tableInfo = (TableDataInfo) pageRows.get("page");
//        List<?> list = tableInfo.getRows();
//
//
////        TableDataInfo dsDataPageMap = (TableDataInfo) dsData.get("page");
////        List<Map> list = (List<Map>) dsDataPageMap.getRows();
//
//        //找出字典相关的列。
//        List<CompDataSourceField> dsDictFields = new ArrayList<>();
//        if (dataSourceFields != null) {
//            for (CompDataSourceField o : dataSourceFields) {
//                if (o == null) continue;
//                if (o.table_col_id == null) continue;
//                if (!Objects.equals("numeric", o.data_type)) continue;
//                if (o.rel_dict_id == null) continue;
//                dsDictFields.add(o);
//            }
//        }
//
//
//        //查看列字典选项
//        List<Map> dictItems = new ArrayList<>();
//        //循环取字典的值
//        for (CompDataSourceField dictCol : dsDictFields) {
//            String field = dictCol.field;
//            List dictItemIds = (List) list.stream().map(o -> field).collect(Collectors.toList());
//            if (dictItemIds.size() > 0) {
//                List<Map> dictItem = baseDBService.selectIn("v_dict_item", "id", dictItemIds);
//                dictItems.addAll(dictItem);
//            }
//        }
//        Map<String, Object> re = new HashMap();
//        re.put("dictItems", dictItems);
//
//        List<List<String>> headList = new ArrayList();
//        for (int i = 0; i < dataSourceFields.size(); i++) {
//            CompDataSourceField col = dataSourceFields.get(i);
//
//            if (col.field_type == "ds_total" || col.field_type == "ds_rows_length") continue;
//            if (col.table_col_id != null) {
//                if (col.fieldName != null) {
//                    headList.add(new ArrayList() {{
//                        add(col.fieldName);
//                    }});
//                }
//            } else {
//                headList.add(new ArrayList() {{
//                    add(col.fieldName);
//                }});
//            }
//        }
//        List<List<Object>> dataList = new ArrayList();
//        for (int i = 0; i < list.size(); i++) {
//            Map oneData = (Map) list.get(i);
//            List<Object> items = new ArrayList<>();
//            for (int j = 0; j < dataSourceFields.size(); j++) {
//                CompDataSourceField col = dataSourceFields.get(j);
//                if (col.field == null) {
//                    continue;
//                }
//                String colname = col.field;
//                if (oneData.get(colname) != null) {
//                    String column_type = col.data_type != null ? col.data_type : "varchar";
//                    if (column_type.equals("numeric")) {
//                        try {
//                            items.add(new BigDecimal(oneData.get(colname).toString()));
//                        } catch (NumberFormatException e) {
//                            items.add(oneData.get(colname).toString());
//                        }
//                    } else {
//                        items.add(oneData.get(colname).toString());
//                    }
//                } else {
//                    items.add(" ");
//                }
//            }
//            dataList.add(items);
//            items = new ArrayList<>();
//        }
//        System.out.println(headList);
//        System.out.println(dataList);
//
//        List list1 = dataList;
//        //头的策略
//        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
//        // 背景设置为蓝色
//        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        // 这个策略是 头是头的样式 内容是内容的样式 其他的策略可以自己实现
//        HorizontalCellStyleStrategy horizontalCellStyleStrategy =
//                new HorizontalCellStyleStrategy(headWriteCellStyle, new WriteCellStyle());
//
//        WriteFont headWriteFont = new WriteFont();
//        //headWriteFont.setFontHeightInPoints((short)16);
//        headWriteCellStyle.setWriteFont(headWriteFont);
//
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setCharacterEncoding("utf-8");
//        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
//        String fileName = URLEncoder.encode("模板文件", "UTF-8").replaceAll("\\+", "%20");
//        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
//        EasyExcel.write(response.getOutputStream())
//                //加载样式
//                .registerWriteHandler(horizontalCellStyleStrategy)
//                //自动列宽
//                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
//                // 这里放入动态头
//                .head(headList).sheet("sheet1")
//                // 当然这里数据也可以用 List<List<String>> 去传入
//                .doWrite(dataList);
//
//    }

    @RequestMapping("/exportZip")
    @ResponseBody
    public void exportFilesZip(@RequestBody Map map, HttpServletResponse response, HttpSession session) throws Exception {
        List<Map<String, Object>> sessionList = (List<Map<String, Object>>) map.get("session");
        List<Long> targetIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(sessionList)) {
            for (Map<String, Object> sessionItem : sessionList) {
                Object idObj = sessionItem.get("id_");
                if (idObj != null) {
                    if (idObj instanceof Number) {
                        targetIds.add(((Number) idObj).longValue());
                    } else if (idObj instanceof String) {
                        try {
                            targetIds.add(Long.parseLong((String) idObj));
                        } catch (NumberFormatException e) {
                            System.err.println("ID 格式错误: " + idObj);
                        }
                    }
                }
            }
        }
        System.out.println("从 session 中提取的 ID 列表: " + targetIds);
        List<Map> persons = new ArrayList<>();
        if (targetIds!=null){
        for (Long targetId : targetIds) {
            Map map1 = baseDBService.selectOne("z_table2510231456315421", Lutils.genMap("id_", targetId));
            if (map1!=null){
                persons.add(map1);
            }
        }
        }
        if (persons==null){
            return;
        }
        HashMap<String, List<FileInfo>> fileMap = new HashMap<>();
        persons.forEach(p -> {
            String name = (String) p.get("xingming");
            List<Map> files = baseDBService.selectEq("z_table2511031604522431",Lutils.genMap("z_table2510231456315421_id", p.get("id_")));
            List<FileInfo> fileList = new ArrayList<>();
            files.forEach(file -> {
                String zhengjianwenjian = (String) file.get("zhengjianwenjian");
                JSONArray jsonArray = JSONArray.parseArray(zhengjianwenjian);
                if (CollUtil.isEmpty(jsonArray)) {
                    return;
                }
                JSONObject file1 = (JSONObject) jsonArray.get(0);
                FileInfo fileInfo = new FileInfo(file1.getString("name"), file1.getString("url"));
                fileList.add(fileInfo);
            });
            fileMap.put(name, fileList);
        });

        // 设置响应头为ZIP格式
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode( "文件导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            int successCount = 0;


            // 创建总文件夹
            String mainFolderName = "文件";
            for (Map.Entry<String, List<FileInfo>> folderEntry : fileMap.entrySet()) {
                String folderName = folderEntry.getKey();
                List<FileInfo> fileInfos = folderEntry.getValue();
                int folderFileCount = 0;
                for (FileInfo fileInfo : fileInfos) {
                    try {
                        // 从URL中提取文件路径
                        String filePath = extractFilePathFromUrl(fileInfo.url);
                        // 创建ZIP条目，路径为：总文件夹/数据文件夹/文件名
                        String zipEntryPath = mainFolderName + "/" + folderName + "/" + fileInfo.fileName;
                        ZipEntry zipEntry = new ZipEntry(zipEntryPath);
                        zos.putNextEntry(zipEntry);
                        // 调用WebDAV服务下载文件并写入ZIP
                        webDavService.download(filePath, zos);
                        zos.closeEntry();
                        successCount++;
                        folderFileCount++;

                    } catch (Exception e) {
                        System.err.println("下载文件失败: " + fileInfo.fileName + ", URL: " + fileInfo.url + ", 错误: " + e.getMessage());
                    }
                }
            }
            zos.finish();

        } catch (Exception e) {
            throw new RuntimeException("创建ZIP文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/downloadQrcodeZip")
    @ResponseBody
    public ResponseEntity<InputStreamResource> generateQRCodeZip(@RequestBody Map map) {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) map.get("qrCodes");
        List<QRCodeReq> qrCodes = JSON.parseArray(JSON.toJSONString(mapList), QRCodeReq.class);
        try {
            // 创建临时文件
            File tempFile = File.createTempFile("qrcodes", ".zip");
            tempFile.deleteOnExit();

            // 创建ZIP输出流
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (QRCodeReq qrCode : qrCodes) {
                    // 解码base64数据
                    byte[] imageBytes = Base64.getDecoder().decode(qrCode.getBase64Data());

                    // 创建ZIP条目
                    String fileName = qrCode.getFileName();
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);

                    // 写入图像数据
                    zos.write(imageBytes);
                    zos.closeEntry();
                }

                zos.finish();
            }

            // 准备文件下载
            InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qrcodes.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(tempFile.length())
                    .body(resource);

        } catch (IOException e) {
            throw new RuntimeException("生成ZIP文件失败", e);
        }
    }

    @RequestMapping("/exportTemp")
    @ResponseBody
    public void exportTemp(HttpServletResponse response) throws Exception {
        HashMap<String, List<FileInfo>> fileMap = new HashMap<>();
        List<Map> persons = baseDBService.selectEq("z_table2510231456315421", Lutils.genMap("1", 1));
        persons.forEach(p -> {
            String name = (String) p.get("xingming");
            List<Map> files = baseDBService.selectEq("z_table2511031604522431",Lutils.genMap("z_table2510231456315421_id", p.get("id_")));
            List<FileInfo> fileList = new ArrayList<>();
            files.forEach(file -> {
                String zhengjianwenjian = (String) file.get("zhengjianwenjian");
                JSONArray jsonArray = JSONArray.parseArray(zhengjianwenjian);
                if (CollUtil.isEmpty(jsonArray)) {
                    return;
                }
                JSONObject file1 = (JSONObject) jsonArray.get(0);
                FileInfo fileInfo = new FileInfo(file1.getString("name"), file1.getString("url"));
                fileList.add(fileInfo);
            });
            fileMap.put(name, fileList);
        });
        System.out.println("fileMap: " + fileMap);

        // 设置响应头为ZIP格式
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("证件文件导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".zip");
        String mainFolderName = "文件";
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            int successCount = 0;
            int totalFiles = 0;

            // 计算总文件数
            for (List<FileInfo> fileList : fileMap.values()) {
                totalFiles += fileList.size();
            }
            for (Map.Entry<String, List<FileInfo>> folderEntry : fileMap.entrySet()) {
                String folderName = folderEntry.getKey();
                List<FileInfo> fileInfos = folderEntry.getValue();
                int folderFileCount = 0;
                for (FileInfo fileInfo : fileInfos) {
                    try {
                        // 从URL中提取文件路径
                        String filePath = extractFilePathFromUrl(fileInfo.url);
                        // 创建ZIP条目，路径为：总文件夹/数据文件夹/文件名
                        String zipEntryPath = mainFolderName + "/" + folderName + "/" + fileInfo.fileName;
                        ZipEntry zipEntry = new ZipEntry(zipEntryPath);
                        zos.putNextEntry(zipEntry);
                        // 调用WebDAV服务下载文件并写入ZIP
                        webDavService.download(filePath, zos);
                        zos.closeEntry();
                        successCount++;
                        folderFileCount++;

                    } catch (Exception e) {
                        System.err.println("下载文件失败: " + fileInfo.fileName + ", URL: " + fileInfo.url + ", 错误: " + e.getMessage());
                    }
                }
            }


            zos.finish();

            System.out.println("导出完成，成功导出 " + successCount + "/" + totalFiles + " 个文件");


        } catch (Exception e) {
            throw new RuntimeException("创建ZIP文件失败: " + e.getMessage());
        }
    }

    private String getFolderName(Map oneData, String nameField, Set<String> usedFolderNames, int index) {
        String folderName = "未知数据";
        if (oneData.get(nameField) != null) {
            folderName = oneData.get(nameField).toString().trim();
        }
        if (folderName.isEmpty()) {
            folderName = "未命名数据";
        }
        String originalName = folderName;
        int counter = 1;
        while (usedFolderNames.contains(folderName)) {
            folderName = originalName + "_" + counter;
            counter++;
        }
        return folderName;
    }

    // 生成唯一文件名
    private String generateUniqueFileName(String fileName, Set<String> usedFileNames) {
        String uniqueName = fileName;
        int counter = 1;
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        while (usedFileNames.contains(uniqueName)) {
            uniqueName = baseName + "_" + counter + extension;
            counter++;
        }

        return uniqueName;
    }

    // 从URL中提取文件路径（保留原方法）
    private String extractFilePathFromUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.replaceFirst("^https?://[^/]+", "");
        }
        return url;
    }

    // 文件信息类
    private static class FileInfo {
        String fileName;
        String url;

        FileInfo(String fileName, String url) {
            this.fileName = fileName;
            this.url = url;
        }
    }

    @PostMapping("/previewByUrl")
    public void previewByUrl(@RequestBody DownloadRequest request, HttpServletResponse response) throws IOException {
        String url = request.getUrl();
        // 标记响应是否已提交
        boolean responseCommitted = false;
        // 支持的CAD文件扩展名
        final Set<String> CAD_EXTENSIONS = new HashSet<>(Arrays.asList(
                "dwg", "dxf", "dwt", "dgn", "dwf", "ifc", "stl", "plt", "xls", "ppt"
        ));
        try {
            // 处理本地路径：补充file://协议（支持本地文件）
            String processedUrl = processLocalPath(url);
            URL validatedUrl = new URL(processedUrl);

            // 获取文件名并提取扩展名
            String filename = validatedUrl.getFile();
            filename = filename.substring(filename.lastIndexOf("/") + 1); // 处理URL中的路径
            if (filename.isEmpty()) {
                throw new IOException("无法获取文件名");
            }
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            // 创建临时目录
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "preview");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            // 创建带认证的URL连接
            URLConnection connection = validatedUrl.openConnection();
            connection.setRequestProperty("Authorization", webDavService.authHeader());
            // 处理Word文档
            if ("doc".equals(extension) || "docx".equals(extension)) {
                // 直接使用新工具方法转换
                String pdfFilename = filename.replaceAll("\\.\\w+$", ".pdf");
                WordToPdfUtil.convertDocxToPdf(
                        connection.getInputStream(),
                        response,
                        pdfFilename.replace(".pdf", "")  // 移除后缀，工具类会自动添加.pdf
                );
                responseCommitted = true;
                return;
            }
            // 处理CAD文件
            if (CAD_EXTENSIONS.contains(extension)) {
                // 下载原始文件到临时目录
                File inputFile = new File(tempDir, filename);
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(inputFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                if (!inputFile.exists()) {
                    throw new FileNotFoundException("CAD文件不存在: " + inputFile.getAbsolutePath());
                }

                // 设置JPEG响应头
                String jpegFilename = filename.replaceAll("\\.\\w+$", ".jpg");
                response.setContentType("image/jpeg");
                response.addHeader("Content-Disposition",
                        "inline; filename=" + URLEncoder.encode(jpegFilename, "UTF-8"));

                // 转换为JPEG并输出
                try (FileInputStream fis = new FileInputStream(inputFile);
                     ServletOutputStream sos = response.getOutputStream()) {
                    convertCadToJpeg(fis, sos);
                    responseCommitted = true;
                } finally {
                    // 删除临时文件
                    if (inputFile.exists()) {
                        inputFile.delete();
                    }
                }
                return;
            }
            // 处理非CAD文件
            response.addHeader("Content-Disposition",
                    String.format("inline;filename=%s", URLEncoder.encode(filename, "UTF-8")));
            response.setContentType(determineContentType(filename));

            try (InputStream inputStream = connection.getInputStream();
                 ServletOutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                // 标记响应已提交
                responseCommitted = true;
            }

        } catch (MalformedURLException e) {
            // 仅在响应未提交时调用sendError
            if (!responseCommitted && !response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的 URL 格式");
            }
        } catch (IOException e) {
            if (!responseCommitted && !response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件预览失败：" + e.getMessage());
            }
        } catch (Exception e) {
            if (!responseCommitted && !response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CAD文件转换失败：" + e.getMessage());
            }
        }
    }

    // 处理本地路径，补充file://协议
    private String processLocalPath(String url) {
        // 判断是否是本地路径（不含协议且包含文件分隔符）
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://")) {
            // 处理Windows路径（C:\xxx）
            if (url.contains(":") && (url.contains("\\") || url.contains("/"))) {
                // Windows路径需要转换为file:///C:/xxx（注意三个斜杠）
                return "file:///" + url.replace("\\", "/");
            } else if (url.startsWith("/")) {
                // Linux/Mac绝对路径：/home/xxx -> file:///home/xxx
                return "file://" + url;
            }
        }
        return url;
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "dwg":
                return "application/acad";
            case "dxf":
                return "image/vnd.dxf";
            default:
                return "application/octet-stream";
        }
    }

    @PostMapping("/previewLocalFile")
    public void previewLocalFile(@RequestBody Map<String, String> request, HttpServletResponse response) throws IOException {
        String filePath = request.get("filePath");

        // 安全验证：确保文件路径在允许的目录下
        if (!isValidFilePath(filePath)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to this file is not allowed");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        String contentType = determineContentType(file.getName());
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + URLEncoder.encode(file.getName(), "UTF-8") + "\"");

        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // 验证文件路径是否在允许的目录下
    private boolean isValidFilePath(String filePath) {
        try {
            Path resolvedPath = Paths.get(filePath).normalize().toAbsolutePath();
            Path allowedPath = Paths.get(ROOT_PATH).normalize().toAbsolutePath();
            return resolvedPath.startsWith(allowedPath);
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * CAD转JPEG方法
     */
    private void convertCadToJpeg(InputStream cadStream, OutputStream imageStream) throws Exception {
        // 系统级优化
        System.setProperty("java.awt.headless", "true");
        System.setProperty("org.apache.commons.imaging.ignoreMetadata", "true");

        try (Image cadImage = Image.load(cadStream)) {
            JpegOptions jpegOptions = new JpegOptions();
            CadRasterizationOptions rasterOptions = new CadRasterizationOptions();

            // === 渲染优化 ===
            rasterOptions.setPageWidth(640);
            rasterOptions.setPageHeight(480);
            rasterOptions.setBackgroundColor(Color.getWhite());

            // 20.3 版本替代方案（不使用 SingleColor）
            rasterOptions.setDrawType(CadDrawTypeMode.UseObjectColor); // 使用对象颜色
            rasterOptions.setDrawColor(Color.getBlack()); // 强制黑色渲染

            // 禁用非必要计算
            rasterOptions.setAutomaticLayoutsScaling(false);
            rasterOptions.setLayouts(new String[]{"Model"});

            // === JPEG 设置 ===
            jpegOptions.setQuality(65);
            jpegOptions.setVectorRasterizationOptions(rasterOptions);

            // 使用缓冲流
            try (BufferedOutputStream bos = new BufferedOutputStream(imageStream)) {
                cadImage.save(bos, jpegOptions);
            }
        }
    }
//
//    // 生成缩略图（保持比例）
//    private static byte[] resizeImage(byte[] src, int w, int h, String ext) throws IOException {
//        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
//            net.coobird.thumbnailator.Thumbnails.of(new java.io.ByteArrayInputStream(src))
//                    .size(w, h)                                   // 按最长边等比缩放到不超过 w/h
//                    .outputFormat(normalizeExtForThumb(ext))      // 与原扩展匹配（jpeg 归一到 jpg）
//                    .toOutputStream(bos);
//            return bos.toByteArray();
//        }
//    }

    // 根据扩展映射 Content-Type
    private static String contentTypeOf(String ext) {
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tif":
            case "tiff":
                return "image/tiff";
            default:
                return "application/octet-stream";
        }
    }

    // 给缩略图输出格式做个归一（避免 jpeg/ JPG 等大小写差异）
    private static String normalizeExtForThumb(String ext) {
        String e = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(e)) return "jpg";
        if (e.isEmpty()) return "jpg"; // 没扩展就默认 jpg
        return e;
    }


}

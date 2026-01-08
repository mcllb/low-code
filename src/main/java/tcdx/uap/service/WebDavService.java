package tcdx.uap.service;



import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.util.EntityUtils;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebDavService {

    @Value("${webdav.base-url:http://47.97.38.187}")
    private String baseUrl;

    @Value("${webdav.base-path:/home/cesc}")
    private String basePath;

    @Value("${webdav.username:}")
    private String username;

    @Value("${webdav.password:}")
    private String password;

    private final CloseableHttpClient http = HttpClients.custom()
            .disableAutomaticRetries()
            .build();

    public String authHeader() {
        String raw = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String norm(String s) {
        return s.replaceAll("/+$", "");
    }

    private static String encodeSegment(String s) {
        try {
            // 只编码段内字符；URLEncoder 把空格编码为 +，需要还原成 %20
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("%7E", "~"); // 常见友好字符
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 安全构造 URI：按段编码 path，保留斜杠；同时兼容 baseUrl 上自带的 path 前缀（若有） */
    private URI toUri(String rawPath) {
        try {
            // 解析 baseUrl（支持 http://host、http://host:port、http://host:port/prefix）
            URL base = new URL(baseUrl);

            // 组合 baseUrl 的 path 前缀（如果有）与传入的 rawPath
            String basePrefix = base.getPath() == null ? "" : base.getPath();
            String combinedPath = (basePrefix + "/" + (rawPath == null ? "" : rawPath)).replaceAll("/+", "/");

            // 去掉开头的多余斜杠，按段编码，再加回前导斜杠
            String noLead = combinedPath.replaceFirst("^/+", "");
            String encPath = Arrays.stream(noLead.split("/"))
                    .filter(seg -> !seg.isEmpty())
                    .map(WebDavService::encodeSegment)
                    .collect(Collectors.joining("/", "/", "")); // 确保以 / 开头

            // 用分量构造，避免把未编码字符串塞给 URI.create
            return new URI(
                    base.getProtocol(),
                    null,
                    base.getHost(),
                    base.getPort(),   // -1 表示默认端口，JDK 会处理
                    encPath,          // 已编码的路径
                    null,
                    null
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path for URI: baseUrl=" + baseUrl + ", rawPath=" + rawPath, e);
        }
    }
    private URI toUri2(String rawPath) {
        try {
            // 直接拼接 baseUrl 和 rawPath
            String fullUrl = norm(baseUrl) + (rawPath.startsWith("/") ? rawPath : "/" + rawPath);
            return URI.create(fullUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path for URI: baseUrl=" + baseUrl + ", rawPath=" + rawPath, e);
        }
    }
    private URI toUri1(String path) {
        String p = path.startsWith("/") ? path : "/" + path;
        return URI.create(norm(baseUrl) + p);
    }

    /** MKCOL 请求类 */
    static final class HttpMkCol extends HttpRequestBase {
        static final String METHOD_NAME = "MKCOL";
        @Override public String getMethod() { return METHOD_NAME; }
        HttpMkCol(final URI uri) { setURI(uri); }
        HttpMkCol(final String uri) { setURI(URI.create(uri)); }
    }

    /** 逐级创建目录（忽略已存在/父级已存在的情况） */
    public void ensureDirs(String fullPath) throws Exception {
        String[] parts = fullPath.split("/");
        String cur = "";
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            cur += "/" + part;
            HttpMkCol mkcol = new HttpMkCol(toUri(cur));
            mkcol.addHeader("Authorization", authHeader());
            try (CloseableHttpResponse resp = http.execute(mkcol)) {
                int sc = resp.getStatusLine().getStatusCode();
                EntityUtils.consumeQuietly(resp.getEntity());
                // 201=新建成功；405=已存在；409=父级不存在（下一轮会把父级建出来）
                if (sc == 201 || sc == 405) continue;
                if (sc == 409) continue;
                if (sc >= 400) throw new RuntimeException("MKCOL " + cur + " failed: " + sc);
            }
        }
    }

    /** 上传（PUT） */
    public String put(String targetPath, InputStream in, long length, String contentType) throws Exception {
        HttpPut put = new HttpPut(toUri(targetPath));
        put.addHeader("Authorization", authHeader());
        if (contentType != null && !contentType.isEmpty()) {
            put.setHeader("Content-Type", contentType);
        }
        put.setEntity(new InputStreamEntity(in, length));
        try (CloseableHttpResponse resp = http.execute(put)) {
            int sc = resp.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(resp.getEntity());
            if (sc == 200 || sc == 201 || sc == 204) {
                return toUri(targetPath).toString();
//                return norm(baseUrl) + (targetPath.startsWith("/") ? targetPath : ("/" + targetPath));
            }
            throw new RuntimeException("PUT failed: " + sc);
        }
    }

    /** 下载（GET）到输出流 */
    public void download(String targetPath, OutputStream out) throws Exception {
        HttpGet get = new HttpGet(toUri2(targetPath));
        get.addHeader("Authorization", authHeader());
        try (CloseableHttpResponse resp = http.execute(get)) {
            int sc = resp.getStatusLine().getStatusCode();
            if (sc >= 400) throw new RuntimeException("GET failed: " + sc);
            resp.getEntity().writeTo(out);
            EntityUtils.consumeQuietly(resp.getEntity());
        }
    }

    /** 暴露 basePath 给外部 */
    public String getBasePath() { return basePath; }
}

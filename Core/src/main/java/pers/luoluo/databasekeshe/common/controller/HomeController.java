package pers.luoluo.databasekeshe.common.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("""
                <!doctype html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <title>PSM-Smart Backend</title>
                </head>
                <body>
                    <h1>PSM-Smart Backend is running</h1>
                    <p>后端 API 服务已启动。</p>
                    <ul>
                        <li>登录接口：POST /api/auth/login</li>
                        <li>注册接口：POST /api/auth/register</li>
                        <li>前端开发地址：http://localhost:5173/</li>
                    </ul>
                </body>
                </html>
                """);
    }
}

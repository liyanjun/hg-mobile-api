package com.mobile.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobile.api.utils.DateUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

/**
 * 用于跨网文件交换用讯
 *
 * @author liyanjun
 */
@Controller
public class ApiMobileController {

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求文件目录
     */
    @Value("${api.switch.path.request}")
    private String switchPathRequest;
    /**
     * 响应文件目录
     */
    @Value("${api.switch.path.response}")
    private String switchPathResponse;
    /**
     * 超时时间
     */
    @Value("${api.timeout}")
    private Long timeout;

    /**
     * 用于DTC传输，flag可以用于区分请求，data用于传输数据
     * 请求样例：{"flag": "queryTemplate", "data": ""}
     *
     * @param param
     * @param response
     *
     * @throws Exception
     */
    @PostMapping("doTransfer")
    public void doTransfer(@RequestBody Map param, HttpServletResponse response) throws Exception {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // 分别创建请求文件和响应文件路径
        String switchNameRequest = switchPathRequest + "/" + DateUtil.getDays() + "/" + param.get("flag") + System.currentTimeMillis();
        String switchNameResponse = switchPathResponse + "/" + DateUtil.getDays() + "/" + param.get("flag") + System.currentTimeMillis();
        // 写请求文件
        FileUtils.writeStringToFile(new File(switchNameRequest), objectMapper.writeValueAsString(param), "UTF-8", false);
        // 记录超时时间
        long beginWait = System.currentTimeMillis() + timeout;
        // 等待1秒以后，开始轮询请求文件目录
        Thread.sleep(1000);
        while (!FileUtils.getFile(new File(switchNameResponse)).exists()) {
            // 文件依然不存在，并且超时，返回超时，请求结束
            if (beginWait < System.currentTimeMillis()) {
                writeBytes(new ByteArrayInputStream("{\"code\": 500, \"msg\" : \"请求超时\"}".getBytes("UTF-8")), response.getOutputStream());
                return;
            }
            Thread.sleep(500);
        }
        // 找到对应文件，写回请求
        writeBytes(FileUtils.openInputStream(new File(switchNameResponse)), response.getOutputStream());
    }

    /**
     * 写响应流
     *
     * @param in
     * @param out
     *
     * @throws IOException
     */
    private void writeBytes(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int length = -1;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
            out.flush();
        }
        in.close();
        out.close();
    }
}

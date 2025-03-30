package com.sky.controller.pay;

import com.sky.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 处理微信支付请求
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class PayController {
    @Autowired
    private PayService payService;

    @PostMapping(value = "/pay", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> payProcess(@RequestBody Map<String, Object> requestBody) {
        // 调用支付处理方法
        Map<String, Object> response = payService.payProcess(requestBody);
        String code = (String) response.get("code");
        if (code != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> refundProcess(@RequestBody Map<String, Object> requestBody) {
        // 调用支付处理方法
        Map<String, Object> response = payService.refundProcess(requestBody);
        // 这里成功与否都封装在code中，交给OrderService的业务层处理
        return ResponseEntity.ok(response);
    }

}

package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PayServiceImpl implements PayService {
    // 构造错误响应的辅助方法
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", "FAIL");
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * 支付处理
     * @param requestBody
     * @return
     */
    public Map<String, Object> payProcess(Map<String, Object> requestBody) {
        // 从请求体中解析金额
        String jsonString = JSONObject.toJSONString(requestBody);
        // 輸出解析出的请求体
        log.info("请求数据: {}", jsonString);

        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONObject amountObj = jsonObject.getJSONObject("amount");
        String total = amountObj.getString("total");
        Object openid = jsonObject.getJSONObject("payer").getString("openid");
        String mchid = jsonObject.getString("mchid");
        String orderNum = jsonObject.getString("out_trade_no");
        String notifyUrl = jsonObject.getString("notify_url");
        BigDecimal amount;
        try {
            amount = new BigDecimal(total);
            if (amount.signum() < 0) return createErrorResponse("支付金额须大于0！");
        } catch (Exception e) {
            return createErrorResponse("未知错误！");
        }

        // TODO:将支付金额存到redis中

        // 请求支付成功接口，修改订单状态，对应的url是notify_url
        // 构造支付成功的通知数据
        JSONObject notifyData = new JSONObject();
        notifyData.put("out_trade_no", orderNum); // 商户订单号
        notifyData.put("transaction_id", "TRANS_" + mchid + "_" + System.currentTimeMillis()); // 模拟微信支付交易号
        notifyData.put("trade_state", "SUCCESS"); // 支付状态
        notifyData.put("mchid", mchid); // 商户号
        notifyData.put("openid", openid); // 用户 openid

        // 使用 CloseableHttpClient 发送请求
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(notifyUrl);
        httpPost.addHeader(org.apache.http.HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
        httpPost.addHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());

        // 设置请求体
        httpPost.setEntity(new StringEntity(notifyData.toJSONString(), "UTF-8"));

        try {
            // 发起请求
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                // 获取响应
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.info("支付成功通知响应: {}", responseBody);

                // 检查响应状态码（可选）
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    log.error("支付成功通知失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    return createErrorResponse("支付成功通知失败，状态码: " + statusCode);
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("调用支付成功通知失败，订单号: {}, 错误: {}", orderNum, e.getMessage());
            return createErrorResponse("支付成功通知失败: " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                log.error("关闭 HTTP 客户端失败: {}", e.getMessage());
            }
        }

        // 根据当前时间戳生成 prepay_id
        String prepayId = "prepay_" + System.currentTimeMillis();

        // 构造响应
        Map<String, Object> response = new HashMap<>();
        response.put("prepay_id", prepayId);

        // 返回封装好的响应体
        return response;
    }

    /**
     * 退款处理
     * @param requestBody
     * @return
     */
    public Map<String, Object> refundProcess(Map<String, Object> requestBody) {
        // 从请求体中解析金额
        String jsonString = JSONObject.toJSONString(requestBody);
        // 輸出解析出的请求体
        log.info("请求数据: {}", jsonString);

        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONObject amountObj = jsonObject.getJSONObject("amount");
        // 退款金额
        String refundStr = amountObj.getString("refund");
        // 原始金额
        String totalStr = amountObj.getString("total");
        // 商户订单号
        String orderNum = jsonObject.getString("out_trade_no");
        // 退款单号
        String refundNum = jsonObject.getString("out_refund_no");
        // 请求退款成功接口
        String notifyUrl = jsonObject.getString("notify_url");

        // 验证两个单号是否一致
        if(!orderNum.equals(refundNum))
            return createErrorResponse("退款单号和原始单号不一致！");

        // 验证退款金额是否大于原始金额
        try {
            BigDecimal total = new BigDecimal(totalStr);
            BigDecimal refund = new BigDecimal(refundStr);
            if (total.compareTo(refund) < 0)
                return createErrorResponse("订单退款金额不可以大于原始金额！");
        } catch (Exception e) {
            return createErrorResponse("未知错误！");
        }

        // 以下不再请求接口处理退款成功的情况
        // 直接在这里模拟

        // 请求退款接口，修改订单状态，对应的url是notify_url（PayNotifyController，退款成功的处理没实现）
        // 打印请求的退款成功处理url
        log.info("退款成功请求地址: {}", notifyUrl);

        // 构造响应（返回退款状态）
        Map<String, Object> response = new HashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "退款成功");

        // 返回封装好的响应体
        return response;
    }


}

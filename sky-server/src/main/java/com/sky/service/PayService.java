package com.sky.service;

import java.util.Map;

public interface PayService {
    Map<String, Object> payProcess(Map<String, Object> requestBody);

    Map<String, Object> refundProcess(Map<String, Object> requestBody);
}

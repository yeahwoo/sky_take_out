package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("自定义的redis模板");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 注入Spring Data Redis的连接工厂，绑定到当前模板上，可以从连接工厂中的连接池取得连接
        // 连接工程读取配置文件，与redis服务器建立连接
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置序列化器
        // key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // value的序列化器（可以将对象转为redis的字符串，但是StringRedisSerializer只能转String类型到redis的字符串）
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return redisTemplate;
    }
}

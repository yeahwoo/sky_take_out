package com.sky.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
@EnableCaching
public class RedisConfiguration {
    @Bean
    // 键序列化器，直接用StringRedisSerializer
    public RedisSerializer<String> keySerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    // 值序列化器，使用Jackson2JsonRedisSerializer
    public RedisSerializer<Object> valueSerializer() {
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 支持 LocalDateTime
        jsonSerializer.setObjectMapper(objectMapper);
        return jsonSerializer;
    }

    @Bean
    // 这里被spring扫描的类中的方法的参数会通过spring IOC自动匹配对应类型的bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                       RedisSerializer<String> keySerializer,
                                                       RedisSerializer<Object> valueSerializer) {
        log.info("自定义的redis模板");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 注入Spring Data Redis的连接工厂，绑定到当前模板上，可以从连接工厂中的连接池取得连接
        // 连接工程读取配置文件，与redis服务器建立连接
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // Key 使用 StringRedisSerializer
        redisTemplate.setKeySerializer(keySerializer);
        // Value 使用 Jackson2JsonRedisSerializer
        redisTemplate.setValueSerializer(valueSerializer);
        return redisTemplate;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          RedisSerializer<String> keySerializer) {
        log.info("自定义的RedisCacheManager");
        // spring cache将Result类直接序列化存入redis，因此解析时需要指定值序列化器的泛型
        Jackson2JsonRedisSerializer<Result> jsonSerializer = new Jackson2JsonRedisSerializer<>(Result.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 支持 LocalDateTime
        jsonSerializer.setObjectMapper(objectMapper);
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}

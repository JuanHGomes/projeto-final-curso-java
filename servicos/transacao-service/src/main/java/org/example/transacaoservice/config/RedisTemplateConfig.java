package org.example.transacaoservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer = GenericJacksonJsonRedisSerializer.builder().build();

        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(genericJacksonJsonRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}

package cz.example.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisLogPublisher implements AutoCloseable {

    private final ObjectMapper objectMapper;
    private final JedisPool jedisPool;

    public RedisLogPublisher(ObjectMapper objectMapper, String host, int port) {
        this.objectMapper = objectMapper;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(5);
        poolConfig.setMaxTotal(10);
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }

    public void publish(Long tsNs, String logLine, String stageId) {
        LogEntryDto logEntry = new LogEntryDto(tsNs, logLine);
        String redisChannel = "logs:stage:" + stageId;

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonMessage = objectMapper.writeValueAsString(logEntry);

            jedis.publish(redisChannel, jsonMessage);
        } catch (Exception e) {
            System.err.println("[RedisLogPublisher] Failed to publish log: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}

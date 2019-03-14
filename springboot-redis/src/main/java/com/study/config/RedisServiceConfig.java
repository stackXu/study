package com.study.config;


import com.study.listener.RddMessageListener;
import com.study.listener.RedisMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisServiceConfig {

    @Value("${redis.task.host}")
    private String redisHost;

    @Value("${redis.task.port}")
    private int redisPort;

    @Value("${redis.task.pass}")
    private String redisPass;

    @Value("${redis.task.db}")
    private int redisDb;

    @Value("${redis.task.config.timeout}")
    private String timeout;
    @Value("${redis.task.config.maxTotal}")
    private int maxTotal;
    @Value("${redis.task.config.maxIdle}")
    private int maxIdle;
    @Value("${redis.task.config.maxWaitMillis}")
    private int maxWaitMillis;
    @Value("${redis.task.config.minEvictableIdleTimeMillis}")
    private String minEvictableIdleTimeMillis;
    @Value("${redis.task.config.numTestsPerEvictionRun}")
    private int numTestsPerEvictionRun;
    @Value("${redis.task.config.timeBetweenEvictionRunsMillis}")
    private String timeBetweenEvictionRunsMillis;
    @Value("${redis.task.config.testOnBorrow}")
    private Boolean testOnBorrow;
    @Value("${redis.task.config.testWhileIdle}")
    private Boolean testWhileIdle;

    @Bean
    @Primary
    public RedisConnectionFactory taskConnectionFactory() {
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
        connectionFactory.setPort(redisPort);
        connectionFactory.setHostName(redisHost);
        connectionFactory.setDatabase(redisDb);
        connectionFactory.setPassword(redisPass);

        //配置连接池属性
        connectionFactory.setTimeout(Integer.parseInt(timeout));
        connectionFactory.getPoolConfig().setMaxIdle(maxIdle);
        connectionFactory.getPoolConfig().setMaxTotal(maxTotal);
        connectionFactory.getPoolConfig().setMaxWaitMillis(maxWaitMillis);
        connectionFactory.getPoolConfig().setMinEvictableIdleTimeMillis(
                Integer.parseInt(minEvictableIdleTimeMillis));
        connectionFactory.getPoolConfig()
                .setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        connectionFactory.getPoolConfig().setTimeBetweenEvictionRunsMillis(
                Integer.parseInt(timeBetweenEvictionRunsMillis));
        connectionFactory.getPoolConfig().setTestOnBorrow(testOnBorrow);
        connectionFactory.getPoolConfig().setTestWhileIdle(testWhileIdle);

        return connectionFactory;
    }

    @Bean
    public RedisTemplate taskRedisTemplate() {
        RedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(taskConnectionFactory());
        return template;
    }

    @Bean
    public RedisConnectionFactory rddConnectionFactory() {
        // 推荐使用
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setDatabase(3);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(redisPass));

        JedisConnectionFactory redisStandaloneConfigurationFactory = new JedisConnectionFactory(redisStandaloneConfiguration);
        //同上
//        redisStandaloneConfigurationFactory.getPoolConfig().setMaxIdle();

//        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
//        connectionFactory.setPort(redisPort);
//        connectionFactory.setHostName(redisHost);
//        connectionFactory.setDatabase(3);
//        connectionFactory.setPassword(redisPass);
//
        //todo something wrong
        /**
         * org.springframework.data.redis.RedisConnectionFailureException: Cannot get Jedis connection; nested exception is redis.clients.jedis.exceptions.JedisConnectionException: Could not get a resource from the pool
         */
//        JedisShardInfo jedisShardInfo = new JedisShardInfo(redisHost,redisPort );
//        jedisShardInfo.setConnectionTimeout(10000);
//        jedisShardInfo.setSoTimeout(10000);
//        jedisShardInfo.setPassword(redisPass);
//        JedisConnectionFactory jedisShardInfoFactory = new JedisConnectionFactory(jedisShardInfo);

        return redisStandaloneConfigurationFactory;
    }

    @Bean("rddRedisTemplate")
    public StringRedisTemplate rddRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(rddConnectionFactory());

//        RedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
//        RedisSerializer stringRedisSerializer = new StringRedisSerializer();
//        // key 的序列化采用 StringRedisSerializer
//        template.setKeySerializer(stringRedisSerializer);
//        template.setHashKeySerializer(stringRedisSerializer);
//        // value 值的序列化采用 GenericJackson2JsonRedisSerializer
//        template.setValueSerializer(genericJackson2JsonRedisSerializer);
//        template.setHashValueSerializer(genericJackson2JsonRedisSerializer);

        return template;
    }

    @Bean
    public RedisScript<Boolean> lockScript() {
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<Boolean>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/redis-lock.lua")));
        redisScript.setResultType(Boolean.class);
        return redisScript;
    }

    @Bean
    public RedisScript<Boolean> unlockScript() {
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<Boolean>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/redis-unlock.lua")));
        redisScript.setResultType(Boolean.class);
        return redisScript;
    }

    @Bean
        //key过期监听,指定数据库
    RedisMessageListenerContainer keyExpirationListenerContainer(RedisMessageListener listener, RddMessageListener rddListener) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(taskConnectionFactory());
        listenerContainer.addMessageListener(listener, new PatternTopic("__keyevent@" + redisDb + "__:expired"));
        listenerContainer.addMessageListener(rddListener, new PatternTopic("__keyevent@" + redisDb + "__:expired"));
        return listenerContainer;
    }

}

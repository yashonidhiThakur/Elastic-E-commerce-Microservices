package com.ecommerce.inventory.sharded;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class LuaScripts {

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> reserveScript;
    private DefaultRedisScript<Long> commitScript;
    private DefaultRedisScript<Long> releaseScript;

    public LuaScripts(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public StringRedisTemplate getRedisTemplate() { return redisTemplate; }
    public DefaultRedisScript<Long> getReserveScript() { return reserveScript; }
    public DefaultRedisScript<Long> getCommitScript() { return commitScript; }
    public DefaultRedisScript<Long> getReleaseScript() { return releaseScript; }

    @PostConstruct
    public void init() {
        reserveScript = loadScript("lua/reserve.lua", Long.class);
        commitScript = loadScript("lua/commit.lua", Long.class);
        releaseScript = loadScript("lua/release.lua", Long.class);

        loadIntoRedis(reserveScript);
        loadIntoRedis(commitScript);
        loadIntoRedis(releaseScript);
    }

    private DefaultRedisScript<Long> loadScript(String path, Class<Long> resultType) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        Resource resource = new ClassPathResource(path);
        script.setLocation(resource);
        script.setResultType(resultType);
        return script;
    }

    private void loadIntoRedis(DefaultRedisScript<?> script) {
        String scriptAsString = script.getScriptAsString();
        if (scriptAsString != null) {
            redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
                return connection.scriptLoad(scriptAsString.getBytes());
            });
        }
    }
}

-- KEYS[1] = inv:{productId}:cell:{cellIndex}
-- KEYS[2] = inv:{productId}:cell:{cellIndex}:res:{userId}:{nonce}
-- ARGV[1] = qty
-- ARGV[2] = ttlSeconds

local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '0')
local qty = tonumber(ARGV[1])

if available < qty then
    return 0
end

redis.call('HINCRBY', KEYS[1], 'available', -qty)
redis.call('HINCRBY', KEYS[1], 'reserved', qty)

local redisTime = redis.call('TIME')
local expiryTimestamp = tonumber(redisTime[1]) + tonumber(ARGV[2])
local value = qty .. ':' .. expiryTimestamp

-- Set EX to a much longer duration (e.g., 24 hours) to ensure the sweeper finds it before Redis deletes it.
redis.call('SET', KEYS[2], value, 'EX', tonumber(ARGV[2]) + 86400)

return 1

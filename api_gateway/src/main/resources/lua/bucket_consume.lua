local bucketKey = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local tokens
local lastRefillAt

local bucketInfo = redis.call('HMGET', bucketKey, 'tokens', 'lastRefillAt')

if bucketInfo[1] then
    tokens = tonumber(bucketInfo[1])
    lastRefillAt = tonumber(bucketInfo[2])
else
    tokens = capacity
    lastRefillAt = now
end

local timePassedMs = math.max(0, now - lastRefillAt)
local newlyEarned = math.floor((timePassedMs * refillRate) / 1000)

if newlyEarned > 0 then
    tokens = math.min(capacity, tokens + newlyEarned)
    lastRefillAt = now
end

if tokens >= 1 then
    tokens = tokens - 1
    redis.call('HMSET', bucketKey, 'tokens', tokens, 'lastRefillAt', lastRefillAt)
    redis.call('EXPIRE', bucketKey, 3600)
    return 1
else
    -- If we didn't consume, we still might want to save the state if we earned tokens but didn't have enough to consume?
    -- Actually, if we didn't have enough to consume, tokens was < 1, which means 0, and newlyEarned was 0.
    -- If newlyEarned was > 0, then tokens would be >= 1, and we would have consumed!
    -- So newlyEarned is 0, no state to save.
    return 0
end

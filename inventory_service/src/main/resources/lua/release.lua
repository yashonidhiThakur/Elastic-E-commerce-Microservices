-- KEYS[1] = inv:{productId}:cell:{cellIndex}
-- KEYS[2] = inv:{productId}:cell:{cellIndex}:res:{userId}:{nonce}
-- ARGV[1] = qty

local qty = tonumber(ARGV[1])

-- Remove reservation marker
local delCount = redis.call('DEL', KEYS[2])

if delCount > 0 then
    -- Revert available and reserved
    redis.call('HINCRBY', KEYS[1], 'available', qty)
    redis.call('HINCRBY', KEYS[1], 'reserved', -qty)
    return 1
else
    -- Reservation marker not found (already expired/released or never existed)
    return 0
end

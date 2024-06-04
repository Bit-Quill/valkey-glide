#!lua name=experiment

local function sleep(keys, args)
  local step = 0
  while (true) do
    struct.pack('HH', 1, 2)
  end
  return 'OK'
end

redis.register_function{
function_name='sleep',
callback=sleep,
flags={ 'no-writes' }
}

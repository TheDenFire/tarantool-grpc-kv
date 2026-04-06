local listen       = os.getenv('TARANTOOL_LISTEN') or '0.0.0.0:3301'
local space_name   = os.getenv('KV_SPACE_NAME')    or 'kv'
local memtx_memory = tonumber(os.getenv('MEMTX_MEMORY')) or 2 * 1024 * 1024 * 1024
local db_user      = os.getenv('TARANTOOL_USER')     or 'guest'
local db_password  = os.getenv('TARANTOOL_PASSWORD') or ''

box.cfg{
	listen       = listen,
	memtx_memory = memtx_memory,
}

box.schema.user.create(db_user, { password = db_password, if_not_exists = true })
box.schema.user.grant(db_user, 'read,write,execute', 'universe', nil, { if_not_exists = true })

local kv = box.schema.space.create(space_name, {
	if_not_exists = true,
	format = {
		{name = 'key',   type = 'string'},
		{name = 'value', type = 'varbinary', is_nullable = true}
	}
})

kv:create_index('primary', {
	type = 'TREE',
	parts = {'key'},
	if_not_exists = true
})
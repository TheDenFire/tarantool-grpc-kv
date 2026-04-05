local listen = os.getenv('TARANTOOL_LISTEN') or '0.0.0.0:3301'
local space_name = os.getenv('KV_SPACE_NAME') or 'KV'

box.cfg{
	listen = listen
}

box.once("schema-init", function()
	box.schema.user.grant('guest', 'read,write,execute', 'universe')

	local kv = box.schema.space.create(space_name, {
		format = {
			{name = 'key',    type = 'string'},
			{name = 'value',  type = 'string', is_nullable = true}
		}
	})

	kv:create_index('primary', {
		type = 'TREE',
		parts = {'key'}
	})
end)

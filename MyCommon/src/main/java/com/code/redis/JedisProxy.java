package com.code.redis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.code.utils.JsonUtil;

public class JedisProxy implements JedisFace {
	private static Log log = LogFactory.getLog(JedisProxy.class);

	private Jedis jedis;
	private PooledJedis pool;

	public JedisProxy(PooledJedis pool) {
		this.pool = pool;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#setStr(java.lang.String, java.lang.String,
	 * int, boolean)
	 */
	@Override
	public boolean setStr(String key, String value, int seconds, boolean override) {
		boolean ok = false;
		try{
			this.jedis = pool.getJedis();
			if (override == false && jedis.exists(key)) {
				ok = false;
			} else {
				if (seconds < 1) {
					jedis.set(key, value);
				} else {
					jedis.setex(key, seconds, value);
				}
				ok = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return ok;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#getStr(java.lang.String)
	 */
	@Override
	public String getStr(String key) {
		try{
			this.jedis = pool.getJedis();
			return jedis.get(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#setObj(java.lang.String, java.lang.Object,
	 * int, boolean)
	 */
	@Override
	public boolean setObj(String key, Object value, int seconds, boolean override) {
		boolean ok = false;
		try{
			this.jedis = pool.getJedis();
			byte[] data = SerialUtil.encode(value);
			if (data == null) {
				log.info("setObj SerialUtil.encode error");
				return false;
			}
			if (!override && jedis.exists(key)) {
				ok = false;
			} else {
				if (seconds < 1) {
					jedis.set(key.getBytes(), data);
				} else {
					jedis.setex(key.getBytes(), seconds, data);
				}
				ok = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return ok;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#getObj(java.lang.String)
	 */
	@Override
	public Object getObj(String key) {
		Object obj = null;
		try{
			this.jedis = pool.getJedis();
			byte[] data = jedis.get(key.getBytes());
			if (data == null || data.length == 0) {
				log.debug("redis data is empty " + key);
			}
			if (data != null) {
				obj = SerialUtil.decode(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return obj;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#del_(java.lang.String)
	 */
	@Override
	public boolean del(String key) {
		boolean ok = false;
		try {
			this.jedis = pool.getJedis();
			jedis.del(key);
			ok = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return ok;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#hmset(java.lang.String, java.util.Map, int)
	 */
	@Override
	public boolean hmset(String key, Map<String, Object> values, int timeout) {
		String ok = "";
		try {
			this.jedis = pool.getJedis();
			Map<byte[], byte[]> hash = new HashMap<byte[], byte[]>();
			values.forEach(new BiConsumer<String, Object>() {
				@Override
				public void accept(String key, Object value) {
					byte[] data = SerialUtil.encode(value);
					hash.put(key.getBytes(), data);
				}
			});
			ok = jedis.hmset(key.getBytes(), hash);
			jedis.expire(key.getBytes(), timeout);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		
		return ok.equals("ok");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#hset(java.lang.String, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public boolean hset(String key, String field, Object obj) {
		long ok = 0;
		try {
			this.jedis = pool.getJedis();
			byte[] data = SerialUtil.encode(obj);
			ok = jedis.hset(key.getBytes(), field.getBytes(), data);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return ok == 0 || ok == 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#hget_(java.lang.String, java.lang.String)
	 */
	@Override
	public Object hget(String key, String field) {
		Object obj = null;
		try {
			this.jedis = pool.getJedis();
			byte[] bytes = jedis.hget(key.getBytes(), field.getBytes());
			if (bytes != null) {
				obj = SerialUtil.decode(bytes);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return obj;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mmb.redis.JedisFace#hgetAll_(java.lang.String)
	 */
	@Override
	public Map<String, Object> hgetAll(String key) {
		try {
			this.jedis = pool.getJedis();
			return hgetAll(key,jedis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return null;
	}

	private Map<String, Object> hgetAll(String key, Jedis jedis) {
		Map<byte[], byte[]> values = null;
		Map<String, Object> data = new HashMap<String, Object>();
		values = jedis.hgetAll(key.getBytes());
		values.forEach(new BiConsumer<byte[], byte[]>() {
			@Override
			public void accept(byte[] key, byte[] val) {
				data.put(new String(key), SerialUtil.decode(val));
			}
		});
		return data;
	}
	/*@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		try {
			this.jedis = pool.getJedis();
			result = method.invoke(this, args);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return result;
	}*/

	@Override
	public boolean exists(String id) {
		try {
			this.jedis = pool.getJedis();
			return jedis.exists(id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		
		return false;
	}

	@Override
	public Map<String, Object> hgetAll(String key, Integer dbIdx) {
		try {
			this.jedis = pool.getJedis();
			jedis.select(dbIdx);
			return hgetAll(key,jedis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return null;
	}

	@Override
	public long incr(String key,int seconds) {
		try {
			this.jedis = pool.getJedis();
			long v = jedis.incr(key);
			jedis.expire(key, seconds);
			return v;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return 0;
	}

	@Override
	public Long expire(String key, int seconds) {
		try {
			this.jedis = pool.getJedis();
			return jedis.expire(key, seconds);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return 0L;
	}
	
	@Override
	public Long publish(String channel, String message) {
		try {
			this.jedis = pool.getJedis();
			return jedis.publish(channel, message);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
		return 0L;
	}
	
	@Override
	public void subscribe(JedisPubSub pubsub,String... channel) {
		try {
			this.jedis = pool.getJedis();
			//TODO 要弄清楚subscribe是否需要返还资源
			jedis.subscribe(pubsub,channel );
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
	}

	@Override
	public void batchPublish(String channel,List<Object> services) {
		try {
			this.jedis = pool.getJedis();
			services.forEach(new Consumer<Object>() {
				@Override
				public void accept(Object t) {
					jedis.publish(channel, JsonUtil.java2json(t));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pool != null) {
				if (jedis != null)
					pool.returnResource(jedis);
			}
		}
	}
}

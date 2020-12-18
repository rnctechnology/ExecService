package com.rnctech.nrdataservice.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * resource object
 * @author zilin 2020.09
 */

public class JResource extends Resource {

	private final String className;

	JResource(ResourcePool pool, ResourceId resourceId, Object r) {
		super(pool, resourceId, r);
		this.className = r.getClass().getName();
	}

	JResource(ResourcePool pool, ResourceId resourceId, boolean serializable, String className) {
		super(pool, resourceId, serializable);
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	public Object invokeMethod(String methodName, Class[] paramTypes, Object[] params) {
		if (r != null) {
			try {
				Method method = r.getClass().getMethod(methodName, paramTypes);
				method.setAccessible(true);
				Object ret = method.invoke(r, params);
				return ret;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return null;
			}
		} else {
			return null;
		}
	}

	public JResource invokeMethod(String methodName, Class[] paramTypes, Object[] params, String returnResourceName) {
		if (r != null) {
			try {
				Method method = r.getClass().getMethod(methodName, paramTypes);
				Object ret = method.invoke(r, params);
				pool.put(resourceId.getId(), resourceId.getSourceType(), returnResourceName, ret);
				return (JResource) pool.get(resourceId.getId(), resourceId.getSourceType(), returnResourceName);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return null;
			}
		} else {
			return null;
		}
	}

	public static ByteBuffer serializeObject(Object o) throws IOException {
		return ByteBuffer.wrap(toByteArray(o));
	}
	
	public static byte[] toByteArray(Object o) throws IOException {
		if (o == null || !(o instanceof Serializable)) {
			return null;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos;
			oos = new ObjectOutputStream(out);
			oos.writeObject(o);
			oos.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}

	public static Object deserializeObject(ByteBuffer buf) throws IOException, ClassNotFoundException {
		if (buf == null) {
			return null;
		}
		InputStream ins = ByteBufferInputStream.get(buf);
		ObjectInputStream oin;
		Object object = null;

		oin = new ObjectInputStream(ins);
		object = oin.readObject();
		oin.close();
		ins.close();

		return object;
	}
	
}

package com.echo.flaginfo.minirpc.core.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.echo.flaginfo.minirpc.core.message.Message;

/**
 * 基于jdk的序列、反序列化
 * @author yinzhidong
 *
 */
@SuppressWarnings("all")
public class JDKSerializer implements Serializer {

	@Override
	public <E extends Message> E encoder(byte[] bytes, Class<E> messageClass)
			throws IOException, ClassNotFoundException {
		final E message;
		
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
		message = (E) objectInputStream.readObject();
		objectInputStream.close();
		
		return message;
	}
	
	
	@Override
	public byte[] decoder(Message message) throws IOException {
		final byte[] bytes;
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(message);
		bytes = byteArrayOutputStream.toByteArray();
		objectOutputStream.close();
		return bytes;
	}

}

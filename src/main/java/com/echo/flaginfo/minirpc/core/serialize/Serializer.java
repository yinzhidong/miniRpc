package com.echo.flaginfo.minirpc.core.serialize;

import java.io.IOException;

import com.echo.flaginfo.minirpc.core.message.Message;

public interface Serializer {

	 /**
     * 反序列化
     */
    <E extends Message> E encoder(byte[] bytes, Class<E> messageClass) throws IOException, ClassNotFoundException;

    /**
     * 序列化
     */
    byte[] decoder(Message message) throws IOException;
	
}

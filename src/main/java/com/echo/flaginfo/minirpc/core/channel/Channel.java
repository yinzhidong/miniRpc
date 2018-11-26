package com.echo.flaginfo.minirpc.core.channel;

import java.io.Closeable;
import java.io.Serializable;

import com.echo.flaginfo.minirpc.core.message.Message;
import com.echo.flaginfo.minirpc.exception.MiniRpcException;

public interface Channel extends Closeable,Serializable{

	boolean isOpen();
	
	String getChannelId();
	
	
	/**
	 * 读取数据
	 * @param messageClass
	 * @return
	 */
	<T extends Message> T readMessage(Class<T> messageClass) throws MiniRpcException;
	
	/**
	 * 写数据
	 * @param message
	 */
	void writeMessage(final Message message) throws MiniRpcException;
}

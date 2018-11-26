package com.echo.flaginfo.minirpc.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeoutException;

import com.echo.flaginfo.minirpc.core.message.RequestMesage;
import com.echo.flaginfo.minirpc.core.message.ResponseMessage;
import com.echo.flaginfo.minirpc.core.serialize.Serializer;

public interface Client extends Closeable{

	void connectServer(SocketAddress socketAddress) throws IOException, InterruptedException, TimeoutException;
	
	Client threadSize(int threadSize) throws IllegalArgumentException;

	Client serializer(Serializer serializer);

	Client timeout(long timeout);
	
    <S> S getService(Class<S> clazz);

    <S> S getService(String name, Class<S> clazz);
    
    ResponseMessage invokeRPC(RequestMesage requestMesage);
}

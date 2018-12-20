package com.echo.flaginfo.minirpc.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import com.echo.flaginfo.minirpc.core.serialize.Serializer;

public interface Server extends Closeable {

	Server bindPort(final int port);

	Server threadSize(final int threadSize);

	Server timeOut(final long timeOut);

	Server register(Object obj);

	Server register(String name, Object obj);

	Server register(Map<String, Object> serverMap);

	/**
	 * 注册序列号接口
	 * @param serializer
	 * @return
	 */
	Server serializer(Serializer serializer);

	/**
	 * 启动服务
	 * @throws Exception
	 */
	void startServer() throws IOException;

}

package com.echo.flaginfo.minirpc.client;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.echo.flaginfo.minirpc.constant.Constant;
import com.echo.flaginfo.minirpc.core.channel.AioChannel;
import com.echo.flaginfo.minirpc.core.channel.Channel;
import com.echo.flaginfo.minirpc.core.message.RequestMesage;
import com.echo.flaginfo.minirpc.core.message.ResponseMessage;
import com.echo.flaginfo.minirpc.core.message.ResultEntity;
import com.echo.flaginfo.minirpc.core.serialize.JDKSerializer;
import com.echo.flaginfo.minirpc.core.serialize.Serializer;

public class AioRpcClient implements Client {

	private final PrintStream console = System.out;
	private int threadSize = Constant.DEFAULT_THREAD_SIZE;
	private long timeout = Constant.DEFAULT_TIME_OUT;
	private Serializer serializer = new JDKSerializer();

	private AsynchronousChannelGroup group;
	private SocketAddress socketAddress;
	private Channel channel;

	@Override
	public void close() throws IOException {

	}

	@Override
	public void connectServer(SocketAddress socketAddress) throws IOException, InterruptedException, TimeoutException {
		this.group = AsynchronousChannelGroup.withFixedThreadPool(this.threadSize, new AIOThreadFactory());
		final AsynchronousSocketChannel asynchronousSocketChannel = AsynchronousSocketChannel.open(this.group);
		asynchronousSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		asynchronousSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		asynchronousSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

		this.socketAddress = socketAddress;
		try {
			asynchronousSocketChannel.connect(this.socketAddress).get(this.timeout, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException | TimeoutException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
			console.println("连接失败....e=" + e.getMessage());
		}
		
		this.channel = new AioChannel(asynchronousSocketChannel, this.serializer, this.timeout);
	}

	@Override
	public Client threadSize(int threadSize) throws IllegalArgumentException {
		if (threadSize <= 0) {
			throw new IllegalArgumentException("threadSize can not <= 0");
		}
		this.threadSize = threadSize;
		return this;
	}

	@Override
	public Client serializer(Serializer serializer) throws IllegalArgumentException {
		Objects.requireNonNull(serializer, "serializer can not be null");
		this.serializer = serializer;
		return this;
	}

	@Override
	public Client timeout(long timeout) throws IllegalArgumentException {
		if (timeout <= 0) {
			throw new IllegalArgumentException("timeout can not <= 0");
		}
		this.timeout = timeout;
		return this;
	}

	@Override
	public <S> S getService(Class<S> clazz) {
		return this.getService(clazz.getSimpleName(), clazz);
	}
	
	
	/**
	 * 使用jdk自带的动态代理
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <S> S getService(String name, Class<S> clazz) {
		
		return (S)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				final RequestMesage requestMesage = new RequestMesage();
				requestMesage.setServiceName(name);
				requestMesage.setMethodName(method.getName());
				
				if (Objects.nonNull(args) && args.length != 0) {
					requestMesage.setArgs(args);
					
					final Class[] argsClass = new Class[args.length];
	                for (int i = 0; i < args.length; i++) {
	                    argsClass[i] = args[i].getClass();
	                }
	                requestMesage.setArgsClassTypes(argsClass);
				}
				final ResponseMessage responseMessage = invokeRPC(requestMesage);
				if (null == responseMessage) {
					console.println("RPC调用返回null....");
					return null;
				}
				if (responseMessage.getResultEntity() != ResultEntity.SUCCESS) {
					throw new RuntimeException(responseMessage.getResultEntity().getMsg());
				}
				return responseMessage.getResponseObject();
			}
		});
	}

	@Override
	public ResponseMessage invokeRPC(RequestMesage requestMesage) {
		this.channel.writeMessage(requestMesage);
		return this.channel.readMessage(ResponseMessage.class);
	}
	
	
	static class AIOThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final String namePrefix;

		AIOThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "Aio-ThreadFactory-" + poolNumber.getAndIncrement() + "-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

}

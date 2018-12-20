package com.echo.flaginfo.minirpc.server;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.echo.flaginfo.minirpc.constant.Constant;
import com.echo.flaginfo.minirpc.core.channel.AioChannel;
import com.echo.flaginfo.minirpc.core.channel.Channel;
import com.echo.flaginfo.minirpc.core.message.RequestMesage;
import com.echo.flaginfo.minirpc.core.message.ResponseMessage;
import com.echo.flaginfo.minirpc.core.message.ResultEntity;
import com.echo.flaginfo.minirpc.core.serialize.JDKSerializer;
import com.echo.flaginfo.minirpc.core.serialize.Serializer;
import com.echo.flaginfo.minirpc.exception.MiniRpcException;

public class AioRpcServer implements Server {

	private final PrintStream console = System.out;
	private int threadSize = Constant.DEFAULT_THREAD_SIZE;
	private long timeout = Constant.DEFAULT_TIME_OUT;
	private Serializer serializer = new JDKSerializer();
	private final ExecutorService executorService = Executors.newFixedThreadPool(10000);

	private int port = 9000;
	private AsynchronousChannelGroup group;
	private AsynchronousServerSocketChannel channel;
	private final Map<String, Object> serviceMap;

	public AioRpcServer() {
		this.serviceMap = new HashMap<>();
	}

	@Override
	public void close() throws IOException {
		if (null != this.channel) {
			this.channel.close();
		}
		if (null != this.group) {
			this.group.shutdownNow();
		}
	}

	@Override
	public Server bindPort(int port) {
		if (port <= 1000) {
			throw new IllegalArgumentException("port can not <= 1000");
		}
		this.port = port;
		return this;
	}

	@Override
	public Server threadSize(int threadSize) {
		if (threadSize <= 0) {
			throw new IllegalArgumentException("threadSize can not <= 0");
		}
		this.threadSize = threadSize;
		return this;
	}

	@Override
	public Server timeOut(long timeout) {
		Objects.requireNonNull(timeout, "timeout is null");
		if (timeout <= 0) {
			throw new IllegalArgumentException("timeout can not <= 0");
		}
		this.timeout = timeout;
		return this;
	}

	@Override
	public Server register(Object obj) {
		Objects.requireNonNull(obj, "obj is null");
		this.serviceMap.put(obj.getClass().getSimpleName(), obj);
		return this;
	}

	@Override
	public Server register(String name, Object obj) {
		Objects.requireNonNull(name, "name is null");
		Objects.requireNonNull(obj, "obj is null");
		this.serviceMap.put(name, obj);
		return this;
	}

	@Override
	public Server register(Map<String, Object> serviceMap) {
		Objects.requireNonNull(serviceMap, "serviceMap is null");
		serviceMap.forEach(this::register);
		return this;
	}

	@Override
	public Server serializer(Serializer serializer) {
		Objects.requireNonNull(serializer, "serializer is null");
		this.serializer = serializer;
		return this;
	}

	@Override
	public void startServer() throws IOException {
		console.println("AioRpcServer is running....");

		this.group = AsynchronousChannelGroup.withFixedThreadPool(this.threadSize, new AIOThreadFactory());
		this.channel = AsynchronousServerSocketChannel.open(this.group)
				.setOption(StandardSocketOptions.SO_REUSEADDR, true)
				.bind(new InetSocketAddress("127.0.0.1", this.port));

		this.channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

			@Override
			public void completed(AsynchronousSocketChannel result, Void attachment) {
				channel.accept(null, this);

				String localAddress = null;
				String remoteAddress = null;
				try {
					localAddress = result.getLocalAddress().toString();
					remoteAddress = result.getRemoteAddress().toString();
					console.println("创建连接,localAddress=" + localAddress + "remoteAddress=" + remoteAddress);
				} catch (final IOException e) {
					e.printStackTrace();
					console.println("AioRpcServer accept is error... msg=" + e.getMessage());
				}

				// 处理请求
				final Channel requestChannel = new AioChannel(result, serializer, timeout);
				while (requestChannel.isOpen()) {
					handlerRequestChannel(requestChannel);
				}
				console.println("断开连接,localAddress=" + localAddress + "remoteAddress=" + remoteAddress);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				console.println("RPC通信失败...msg=" + exc.getMessage());
				try {
					close();
				} catch (final IOException e) {
					e.printStackTrace();
					console.println("RPC关闭通道异常...msg=" + e.getMessage());
				}
			}

		});
	}

	protected void handlerRequestChannel(Channel requestChannel) {
		try {
			final RequestMesage request = requestChannel.readMessage(RequestMesage.class);
			if (Objects.nonNull(request)) {
				final String serviceName = request.getServiceName();
				final Object obj = this.serviceMap.get(serviceName);
				final Method method = obj.getClass().getMethod(request.getMethodName(), request.getArgsClassTypes());

				this.executorService.execute(() -> {
					Object response = null;
					try {
						response = method.invoke(obj, request.getArgs());
					} catch (final Exception e) {
						e.printStackTrace();
					}
					
					final ResponseMessage responseMessage = new ResponseMessage();
					responseMessage.setId(request.getId());
					responseMessage.setResultEntity(ResultEntity.SUCCESS);
					responseMessage.setResponseObject(response);
					requestChannel.writeMessage(responseMessage);
				});
			}
		} catch (final Exception e) {
			e.printStackTrace();
			
			if (e instanceof MiniRpcException) {
				if (channel.isOpen()) {
					try {
						channel.close();
					} catch (final IOException ioException) {
						ioException.printStackTrace();
					}
				}
			}
		}

	}

	static class AIOThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final String namePrefix;

		AIOThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "AioServer-ThreadFactory-" + poolNumber.getAndIncrement() + "-thread";
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

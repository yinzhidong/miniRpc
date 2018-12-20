# RPC

## 它是什么

`RPC（Remote Procedure Call）`—远程过程调用，它是一种通过网络从远程计算机程序上请求服务，而不需要了解底层网络技术的协议。RPC协议假定某些传输协议的存在，如TCP或UDP，为通信程序之间携带信息数据。在OSI网络通信模型中，RPC跨越了传输层和应用层。RPC使得开发包括网络分布式多程序在内的应用程序更加容易。

> [度娘](https://baike.baidu.com/item/%E8%BF%9C%E7%A8%8B%E8%BF%87%E7%A8%8B%E8%B0%83%E7%94%A8%E5%8D%8F%E8%AE%AE/6893245?fromtitle=RPC&fromid=609861&fr=aladdin)




# AIO(Asynchronous IO)

## 介绍

`AIO`最大的一个特性就是异步能力，这种能力对socket与文件I/O都起作用。AIO其实是一种在读写操作结束之前允许进行其他操作的I/O处理。AIO是对JDK1.4中提出的同步非阻塞I/O(NIO)的进一步增强。


jdk7主要增加了三个新的异步通道:
- AsynchronousFileChannel: 用于文件异步读写
- AsynchronousSocketChannel: 客户端异步socket
- AsynchronousServerSocketChannel: 服务器异步socket


## Unix中的I/O模型

在具体看AIO之前，我们需要知道一些必要的前提概念。真的很重要！！！重要的事说三遍。。。



`Unix定义了五种I/O模型`
- 阻塞I/O
- 非阻塞I/O
- I/O复用（select、poll、linux 2.6种改进的epoll)
- 信号驱动IO（SIGIO）
- 异步I/O（POSIX的aio_系列函数）

> [来，连接给你整理好了。请点击吧](http://www.cnblogs.com/virusolf/p/4946975.html)




`两种IO多路复用方案:Reactor and Proactor`
- Reactor模式是基于同步I/O的，而Proactor模式是和异步I/O相关的。
- Reactor：能收了你跟俺说一声。Proactor: 你给我收十个字节，收好了跟俺说一声。

> [来，点击吧](https://segmentfault.com/a/1190000002715832)



----


# 代码实现介绍

## 序列化和反序列化(字节与对象的相爱相杀)

`这里用的是java自带的序列化`


```java
@SuppressWarnings("all")
public class JDKSerializer implements Serializer {

	@Override
	public <E extends Message> E encoder(byte[] bytes)
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
```
> 这里面又有文章可做，不然为什么会杀出个protobuf ^_^。




## Service的动态代理

**不啰嗦，直接上热乎乎的代码了**

```java
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
```

- Proxy.newProxyInstance嗯，是熟悉的味道，熟悉的配方^_^...


## 读写数据


`读取从服务端响应的消息`

```java
public <T extends Message> T readMessage(Class<T> messageClass) throws MiniRpcException{
	try {
		if (this.isOpen()) {

			final ByteBuffer messageLength = ByteBuffer.allocate(4);

			// 异步阻塞读取
			Integer integer = this.channel.read(messageLength).get(this.timeout, TimeUnit.MILLISECONDS);
			if (-1 != integer) {
				messageLength.flip();
				int length = messageLength.getInt();
				final ByteBuffer message = ByteBuffer.allocate(length);
				this.channel.read(message).get(this.timeout, TimeUnit.MILLISECONDS);

				message.flip();
                return this.serializer.encoder(message.array());
			}else {
				console.println("关闭连接 .....getLocalAddress=" + this.channel.getLocalAddress() + "/getRemoteAddress=" + this.channel.getRemoteAddress());
				close();
				return null;
			}
		}
	} catch (final TimeoutException | ExecutionException e) {
		e.printStackTrace();
        throw new MiniRpcException(e);
    } catch (final Exception e) {
    	console.println("读取数据异常...e=" + e);
    }

	return null;
}

```


`向服务端写消息`

```java
public void writeMessage(final Message message) {
	try {
		if (this.isOpen()) {
			final byte[] bytes = this.serializer.decoder(message);
			final ByteBuffer byteBuffer = ByteBuffer.allocate(4 + bytes.length);
			byteBuffer.putInt(bytes.length);
			byteBuffer.put(bytes);
			byteBuffer.flip();

			// 向服务端写消息,异步阻塞
			Integer integer = this.channel.write(byteBuffer).get(this.timeout, TimeUnit.MILLISECONDS);
			if (-1 == integer) {
				console.println("is open=" + this.isOpen());
				console.println("客户端连接断了....");
			}
		}
	} catch (TimeoutException | ExecutionException e) {
        e.printStackTrace();
        throw new MiniRpcException(e);
    } catch (Exception e) {
		e.printStackTrace();
	}
}

```



**这里我逃避了一个问题**
- 真实的异步非阻塞，在read和write的时候，都需要进行异步的IO读写操作。而不是channel.write(byteBuffer).get()；channel.read(byteBuffer).get()

- get方法的调用，就说明了你的程序在这里阻塞了。你暴露了你的软弱，呜呜呜~~~

**在通过异步IO读写的过程中，你会遇到这样几个无法逃避的问题**
- 如何设置一个合理的ByteBuffer的大小
- 如何处理请求的半包、黏包问题
- 如何在服务端，标识每一个通道请求。在能标识每个通道请求时，如何实现他们的keepAlived。不必每次在内存中开辟一个新的AsynchronousSocketChannel。

- ...

> 啊！！！这么多问题，我的头已经大了

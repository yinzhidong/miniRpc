package com.echo.flaginfo.minirpc.core.channel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.echo.flaginfo.minirpc.core.message.Message;
import com.echo.flaginfo.minirpc.core.serialize.Serializer;
import com.echo.flaginfo.minirpc.exception.MiniRpcException;

public class AioChannel implements Channel {

	private final PrintStream console = System.out;
	private AsynchronousSocketChannel channel;
	private Serializer serializer;
	private long timeout;

	public AioChannel(AsynchronousSocketChannel channel, Serializer serializer, long timeout) {
		super();
		this.channel = channel;
		this.serializer = serializer;
		this.timeout = timeout;
	}

	@Override
	public void close() throws IOException {
		if (null != this.channel) {
			this.channel.shutdownInput();
			this.channel.shutdownOutput();
			this.channel.close();
		}
	}

	@Override
	public boolean isOpen() {
		return this.channel.isOpen();
	}

	@Override
	public String getChannelId() {
		return null;
	}

	/**
	 * 读取从服务端响应的消息
	 */
	@Override
	public <T extends Message> T readMessage(Class<T> messageClass) throws MiniRpcException{

		try {
			if (this.isOpen()) {
				final ByteBuffer messageLength = ByteBuffer.allocate(4);
				// 异步阻塞读取
				Integer integer = this.channel.read(messageLength).get(this.timeout, TimeUnit.MILLISECONDS);
				if (-1 == integer) {
					console.println("关闭连接 .....getLocalAddress=" + this.channel.getLocalAddress() + "/getRemoteAddress=" + this.channel.getRemoteAddress());
					close();
					return null;
				}
				int length = messageLength.getInt();
				final ByteBuffer message = ByteBuffer.allocate(length);
				this.channel.read(message).get(this.timeout, TimeUnit.MILLISECONDS);
				
				message.flip();
                return this.serializer.encoder(message.array(), messageClass);
			}

		} catch (final TimeoutException | ExecutionException e) {
            throw new MiniRpcException(e);
        } catch (final Exception e) {
        	console.println("读取数据异常...e=" + e);
        }

		return null;
	}

	/**
	 * 向服务端写消息
	 */
	@Override
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

}

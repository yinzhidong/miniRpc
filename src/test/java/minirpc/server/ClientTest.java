package minirpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import com.echo.flaginfo.minirpc.client.AioRpcClient;
import com.echo.flaginfo.minirpc.client.Client;

import minirpc.service.TestService;

public class ClientTest {

	public static void main(String[] args) {
		try {
			Client client = new AioRpcClient();
			client.connectServer(new InetSocketAddress("127.0.0.1", 4567));
			
			TestService service = client.getService("test", TestService.class);
			String say = service.say("Hello!");
			System.out.println(say);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		
	}
}

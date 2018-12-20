package minirpc.server;

import com.echo.flaginfo.minirpc.server.AioRpcServer;
import com.echo.flaginfo.minirpc.server.Server;

import minirpc.service.TestServiceImpl;

public class ServerTest {

    public static void main(String[] args) throws Exception {
        Server aioRpcServer = new AioRpcServer();
		aioRpcServer.threadSize(10)
		            .register("test", new TestServiceImpl())
		            .bindPort(4567)
		            .startServer();
    }
}

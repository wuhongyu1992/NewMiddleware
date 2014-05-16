package middleware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class MiddleServer extends MiddleSocketChannel {

	public MiddleServer() {
		super();
	}

	public void startServer(SocketChannel inSocketChannel) {

		socketChannel = inSocketChannel;
		try {
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// System.out.println("startServer");
	}

	public int getClientPort() {
		int port = -1;
		try {
			port = ((InetSocketAddress) socketChannel.getRemoteAddress()).getPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

}

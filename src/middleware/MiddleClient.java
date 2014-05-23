package middleware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class MiddleClient extends MiddleSocketChannel {

	public MiddleClient(String ip, int port) {
		super(ip, port);
		connectClient = false;
	}

	public void startClient() {
		try {
			socketChannel = SocketChannel.open(new InetSocketAddress(ipAddr,
					portNum));
			socketChannel.configureBlocking(false);
		} catch (IOException ioe) {
			System.out.println("Error in connecting to server");
		}
	}
	//
	// public boolean isConnected() {
	// return socket.isConnected();
	// }

	// public void reconnect() {
	// try {
	// socket.connect(socket.getRemoteSocketAddress());
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	// public int getInput(byte[] byteArray) {
	// int len = 0;
	// try {
	//
	// len = inData.read(byteArray);
	// } catch (IOException ioe) {
	// System.out.println("Error in receiving data stream");
	// }
	// System.out.println(len);
	// return len;
	// }
	//
	// public void sendOutput(byte[] b, int len) {
	// try {
	// outData.write(b, 0, len);
	// } catch (IOException e) {
	// System.out.println("Error in output");
	// }
	// }

}

package middleware;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class MiddleServerSocket extends Thread {
	private SharedData sharedData;
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;
	private File dir;

	MiddleServerSocket(SharedData s) {
		sharedData = s;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			selector = Selector.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(s.getMiddlePortNum()));
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		dir = new File(sharedData.getFilePathName() + "/Transactions");
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	public void run() {
		while (!sharedData.isEndOfProgram()) {
			SocketChannel socketChannel = null;
			try {
				socketChannel = serverSocketChannel.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (socketChannel != null) {
				sharedData.setClearClients(false);
				MiddlewareUnit newUnit = new MiddlewareUnit(sharedData);
				if (newUnit.setUp(socketChannel)) {
					newUnit.start();
					// sharedData.addUnit(newUnit);
				}
			}

			if (sharedData.getFileBufferSize() >= sharedData.getOutputSize()) {
				sharedData.flushOutput();
			}
		}

		System.out.println("server socket end");
	}
}

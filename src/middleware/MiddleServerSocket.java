package middleware;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class MiddleServerSocket extends Thread {
	private SharedData sharedData;
	private ServerSocketChannel serverSocketChannel;
	private File dir;
	private int numWorkers;
	private MiddleWorker[] workers;

	MiddleServerSocket(SharedData s) {
		sharedData = s;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(
					new InetSocketAddress(s.getMiddlePortNum()));
			serverSocketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		dir = new File(sharedData.getFilePathName() + "/Transactions");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		numWorkers = sharedData.getNumWorkers();
		workers = new MiddleWorker[numWorkers];
		for (int i = 0; i < numWorkers; ++i) {
			workers[i] = new MiddleWorker(sharedData);
			workers[i].start();
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

				MiddleServer middleServer = new MiddleServer();
				MiddleClient middleClient = new MiddleClient(
						sharedData.getServerIpAddr(),
						sharedData.getServerPortNum());

				middleServer.startServer(socketChannel);
				middleClient.startClient();

				middleServer.register(sharedData.selector, middleClient);
				
				middleClient.register(sharedData.selector, middleServer);

				sharedData.putInMap(middleServer.socketChannel, middleServer);
				sharedData.putInMap(middleClient.socketChannel, middleClient);
			}

			if (sharedData.getFileBufferSize() >= sharedData.getOutputSize()) {
				sharedData.flushOutput();
			}
		}

		sharedData.flushOutput();

		System.out.println("server socket end");
	}
}

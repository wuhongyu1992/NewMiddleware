package middleware;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

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
		try {
			sharedData.selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sharedData.keyIterator = sharedData.selector.selectedKeys().iterator();
		workers = new MiddleWorker[numWorkers];
		for (int i = 0; i < numWorkers; ++i) {
			workers[i] = new MiddleWorker(sharedData);
			workers[i].start();
		}

		sharedData.socketMap = new HashMap<SocketChannel, MiddleSocketChannel>();

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

				MiddleServer middleServer = new MiddleServer(socketChannel);
				MiddleClient middleClient = new MiddleClient(
						sharedData.getServerIpAddr(),
						sharedData.getServerPortNum());

				middleServer.startServer(null);
				middleClient.startClient();

				middleServer.register(sharedData.selector, middleClient);

				middleClient.register(sharedData.selector, middleServer);

				sharedData.socketMap.put(middleServer.socketChannel, middleServer);
				sharedData.socketMap.put(middleClient.socketChannel, middleClient);
			}
		}


		System.out.println("server socket end");
	}
}

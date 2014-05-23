package middleware;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NewServerSocket extends Thread {
	private SharedData sharedData;
	private ServerSocketChannel serverSocketChannel;
	private File dir;
	private int numWorkers;
	private NewWorker[] workers;

	NewServerSocket(SharedData s) {
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
		workers = new NewWorker[numWorkers];
		for (int i = 0; i < numWorkers; ++i) {
			Selector selector = null;
			try {
				selector = Selector.open();
			} catch (IOException e) {
				e.printStackTrace();
			}

			workers[i] = new NewWorker(sharedData, selector);
			workers[i].start();
		}

	}

	public void run() {
		int count = 0;
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
				TransactionData transactionData = new TransactionData(
						sharedData, middleServer);
				middleServer.startServer(transactionData);
				middleClient.startClient();

				middleServer.register(workers[count % numWorkers].selector,
						middleClient);

				middleClient.register(workers[count % numWorkers].selector,
						middleServer);

				workers[count].socketMap.put(middleServer.socketChannel,
						middleServer);
				workers[count].socketMap.put(middleClient.socketChannel,
						middleClient);
				++count;
			}

			if (sharedData.getFileBufferSize() >= sharedData.getOutputSize()) {
				sharedData.flushOutput();
			}
		}

		sharedData.flushOutput();

		System.out.println("server socket end");
	}
}

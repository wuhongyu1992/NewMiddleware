package middleware;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestHandler extends Thread {
	private SharedData sharedData;
	private ServerSocketChannel serverSocketChannel;
	private File dir;
	private int numWorkers;
	private HashMap<SocketChannel, MiddleSocketChannel> socketMap;
	private Selector selector;
	private Iterator<SelectionKey> keyIterator;
	private ExecutorService threadPool;

	private ConcurrentLinkedQueue<ByteBuffer> buffers;

	RequestHandler(SharedData s) {
		sharedData = s;
		sharedData.setSelectTime(0);
		sharedData.setInputTime(0);
		sharedData.setOutputTime(0);

		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(
					new InetSocketAddress(s.getMiddlePortNum()));
			serverSocketChannel.configureBlocking(false);
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		dir = new File(sharedData.getFilePathName() + "/Transactions");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		numWorkers = sharedData.getNumWorkers();
		socketMap = new HashMap<SocketChannel, MiddleSocketChannel>();
		threadPool = Executors.newFixedThreadPool(numWorkers);
		buffers = new ConcurrentLinkedQueue<ByteBuffer>();
		for (int i = 0; i < 2 * numWorkers; ++i) {
			buffers.add(ByteBuffer.allocateDirect(sharedData.getMaxSize()));
		}
	}

	public void run() {
		while (!sharedData.isEndOfProgram()) {

			long ts = System.currentTimeMillis();
			try {
				selector.select();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			keyIterator = selector.selectedKeys().iterator();
			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				keyIterator.remove();
				if (key.isAcceptable()) {
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

						middleServer.register(selector, middleClient);

						middleClient.register(selector, middleServer);

						socketMap.put(middleServer.socketChannel, middleServer);
						socketMap.put(middleClient.socketChannel, middleClient);
					}
				} else if (key.isReadable()) {
					while (buffers.isEmpty()) {
					}
					RequestWorker worker = new RequestWorker(sharedData,
							buffers, socketMap.get(key.channel()),
							(MiddleSocketChannel) key.attachment());
					threadPool.execute(worker);
				}
			}

			// if (sharedData.getFileBufferSize() >= sharedData.getOutputSize())
			// {
			// sharedData.flushOutput();
			// }

			sharedData.addSelectTime(System.currentTimeMillis() - ts);
			System.out.println(buffers.size());
		}

		sharedData.flushOutput();

		threadPool.shutdown();
		long t0 = sharedData.getSelectTime();
		long t1 = sharedData.getInputTime();
		long t2 = sharedData.getOutputTime();
		long t3 = sharedData.getReturnTime();
		long t = t0 + t1 + t2 + t3;
		System.out.println("t0: " + ((double) t0) / t);
		System.out.println("t1: " + ((double) t1) / t);
		System.out.println("t2: " + ((double) t2) / t);
		System.out.println("t3: " + ((double) t3) / t);

		System.out.println("server socket end");
	}
}

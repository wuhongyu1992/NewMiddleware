package middleware;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class NewWorker extends Thread {
	private SharedData sharedData;
	private MiddleSocketChannel from;
	private MiddleSocketChannel to;
	private ByteBuffer buffer;
	private Iterator<SelectionKey> keyIterator;

	public HashMap<SocketChannel, MiddleSocketChannel> socketMap;
	public Selector selector;

	private long ts0, ts1, ts2, ts3, ts4;
	private long t0 = 0, t1 = 0, t2 = 0, t3 = 0;

	NewWorker(SharedData s, Selector s1) {
		sharedData = s;
		buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
		selector = s1;
		socketMap = new HashMap<SocketChannel, MiddleSocketChannel>();
		from = null;
		to = null;
	}

	public void run() {

		while (!sharedData.isClearClients()) {
			ts0 = System.currentTimeMillis();

			try {
				selector.selectNow();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ts1 = System.currentTimeMillis();
			t0 += ts1 - ts0;
			keyIterator = selector.selectedKeys().iterator();
			while (keyIterator.hasNext()) {

				ts1 = System.currentTimeMillis();

				SelectionKey key = keyIterator.next();
				keyIterator.remove();
				from = socketMap.get(key.channel());
				to = (MiddleSocketChannel) key.attachment();

				ts2 = System.currentTimeMillis();

				if (to == null || from == null)
					continue;
				buffer.clear();
				int len = from.getInput(buffer);

				ts3 = System.currentTimeMillis();

				if (len <= 0)
					continue;
				to.sendOutput(buffer, len);

				ts4 = System.currentTimeMillis();

				t1 += ts2 - ts1;
				t2 += ts3 - ts2;
				t3 += ts4 - ts3;
			}
		}
		long t = t0 + t1 + t2 + t3;
		System.out.println("t0: " + ((double) t0) / t);
		System.out.println("t1: " + ((double) t1) / t);
		System.out.println("t2: " + ((double) t2) / t);
		System.out.println("t3: " + ((double) t3) / t);

	}

}

package middleware;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class MiddleWorker extends Thread {
	private SharedData sharedData;
	private MiddleSocketChannel from;
	private MiddleSocketChannel to;
	private ByteBuffer buffer;

	private long ts0, ts1, ts2, ts3;
	private long t0 = 0, t1 = 0, t2 = 0;

	MiddleWorker(SharedData s) {
		sharedData = s;
		buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
		from = null;
		to = null;
	}

	public void run() {

		while (!sharedData.isEndOfProgram()) {
			ts0 = System.currentTimeMillis();

			SelectionKey key = sharedData.getSelectionKey();
			from = sharedData.socketMap.get(key.channel());
			to = (MiddleSocketChannel) key.attachment();

			ts1 = System.currentTimeMillis();

			if (to == null || from == null)
				continue;
			buffer.clear();
			int len = from.getInput(buffer);

			ts2 = System.currentTimeMillis();

			if (len <= 0)
				continue;
			to.sendOutput(buffer, len);

			ts3 = System.currentTimeMillis();

			t0 += ts1 - ts0;
			t1 += ts2 - ts1;
			t2 += ts3 - ts2;
		}
		long t = t0 + t1 + t2;
		System.out.println("t0: " + ((double) t0)/t);
		System.out.println("t1: " + ((double) t1)/t);
		System.out.println("t2: " + ((double) t2)/t);

	}

}

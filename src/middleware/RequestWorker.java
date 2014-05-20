package middleware;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestWorker extends Thread {
	private SharedData sharedData;
	private MiddleSocketChannel from;
	private MiddleSocketChannel to;
	private ByteBuffer buffer;
	private ConcurrentLinkedQueue<ByteBuffer> buffers;

	private long ts0, ts1, ts2, ts3;

	RequestWorker(SharedData s, ConcurrentLinkedQueue<ByteBuffer> b,
			MiddleSocketChannel f, MiddleSocketChannel t) {
		sharedData = s;
		// buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
		buffers = b;
		buffer = buffers.poll();
		from = f;
		to = t;
	}

	public void run() {

		if (to == null || from == null) {
			buffers.add(buffer);
			return;
		}
		ts0 = System.currentTimeMillis();

		buffer.clear();
		int len = from.getInput(buffer);

		ts1 = System.currentTimeMillis();

		if (len <= 0) {
			buffers.add(buffer);
			return;
		}
		to.sendOutput(buffer, len);

		ts2 = System.currentTimeMillis();

		buffers.add(buffer);

		ts3 = System.currentTimeMillis();
		sharedData.addInputTime(ts1 - ts0);
		sharedData.addOutputTime(ts2 - ts1);
		sharedData.addReturnTime(ts3 - ts2);

	}

}

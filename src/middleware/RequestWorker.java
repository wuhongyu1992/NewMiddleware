package middleware;

import java.nio.ByteBuffer;

public class RequestWorker extends Thread {
	private SharedData sharedData;
	private MiddleSocketChannel from;
	private MiddleSocketChannel to;
	private ByteBuffer buffer;

	private long ts0, ts1, ts2;

	RequestWorker(SharedData s, ByteBuffer b, MiddleSocketChannel f,
			MiddleSocketChannel t) {
		sharedData = s;
		// buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
		buffer = b;
		from = f;
		to = t;
	}

	public void run() {

		if (to == null || from == null)
			return;
		ts0 = System.currentTimeMillis();

		buffer.clear();
		int len = from.getInput(buffer);

		ts1 = System.currentTimeMillis();

		if (len < 0)
			return;
		to.sendOutput(buffer, len);

		ts2 = System.currentTimeMillis();

		sharedData.addInputTime(ts1 - ts0);
		sharedData.addOutputTime(ts2 - ts1);

	}

}

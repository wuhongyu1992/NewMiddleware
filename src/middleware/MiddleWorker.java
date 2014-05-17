package middleware;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class MiddleWorker extends Thread {
	private SharedData sharedData;
	private MiddleSocketChannel from;
	private MiddleSocketChannel to;
	private ByteBuffer buffer;

	MiddleWorker(SharedData s) {
		sharedData = s;
		buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
		from = null;
		to = null;
	}

	public void run() {

		while (!sharedData.isEndOfProgram()) {
			SelectionKey key = sharedData.getSelectionKey();
			from = sharedData.getSocket(key.channel());
			to = (MiddleSocketChannel) key.attachment();
			
			if (to == null || from == null) continue;
			buffer.clear();
			int len = from.getInput(buffer);
			if (len <= 0)
				continue;
			to.sendOutput(buffer, len);
		}

	}

}

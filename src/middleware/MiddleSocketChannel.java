package middleware;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class MiddleSocketChannel {
	protected SocketChannel socketChannel;
	protected String ipAddr;
	protected int portNum;

	public MiddleSocketChannel(String ip, int port) {
		ipAddr = ip;
		portNum = port;
		socketChannel = null;
	}

	public MiddleSocketChannel() {
		socketChannel = null;
	}

	public void sendOutput(ByteBuffer buffer, int len) {
		try {
			buffer.position(0);
			buffer.limit(len);
			socketChannel.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error in output");
		}
	}

	public int getInput(ByteBuffer buffer) {
		int len = 0;
		try {
			len = socketChannel.read(buffer);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Error in receiving data stream");
		}
		// System.out.println(len);
		return len;
	}

	public boolean isConnected() {
		return socketChannel.isConnected();
	}

	public void close() {
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void register(Selector selector, Object attachment) {
		try {
			socketChannel.register(selector, SelectionKey.OP_READ, attachment);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		}
	}

}

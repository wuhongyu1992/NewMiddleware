package middleware;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The thread to transit packets from clients and server to each other and log
 * clients transactions data and latencies if required
 * 
 * @author Hongyu Wu
 * 
 */
public class NewWorker extends Thread {
  private SharedData sharedData;
  private MiddleSocketChannel from;
  private MiddleSocketChannel to;
  private byte[] data;
  private ByteBuffer buffer;
  private Iterator<SelectionKey> keyIterator;

  public HashMap<SocketChannel, MiddleSocketChannel> socketMap;
  public Selector selector;

  private long ts0, ts1, ts2, ts3, ts4, ts5;
  private long t0 = 0, t1 = 0, t2 = 0, t3 = 0, t4 = 0;

  NewWorker(SharedData s, Selector s1) {
    sharedData = s;
    data = new byte[sharedData.getMaxSize()];
    // buffer = ByteBuffer.allocateDirect(sharedData.getMaxSize());
    buffer = ByteBuffer.wrap(data);
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

        if (len <= 0) {
          if (from.connectClient) {
            ((MiddleServer) from).transactionData.flushToFile();
            ((MiddleServer) from).transactionData.closeFileOutputStream();
          }
          continue;
        }
        to.sendOutput(buffer, len);

        ts4 = System.currentTimeMillis();

        if (from.connectClient) {
          ((MiddleServer) from).transactionData.checkAutoCommit(data, len);
          if (sharedData.isOutputToFile()) {
            ((MiddleServer) from).transactionData.processData(data, len, ts3);
          }
        } else if (sharedData.isOutputToFile()
            && ((MiddleServer) to).transactionData.endingTrax) {
          ((MiddleServer) to).transactionData.endTrax(ts4);
        }

        ts5 = System.currentTimeMillis();

        t1 += ts2 - ts1;
        t2 += ts3 - ts2;
        t3 += ts4 - ts3;
        t4 += ts5 - ts4;
      }
    }
    long t = t0 + t1 + t2 + t3 + t4;
    System.out.println("t0: " + ((double) t0) / t);
    System.out.println("t1: " + ((double) t1) / t);
    System.out.println("t2: " + ((double) t2) / t);
    System.out.println("t3: " + ((double) t3) / t);
    System.out.println("t4: " + ((double) t4) / t);

  }

}

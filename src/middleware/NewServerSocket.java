package middleware;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The thread to listen to middle port and construct connection to both clients
 * and server
 * 
 * @author Hongyu Wu
 * 
 */
public class NewServerSocket extends Thread {
  private SharedData sharedData;
  private ServerSocketChannel serverSocketChannel;
  private ServerSocketChannel adminServerSocketChannel;
  private Selector selector;
  private Iterator<SelectionKey> keyIterator;
  private File dir;
  private int numWorkers;
  private NewWorker[] workers;
  private byte[] data;
  private ByteBuffer buffer;
  private StringBuilder stringBuilder;

  NewServerSocket(SharedData s) {
    sharedData = s;
    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.socket().bind(
          new InetSocketAddress(s.getMiddlePortNum()));
      serverSocketChannel.configureBlocking(false);

      adminServerSocketChannel = ServerSocketChannel.open();
      adminServerSocketChannel.socket().bind(
          new InetSocketAddress(s.getAdminPortNum()));
      adminServerSocketChannel.configureBlocking(false);

      selector = Selector.open();

      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      adminServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      e.printStackTrace();
    }

    keyIterator = null;

    dir = new File(sharedData.getFilePathName() + "/Transactions");
    if (!dir.exists()) {
      dir.mkdirs();
    } else {
      for (File f : dir.listFiles()) {
        if (!f.delete()) {
          // TODO
        }
      }
    }

    numWorkers = sharedData.getNumWorkers();
    workers = new NewWorker[numWorkers];
    for (int i = 0; i < numWorkers; ++i) {
      Selector tmpS = null;
      try {
        tmpS = Selector.open();
      } catch (IOException e) {
        e.printStackTrace();
      }

      workers[i] = new NewWorker(sharedData, tmpS);
      workers[i].start();
    }

    data = new byte[sharedData.getMaxSize()];
    buffer = ByteBuffer.wrap(data);

    sharedData.txId = new AtomicInteger(0);
    sharedData.allTransactionData = new ArrayList<TransactionData>();
    sharedData.allTransactions = new ConcurrentSkipListMap<Integer, ConcurrentLinkedQueue<ByteBuffer>>();

  }

  public void run() {
    int count = 0;
    while (!sharedData.isEndOfProgram()) {

      try {
        selector.selectNow();
      } catch (IOException e) {
        e.printStackTrace();
      }

      keyIterator = selector.selectedKeys().iterator();
      while (keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        keyIterator.remove();

        if (key.isAcceptable()) {
          if (key.channel() == serverSocketChannel) {
            SocketChannel socketChannel = null;
            try {
              socketChannel = serverSocketChannel.accept();
            } catch (IOException e) {
              e.printStackTrace();
            }

            if (socketChannel != null) {

              MiddleClient middleClient = new MiddleClient(
                  sharedData.getServerIpAddr(), sharedData.getServerPortNum());
              middleClient.startClient();

              MiddleServer middleServer = new MiddleServer(socketChannel);
              TransactionData transactionData = new TransactionData(sharedData,
                  middleServer);
              middleServer.startServer(transactionData);

              int len = 0;
              buffer.clear();
              len = middleClient.getInput(buffer);
              middleServer.sendOutput(buffer, len);

              buffer.clear();
              len = middleServer.getInput(buffer);
              transactionData.setUserId(getUserId(data));
              middleClient.sendOutput(buffer, len);

              middleServer.setNonBlocking();
              middleClient.setNonBlocking();

              if (sharedData.isOutputToFile()) {
                transactionData.openFileOutputStream();
              }

              sharedData.allTransactionData.add(transactionData);

              workers[count % numWorkers].socketMap.put(
                  middleServer.socketChannel, middleServer);
              workers[count % numWorkers].socketMap.put(
                  middleClient.socketChannel, middleClient);

              middleServer.register(workers[count % numWorkers].selector,
                  middleClient);

              middleClient.register(workers[count % numWorkers].selector,
                  middleServer);

              ++count;
            }
          } else if (key.channel() == adminServerSocketChannel) {

          }
        }
      }

    }

    System.out.println("server socket end");
  }

  private String getUserId(byte[] b) {
    int i = 36;
    while (b[i] != '\0' && i < b.length) {
      stringBuilder.append((char) b[i]);
      ++i;
    }
    return stringBuilder.toString();
  }

  private void printAllTransactions() {
    ConcurrentLinkedQueue<ByteBuffer> tmpQ = sharedData.allTransactions
        .firstEntry().getValue();
    int tmpK = sharedData.allTransactions.firstEntry().getKey();
    ByteBuffer tmpB = tmpQ.poll();

    try {
      sharedData.allLogFileOutputStream.write(tmpB.array(), 0, tmpB.position());
    } catch (IOException e) {
      e.printStackTrace();
    }

    sharedData.allTransactions.remove(tmpK);
    if (!tmpQ.isEmpty()) {
      int newK = getNewKey(tmpQ.peek());
      sharedData.allTransactions.put(newK, tmpQ);
    }

  }

  private int getNewKey(ByteBuffer buf) {
    byte[] b = buf.array();
    int i = 0;
    int newK = 0;
    while (b[i] != ',') {
      newK = newK * 10 + (int) (b[i] - '0');
      ++i;
    }
    return newK;
  }

  private void startMonitoring() {
    for (File f : dir.listFiles()) {
      if (!f.delete()) {
        // TODO
      }
    }

    for (int i = 0; i < sharedData.allTransactionData.size();) {
      TransactionData tmp = sharedData.allTransactionData.get(i);
      if (tmp.isAlive) {
        tmp.openFileOutputStream();
        ++i;
      } else {
        sharedData.allTransactionData.remove(i);
      }
    }

    try {
      sharedData.allLogFileOutputStream = new BufferedOutputStream(
          new FileOutputStream(new File(sharedData.getFilePathName()
              + "/Transactions/allLogs")));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    sharedData.txId.set(0);

    sharedData.setOutputToFile(true);

  }

  private void stopMonitoring() {
    sharedData.setOutputToFile(false);

    for (int i = 0; i < sharedData.allTransactionData.size();) {
      TransactionData tmp = sharedData.allTransactionData.get(i);
      if (tmp.isAlive) {
        tmp.closeFileOutputStream();
        ++i;
      } else {
        sharedData.allTransactionData.remove(i);
      }
    }

  }

}

package middleware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Server SocketChannel to connect to clients, inherited from MiddleSocketChannel
 * 
 * @author Hongyu Wu
 * 
 */
public class MiddleServer extends MiddleSocketChannel {
  public TransactionData transactionData;

  public MiddleServer(SocketChannel s) {
    super();
    connectClient = true;

    socketChannel = s;
    try {
      socketChannel.configureBlocking(false);
    } catch (IOException e) {
      e.printStackTrace();
    }
    transactionData = null;
  }

  public void startServer(TransactionData t) {

    transactionData = t;

    // System.out.println("startServer");
  }

  public int getClientPort() {
    int port = -1;
    try {
      port = ((InetSocketAddress) socketChannel.getRemoteAddress()).getPort();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return port;
  }

}

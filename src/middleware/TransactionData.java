package middleware;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

/**
 * The class to store and print data and info about transaction between one
 * client and server
 * 
 * @author Hongyu Wu
 * 
 */
public class TransactionData {
  public static final byte MYSQL_QUERY = 3;

  private MiddleServer middleServer;
  private SharedData sharedData;
  private int clientPortNum;
  private String userId;

  private long traxStart;
  private long traxEnd;
  private boolean inTrax;
  private boolean autoCommit;
  private boolean tempAutoCommit;

  private Timestamp timestamp;

  private int TxID;

  private File file;
  private BufferedOutputStream fileOutputStream;

  private ByteBuffer curTransaction;
  private int bufferSize = 1024;

  // public ConcurrentLinkedQueue<ByteBuffer> transactions;
  public boolean endingTrax;
  public boolean isAlive;

  TransactionData(SharedData s, MiddleServer server) {
    sharedData = s;
    middleServer = server;
    clientPortNum = middleServer.getClientPort();
    userId = null;
    sharedData.getMaxSize();

    inTrax = false;
    autoCommit = true;
    tempAutoCommit = true;
    endingTrax = false;
    isAlive = true;
    // transactions = new ConcurrentLinkedQueue<ByteBuffer>();

    timestamp = new Timestamp(0);

  }

  public void processData(byte[] data, int len, long recTime) {

    if (!inTrax) {
      traxStart = recTime;
      timestamp.setTime(traxStart);
      String s = "," + clientPortNum + "," + userId + ","
          + timestamp.toString() + ",{";
      if (len - 5 + s.length() > bufferSize)
        bufferSize *= 2;

      curTransaction = ByteBuffer.allocate(bufferSize);
      curTransaction.put(s.getBytes());
      curTransaction.put(data, 5, len - 5);

      if ((autoCommit && tempAutoCommit) || traxEnd(data, len)) {
        endingTrax = true;
      } else {
        inTrax = true;
      }

    } else {

      while (len - 4 > curTransaction.remaining()) {
        ByteBuffer tmp = curTransaction;
        bufferSize *= 2;
        curTransaction = ByteBuffer.allocate(bufferSize);
        tmp.limit(tmp.position());
        tmp.position(0);
        curTransaction.put(tmp);
      }
      curTransaction.put((byte) ';');
      curTransaction.put(data, 5, len - 5);

      if (traxEnd(data, len)) {
        inTrax = false;
        endingTrax = true;
      }

    }
  }

  public void endTrax(long t) {
    traxEnd = t;
    timestamp.setTime(traxEnd);

    String s = "}," + timestamp.toString() + "," + (traxEnd - traxStart) + "\n";

    if (s.length() > curTransaction.remaining()) {
      ByteBuffer tmp = curTransaction;
      curTransaction = ByteBuffer.allocate(bufferSize + s.length());
      tmp.limit(tmp.position());
      tmp.position(0);
      curTransaction.put(tmp);
    }
    curTransaction.put(s.getBytes());
    TxID = sharedData.txId.incrementAndGet();
    sharedData.allTransactions.put(TxID, curTransaction);
    try {
      fileOutputStream.write(Integer.toString(TxID).getBytes());
      fileOutputStream.write(curTransaction.array(), 0,
          curTransaction.position());
    } catch (IOException e) {
      e.printStackTrace();
    }

    curTransaction = null;

    endingTrax = false;
  }

  public void checkAutoCommit(byte[] data, int len) {
    if (len < 6 || len > 30)
      return;

    String s = new String(data, 5, len - 5);
    s = s.toLowerCase();
    s = s.replaceAll("\\s", "");
    if (s.contentEquals("setautocommit=0"))
      autoCommit = false;
    else if (s.contentEquals("setautocommit=1"))
      autoCommit = true;

    if (autoCommit) {
      if (s.contentEquals("begin") || s.contentEquals("starttransaction")) {
        tempAutoCommit = false;
      }
      if (s.contentEquals("commit") || s.contentEquals("rollback")) {
        tempAutoCommit = true;
      }
    }

  }

  private boolean traxEnd(byte[] data, int len) {
    if (len < 6 || len > 18)
      return false;
    String s = new String(data, 5, len - 5);

    s = s.toLowerCase();
    s = s.replaceAll("\\s", "");
    if (s.contentEquals("commit") || s.contentEquals("rollback"))
      return true;
    else
      return false;
  }

  public void flushToFile() {
    if (fileOutputStream != null) {
      try {
        fileOutputStream.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void openFileOutputStream() {

    file = new File(sharedData.getFilePathName() + "/Transactions/client-"
        + clientPortNum + ".txt");
    try {
      fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }

  public void closeFileOutputStream() {
    if (fileOutputStream == null)
      return;

    try {
      fileOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    fileOutputStream = null;
  }

}

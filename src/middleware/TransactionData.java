package middleware;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * The class to store and print data and info about transaction between one
 * client and server
 * 
 * @author Hongyu Wu
 * 
 */
public class TransactionData {

  private MiddleServer middleServer;
  private SharedData sharedData;
  private int clientPortNum;
  private String userId;

  private long traxStart;
  private long traxEnd;
  private boolean inTrax;
  private boolean autoCommit;

  private Timestamp timestamp;

  private File file;
  private BufferedOutputStream fileOutputStream;

  public boolean endingTrax;

  TransactionData(SharedData s, MiddleServer server) {
    sharedData = s;
    middleServer = server;
    clientPortNum = middleServer.getClientPort();
    userId = null;
    sharedData.getMaxSize();

    inTrax = false;
    autoCommit = true;
    endingTrax = false;

    timestamp = new Timestamp(0);

    file = new File(sharedData.getFilePathName() + "/Transactions/client-"
        + clientPortNum + ".txt");
    try {
      fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }

  public void processData(byte[] data, int len, long recTime) {
    String s = "";
    if (!inTrax) {
      if (traxBegin(data, len)) {
        inTrax = true;
        traxStart = recTime;
        timestamp.setTime(traxStart);
        s += sharedData.txId.incrementAndGet() + "," + clientPortNum + "," + userId + ","
            + timestamp.toString() + ",{";
        try {
          fileOutputStream.write(s.getBytes());
          fileOutputStream.write(data, 5, len - 5);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

    } else {

      try {
        fileOutputStream.write(';');
        fileOutputStream.write(data, 5, len - 5);
      } catch (IOException e) {
        e.printStackTrace();
      }

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
    try {
      fileOutputStream.write(s.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    endingTrax = false;
  }

  public void checkAutoCommit(byte[] data, int len) {
    if (len < 6 || len > 21)
      return;

    String s = new String(data, 5, len - 5);
    s = s.toLowerCase();
    s = s.replaceAll("\\s", "");
    if (s.contentEquals("setautocommit=0"))
      autoCommit = false;
    if (s.contentEquals("setautocommit=1"))
      autoCommit = true;

  }

  private boolean traxBegin(byte[] data, int len) {
    if (!autoCommit)
      return true;
    if (len < 6 || len > 22)
      return false;

    String s = new String(data, 5, len - 5);
    s = s.toLowerCase();
    s = s.replaceAll("\\s", "");
    if (s.contentEquals("begin") || s.contentEquals("starttransaction"))
      return true;
    else
      return false;
  }

  private boolean traxEnd(byte[] data, int len) {
    if (len < 6 || len > 15)
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

}

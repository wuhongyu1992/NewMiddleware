package middleware;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
  private boolean endingMonitoring;
  private boolean sendingFiles;
  private boolean monitoring;

  private Map<String, byte[]> userInfo;

  private String zipFileName = "LogFiles.zip";

  private byte[] fileBuffer;

  private MiddleSocketChannel curUser;

  private ArrayList<MiddleSocketChannel> userList;

  NewServerSocket(SharedData s) {
    sharedData = s;
    try {
      serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.socket().bind(
          new InetSocketAddress(s.getMiddlePortNum()));
      serverSocketChannel.configureBlocking(false);

    } catch (IOException e) {
      System.out.println("Error: cannot bind to port " + s.getMiddlePortNum());
      e.printStackTrace();
    }

    try {
      adminServerSocketChannel = ServerSocketChannel.open();
      adminServerSocketChannel.socket().bind(
          new InetSocketAddress(s.getAdminPortNum()));
      adminServerSocketChannel.configureBlocking(false);
    } catch (IOException e) {
      System.out.println("Error: cannot bind to port " + s.getAdminPortNum());
      e.printStackTrace();
    }

    try {
      selector = Selector.open();
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      adminServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      e.printStackTrace();
    }

    keyIterator = null;

    dir = new File(sharedData.getFilePathName() + File.separator
        + "Transactions");
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
    endingMonitoring = false;
    sendingFiles = false;
    monitoring = false;

    sharedData.allTransactionData = new ArrayList<TransactionData>();
    sharedData.allTransactions = new ConcurrentSkipListMap<Integer, byte[]>();
    // sharedData.allStatementsInfo = new ConcurrentLinkedQueue<byte[]>();
    sharedData.allQueries = new ConcurrentSkipListMap<Long, QueryData>();

    userInfo = Encrypt.getUsrMap(sharedData.getUserInfoFilePath());

    fileBuffer = new byte[1024];

    curUser = null;

    userList = new ArrayList<MiddleSocketChannel>();

  }

  public void run() {
    int count = 0;
    while (!sharedData.isEndOfProgram()) {

      try {
        if (monitoring) {
          selector.select(10);
        } else {
          selector.select();
        }
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
            SocketChannel sock = null;
            try {
              sock = adminServerSocketChannel.accept();
            } catch (IOException e) {
              e.printStackTrace();
            }
            if (sock != null) {
              try {
                sock.configureBlocking(true);
              } catch (IOException e) {
                e.printStackTrace();
              }
              MiddleSocketChannel middleSocketChannel = new MiddleSocketChannel(
                  sock);
              middleSocketChannel.setNonBlocking();

              middleSocketChannel.register(selector, middleSocketChannel);

            }
          }
        } else if (key.isReadable()) {
          MiddleSocketChannel middleSocketChannel = (MiddleSocketChannel) key
              .attachment();

          buffer.clear();
          int len = middleSocketChannel.getInput(buffer);
          if (len == -1) {
            middleSocketChannel.cancelKey();
            continue;
          }
          buffer.position(0);

          int packetID = 0;
          long packetLength = -1;
          boolean isValidPacket = true;

          try {
            packetID = buffer.getInt();
            packetLength = buffer.getLong();
          } catch (BufferUnderflowException e) {
            buffer.clear();
            buffer.putInt(102);
            String response = "Invalid packet header";
            buffer.putLong(response.length());
            buffer.put(response.getBytes());
            isValidPacket = false;
            middleSocketChannel.sendOutput(buffer, buffer.position());
          }

          if (isValidPacket) {
            if (packetID == 100) {
              if (userList.contains(middleSocketChannel)) {
                buffer.clear();
                buffer.putInt(102);
                String response = "You have already logged in";
                buffer.putLong(response.length());
                buffer.put(response.getBytes());
                middleSocketChannel.sendOutput(buffer, buffer.position());
              } else if (packetLength <= 0) {
                buffer.clear();
                buffer.putInt(102);
                String response = "Invalid packet length";
                buffer.putLong(response.length());
                buffer.put(response.getBytes());
                middleSocketChannel.sendOutput(buffer, buffer.position());
              } else {
                String userID = null;
                byte[] password = new byte[Encrypt.MAX_LENGTH];
                byte[] packet = new byte[(int) packetLength];
                buffer.get(packet);
                userID = parseLogInPacket(packet, password);
                if (userInfo.get(userID) != null
                    && Arrays.equals(((byte[]) userInfo.get(userID)),
                        Encrypt.encrypt(password))) {
                  buffer.clear();
                  buffer.putInt(101);
                  buffer.putLong(0);
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                  userList.add(middleSocketChannel);
                } else {
                  buffer.clear();
                  buffer.putInt(102);
                  String response = "Invalid User ID or password";
                  buffer.putLong(response.length());
                  buffer.put(response.getBytes());
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                }
              }

            } else if (packetID == 200) {
              if (userList.contains(middleSocketChannel)) {
                if (sharedData.isOutputToFile() || endingMonitoring
                    || sendingFiles) {
                  String response = "Current monitoring not finished";
                  buffer.clear();
                  buffer.putInt(202);
                  buffer.putLong(response.length());
                  buffer.put(response.getBytes());
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                } else {
                  startMonitoring();
                  buffer.clear();
                  buffer.putInt(201);
                  buffer.putLong(0);
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                  curUser = middleSocketChannel;
                }
              } else {
                buffer.clear();
                buffer.putInt(102);
                String response = "You have not been registered";
                buffer.putLong(response.length());
                buffer.put(response.getBytes());
                middleSocketChannel.sendOutput(buffer, buffer.position());
              }

            } else if (packetID == 300) {
              if (userList.contains(middleSocketChannel)) {
                if (!sharedData.isOutputToFile()) {
                  String response = "No monitoring running";
                  buffer.clear();
                  buffer.putInt(302);
                  buffer.putLong(response.length());
                  buffer.put(response.getBytes());
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                } else if (middleSocketChannel != curUser) {
                  String response = "Monitoring running by other user";
                  buffer.clear();
                  buffer.putInt(302);
                  buffer.putLong(response.length());
                  buffer.put(response.getBytes());
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                } else if (endingMonitoring) {
                  String response = "Writing log files, please wait";
                  buffer.clear();
                  buffer.putInt(302);
                  buffer.putLong(response.length());
                  buffer.put(response.getBytes());
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                } else {
                  stopMonitoring();
                }
              } else {
                buffer.clear();
                buffer.putInt(102);
                String response = "You have not been registered";
                buffer.putLong(response.length());
                buffer.put(response.getBytes());
                middleSocketChannel.sendOutput(buffer, buffer.position());
              }

            } else if (packetID == 400) {
              if (userList.contains(middleSocketChannel)) {
                if (monitoring) {
                  buffer.clear();
                  buffer.putInt(402);
                  buffer.putLong(0);
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                } else {
                  buffer.clear();
                  buffer.putInt(401);
                  buffer.putLong(0);
                  middleSocketChannel.sendOutput(buffer, buffer.position());
                }
              } else {
                buffer.clear();
                buffer.putInt(102);
                String response = "You have not been registered";
                buffer.putLong(response.length());
                buffer.put(response.getBytes());
                middleSocketChannel.sendOutput(buffer, buffer.position());
              }

            } else {
              buffer.clear();
              buffer.putInt(102);
              String response = "Invalid packet ID";
              buffer.putLong(response.length());
              buffer.put(response.getBytes());
              middleSocketChannel.sendOutput(buffer, buffer.position());
            }
          }
        }
      }

      if (!sharedData.allTransactions.isEmpty()
          || !sharedData.allQueries.isEmpty()) {
        int c = sharedData.allQueries.size();
        while (c > 0) {
          printQueries();
          --c;
        }
        // c = sharedData.allStatementsInfo.size();
        // while (c > 0) {
        // printStatementsInfo();
        // --c;
        // }
        c = sharedData.allTransactions.size();
        while (c > 0) {
          printTransactions();
          --c;
        }
      } else if (endingMonitoring) {
        try {
          sharedData.tAllLogFileOutputStream.flush();
          sharedData.tAllLogFileOutputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          sharedData.sAllLogFileOutputStream.flush();
          sharedData.sAllLogFileOutputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          sharedData.qAllLogFileOutputStream.flush();
          sharedData.qAllLogFileOutputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

        if (curUser != null) {

          System.out.println("ready to compress log files");

          if (zipAllFiles()) {

            System.out
                .println("finish compressing files, ready to send zip file");

            File zipFile = new File(zipFileName);

            FileInputStream fis = null;
            try {
              fis = new FileInputStream(zipFile);
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            }

            FileChannel fc = fis.getChannel();

            buffer.clear();
            buffer.putInt(301);
            buffer.putLong(zipFile.length());
            curUser.sendOutput(buffer, buffer.position());
            long position = 0;
            long remaining = zipFile.length();
            long len = 0;
            while (remaining > 0) {
              try {
                len = fc.transferTo(position, 1024, curUser.socketChannel);
              } catch (IOException e) {
                e.printStackTrace();
              }
              position += len;
              remaining -= len;
              len = 0;
            }

            System.out.println("finish sending zip file");

          } else {
            String response = "fail to compress log files";
            buffer.clear();
            buffer.putInt(302);
            buffer.putLong(response.length());
            buffer.put(response.getBytes());
            curUser.sendOutput(buffer, buffer.position());
          }
          endingMonitoring = false;
          monitoring = false;
          curUser = null;
        }

      }

    }

  }

  private boolean zipAllFiles() {

    File zipFile = new File(zipFileName);
    int index = 0;
    while (zipFile.exists()) {
      if (!zipFile.delete()) {
        ++index;
        zipFileName = "LogFiles_" + index + ".zip";
        zipFile = new File(zipFileName);
      }
    }

    try {

      FileOutputStream fos = new FileOutputStream(zipFileName);

      ZipOutputStream zos = new ZipOutputStream(fos);

      File[] files = dir.listFiles();

      for (int i = 0; i < files.length; i++) {

        FileInputStream fis = new FileInputStream(files[i]);

        // begin writing a new ZIP entry, positions the stream to the start of
        // the entry data
        zos.putNextEntry(new ZipEntry(files[i].getName()));

        int length;

        while ((length = fis.read(fileBuffer)) > 0) {
          zos.write(fileBuffer, 0, length);
        }

        zos.closeEntry();

        // close the InputStream
        fis.close();
      }

      // close the ZipOutputStream
      zos.close();

    } catch (IOException ioe) {
      return false;
    }

    return true;

  }

  private String parseLogInPacket(byte[] packet, byte[] password) {
    int i = 0;
    StringBuilder s = new StringBuilder();
    for (; i < packet.length; ++i) {
      if (packet[i] == '=' && i + 1 < packet.length) {
        ++i;
        do {
          s.append((char) packet[i]);
          ++i;
        } while (i < packet.length && packet[i] != ' ');
        break;
      }
    }
    for (; i < packet.length; ++i) {
      if (packet[i] == '=' && i + 1 < packet.length) {
        ++i;
        int j = 0;
        do {
          password[j] = packet[i];
          ++i;
          ++j;
        } while (i < packet.length && j < password.length);
        break;
      }
    }

    return s.toString();
  }

  private String getUserId(byte[] b) {
    int i = 36;
    StringBuilder stringBuilder = new StringBuilder();
    while (b[i] > (byte) 32 && b[i] < (byte) 127 && i < b.length) {
      stringBuilder.append((char) b[i]);
      ++i;
    }
    return stringBuilder.toString();
  }

  private void printTransactions() {
    int TxID = sharedData.allTransactions.firstKey();
    byte[] tmpB = sharedData.allTransactions.pollFirstEntry().getValue();

    try {
      sharedData.tAllLogFileOutputStream.write(Integer.toString(TxID)
          .getBytes());
      sharedData.tAllLogFileOutputStream.write(tmpB);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  //
  // private void printStatementsInfo() {
  // byte[] tmpB = sharedData.allStatementsInfo.poll();
  //
  // try {
  // sharedData.sAllLogFileOutputStream.write(tmpB);
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  //
  // }

  private void printQueries() {
    long qId = sharedData.allQueries.firstKey();
    QueryData tmp = sharedData.allQueries.pollFirstEntry().getValue();

    try {
      sharedData.sAllLogFileOutputStream.write(tmp.statementInfo);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      sharedData.qAllLogFileOutputStream.write(Long.toString(qId).getBytes());
      sharedData.qAllLogFileOutputStream.write(tmp.query.array(), 0,
          tmp.query.position());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void startMonitoring() {
    for (File f : dir.listFiles()) {
      if (!f.delete()) {
        // TODO
      }
    }

    if (!zipFileName.contentEquals("LogFiles.zip")) {
      zipFileName = "LogFiles.zip";
    }
    File oldZipFile = new File(zipFileName);
    if (oldZipFile.exists()) {
      oldZipFile.delete();
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
      sharedData.tAllLogFileOutputStream = new BufferedOutputStream(
          new FileOutputStream(new File(sharedData.getFilePathName()
              + "/Transactions/allLogs-t.txt")));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    try {
      sharedData.sAllLogFileOutputStream = new BufferedOutputStream(
          new FileOutputStream(new File(sharedData.getFilePathName()
              + "/Transactions/allLogs-s.txt")));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    try {
      sharedData.qAllLogFileOutputStream = new BufferedOutputStream(
          new FileOutputStream(new File(sharedData.getFilePathName()
              + "/Transactions/allLogs-q.txt")));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    sharedData.txId.set(0);
    sharedData.queryId.set(0);

    sharedData.setOutputToFile(true);
    monitoring = true;
    selector.wakeup();

    System.out.println("start monitoring");

  }

  public void stopMonitoring() {
    sharedData.setOutputToFile(false);
    endingMonitoring = true;

    for (int i = 0; i < sharedData.allTransactionData.size();) {
      TransactionData tmp = sharedData.allTransactionData.get(i);
      if (tmp.isAlive) {
        tmp.closeFileOutputStream();
        ++i;
      } else {
        sharedData.allTransactionData.remove(i);
      }
    }

    System.out.println("stop monitoring");

  }

}

package middleware;

import java.util.Scanner;

public class NewMiddleware {

  public static void main(String[] args) {
    if (args.length < 5) {
      System.out.println("Error: too few arguments");
      System.out
          .println("Usage: ./middleware <listening_port> <MySQL IP> <MySQL Port> <Thread Number> <UserPasswordFile>");
      return;
    }
    SharedData sharedData = new SharedData();
    int middlePortNum = Integer.parseInt(args[0]);
    if (middlePortNum > 65536 || middlePortNum < 0) {
      System.out.println("Error: invalid listening port: " + middlePortNum);
      return;
    }

    String serverIpAddr = args[1];
    int serverPortNum = Integer.parseInt(args[2]);
    if (serverPortNum > 65536 || serverPortNum < 0) {
      System.out.println("Error: invalid server port: " + serverPortNum);
      return;
    }

    int numThreads = Integer.parseInt(args[3]);
    if (numThreads <= 0) {
      System.out.println("Error: invalid thread number: " + numThreads);
      return;
    }

    sharedData.setUserInfoFilePath(args[4]);

    int adminPortNum = 3334;
    if (args.length > 5) {
      adminPortNum = Integer.parseInt(args[4]);
    }

    sharedData.setMaxSize(16 * 1024);
    sharedData.setServerIpAddr(serverIpAddr);
    sharedData.setServerPortNum(serverPortNum);
    sharedData.setMiddlePortNum(middlePortNum);
    sharedData.setAdminPortNum(adminPortNum);
    sharedData.setFilePathName(".");
    sharedData.setOutputToFile(false);
    sharedData.setNumWorkers(numThreads);

    // MiddleServerSocket middleServerSock = new MiddleServerSocket(sharedData);
    // middleServerSock.start();

    NewServerSocket serverSocket = new NewServerSocket(sharedData);
    serverSocket.start();

    // RequestHandler requestHandler = new RequestHandler(sharedData);
    // requestHandler.start();
    
    Scanner scanner = new Scanner(System.in);
    
    while (!sharedData.isEndOfProgram()) {

      String line = null;
      if (scanner.hasNextLine()) {
        line = scanner.nextLine();
      }
      if (line == null || line.isEmpty())
        continue;
      if (line.contentEquals("q")) {
        sharedData.setEndOfProgram(true);
      }
      if (line.contentEquals("o")) {
        serverSocket.startMonitoring();
        sharedData.setOutputToFile(true);
      }
      if (line.contentEquals("c")) {
        serverSocket.stopMonitoring();
        sharedData.setOutputToFile(false);
      }
      if (line.contentEquals("f")) {
        sharedData.setOutputFlag(false);
      }
      if (line.contentEquals("t")) {
        sharedData.setOutputFlag(true);
      }
    }
    scanner.close();
    
    return;
  }

}

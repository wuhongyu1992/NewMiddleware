package middleware;

import java.util.Scanner;

public class NewMiddleware {

	public static void main(String[] args) {
		SharedData sharedData = new SharedData();

		sharedData.setMaxSize(16 * 1024);
		sharedData.setServerIpAddr("127.0.0.1");
		sharedData.setServerPortNum(3306);
		sharedData.setMiddlePortNum(3320);
		sharedData.setFilePathName(".");
		sharedData.setOutputToFile(true);
		sharedData.setFileOutputStream();
		sharedData.setOutputSize(10000);
		sharedData.setNumWorkers(4);

//		MiddleServerSocket middleServerSock = new MiddleServerSocket(sharedData);
//		middleServerSock.start();
		
		NewServerSocket serverSocket = new NewServerSocket(sharedData);
		serverSocket.start();
		
//		RequestHandler requestHandler = new RequestHandler(sharedData);
//		requestHandler.start();

		Scanner scanner = new Scanner(System.in);
		String s = "";

		System.out.println("Start");

		while (!sharedData.isEndOfProgram()) {
			s = scanner.nextLine();
			if (s.isEmpty())
				continue;
			if (s.contentEquals("q")) {
				sharedData.setEndOfProgram(true);
			}
			if (s.contentEquals("o")) {
				sharedData.setOutputToFile(true);
			}
			if (s.contentEquals("c")) {
				sharedData.setOutputToFile(false);
			}
			if (s.contentEquals("f")) {
				sharedData.setOutputFlag(false);
			}
			if (s.contentEquals("t")) {
				sharedData.setOutputFlag(true);
			}

			if (s.contentEquals("p")) {
				sharedData.setClearClients(true);
			}
			// if (s.contentEquals("k")) {
			// sharedData.killAllUnits();
			// }

			if (s.charAt(0) == 's') {
				s = s.replace('s', ' ');
				s = s.trim();
				try {
					int bufferSize = Integer.parseInt(s);
					sharedData.setOutputSize(bufferSize);
					System.out.println("Set file buffer size to " + bufferSize);
				} catch (Exception e) {
					System.out.println("invalid input");
				}
			}

		}

		sharedData.flushOutput();
		System.out.println("main end");
		scanner.close();

		return;
	}

}

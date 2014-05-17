package middleware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MiddlewareUnit extends Thread {

	private MiddleServer middleServer;
	private MiddleClient middleClient;
	private SharedData sharedData;
	private int maxSize;
	private int clientPortNum;

	private byte[] clientData;
	private int clientDataLen;
	private ByteBuffer clientBuffer;

	private byte[] serverData;
	private int serverDataLen;
	private ByteBuffer serverBuffer;

	// private ArrayList<Byte> clientDataArray;
	// private ArrayList<Byte> serverDataArray;

	private ArrayList<String> trax;
	private int traxNum;
	private long traxStart;
	private long traxEnd;
	private boolean inTrax;
	private boolean autoCommit;

	// private int latency;
	private long sendTime;
	private long recTime;
	private Calendar cal;
	private Date date;

	private File file;
	private FileOutputStream fileOutputStream;
	private PrintWriter printWriter;

	private int clientID;

	private long ts0, ts1, ts2, ts3, ts4, ts5, ts6;
	private long t0 = 0, t1 = 0, t2 = 0, t3 = 0, t4 = 0, t5 = 0;

	MiddlewareUnit(SharedData s) {
		sharedData = s;
		maxSize = sharedData.getMaxSize();

		middleServer = new MiddleServer();
		middleClient = new MiddleClient(sharedData.getServerIpAddr(),
				sharedData.getServerPortNum());

		clientData = new byte[maxSize];
		clientDataLen = 0;
		clientBuffer = ByteBuffer.wrap(clientData);

		serverData = new byte[maxSize];
		serverDataLen = 0;
		serverBuffer = ByteBuffer.wrap(serverData);

		// clientDataArray = new ArrayList<Byte>();
		// serverDataArray = new ArrayList<Byte>();

		trax = new ArrayList<String>();
		traxNum = 0;
		inTrax = false;
		autoCommit = true;

		sendTime = 0;
		recTime = 0;
		cal = new GregorianCalendar();
		date = new Date();

		file = null;
		fileOutputStream = null;
		printWriter = null;
	}

	public void run() {

		while (!sharedData.isEndOfProgram() && !sharedData.isClearClients()) {

			ts0 = System.currentTimeMillis();

			clientDataLen = middleServer.getInput(clientBuffer);
			if (clientDataLen > 0) {

				ts1 = System.currentTimeMillis();
				// showData(clientData, clientDataLen);
				t0 += ts1 - ts0;

				checkAutoCommit();
				if (sharedData.isOutputToFile() && !inTrax && traxBegin()) {
					inTrax = true;
					traxStart = System.currentTimeMillis();
					// System.out.println("Transaction begins.");

				}

				recTime = 0;

				ts2 = System.currentTimeMillis();

				middleClient.sendOutput(clientBuffer, clientDataLen);
				clientBuffer.clear();

				sendTime = System.currentTimeMillis();
				if (clientQuit())
					break;
				ts3 = System.currentTimeMillis();

				if (inTrax) {
					addSQLToTrax();
					if (traxEnd()) {
						inTrax = false;
						traxEnd = System.currentTimeMillis();
						// System.out.println("Transaction ends.");

						printTrax();
						trax.clear();
					}
				}
				ts4 = System.currentTimeMillis();

				t1 += ts2 - ts1;
				t2 += ts3 - ts2;
				t3 += ts4 - ts3;
			}

			ts4 = System.currentTimeMillis();

			serverDataLen = middleClient.getInput(serverBuffer);

			if (serverDataLen > 0) {
				ts5 = System.currentTimeMillis();
				// showData(serverData, serverDataLen);

				middleServer.sendOutput(serverBuffer, serverDataLen);
				serverBuffer.clear();

				ts6 = System.currentTimeMillis();

				t4 += ts5 - ts4;
				t5 += ts6 - ts5;
			}

		}
		middleServer.close();
		middleClient.close();

		if (printWriter != null) {
			printWriter.close();
		}

		if (printWriter != null)
			printWriter.flush();
		System.out.println("client(" + clientPortNum + ") quit");
		long t = t0 + t1 + t2 + t3 + t4 + t5;
		System.out.println("t0: " + (double) t0 / t);
		System.out.println("t1: " + (double) t1 / t);
		System.out.println("t2: " + (double) t2 / t);
		System.out.println("t3: " + (double) t3 / t);
		System.out.println("t4: " + (double) t4 / t);
		System.out.println("t5: " + (double) t5 / t);
		System.out.println();

	}

	private boolean clientQuit() {
		if (clientDataLen < 5)
			return false;
		if (clientData[4] == (byte) 1)
			return true;
		return false;
	}

	synchronized public boolean setUp(SocketChannel socketChannel) {
		middleServer.startServer(socketChannel);
		middleClient.startClient();
		clientPortNum = middleServer.getClientPort();

		// connect server
		serverBuffer.clear();
		do {
			serverDataLen = middleClient.getInput(serverBuffer);
		} while (serverDataLen == 0);

		if (sharedData.isOutputFlag()) {
			System.out.println("s");
			showData(serverData, serverDataLen);
		}
		middleServer.sendOutput(serverBuffer, serverDataLen);

		// get client info
		clientBuffer.clear();
		do {
			clientDataLen = middleServer.getInput(clientBuffer);
		} while (clientDataLen == 0);

		if (sharedData.isOutputFlag()) {
			System.out.println("c");
			showData(clientData, clientDataLen);
			System.out.println("send client info");
		}

		middleClient.sendOutput(clientBuffer, clientDataLen);

		// get server OK packet
		serverBuffer.clear();
		do {
			serverDataLen = middleClient.getInput(serverBuffer);
		} while (serverDataLen == 0);

		if (sharedData.isOutputFlag()) {
			System.out.println("s");
			showData(serverData, serverDataLen);
		}

		if (isErrorPacket(serverData, serverDataLen)) {
			printFailConnection();
			return false;
		}

		if (sharedData.isOutputFlag())
			System.out.println("get server OK packet");

		middleServer.sendOutput(serverBuffer, serverDataLen);

		sharedData.addClient();
		clientID = sharedData.getNumClient();
		try {
			setFileOutputStream();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("client(" + clientPortNum + ") login");

		clientBuffer.clear();
		serverBuffer.clear();
		return true;
	}

	private boolean isErrorPacket(byte[] b, int len) {
		if (len < 5)
			return false;
		if (b[4] == (byte) 255)
			return true;

		return false;
	}

	private static void showData(byte[] b, int len) {
		System.out.print(len + ": ");
		System.out.printf("%02x", b[0]);
		System.out.print(" ");
		System.out.printf("%02x", b[1]);
		System.out.print(" ");
		System.out.printf("%02x", b[2]);
		System.out.print(" ");
		System.out.printf("%02x", b[3]);
		System.out.print(" ");
		System.out.printf("%02x", b[4]);
		System.out.print(" ");
		for (int i = 5; i < len; ++i) {
			if (b[i] < (byte) 32) {
				// System.out.print(new String ("'"));
				// System.out.print((byte) b[i]);
				// System.out.print(new String ("'"));
				System.out.print(".");

			} else if (b[i] >= (byte) 32 && b[i] < (byte) 127)
				System.out.print((char) b[i]);
			else
				System.out.printf(" %02x ", b[i]);
		}
		System.out.println();

	}

	private void addSQLToTrax() {
		StringBuilder sb = new StringBuilder();

		sb.append("Statement ID: ");
		sb.append(trax.size() / 2 + 1);
		sb.append("   Start: ");
		sb.append(getTimeString(sendTime));
		sb.append("   End: ");
		sb.append(getTimeString(recTime));
		trax.add(sb.toString());

		sb = new StringBuilder(clientDataLen - 5);
		for (int i = 5; i < clientDataLen; ++i) {
			if (clientData[i] < (byte) 32) {
				sb.append('.');

			} else {
				sb.append((char) clientData[i]);
			}

		}
		trax.add(sb.toString());
	}

	private String getTimeString(long t) {
		date.setTime(t);
		cal.setTime(date);
		String s = "";

		s += cal.get(Calendar.YEAR) + '-' + cal.get(Calendar.MONTH) + 1 + '-'
				+ cal.get(Calendar.DATE) + ' ' + cal.get(Calendar.HOUR) + ':'
				+ cal.get(Calendar.MINUTE) + ':' + cal.get(Calendar.SECOND)
				+ ',' + cal.get(Calendar.MILLISECOND);

		return s;
	}

	private void setFileOutputStream() throws FileNotFoundException {
		file = new File(sharedData.getFilePathName() + "/Transactions/C"
				+ clientPortNum + ".txt");
		try {
			fileOutputStream = new FileOutputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		printWriter = new PrintWriter(fileOutputStream, false);
	}

	private void printTrax() {
		++traxNum;
		String s = "Client ID: " + clientID + "   Transaction ID: " + traxNum
				+ "   Start: " + getTimeString(traxStart) + "   End: "
				+ getTimeString(traxEnd) + "   Latency: "
				+ (traxEnd - traxStart) + " ms";
		printWriter.println(s);
		sharedData.printTrax(s);
		for (int i = 0; i < trax.size(); ++i) {
			printWriter.println(trax.get(i));
			sharedData.printTrax(trax.get(i));
			// System.out.println(trax.get(i));
		}
		printWriter.println();
		sharedData.printTrax("");
		sharedData.addFileBufferSize(trax.size() / 2);

	}

	private void checkAutoCommit() {
		if (clientDataLen < 6 || clientDataLen > 21)
			return;

		String s = new String(clientData, 5, clientDataLen - 5);
		s = s.toLowerCase();
		s = s.replaceAll("\\s", "");
		if (s.contentEquals("setautocommit=0"))
			autoCommit = false;
		if (s.contentEquals("setautocommit=1"))
			autoCommit = true;

	}

	private boolean traxBegin() {
		if (!autoCommit)
			return true;
		if (clientDataLen < 6 || clientDataLen > 22)
			return false;

		String s = new String(clientData, 5, clientDataLen - 5);
		s = s.toLowerCase();
		s = s.replaceAll("\\s", "");
		// System.out.println(s);
		if (s.contentEquals("begin") || s.contentEquals("starttransaction"))
			return true;
		else
			return false;
	}

	// public void addToClientData(byte[] clientData, int clientDataLen) {
	//
	// }

	private boolean traxEnd() {
		if (clientDataLen < 6 || clientDataLen > 15)
			return false;
		String s = new String(clientData, 5, clientDataLen - 5);

		s = s.toLowerCase();
		s = s.replaceAll("\\s", "");
		if (s.contentEquals("commit") || s.contentEquals("rollback"))
			return true;
		else
			return false;
	}

	private void printFailConnection() {
		System.out.println("client(" + clientPortNum + ") fails connection.");
	}

}

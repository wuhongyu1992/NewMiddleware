package middleware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SharedData {
	private int maxSize;
	private int numClient;
	private String serverIpAddr;
	private int serverPortNum;
	private int middlePortNum;
	private boolean endOfProgram;
	private boolean outputToFile;
	private boolean clearClients;
	private String filePathName;
	private File file;
	private FileOutputStream fileOutputStream;
	private PrintWriter printWriter;
	private int fileBufferSize;
	private ArrayList<MiddlewareUnit> units;
	private boolean outputFlag;
	private int outputSize;
	private HashMap<SocketChannel, MiddleSocketChannel> socketMap;
	private int numWorkers;
	private Iterator<SelectionKey> keyIterator;

	private long selectTime, inputTime, outputTime;

	public Selector selector;

	SharedData() {
		maxSize = 1024;
		numClient = 0;
		endOfProgram = false;
		outputToFile = false;
		filePathName = null;
		units = new ArrayList<MiddlewareUnit>();
		outputFlag = false;
		clearClients = false;
		socketMap = new HashMap<SocketChannel, MiddleSocketChannel>();
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		keyIterator = selector.selectedKeys().iterator();
	}

	synchronized public SelectionKey getSelectionKey() {

		while (!keyIterator.hasNext()) {
			try {
				selector.selectNow();
			} catch (IOException e) {
				e.printStackTrace();
			}
			keyIterator = selector.selectedKeys().iterator();
		}

		SelectionKey key = keyIterator.next();

		keyIterator.remove();
		return key;

	}

	public void putInMap(SocketChannel sc, MiddleSocketChannel msc) {
		socketMap.put(sc, msc);
	}

	public void setFileOutputStream() {
		file = new File(filePathName + "/Transactions/AllTransactions.txt");
		int i = 0;
		while (file.exists() && !file.isDirectory()) {
			++i;
			file = new File(filePathName + "/Transactions/AllTransactions(" + i
					+ ").txt");
		}

		try {
			fileOutputStream = new FileOutputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		printWriter = new PrintWriter(fileOutputStream, false);
	}

	public void printTrax(String s) {
		printWriter.println(s);
	}

	public void flushOutput() {
		printWriter.flush();
		fileBufferSize = 0;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getNumClient() {
		return numClient;
	}

	public void addClient() {
		++numClient;
	}

	public void subClient() {
		--numClient;
	}

	public int getMiddlePortNum() {
		return middlePortNum;
	}

	public void setMiddlePortNum(int middlePortNum) {
		this.middlePortNum = middlePortNum;
	}

	public int getServerPortNum() {
		return serverPortNum;
	}

	public void setServerPortNum(int serverPortNum) {
		this.serverPortNum = serverPortNum;
	}

	public String getServerIpAddr() {
		return serverIpAddr;
	}

	public void setServerIpAddr(String serverIpAddr) {
		this.serverIpAddr = serverIpAddr;
	}

	public boolean isEndOfProgram() {
		return endOfProgram;
	}

	public void setEndOfProgram(boolean endOfProgram) {
		this.endOfProgram = endOfProgram;
	}

	public boolean isOutputToFile() {
		return outputToFile;
	}

	public void setOutputToFile(boolean outputToFile) {
		this.outputToFile = outputToFile;
	}

	public String getFilePathName() {
		return filePathName;
	}

	public void setFilePathName(String filePathName) {
		this.filePathName = filePathName;
		file = new File(filePathName + "/Transactions");
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public int getFileBufferSize() {
		return fileBufferSize;
	}

	public void addFileBufferSize(int n) {
		fileBufferSize += n;
	}

	public void addUnit(MiddlewareUnit newUnit) {
		units.add(newUnit);
	}

	public boolean isOutputFlag() {
		return outputFlag;
	}

	public void setOutputFlag(boolean outputFlag) {
		this.outputFlag = outputFlag;
	}

	public boolean isClearClients() {
		return clearClients;
	}

	public void setClearClients(boolean clearClients) {
		this.clearClients = clearClients;
	}

	public int getOutputSize() {
		return outputSize;
	}

	public void setOutputSize(int outputSize) {
		this.outputSize = outputSize;
	}

	public int getNumWorkers() {
		return numWorkers;
	}

	public void setNumWorkers(int numWorkers) {
		this.numWorkers = numWorkers;
	}

	public MiddleSocketChannel getSocket(SelectableChannel key) {
		return socketMap.get(key);
	}

	public long getSelectTime() {
		return selectTime;
	}

	public void setSelectTime(long selectTime) {
		this.selectTime = selectTime;
	}

	public long getInputTime() {
		return inputTime;
	}

	public void setInputTime(long inputTime) {
		this.inputTime = inputTime;
	}

	public long getOutputTime() {
		return outputTime;
	}

	public void setOutputTime(long outputTime) {
		this.outputTime = outputTime;
	}

	public void addSelectTime(long t) {
		this.selectTime += t;
	}

	public void addInputTime(long t) {
		this.inputTime += t;
	}

	public void addOutputTime(long t) {
		this.outputTime += t;
	}

}

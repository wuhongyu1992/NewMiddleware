package middleware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

/** 
 * The class to place data shared by all other classes
 * 
 * @author Hongyu Wu
 */
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
	private boolean outputFlag;
	private int numWorkers;

	private long selectTime, inputTime, outputTime, returnTime;

	public HashMap<SocketChannel, MiddleSocketChannel> socketMap;
	public Selector selector;
	public Iterator<SelectionKey> keyIterator;

	SharedData() {
		maxSize = 1024;
		numClient = 0;
		endOfProgram = false;
		outputToFile = false;
		filePathName = null;
		outputFlag = false;
		clearClients = false;
		
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

	public int getNumWorkers() {
		return numWorkers;
	}

	public void setNumWorkers(int numWorkers) {
		this.numWorkers = numWorkers;
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

	public long getReturnTime() {
		return returnTime;
	}

	public void setReturnTime(long returnTime) {
		this.returnTime = returnTime;
	}

	public void addReturnTime(long t) {
		this.returnTime += t;
	}

}

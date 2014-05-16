package middleware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

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

	SharedData() {
		maxSize = 1024;
		numClient = 0;
		endOfProgram = false;
		outputToFile = false;
		filePathName = null;
		units = new ArrayList<MiddlewareUnit>();
		outputFlag = false;
		clearClients = false;
	}
	
	public void setFileOutputStream() {
		file = new File(filePathName + "/Transactions/AllTransactions.txt");
		int i = 0;
		while (file.exists() && !file.isDirectory()) {
			++i;
			file = new File(filePathName + "/Transactions/AllTransactions(" + i + ").txt");
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

	/*@SuppressWarnings("deprecation")
	public void killAllUnits() {
		while (!units.isEmpty()) {
			if (units.get(0).isAlive())
				units.remove(0);
			else 
				units.get(0).stop();
		}
	}*/

}

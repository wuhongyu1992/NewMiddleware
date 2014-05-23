package middleware;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TransactionData {

	private MiddleServer middleServer;
	private SharedData sharedData;
	private int clientPortNum;

	private int traxId;
	private long traxStart;
	private long traxEnd;
	private boolean inTrax;
	private boolean autoCommit;

	private Calendar cal;
	private Date date;

	private File file;
	private BufferedOutputStream fileOutputStream;

	private int statementId;

	public boolean endingTrax;

	TransactionData(SharedData s, MiddleServer server) {
		sharedData = s;
		middleServer = server;
		clientPortNum = middleServer.getClientPort();
		sharedData.getMaxSize();

		traxId = 0;
		inTrax = false;
		autoCommit = true;
		endingTrax = false;

		statementId = 0;

		cal = new GregorianCalendar();
		date = new Date();

		file = new File(sharedData.getFilePathName() + "/Transactions/Client-"
				+ clientPortNum + ".txt");
		try {
			fileOutputStream = new BufferedOutputStream(new FileOutputStream(
					file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void processData(byte[] data, int len, long recTime) {
		String s = "";
		if (!inTrax) {
			if (traxBegin(data, len)) {
				inTrax = true;
				statementId = 1;
				++traxId;
				traxStart = recTime;
				s += "------Transaction ID: " + traxId + "   Start Time: "
						+ getTimeString(traxStart) + "------\nStatement ID: "
						+ statementId + "\n";
				try {
					fileOutputStream.write(s.getBytes());
					fileOutputStream.write(data, 5, len - 5);
					fileOutputStream.write('\n');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} else {
			++statementId;

			s += "Statement ID: " + statementId + "\n";

			try {
				fileOutputStream.write(s.getBytes());
				fileOutputStream.write(data, 5, len - 5);
				fileOutputStream.write('\n');
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (traxEnd(data, len)) {
				inTrax = false;
				statementId = 0;
				endingTrax = true;

			}

		}
	}

	public void endTrax(long t) {
		traxEnd = t;

		String s = "------" + "End Time: " + getTimeString(traxEnd)
				+ "   Latency: " + (traxEnd - traxStart) + " ms------\n\n";
		try {
			fileOutputStream.write(s.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		endingTrax = false;
	}

	public String getTimeString(long t) {
		date.setTime(t);
		cal.setTime(date);
		String s = "";

		s += cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + '-'
				+ cal.get(Calendar.DATE) + ' ' + cal.get(Calendar.HOUR) + ':'
				+ cal.get(Calendar.MINUTE) + ':' + cal.get(Calendar.SECOND)
				+ ',' + cal.get(Calendar.MILLISECOND);

		return s;
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

}

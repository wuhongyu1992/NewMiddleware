
package middleware;

import java.security.MessageDigest;  
import java.util.HashMap;
import java.util.Map;

import java.security.NoSuchAlgorithmException;  
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
  
public class Encrypt {  
    public static byte[] eccrypt(char[] info) throws NoSuchAlgorithmException{  
        MessageDigest md5 = MessageDigest.getInstance("MD5");  
        byte[] srcBytes = String.valueOf(info).getBytes();  
        md5.update(srcBytes);  
        byte[] resultBytes = md5.digest();  
        return resultBytes;  
    }  
    
    public static Map<String, byte[]> getUsrMap(String filePath) throws NoSuchAlgorithmException {
    	Map usrInfo = new HashMap();
    	encode md5 = new encode();   
    	BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filePath));
			while ((sCurrentLine = br.readLine()) != null) {
					char[] tempchars = new char[30];
		            int i = 0, tempchar = 0;
		            while ((tempchar = br.read()) != -1) {
		            	if (((char) tempchar) == '\n') {
		            		usrInfo.put(sCurrentLine, md5.eccrypt(tempchars));
		            		break;
		            	}
		            	tempchars[i++] = (char) tempchar;
		            }
		            for (int j = 0; j < tempchars.length ; j++) tempchars[j] = ' ';	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return usrInfo;
    }
      
      
    public static void main (String[] args) throws NoSuchAlgorithmException{  
        String password = "password2"; // password usr input
        String usrName = "usr1"; // usrname usr input
        char[] msg = new char[30];
        char[] strChar = password.toCharArray();
        for (int i = 0 ; i < strChar.length ; i++){
          msg[i] = strChar[i];
          strChar[i] = ' ';
       }
        
       
        encode md5 = new encode();  
        byte[] resultBytes = md5.eccrypt(msg); 
        String filePath = "   ";
        Map k = md5.getUsrMap(filePath);// 
        if (k.containsKey(usrName)){
        	byte[] tmp = (byte[]) k.get(usrName);
        	if (Arrays.equals(tmp, resultBytes)){System.out.println("get it");}
        }
    }  
  
}  


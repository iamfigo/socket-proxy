package socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketThread extends Thread {
	private Socket socketIn;
	private InputStream isIn;
	private OutputStream osIn;
	//
	private Socket socketOut;
	private InputStream isOut;
	private OutputStream osOut;
	private String hostPort;

	public SocketThread(Socket socket, String hostPort) {
		this.socketIn = socket;
		this.hostPort = hostPort;
		this.setName(hostPort);
	}

	private byte[] buffer = new byte[4096];
	private static final byte[] VER = { 0x5, 0x0 };
	private static final byte[] CONNECT_OK = { 0x5, 0x0, 0x0, 0x1, 0, 0, 0, 0, 0, 0 };
/**

< 05 01 00 
> 05 00 
< 05 01 00 01 65 E2 67 70 00 50 
 host=101.226.103.112,port=80
 
< 05 01 00 
> 05 00 
< 05 01 00 03 0B 71 74 2E 67 74 69 6D 67 2E 63 6E 00 50 
 *
 */
	public void run() {
		try {
			System.out.println("\n\na client connect " + socketIn.getInetAddress() + ":" + socketIn.getPort());
			isIn = socketIn.getInputStream();
			osIn = socketIn.getOutputStream();
			int len = isIn.read(buffer);
			System.out.println("< " + bytesToHexString(buffer, 0, len));
			osIn.write(VER);
			osIn.flush();
			System.out.println("> " + bytesToHexString(VER, 0, VER.length));
			len = isIn.read(buffer);
			System.out.println("< " + bytesToHexString(buffer, 0, len));
			// 查找主机和端口
			
			byte[] dest = new byte[buffer.length-5]; 
			System.arraycopy(buffer, 5, dest, 0, buffer.length);
			String test = new String(dest);
			System.out.println(test);
			
			String host = findHost(buffer, 4, 7);
			if (null != SocketProxy.allowInsideIp) {
				boolean isAllow = false;
				for (String allowIp : SocketProxy.allowInsideIp) {
					if (host.startsWith(allowIp)) {
						isAllow = true;
					}
				}
				if (!isAllow) {
					System.out.println("insideNotAllowConect:" + host);
					socketIn.close();
					return;
				}
			}
			int port = findPort(buffer, 8, 9);
			System.out.println("host=" + host + ",port=" + port);
			if (port > 0 && port < 65535) {
				socketOut = new Socket(host, port);
				socketOut.setSoTimeout(SocketProxy.SO_TIMEOUT);
			} else {
				return;
			}
			isOut = socketOut.getInputStream();
			osOut = socketOut.getOutputStream();
			//
			for (int i = 4; i <= 9; i++) {
				CONNECT_OK[i] = buffer[i];
			}
			osIn.write(CONNECT_OK);
			osIn.flush();
			System.out.println("> " + bytesToHexString(CONNECT_OK, 0, CONNECT_OK.length));
			SocketThreadOutput out = new SocketThreadOutput(isIn, osOut, hostPort + "-out");
			out.start();
			SocketThreadInput in = new SocketThreadInput(isOut, osIn, hostPort + "-in");
			in.start();
			out.join();
			in.join();
		} catch (Exception e) {
			if (SocketProxy.debug) {
				e.printStackTrace();
				System.out.println("a client leave");
			}
		} finally {
			try {
				if (socketIn != null) {
					socketIn.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("socket close");
	}

	public static String findHost(byte[] bArray, int begin, int end) {
		StringBuffer sb = new StringBuffer();
		for (int i = begin; i <= end; i++) {
			sb.append(Integer.toString(0xFF & bArray[i]));
			sb.append(".");		
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static int findPort(byte[] bArray, int begin, int end) {
		int port = 0;
		for (int i = begin; i <= end; i++) {
			//port <<= 8;
			//port += bArray[i];
			port <<= 8;
			port |= bArray[i] & 0xff;
		}
		return port;
	}

	// 4A 7D EB 69
	// 74 125 235 105
	public static final String bytesToHexString(byte[] bArray, int begin, int end) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = begin; i < end; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
			sb.append(" ");
		}
		return sb.toString();
	}
}

package org.javaren.proxy;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * 106.3.131.68:2002 线上代理
 * 10.0.108.12:8999 社区API
 * 10.0.204.51:8999 直播API
 * java -jar socket-proxy.jar port=2001 debug=true allowInsideIp=172.20.16.22 allowOutsideIp=172.20.16.31 so=30
 * @author huit
 *
 */
public class SocketProxy {
	public static int SO_TIMEOUT = 30;
	public static boolean debug = false;
	public static Set<String> allowInsideIp;
	public static Set<String> allowOutsideIp;

	public static void main(String[] args) throws Exception {
		String port = "8888";
		for (String arg : args) {
			if (arg.startsWith("port=")) {
				port = arg.split("=")[1];
			} else if (arg.startsWith("debug=")) {
				debug = Boolean.valueOf(arg.split("=")[1]);
			} else if (arg.startsWith("so=")) {
				SO_TIMEOUT = Integer.valueOf(arg.split("=")[1]);
			} else if (arg.startsWith("allowInsideIp=")) {
				String whiteList = arg.split("=")[1];
				if (null != whiteList && whiteList.length() > 0) {
					allowInsideIp = new HashSet<String>();
					for (String uid : whiteList.split(",")) {
						allowInsideIp.add(uid);
					}
				}
			} else if (arg.startsWith("allowOutsideIp=")) {
				String whiteList = arg.split("=")[1];
				if (null != whiteList && whiteList.length() > 0) {
					allowOutsideIp = new HashSet<String>();
					for (String uid : whiteList.split(",")) {
						allowOutsideIp.add(uid);
					}
				}
			} else {
				System.out.print("java -jar socket-proxy.jar ");
				System.out.println("port=2001 debug=true allowInsideIp=172.20.16.22 allowOutsideIp=172.20.16.31 so=10");
			}
		}
		int portInt = Integer.valueOf(port);

		System.out.println("serverStart->port=" + port + " debug=" + debug + " allowInsideIp=" + allowInsideIp
				+ " allowOutsideIp=" + allowOutsideIp);
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(portInt);
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				socket.setSoTimeout(SO_TIMEOUT);
				String hostPort = socket.getRemoteSocketAddress().toString().substring(1);
				if (null != SocketProxy.allowOutsideIp) {
					boolean isAllow = false;
					for (String allowIp : SocketProxy.allowOutsideIp) {
						if (hostPort.startsWith(allowIp)) {
							isAllow = true;
						}
					}
					if (!isAllow) {
						if (debug) {
							System.out.println("outsideNotAllowConect:" + hostPort);
						}
						socket.close();
						continue;
					}
				}
				new SocketThread(socket, hostPort).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

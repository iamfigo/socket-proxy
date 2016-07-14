package org.javaren.proxy;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * java -jar socket-proxy.jar port=2001 debug=true allowInsideIp=172.20.16.22 allowOutsideIp=172.20.16.31
 * @author huit
 *
 */
public class SocketProxy {
	public static boolean debug = false;
	public static Set<String> allowInsideIp;
	public static Set<String> allowOutsideIp;

	public static void main(String[] args) throws Exception {
		String port = "8888";
		for (String arg : args) {
			if (arg.startsWith("port=")) {
				port = arg.split("=")[1];
				System.out.println("port->" + port);
			} else if (arg.startsWith("debug=")) {
				debug = Boolean.valueOf(arg.split("=")[1]);
				System.out.println("print->" + debug);
			} else if (arg.startsWith("allowInsideIp=")) {
				String whiteList = arg.split("=")[1];
				if (null != whiteList && whiteList.length() > 0) {
					allowInsideIp = new HashSet<String>();
					for (String uid : whiteList.split(",")) {
						allowInsideIp.add(uid);
					}
					System.out.println("allowInsideIp->" + allowInsideIp);
				}
			} else if (arg.startsWith("allowOutsideIp=")) {
				String whiteList = arg.split("=")[1];
				if (null != whiteList && whiteList.length() > 0) {
					allowOutsideIp = new HashSet<String>();
					for (String uid : whiteList.split(",")) {
						allowOutsideIp.add(uid);
					}
					System.out.println("allowOutsideIp->" + allowOutsideIp);
				}
			} else {
				System.out
						.println("java -jar socket-proxy.jar port=2001 debug=true allowInsideIp=172.20.16.22 allowOutsideIp=172.20.16.31");
			}

		}
		int portInt = Integer.valueOf(port);
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(portInt);
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				String hostPort = null;
				if (null != SocketProxy.allowOutsideIp) {
					hostPort = socket.getRemoteSocketAddress().toString().substring(1);
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
				socket.setSoTimeout(30);
				new SocketThread(socket, hostPort).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

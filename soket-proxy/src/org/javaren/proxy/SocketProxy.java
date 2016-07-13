package org.javaren.proxy;

import java.net.ServerSocket;
import java.net.Socket;

public class SocketProxy {
	public static boolean isPrint = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String port = "8888";
		for (String arg : args) {
			if (arg.startsWith("port=")) {
				port = arg.split("=")[1];
			} else if (arg.startsWith("print=")) {
				isPrint = Boolean.valueOf(arg.split("=")[1]);
			} else if (arg.startsWith("permit=")) {

			}
		}
		int portInt = Integer.valueOf(port);
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(portInt);
		System.out.println("socketProxy start at:" + portInt);
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				new SocketThread(socket).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;

/**
 * 将客户端发送过来的数据转发给请求的服务器端，并将服务器返回的数据转发给客户端
 *
 */
public class ProxyTask implements Runnable {
	private Socket clientSocket;
	private Socket serverSocket;

	public ProxyTask(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	/** 已连接到请求的服务器 */
	private static final String AUTHORED = "HTTP/1.1 200 Connection established\r\n\r\n";
	/** 本代理登陆失败(此应用暂时不涉及登陆操作) */
	//private static final String UNAUTHORED="HTTP/1.1 407 Unauthorized\r\n\r\n";
	/** 内部错误 */
	private static final String SERVERERROR = "HTTP/1.1 500 Connection FAILED\r\n\r\n";

	@Override
	public void run() {
		boolean isLog = false;
		HttpRequest request = null;
		InputStream clinetIn = null;
		OutputStream clientOut = null;
		InputStream serverIn = null;
		OutputStream serverOut = null;
		try {
			clinetIn = clientSocket.getInputStream();
			clientOut = clientSocket.getOutputStream();
			request = HttpRequest.readHeader(clinetIn);
			serverSocket = new Socket(request.getHost(), Integer.parseInt(request.getPort()));
			serverSocket.setKeepAlive(true);
			serverIn = serverSocket.getInputStream();
			serverOut = serverSocket.getOutputStream();
		} catch (IOException e2) {
			//e2.printStackTrace();
			return;
		}
		try {
			//从客户端流数据中读取头部，获得请求主机和端口
			if (request.getHost().contains("show") || request.getHost().contains("live")) {
				isLog = true;
			}

			//如果没解析出请求请求地址和端口，则返回错误信息
			if (request.getHost() == null || request.getPort() == null) {
				clientOut.write(SERVERERROR.getBytes());
				clientOut.flush();
				return;
			}

			// 查找主机和端口
			//新开一个线程将返回的数据转发给客户端
			//读取客户端请求过来的数据转发给服务器
			Thread ot = new DataSendThread(serverIn, clientOut, request, serverSocket, clientSocket);
			ot.start();
			if (request.getMethod().equals(HttpRequest.METHOD_CONNECT)) {
				// 将已联通信号返回给请求页面
				clientOut.write(AUTHORED.getBytes());
				clientOut.flush();
			} else {
				//http请求需要将请求头部也转发出去
				byte[] headerData = request.toString().getBytes();
				if (null != serverOut) {
					serverOut.write(headerData);
					serverOut.flush();
				}
			}
			readForwardDate(clinetIn, serverOut, request);
			//等待向客户端转发的线程结束
			ot.join();
			if (isLog) {
				DataComparison.requests.add(request);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (!clientSocket.isOutputShutdown()) {
				//如果还可以返回错误状态的话，返回内部错误
				try {
					clientSocket.getOutputStream().write(SERVERERROR.getBytes());
				} catch (IOException e1) {
				}
			}
		}

		try {
			if (clientSocket != null) {
				clientSocket.close();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
		}
	}

	/**
	 * 读取客户端发送过来的数据，发送给服务器端
	 * 
	 * @param clinetIn
	 * @param serverOut
	 * @param isLog 
	 */
	private void readForwardDate(InputStream clinetIn, OutputStream serverOut, HttpRequest request) {
		byte[] buffer = new byte[4096];
		try {
			int len, totalLen = 0;
			while ((len = clinetIn.read(buffer)) != -1) {
				if (len > 0) {
					serverOut.write(buffer, 0, len);
					serverOut.flush();
					request.requestData.write(buffer, 0, len);
					totalLen += len;
					if (totalLen == request.contentLength) {
						synchronized (request) {
							request.wait();
						}
					}
				}
				if (clientSocket.isClosed() || serverSocket.isClosed()) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				serverSocket.close();// 尝试关闭远程服务器连接，中断转发线程的读阻塞状态
			} catch (IOException e1) {
			}
		}
	}
}
package http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 将服务器端返回的数据转发给客户端
 * 
 * @param serverIn
 * @param clientOut
 */
public class DataSendThread extends Thread {
	private InputStream serverIn;
	private OutputStream clientOut;
	private HttpRequest request;
	private Socket serverSocket;
	private Socket clientSocket;

	public DataSendThread(InputStream serverIn, OutputStream clientOut, HttpRequest request, Socket serverSocket,
			Socket clientSocket) {
		this.serverIn = serverIn;
		this.clientOut = clientOut;
		this.request = request;
		this.serverSocket = serverSocket;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[4096];
		try {
			HttpResponse response = null;
			if (request.getMethod() == HttpRequest.METHOD_GET || request.getMethod() == HttpRequest.METHOD_POST) {
				response = HttpResponse.readHeader(serverIn);
				request.response = response;

				byte[] headerData = response.toString().getBytes();
				clientOut.write(headerData);
				clientOut.flush();
			}
			int len, totalLen = 0;
			while ((len = serverIn.read(buffer)) != -1) {
				if (len > 0) {
					if (null != clientOut) {
						clientOut.write(buffer, 0, len);
						clientOut.flush();
					}
					if (null != response) {
						response.responseData.write(buffer, 0, len);
					}
					totalLen += len;
					if (null != response && response.contentLength > 0) {
						if (totalLen == response.contentLength) {
							synchronized (request) {
								request.notify();
								System.out.println(request);
								System.out.println(response);
							}
						}
					}

				}
				if (null != clientSocket && clientSocket.isOutputShutdown() || serverSocket.isClosed()) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
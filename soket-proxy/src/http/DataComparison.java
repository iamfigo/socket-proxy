package http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class DataComparison implements Runnable {
	public static LinkedBlockingQueue<HttpRequest> requests = new LinkedBlockingQueue<HttpRequest>();

	@Override
	public void run() {
		while (true) {
			try {
				HttpRequest request = requests.take();

				Socket serverSocket = new Socket(request.getHost(), Integer.parseInt(request.getPort()));//TODO 换成老服务
				serverSocket.setKeepAlive(true);
				InputStream serverIn = serverSocket.getInputStream();
				OutputStream serverOut = serverSocket.getOutputStream();

				HttpRequest request2 = new HttpRequest();
				Thread ot = new DataSendThread(serverIn, null, request2, serverSocket, null);
				ot.start();
				if (request.getMethod().equals(HttpRequest.METHOD_CONNECT)) {
				} else {
					//http请求需要将请求头部也转发出去
					byte[] headerData = request.toString().getBytes();
					serverOut.write(headerData);
					serverOut.flush();
				}
				//读取客户端请求过来的数据转发给服务器
				byte[] rquestData = request.requestData.toByteArray();
				serverOut.write(rquestData, 0, rquestData.length);
				serverOut.flush();
				//等待向客户端转发的线程结束
				ot.join();

//				if (request.response.responseData.size() != request2.response.responseData.size()) {
//					System.out.println("not eq!->" + request);
//					System.out.println("responseData->" + request.response.responseData);
//					System.out.println("responseData2->" + request2.response.responseData);
//				} else {
//					System.out.println("eq->" + request.getUrl());
//				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}
}

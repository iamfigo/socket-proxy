package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 将客户端发送过来的数据转发给请求的服务器端，并将服务器返回的数据转发给客户端
 *
 */
public class ProxyTask implements Runnable {
	private Socket socketIn;
	private Socket socketOut;
	
	private long totalUpload=0l;//总计上行比特数
	private long totalDownload=0l;//总计下行比特数

	public ProxyTask(Socket socket) {
		this.socketIn = socket;
	}
	
	private static final SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	/** 已连接到请求的服务器 */
	private static final String AUTHORED = "HTTP/1.1 200 Connection established\r\n\r\n";
	/** 本代理登陆失败(此应用暂时不涉及登陆操作) */
	//private static final String UNAUTHORED="HTTP/1.1 407 Unauthorized\r\n\r\n";
	/** 内部错误 */
	private static final String SERVERERROR = "HTTP/1.1 500 Connection FAILED\r\n\r\n";
	
	@Override
	public void run() {
		
		StringBuilder builder=new StringBuilder();
		boolean isLog = false;
		HttpRequest request = null;
		try {
			builder.append("\r\n").append("Request Time  ：" + sdf.format(new Date()));
			
			InputStream isIn = socketIn.getInputStream();
			OutputStream osIn = socketIn.getOutputStream();
			//从客户端流数据中读取头部，获得请求主机和端口
			request = HttpRequest.readHeader(isIn);
			
			//添加请求日志信息
			builder.append("\r\n").append("From    Host  ：" + socketIn.getInetAddress());
			builder.append("\r\n").append("From    Port  ：" + socketIn.getPort());
			builder.append("\r\n").append("Proxy   Method：" + request.getMethod());
			builder.append("\r\n").append("Request Host  ：" + request.getHost());
			builder.append("\r\n").append("Request Port  ：" + request.getPort());
			
			
			if(request.getHost().contains("show")){
				isLog = true;
			}
			
			//如果没解析出请求请求地址和端口，则返回错误信息
			if (request.getHost() == null || request.getPort() == null) {
				osIn.write(SERVERERROR.getBytes());
				osIn.flush();
				return ;
			}
			
			// 查找主机和端口
			socketOut = new Socket(request.getHost(), Integer.parseInt(request.getPort()));
			socketOut.setKeepAlive(true);
			InputStream isOut = socketOut.getInputStream();
			OutputStream osOut = socketOut.getOutputStream();
			//新开一个线程将返回的数据转发给客户端,串行会出问题，尚没搞明白原因
			Thread ot = new DataSendThread(isOut, osIn,request);
			ot.start();
			if (request.getMethod().equals(HttpRequest.METHOD_CONNECT)) {
				// 将已联通信号返回给请求页面
				osIn.write(AUTHORED.getBytes());
				osIn.flush();
			}else{
				//http请求需要将请求头部也转发出去
				byte[] headerData=request.toString().getBytes();
				totalUpload+=headerData.length;
				osOut.write(headerData);
				osOut.flush();
			}
			//读取客户端请求过来的数据转发给服务器
			readForwardDate(isIn, osOut,isLog);
			//等待向客户端转发的线程结束
			ot.join();
		} catch (Exception e) {
			//e.printStackTrace();
			if(!socketIn.isOutputShutdown()){
				//如果还可以返回错误状态的话，返回内部错误
				try {
					socketIn.getOutputStream().write(SERVERERROR.getBytes());
				} catch (IOException e1) {}
			}
		} finally {
			try {
				if (socketIn != null) {
					socketIn.close();
				}
			} catch (IOException e) {}
			if (socketOut != null) {
				try {
					socketOut.close();
				} catch (IOException e) {}
			}
			//纪录上下行数据量和最后结束时间并打印
			builder.append("\r\n").append("Up    Bytes  ：" + totalUpload);
			builder.append("\r\n").append("Down  Bytes  ：" + totalDownload);
			builder.append("\r\n").append("Closed Time  ：" + sdf.format(new Date()));
			builder.append("\r\n").append("header       ：" + request);
			builder.append("\r\n");
			if(isLog){
				logRequestMsg(builder.toString());
			}
		}	
	}
	
	/**
	 * 避免多线程竞争把日志打串行了
	 * @param msg
	 */
	private synchronized void logRequestMsg(String msg){
		System.out.println(msg);
	}

	/**
	 * 读取客户端发送过来的数据，发送给服务器端
	 * 
	 * @param isIn
	 * @param osOut
	 * @param isLog 
	 */
	private void readForwardDate(InputStream isIn, OutputStream osOut, boolean isLog) {
		byte[] buffer = new byte[4096];
		try {
			int len;
			while ((len = isIn.read(buffer)) != -1) {
				if (len > 0) {
					osOut.write(buffer, 0, len);
					osOut.flush();
				}
				totalUpload+=len;
				if (socketIn.isClosed() || socketOut.isClosed()) {
					break;
				}
			}
		} catch (Exception e) {
			try {
				socketOut.close();// 尝试关闭远程服务器连接，中断转发线程的读阻塞状态
			} catch (IOException e1) {}
		}
	}

	/**
	 * 将服务器端返回的数据转发给客户端
	 * 
	 * @param isOut
	 * @param osIn
	 */
	class DataSendThread extends Thread {
		private InputStream isOut;
		private OutputStream osIn;
		private HttpRequest request;

		DataSendThread(InputStream isOut, OutputStream osIn, HttpRequest request) {
			this.isOut = isOut;
			this.osIn = osIn;
			this.request = request;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[4096];
			try {
				int len;
				while ((len = isOut.read(buffer)) != -1) {
					if (len > 0) {
						request.response.write(buffer, 0, len);
						osIn.write(buffer, 0, len);
						osIn.flush();
						totalDownload+=len;
					}
					if (socketIn.isOutputShutdown() || socketOut.isClosed()) {
						break;
					}
				}
			} catch (Exception e) {}
		}
	}

}
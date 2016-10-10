package http;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * http 代理程序
 *
 */
public class HttpProxy {

	static final int listenPort = 8888;

	public static void main(String[] args) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		ServerSocket serverSocket = new ServerSocket(listenPort);
		final ExecutorService tpe = Executors.newCachedThreadPool();
		System.out.println("Proxy Server Start At " + sdf.format(new Date()));
		System.out.println("listening port:" + listenPort + "……");
		System.out.println();
		System.out.println();
		DataComparison dc = new DataComparison();
		Thread t = new Thread(dc, "data-comparison");
		t.start();

		while (true) {
			Socket client = null;
			try {
				client = serverSocket.accept();
				client.setKeepAlive(true);
				//加入任务列表，等待处理
				tpe.execute(new ProxyTask(client));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

package http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析头部信息
 *
 */
public final class HttpResponse {
	private List<String> header = new ArrayList<String>();
	int contentLength;
	String contentType;
	public ByteArrayOutputStream responseData = new ByteArrayOutputStream();

	public static final int MAXLINESIZE = 4096;

	HttpResponse() {
	}

	/**
	 * 从数据流中读取请求头部信息，必须在放在流开启之后，任何数据读取之前
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static final HttpResponse readHeader(InputStream in) throws IOException {
		HttpResponse header = new HttpResponse();
		StringBuilder sb = new StringBuilder();
		char c = 0;
		//如能识别出请求方式则则继续，不能则退出
		do {
			sb = new StringBuilder();
			while ((c = (char) in.read()) != '\n') {
				sb.append(c);
				if (sb.length() == MAXLINESIZE) {//不接受过长的头部字段
					break;
				}
			}
			if (sb.length() > 1 && header.notTooLong()) {//如果头部包含信息过多，抛弃剩下的部分
				header.addHeaderString(sb.substring(0, sb.length() - 1));
			} else {
				break;
			}
		} while (true);

		return header;
	}

	/**
	 * 
	 * @param str
	 */
	private void addHeaderString(String str) {
		str = str.replaceAll("\r", "");
		header.add(str);
		if (str.startsWith("Host")) {//解析主机和端口
		} else if (str.startsWith("Content-Length")) {
			contentLength = Integer.valueOf(str.split(":")[1].trim());
		} else if (str.startsWith("Content-Type")) {
			contentType = str.split(":")[1].trim();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String str : header) {
			sb.append(str).append("\r\n");
		}
		sb.append("\r\n");
		return sb.toString();
	}

	public boolean notTooLong() {
		return header.size() <= 32;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

}

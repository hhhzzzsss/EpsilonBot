package com.github.hhhzzzsss.epsilonbot.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class DownloadUtils {
	
	private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
	
	public static byte[] DownloadToByteArray(URL url, int maxSize) throws IOException, KeyManagementException, NoSuchAlgorithmException {
		SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(10000);
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0");
		BufferedInputStream downloadStream = new BufferedInputStream(conn.getInputStream());
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		
		try {
			byte buf[] = new byte[1024];
			int n;
			int tot = 0;
			while ((n = downloadStream.read(buf)) > 0) {
				byteArrayStream.write(buf, 0, n);
				tot += n;
				if (tot > maxSize) {
					throw new IOException("File is too large");
				}
				if (Thread.interrupted()) {
					return null;
				}
			}
			return byteArrayStream.toByteArray();
		} finally {
			// Closing a ByteArrayInputStream has no effect, so I do not close it.
			downloadStream.close();
		}
	}
	
	public static InputStream DownloadToOutputStream(URL url, int maxSize) throws KeyManagementException, NoSuchAlgorithmException, IOException {
		return new ByteArrayInputStream(DownloadToByteArray(url, maxSize));
	}

	public static boolean imageIsSafe(InputStream input) throws IOException {
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(input);
		Iterator iter = ImageIO.getImageReaders(imageInputStream);
		long maxSize = 5000L * 5000L;

		if (!iter.hasNext()) {
			imageInputStream.close();
			return false;
		}

		boolean safe = false;
		try {
			ImageReader reader = (ImageReader) iter.next();
			reader.setInput(imageInputStream, true, true);

			long width = reader.getWidth(0);
			long height = reader.getHeight(0);

			safe = (height * width) <= maxSize;
		} finally {
			imageInputStream.close();
		}

		return safe;
	}
}

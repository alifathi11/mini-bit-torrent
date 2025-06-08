package common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class MD5Hash {
	public static String hashFile(String filePath) {

		File file = new File(filePath);

		try (FileInputStream fis = new FileInputStream(file)) {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[1024];
			int read;

			while ((read = fis.read(buffer)) != -1) {
				md.update(buffer, 0, read);
			}

			byte[] digest = md.digest();

			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

package common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.*;

public class FileUtils {

	public static Map<String, String> listFilesInFolder(String folderPath) {

		Map<String, String> fileHashes = new HashMap<>();

		// 1. Create folder object
		File folder = new File(folderPath);
		// 2. Get list of files
		File[] files = folder.listFiles();
		if (files == null) {
			System.out.println("");
			return fileHashes;
		}
		// 3. Calculate MD5 hash for each file
		for (File file : files) {
			String hash = MD5Hash.hashFile(file.getPath());
			String fileName = file.getName();
			fileHashes.put(fileName, hash);
		}
		// 4. Return map of filename to hash
		return fileHashes;
	}

	public static String getSortedFileList(Map<String, String> files) {
		if (files == null || files.isEmpty()) {
			return "";
		}

		List<Map.Entry<String, String>> entries = new ArrayList<>(files.entrySet());
		entries.sort(Map.Entry.<String, String>comparingByKey().thenComparing(Map.Entry::getValue));

		StringBuilder formattedFileNames = new StringBuilder();
		for (Map.Entry<String, String> entry : entries) {
			formattedFileNames.append(entry.getKey())
					.append(" ")
					.append(entry.getValue())
					.append("\n");
		}

		return formattedFileNames.toString();
	}

}

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

	public static String getSortedFilesAndPeers(Map<String, List<String>> filesToSort) {
		if (filesToSort == null || filesToSort.isEmpty()) {
			return "Files not found.";
		}

		List<String[]> entries = new ArrayList<>();

		for (Map.Entry<String, List<String>> entry : filesToSort.entrySet()) {
			String peer = entry.getKey();
			List<String> files = entry.getValue();

			for (String fileEntry : files) {
				int idx = fileEntry.lastIndexOf(' ');
				if (idx != -1) {
					String fileName = fileEntry.substring(0, idx).trim();
					String md5 = fileEntry.substring(idx + 1).trim();
					entries.add(new String[]{fileName, md5, peer});
				}
			}
		}

		entries.sort((e1, e2) -> {
			int cmp = e1[0].compareTo(e2[0]); // compare fileName
			if (cmp != 0) return cmp;

			String ip1 = e1[2].split(":")[0];
			String ip2 = e2[2].split(":")[0];
			return ip1.compareTo(ip2);
		});

		StringBuilder result = new StringBuilder();
		for (String[] e : entries) {
			result.append(e[0]).append(" ").append(e[1]).append(" - ").append(e[2]).append("\n");
		}

		return result.toString();
	}

}

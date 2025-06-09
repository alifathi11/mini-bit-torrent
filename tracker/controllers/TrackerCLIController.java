package tracker.controllers;

import common.models.CLICommands;
import common.utils.FileUtils;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class TrackerCLIController {
	public static String processCommand(String command) {
		TrackerCommands trackerCommand = null;
		Matcher matcher = null;
		boolean matched = false;

		// 1. Find the matching command
		for (TrackerCommands tc : TrackerCommands.values()) {
			if (tc.matches(command)) {
				matched = true;
				matcher = tc.getMatcher(command);
				trackerCommand = tc;
				break; // Only take the first match to avoid ambiguity
			}
		}

		if (!matched || matcher == null) {
			return CLICommands.invalidCommand;
		}

		// 2. Validate match if needed
		boolean groupsMatched = matcher.matches(); // Ensure .group() can be safely called
		String result;

		// 3. Call appropriate handler
		switch (trackerCommand) {
			case REFRESH_FILES:
				result = refreshFiles();
				break;
			case RESET_CONNECTIONS:
				result = resetConnections();
				break;
			case LIST_PEERS:
				result = listPeers();
				break;
			case LIST_FILES:
				if (groupsMatched) {
					String ip = matcher.group("ip");
					int port = Integer.parseInt(matcher.group("port"));
					result = listFiles(ip, port);
				} else {
					result = "Invalid format for list_files command.";
				}
				break;
			case GET_SENDS:
				if (groupsMatched) {
					String ip = matcher.group("ip");
					int port = Integer.parseInt(matcher.group("port"));
					result = getSends(ip, port);
				} else {
					result = "Invalid format for get_sends command.";
				}
				break;
			case GET_RECEIVES:
				if (groupsMatched) {
					String ip = matcher.group("ip");
					int port = Integer.parseInt(matcher.group("port"));
					result = getReceives(ip, port);
				} else {
					result = "Invalid format for get_receives command.";
				}
				break;
			case END:
				result = endProgram();
				break;
			default:
				result = "Command format is not valid.";
				break;
		}

		// 4. Return result or error message
		return result;
	}


	private static String getReceives(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
		if (connection == null) {
			return "Peer not found.";
		}
		Map<String, List<String>> receivedFiles = TrackerConnectionController.getReceives(connection);
		if (receivedFiles == null || receivedFiles.isEmpty()) {
			return "No files received by " + ip + ":" + port;
		}

		return FileUtils.getSortedFilesAndPeers(receivedFiles);
	}

	private static String getSends(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
		if (connection == null) {
			return "Peer not found.";
		}
		Map<String, List<String>> sentFiles = TrackerConnectionController.getSends(connection);
		if (sentFiles == null || sentFiles.isEmpty()) {
			return "No files sent by " + ip + ":" + port;
		}

		return FileUtils.getSortedFilesAndPeers(sentFiles);
	}

	private static String listFiles(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
       	if (connection == null) return "Peer not found.";
        Map<String, String> fileHashes = connection.getFileAndHashes();
		if (fileHashes == null) return "Files not found.";

		return FileUtils.getSortedFileList(fileHashes);
	}

	private static String listPeers() {
		List<PeerConnectionThread> connections = TrackerApp.getConnections();
		if (connections.isEmpty()) {
			return "No peers connected.";
		}

		StringBuilder result = new StringBuilder();

		for (PeerConnectionThread connection : connections) {
			String ip = connection.getOtherSideIP();
			int port = connection.getOtherSidePort();
			result.append(ip).append(":").append(port).append("\n");
		}

		return result.toString();
	}

	private static String resetConnections() {
		for (PeerConnectionThread connection : TrackerApp.getConnections()) {
			if (!connection.isAlive()) {
				TrackerApp.getConnections().remove(connection);
				continue;
			}
			connection.refreshStatus();
			connection.refreshFileList();
		}

		return "Connections reset successfully.";
	}

	private static String refreshFiles() {
		for (PeerConnectionThread connection : TrackerApp.getConnections()) {
			connection.refreshFileList();
		}
		return "Files refreshed successfully.";
	}

	private static String endProgram() {
		TrackerApp.endAll();
		return "Closing the program...";
	}

}

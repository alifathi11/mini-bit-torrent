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
		for (TrackerCommands tc : TrackerCommands.values()) {
			if (tc.matches(command)) {
				matched = true;
				matcher = tc.getMatcher(command);
				trackerCommand = tc;
			}
		}
		if (!matched) return CLICommands.invalidCommand;

		String result;

		// 2. Call appropriate handler
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
				result = listFiles(matcher.group("ip"), Integer.parseInt(matcher.group("port")));
				break;
			case GET_SENDS:
				result = getSends(matcher.group("ip"), Integer.parseInt(matcher.group("port")));
				break;
			case GET_RECEIVES:
				result = getReceives(matcher.group("ip"), Integer.parseInt(matcher.group("port")));
				break;
			case END:
				result = endProgram();
				break;
			default:
				result = "Command format is not valid.";
				break;
		}
		// 3. Return result or error message
		return result;

	}

	private static String getReceives(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
		Map<String, List<String>> receivedFiles = TrackerConnectionController.getReceives(connection);
		if (receivedFiles == null || receivedFiles.isEmpty()) {
			return "No files received by " + ip + ":" + port;
		}

		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : receivedFiles.entrySet()) {
			String sender = entry.getKey();
			for (String file : entry.getValue()) {
				result.append(file).append(" - ").append(sender).append("\n");
			}
		}

		return result.toString();
	}

	private static String getSends(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
		Map<String, List<String>> sentFiles = TrackerConnectionController.getSends(connection);
		if (sentFiles == null || sentFiles.isEmpty()) {
			return "No files sent by " + ip + ":" + port;
		}

		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : sentFiles.entrySet()) {
			String receiver = entry.getKey();
			for (String file : entry.getValue()) {
				result.append(file).append(" - ").append(receiver).append("\n");
			}
		}

		return result.toString();
	}

	private static String listFiles(String ip, int port) {
		PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
       	if (connection == null) return "";
        Map<String, String> fileHashes = connection.getFileAndHashes();
		if (fileHashes == null) return "";

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
			connection.refreshStatus();
			connection.refreshFileList();
		}

		return "";
	}

	private static String refreshFiles() {
		for (PeerConnectionThread connection : TrackerApp.getConnections()) {
			connection.refreshFileList();
		}
		return "";
	}

	private static String endProgram() {
		TrackerApp.endAll();
		return "";
	}
}

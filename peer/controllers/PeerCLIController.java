package peer.controllers;

import common.models.CLICommands;
import common.models.Message;
import common.utils.FileUtils;
import common.utils.MD5Hash;
import peer.app.P2TConnectionThread;
import peer.app.PeerApp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

public class PeerCLIController {
	public static String processCommand(String command) {
		// TODO: Process Peer CLI commands
		// 1. Check command type (END_PROGRAM, DOWNLOAD, LIST)
		PeerCommands peerCommand = null;
		Matcher matcher = null;
		boolean matched = false;
		for (PeerCommands pc : PeerCommands.values()) {
			if (pc.matches(command)) {
				matched = true;
				peerCommand = pc;
				matcher = pc.getMatcher(command);
			}
		}
		if (!matched) return CLICommands.invalidCommand;

		String result;

		// 2. Call appropriate handler
		switch (peerCommand) {
			case DOWNLOAD:
				result = handleDownload(matcher.group("file_name"));
				break;
			case LIST:
				result = handleListFiles();
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

	private static String handleListFiles() {
		Map<String, String> fileList = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
		return FileUtils.getSortedFileList(fileList);
	}


	private static String handleDownload(String fileName) {
		try {
			P2TConnectionThread trackerConnection = PeerApp.getP2TConnection();
			if (trackerConnection == null) {
				return "Error: No connection to tracker.";
			}

			Message responseMessage = P2TConnectionController.sendFileRequest(trackerConnection, fileName);

            String responseStatus = responseMessage.getFromBody("response");
			if (responseStatus.equals("peer_found")) {

				String peerIp = responseMessage.getFromBody("peer_have");
				int peerPort = responseMessage.getFromBody("peer_port");
				String fileMd5 = responseMessage.getFromBody("md5");

				PeerApp.requestDownload(peerIp, peerPort, fileName, fileMd5);
				// return result
				return "";
			} else if (responseStatus.equals("error")) {
				String error = responseMessage.getFromBody("error");
				switch (error) {
					case "not_found":
						return "";
					case "multiple_hash":
						return "";
					default:
						return "Unknown error occurred.";
				}
			} else {
				return "Error: Unexpected tracker response: " + responseStatus;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Error: Download failed due to an exception.";
		}
	}


	public static String endProgram() {
		PeerApp.endAll();
		return "";
	}
}

package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import common.utils.MD5Hash;
import peer.app.P2TConnectionThread;
import peer.app.PeerApp;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2TConnectionController {
	public static Message handleCommand(Message message) {
		// 1. Parse command from message
		String command = message.getFromBody("command");

		// 2. Dispatch based on command
		switch (command) {
			case "status":
				return status();
			case "get_files_list":
				return getFilesList();
			case "get_receives":
				return getReceives();
			case "get_sends":
				return getSends();
			default:
				HashMap<String, Object> errorBody = new HashMap<>();
				errorBody.put("command", command);
				errorBody.put("response", "error");
				errorBody.put("message", "Unknown command: " + command);
				return new Message(errorBody, Message.Type.response);
		}
	}

	private static Message getReceives() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_receives");
		body.put("response", "ok");
		body.put("received_files", PeerApp.getReceivedFiles());

		return new Message(body, Message.Type.response);
	}

	private static Message getSends() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_sends");
		body.put("response", "ok");
		body.put("sent_files", PeerApp.getSentFiles());

		return new Message(body, Message.Type.response);
	}

	public static Message getFilesList() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_files_list");
		body.put("response", "ok");
		body.put("files", FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath()));

		return new Message(body, Message.Type.response);
	}

	public static Message status() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "status");
		body.put("response", "ok");
		body.put("peer", PeerApp.getPeerIP());
		body.put("listen_port", PeerApp.getPeerPort());

        return new Message(body, Message.Type.response);
	}

	public static Message sendFileRequest(P2TConnectionThread tracker, String fileName) throws Exception {
		if (tracker == null) throw new IllegalArgumentException("Tracker connection is null.");

		HashMap<String, Object> body = new HashMap<>();
		body.put("name", fileName);
		Message requestMessage = new Message(body, Message.Type.file_request);

		Message response = tracker.sendAndWaitForResponse(requestMessage, TIMEOUT_MILLIS);

		if (response == null) {
			throw new Exception("No response from tracker.");
		}

		String responseStatus = response.getFromBody("response");

		if (!responseStatus.equals("peer_found") && !responseStatus.equals("error")) {
			throw new Exception("Unexpected response from tracker: " + responseStatus);
		}

		return response;
	}

}

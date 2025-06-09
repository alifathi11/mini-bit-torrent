package tracker.controllers;

import common.models.Message;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.*;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class TrackerConnectionController {
	public static Message handleCommand(Message message) {
		// 1. Validate message type and content
		if (message.getType() != Message.Type.file_request) {
			return null;
		}
		// 2. Find peers having the requested file
		Map<PeerConnectionThread, String> peerAndFileHashes = new HashMap<>();
		String fileName = message.getFromBody("name");

		for (PeerConnectionThread connection : TrackerApp.getConnections()) {
			if (connection.getFileAndHashes().containsKey(fileName)) {
				peerAndFileHashes.put(connection, connection.getFileAndHashes().get(fileName));
			}
		}

		HashMap<String, Object> body = new HashMap<>();
		Set<String> uniqueHashes = new HashSet<>(peerAndFileHashes.values());
		// 3. Check for hash consistency

		if (peerAndFileHashes.isEmpty()) {
			body.put("response", "error");
			body.put("error", "not_found");
		} else if (uniqueHashes.size() != 1) {
			body.put("response", "error");
			body.put("error", "multiple_hash");
		} else {
			body.put("response", "peer_found");
			body.put("md5", uniqueHashes.toArray()[0]);

			Random random = new Random();
			List<PeerConnectionThread> peers = new ArrayList<>(peerAndFileHashes.keySet());
			PeerConnectionThread peer = peers.get(random.nextInt(peers.size()));


			body.put("peer_have", peer.getOtherSideIP());
			body.put("peer_port", peer.getOtherSidePort());
		}

		// 4. Return peer information or error
		return new Message(body, Message.Type.response);

	}

	public static Map<String, List<String>> getSends(PeerConnectionThread connection) {
		// 1. Build command message
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_sends");
		Message message = new Message(body, Message.Type.command);

		// 2. Send message and wait for response
		Message response = connection.sendAndWaitForResponse(message, TIMEOUT_MILLIS);
		if (response == null || !response.getFromBody("response").equals("ok")) {
			System.out.println("");
			return null;
		}
		// 3. Parse and return sent files map
		return response.getFromBody("sent_files");
	}

	public static Map<String, List<String>> getReceives(PeerConnectionThread connection) {
		// 1. Build command message
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_receives");

		Message message = new Message(body, Message.Type.command);
		// 2. Send message and wait for response
		Message response = connection.sendAndWaitForResponse(message, TIMEOUT_MILLIS);
		if (response == null || !response.getFromBody("response").equals("ok")) {
			System.out.println("");
			return null;
		}
		// 3. Parse and return received files map
		return response.getFromBody("received_files");
	}
}

package tracker.app;

import common.models.ConnectionThread;
import common.models.Message;
import tracker.controllers.TrackerConnectionController;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class PeerConnectionThread extends ConnectionThread {
	private HashMap<String, String> fileAndHashes;

	public PeerConnectionThread(Socket socket) throws IOException {
		super(socket);
	}

	@Override
	public boolean initialHandshake() {
		try {
			refreshStatus();
			refreshFileList();
			TrackerApp.addPeerConnection(this);
			throw new UnsupportedOperationException("Initial handshake not implemented yet");
		} catch (Exception e) {
			return false;
		}
	}

	public void refreshStatus() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "status");

		Message request = new Message(body, Message.Type.command);

		Message response = sendAndWaitForResponse(request, TIMEOUT_MILLIS);

		if (response == null || !response.getFromBody("response").equals("ok")) {
			throw new IllegalStateException("Failed to refresh status.");
		}
		// then update peer's IP and port
		this.setOtherSideIP(response.getFromBody("peer"));
		this.setOtherSidePort(response.getIntFromBody("listen_port"));
	}

	public void refreshFileList() {
		HashMap<String, Object> body = new HashMap<>();
		body.put("command", "get_files_list");

		Message request = new Message(body, Message.Type.command);

		Message response = sendAndWaitForResponse(request, TIMEOUT_MILLIS);

		if (response == null || !response.getFromBody("response").equals("ok")) {
			throw new IllegalStateException("Failed to refresh status.");
		}

		Map<String, String> responseFiles = response.getFromBody("files");
		if (responseFiles == null || responseFiles.isEmpty()) {
			return;
		}

        fileAndHashes = new HashMap<>(responseFiles);
	}

	@Override
	protected boolean handleMessage(Message message) {
		if (message.getType() == Message.Type.file_request) {
			sendMessage(TrackerConnectionController.handleCommand(message));
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		super.run();
		TrackerApp.removePeerConnection(this);
	}

	public Map<String, String> getFileAndHashes() {
		return Map.copyOf(fileAndHashes);
	}
}

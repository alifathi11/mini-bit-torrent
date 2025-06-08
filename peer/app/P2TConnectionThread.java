package peer.app;

import common.models.ConnectionThread;
import common.models.Message;
import peer.controllers.P2TConnectionController;

import java.io.IOException;
import java.net.Socket;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2TConnectionThread extends ConnectionThread {

	protected P2TConnectionThread(Socket socket) throws IOException {
		super(socket);
	}

	@Override
	public boolean initialHandshake() {
		try {
			socket.setSoTimeout(TIMEOUT_MILLIS);

			dataInputStream.readUTF();
			Message message1 = P2TConnectionController.status();
			sendMessage(message1);

			dataInputStream.readUTF();
			Message message2 = P2TConnectionController.getFilesList();
			sendMessage(message2);

			socket.setSoTimeout(0);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected boolean handleMessage(Message message) {
		if (message.getType() == Message.Type.command) {
			sendMessage(P2TConnectionController.handleCommand(message));
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		super.run();
		PeerApp.endAll();
		System.exit(0);
	}
}

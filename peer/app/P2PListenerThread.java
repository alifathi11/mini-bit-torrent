package peer.app;

import common.models.Message;
import common.utils.JSONUtils;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static peer.app.PeerApp.TIMEOUT_MILLIS;
import static peer.app.PeerApp.isEnded;

public class P2PListenerThread extends Thread {
	private final ServerSocket serverSocket;

	public P2PListenerThread(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
	}

	private void handleConnection(Socket socket) throws Exception {

		socket.setSoTimeout(TIMEOUT_MILLIS);

		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			String jsonMessage = dis.readUTF();

			if (jsonMessage.isEmpty()) {
				socket.close();
				return;
			}

			Message message = JSONUtils.fromJson(jsonMessage);

			if (message.getType() == Message.Type.download_request) {
				handleDownloadRequest(message, socket);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleDownloadRequest(Message message, Socket socket) throws IOException {
		String fileName = message.getFromBody("name");
		String receiverIp = message.getFromBody("receiver_ip");
		int receiverPort = message.getIntFromBody("receiver_port");

		String receiver = String.format(receiverIp + ":" + receiverPort);

		File file = new File(PeerApp.getSharedFolderPath(), fileName);

		if (!file.exists()) {
			if (!socket.isClosed()) socket.close();
			return;
		}

		TorrentP2PThread downloadThread = new TorrentP2PThread(socket, file, receiver);
		downloadThread.start();
	}

	@Override
	public void run() {
		while (!PeerApp.isEnded()) {
			try {
				Socket socket = serverSocket.accept();
				handleConnection(socket);
			} catch (Exception e) {
				break;
			}
		}

		try {serverSocket.close();} catch (Exception ignored) {}
	}
}

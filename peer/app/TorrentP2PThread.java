package peer.app;

import common.utils.MD5Hash;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class TorrentP2PThread extends Thread {
	private final Socket socket;
	private final File file;
	private final String receiver;
	private final BufferedOutputStream dataOutputStream;

	public TorrentP2PThread(Socket socket, File file, String receiver) throws IOException {
		this.socket = socket;
		this.file = file;
		this.receiver = receiver;
		this.dataOutputStream = new BufferedOutputStream(socket.getOutputStream());
		PeerApp.addTorrentP2PThread(this);
	}

	@Override
	public void run() {
		try (FileInputStream fis = new FileInputStream(file);
			 BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream())) {

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				bos.write(buffer, 0, bytesRead);
			}

			bos.flush();
			String md5 = MD5Hash.hashFile(file.getPath());
			PeerApp.addSentFile(receiver, file.getName() + " " + md5); // check


		} catch (IOException e) {
			System.err.println("Error sending file: " + e.getMessage());
		} finally {
			try {
				if (socket != null && !socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				System.err.println("Error closing socket: " + e.getMessage());
			}
		}
	}

	public void end() {
		try {
			dataOutputStream.close();
			socket.close();
		} catch (Exception e) {}
	}
}

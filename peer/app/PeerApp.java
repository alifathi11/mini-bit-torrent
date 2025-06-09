package peer.app;

import common.models.Message;
import common.utils.JSONUtils;
import common.utils.MD5Hash;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class PeerApp {
	public static final int TIMEOUT_MILLIS = 500;

	private static String PEER_IP;
	private static int PEER_PORT;
	private static Socket socket; // check

	private static String SHARED_FOLDER_PATH;

	private static String TRACKER_IP;
	private static int TRACKER_PORT;

	private static Map<String, List<String>> sentFiles = new HashMap<>();
	private static Map<String, List<String>> receivedFiles = new HashMap<>();

	private static P2PListenerThread peerListenerThread;
	private static P2TConnectionThread trackerConnectionThread;

	private static ArrayList<TorrentP2PThread> torrentP2PThreads = new ArrayList<>();

	private static boolean exitFlag = false;

	public static boolean isEnded() {
		return exitFlag;
	}

	public static void initFromArgs(String[] args) throws Exception {

		String[] peerAddressParts = args[0].split(":");
		String[] trackerAddressParts = args[1].split(":");
		// 1. Parse self address (ip:port)
		PEER_IP = peerAddressParts[0];
		PEER_PORT = Integer.parseInt(peerAddressParts[1]);

		// 2. Parse tracker address (ip:port)
		TRACKER_IP = trackerAddressParts[0];
		TRACKER_PORT = Integer.parseInt(trackerAddressParts[1]);

		socket = new Socket(TRACKER_IP, TRACKER_PORT); // check
		// 3. Set shared folder path
		SHARED_FOLDER_PATH = args[2];
		// 4. Create tracker connection thread
		trackerConnectionThread = new P2TConnectionThread(socket);
		// 5. Create peer listener thread
		peerListenerThread = new P2PListenerThread(PEER_PORT);

	}

	public static void endAll() {
		exitFlag = true;
		// 1. End tracker connection
		if (trackerConnectionThread != null && trackerConnectionThread.isAlive()) {
			trackerConnectionThread.end();
		}
		// 2. End all torrent threads
		for (TorrentP2PThread torrentP2PThread : torrentP2PThreads) {
			if (torrentP2PThread != null && torrentP2PThread.isAlive()) {
				torrentP2PThread.end();
			}
		}
		// 3. Clear file lists
		if (receivedFiles != null) receivedFiles.clear();
		if (sentFiles != null) sentFiles.clear();
	}

	public static void connectTracker() {
		if (trackerConnectionThread != null && !trackerConnectionThread.isAlive()) {
			trackerConnectionThread.start();
		}
	}

	public static void startListening() {
		if (peerListenerThread != null && !peerListenerThread.isAlive()) {
			peerListenerThread.start();
		}
	}

	public static void removeTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
		if (torrentP2PThreads.contains(torrentP2PThread)) {
			torrentP2PThreads.remove(torrentP2PThread);
			torrentP2PThread.end(); // check
		}
	}

	public static void addTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
		// 1. Check if thread is valid
		if (torrentP2PThread == null) return;
		// 2. Check if already exists
		if (torrentP2PThreads.contains(torrentP2PThread)) return;
		// 3. Add to list
		torrentP2PThreads.add(torrentP2PThread);
	}

	public static String getSharedFolderPath() {
		return SHARED_FOLDER_PATH;
	}

	public static void addSentFile(String receiver, String fileNameAndHash) {
		if (sentFiles == null
			|| receiver.isEmpty()
			|| fileNameAndHash.isEmpty()) {
			return;
		}

        if (sentFiles.containsKey(receiver)
			&& sentFiles.get(receiver) != null) {
			sentFiles.get(receiver).add(fileNameAndHash);
		} else {
			sentFiles.put(receiver, new ArrayList<>());
			sentFiles.get(receiver).add(fileNameAndHash);
		}

	}

	public static void addReceivedFile(String sender, String fileNameAndHash) {
		if (receivedFiles == null
				|| sender.isEmpty()
				|| fileNameAndHash.isEmpty()) {
			return;
		}

		if (receivedFiles.containsKey(sender)
			&& receivedFiles.get(sender) != null) {
			receivedFiles.get(sender).add(fileNameAndHash);
		} else {
			receivedFiles.put(sender, new ArrayList<>());
			receivedFiles.get(sender).add(fileNameAndHash);
		}
	}

	public static String getPeerIP() {
		return PEER_IP;
	}

	public static int getPeerPort() {
		return PEER_PORT;
	}

	public static Map<String, List<String>> getSentFiles() {
		return Map.copyOf(sentFiles);
	}

	public static Map<String, List<String>> getReceivedFiles() {
		return Map.copyOf(receivedFiles);
	}

	public static P2TConnectionThread getP2TConnection() {
		return trackerConnectionThread;
	}

	public static String requestDownload(String ip, int port, String filename, String md5) throws Exception {
		// 1. Check if file already exists
		File targetFile = new File(SHARED_FOLDER_PATH, filename);
		if (targetFile.exists()) {
			return "You already have the file!";
		}
		// 2. Create download request message
		HashMap<String, Object> body = new HashMap<>();
		body.put("name", filename);
		body.put("md5", md5);
		body.put("receiver_ip", PEER_IP);
		body.put("receiver_port", PEER_PORT);

		Message downloadRequestMessage = new Message(body, Message.Type.download_request);
		// 3. Connect to peer
		try (Socket peerSocket = new Socket(ip, port);
			 DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
			 BufferedInputStream bis = new BufferedInputStream(peerSocket.getInputStream());
			 FileOutputStream fos = new FileOutputStream(targetFile)) {

			// 4. Send request
			dos.writeUTF(JSONUtils.toJson(downloadRequestMessage));
			dos.flush();

			// 5. Receive file data
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = bis.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}

			// 6. Save file
			fos.flush();

			// 7. Verify file integrity
			String receivedMD5 = MD5Hash.hashFile(targetFile.getPath());
            assert receivedMD5 != null;
            if (!receivedMD5.equals(md5)) {
				targetFile.delete();
				return "The file has been downloaded from peer but is corrupted!";
			}

			// 8. Update received files list
			String sender = ip + ":" + port;
			addReceivedFile(sender, filename + " " + md5); // check
			return "File downloaded successfully: " + filename;

		} catch (IOException e) {
			e.printStackTrace();
			targetFile.delete();
			return "Failed to receive file.";
		}
	}
}


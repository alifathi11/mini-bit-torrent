package peer;

import peer.app.PeerApp;
import peer.controllers.PeerCLIController;

import java.util.Scanner;

public class PeerMain {
	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java peer.PeerMain <self-address:port> <tracker-address:ip> <shared-folder>");
			return;
		}

		try {
			PeerApp.initFromArgs(args);
			PeerApp.connectTracker();
			PeerApp.startListening();
		} catch (Exception e) {
			System.err.println("Error initializing peer: " + e.getMessage());
			return;
		}

		while (!PeerApp.isEnded()) {
			String result = PeerCLIController.processCommand(scanner.nextLine().trim());
			System.out.println(result);
		}
		scanner.close();
	}
}

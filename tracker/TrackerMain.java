package tracker;

import tracker.app.ListenerThread;
import tracker.app.TrackerApp;
import tracker.controllers.TrackerCLIController;

import java.util.Scanner;

public class TrackerMain {
	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java tracker.TrackerMain <port>");
			return;
		}

		try {
			int port = Integer.parseInt(args[0]);
			TrackerApp.setListenerThread(new ListenerThread(port));
			TrackerApp.startListening();
		} catch (Exception e) {
			System.err.println("Error starting tracker: " + e.getMessage());
			return;
		}

		while (!TrackerApp.isEnded()) {
			String result = TrackerCLIController.processCommand(scanner.nextLine().trim());
			System.out.println(result);
		}
		scanner.close();
	}
}

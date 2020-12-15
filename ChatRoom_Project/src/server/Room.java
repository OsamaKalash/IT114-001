package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	// private final static String PM_TRIGGER = "@";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String ROLL = "roll";
	private final static String FLIP = "flip";
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";

	Random rand = new Random();

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread> clients = new ArrayList<ServerThread>();

	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
		}
	}

	private void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			// sendMessage(client, "left the room");
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 * 
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	private boolean processCommands(String message, ServerThread client) {

		boolean wasCommand = false;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];

				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				String clientName;
				switch (command) {
				case CREATE_ROOM:
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						joinRoom(roomName, client);
					}
					wasCommand = true;
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					wasCommand = true;
					break;
				case ROLL:
					int num = rand.nextInt((6 - 1) + 1) + 1;
					String number = "" + num + "";
					sendMessage(client, "<i>you rolled a " + number + "!</i>");
					wasCommand = true;
					break;
				case FLIP:
					int coin = rand.nextInt((2 - 1) + 1) + 1;
					String side = null;
					if (coin == 1)
						side = "heads";
					else if (coin == 2)
						side = "tails";

					sendMessage(client, "<i>your coin landed on " + side + "!</i>");
					wasCommand = true;
					break;
				case MUTE:
					clientName = comm2[1];
					ServerThread muted = null;
					for (ServerThread c : clients) {
						if (c.getClientName().equals(clientName)) {
							muted = c;
						}
					}
					if (muted == null) {
						client.send("System", "That user doesn't exist!");
						wasCommand = true;
						break;
					} else if (!client.mutedUsers.contains(clientName)) {
						if (!client.getClientName().equals(clientName)) {
							client.mutedUsers.add(clientName);
						} else {
							client.send("System", "You can't mute yourself!");
							wasCommand = true;
							break;
						}

						// onIsMuted(clientName, client.isMuted(clientName));
						client.send("System", clientName + " is now muted");
						muted.send("System", "You were muted by " + client.getClientName());
						//client.saveMuted();

					} else {
						client.send("System", "That user is already muted!");
					}
					wasCommand = true;
					break;
				case UNMUTE:
					clientName = comm2[1];
					ServerThread unmuted = null;
					if (client.mutedUsers.contains(clientName)) {
						if (!client.getClientName().equals(clientName)) {
							client.mutedUsers.remove(clientName);
						}
						for (ServerThread c : clients) {
							if (c.getClientName().equals(clientName)) {
								unmuted = c;
							}
						}
						// client.onIsMuted(clientName, client.isMuted(clientName));
						client.send("System", clientName + " is now unmuted");
						unmuted.send("System", "You were unmuted by " + client.getClientName());
						//client.saveMuted();
					} else {
						client.send("System", "That user is not muted!");
					}
					wasCommand = true;
					break;

				case "pm":
					List<String> PmUsers = new ArrayList<String>();
					PmUsers.add(client.getClientName());
					String newMess = message.replace("/pm", "");
					String[] words = message.split(" ");
					for (String word : words) {
						if (word.contains("@")) {
							String name = word.replace("@", "").toLowerCase();
							PmUsers.add(name);
						}
					}

					sendPm(client, newMess, PmUsers);
					wasCommand = true;
					break;
				}
			}

		} catch (

		Exception e) {
			e.printStackTrace();
		}

		return wasCommand;
	}

	// TODO changed from string to ServerThread
	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());

				}
			}
		}
	}

	protected void sendPm(ServerThread sender, String message, List<String> users) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
			// it was a command,don't broadcast
			return;
		}
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName()) && users.contains(client.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}

		}
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}

}

/*
 * Henry Dinh
 * CS 6390.001 Advanced Computer Networks
 * Final Project - Multi-client chat program using sockets
 * Server class - run first and set the number clients.
 * Clients connect to the server via the server's ip address
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class Server extends JFrame {

	// Constant port number
	public static final int PORT_NUMBER = 8000;

	// GUI - Jpanel and text area for displaying text
	private JTextArea chat_box = new JTextArea();
	private CustomCardLayout card_layout;

	// NETWORKING - create a server socket and socket
	private ServerSocket server_socket;
	private Socket socket;

	// MULTITHREADING - for handling many clients
	private ExecutorService thread_pool;

	// max number of clients allowed
	int max_clients = 0;

	// IP Address of host machine running the server
	private String server_ip_address = "";

	// array list to keep track of clients
	static ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();

	// Constructor for the server
	public Server() {
		// Get public IP Address for server
		try {
			server_ip_address = InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Set up GUI stuff
		chat_box.setEditable(false);
		card_layout = new CustomCardLayout(this, chat_box);
		add(card_layout);
		setTitle("Server");
		setSize(500, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);

		// Wait for user to set maximum number of clients
		while (max_clients == 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		addText("Max number of clients: " + max_clients);

		// Set up number of threads for clients
		thread_pool = Executors.newFixedThreadPool(max_clients);

		// Set up socket to start server
		try {
			// set up server socket with port number
			server_socket = new ServerSocket(PORT_NUMBER);

			// Loop forever
			while (true) {
				socket = server_socket.accept();
				// broadcast update message if new client has connected
				if (clients.size() < max_clients) {
					broadcast("Client " + Integer.toString(clients.size() + 1), " has connected.");
				}

				// if chat room is not full, add client to room and client list
				if (clients.size() < max_clients) {
					ClientHandler client = new ClientHandler(this, socket,
							"Client " + Integer.toString(clients.size() + 1), true);
					clients.add(client);
					thread_pool.execute(client);
				} else {
					// Reject client if chat room full
					ClientHandler client = new ClientHandler(this, socket, "Client " + Integer.toString(clients.size()),
							false);
					thread_pool.execute(client);
				}

				// Ensures display text area stays scrolled to the bottom
				chat_box.selectAll();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getIPAddress() {
		return this.server_ip_address;
	}

	// Broadcast message to all clients and display on server text area
	public void broadcast(String broadcaster_name, String message) {
		String text = broadcaster_name + ": " + message;

		// iterate through every client connected and send message
		for (ClientHandler c : clients) {
			c.print(text);
		}

		// Show message on server's chat log also
		addText(text);
	}

	// Display new text to the text area
	public void addText(String text) {
		// ensure new line between messages
		card_layout.chat_box.append(text + "\n");
	}

	// Returns client list
	public ClientHandler[] getClientList() {
		return (ClientHandler[]) clients.toArray();
	}

	public static void main(String[] args) {
		// Create a server
		new Server();
	}

}

class ClientHandler implements Runnable {
	// Properties
	private Server server;
	private Socket socket;

	// input and output streams to/from the client
	private ObjectInputStream input_from_client;
	private ObjectOutputStream output_to_client;

	// Name of the JPanel
	private String name;

	// Checks to see if client has exitted or not
	boolean exit_button_pressed;
	// Disconnect message from client
	private final static String DISCONNECT_MESSAGE = "0DISCONNECT0_0FROM0_0SERVER0_0NOW0";

	public ClientHandler(Server server, Socket socket, String name, boolean accepted_to_server) {
		this.server = server;

		this.socket = socket;

		this.name = name;

		// establish and set up network connection to client's application
		try {
			output_to_client = new ObjectOutputStream(socket.getOutputStream());
			input_from_client = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!accepted_to_server) {
			rejectClient();
		}
	}

	// Method to run client handler
	@Override
	public void run() {
		// Set up ineaddress from socket
		InetAddress inet_address = socket.getInetAddress();

		// Client hasn't pressed exit button yet
		exit_button_pressed = false;

		// run until exit button pressed
		while (!exit_button_pressed) {
			// message received from the client
			String message_from_client;

			try {
				message_from_client = input_from_client.readObject().toString();

				// Check if client wants to exit
				if (checkForExit(message_from_client)) {
					exit_button_pressed = true;
				} else {
					// Send message to all clients
					server.broadcast(this.name, message_from_client);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// if client exitted, close connection
		try {
			socket.close();
			server.broadcast(this.name, "has disconnected from the server.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Remove client from server's list of clients
		Server.clients.remove(this);
	}

	private boolean checkForExit(String s) {
		if (s.contains(DISCONNECT_MESSAGE)) {
			return true;
		} else {
			return false;
		}
	}

	// send message to this client only
	public void print(String message) {
		try {
			output_to_client.writeObject(message);
			// outputToClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// notify client that the chat room is full, then close connection.
	private void rejectClient() {
		print("The server you are trying to reach is full. Try again later.");
		exit_button_pressed = true;
	}
}

// Custom Jpanel used to hold text area
class CustomCardLayout extends JPanel implements ActionListener {

	// Properties
	JPanel cards;
	final static String BUTTON_PANEL = "Card with JButtons";
	final static String TEXT_PANEL = "Card with JTextField";
	JLabel room = new JLabel("Multi Client Chat Room");
	JLabel clients = new JLabel("Enter number of clients");
	JTextField text_box = new JTextField();
	JPanel card1 = new JPanel();
	JTextArea chat_box = new JTextArea();
	JButton button = new JButton("OK");
	JLabel ip_address_label = new JLabel();

	// Server info
	Server server;
	// String server_ip_address;

	@Override
	public void actionPerformed(ActionEvent e) {
		// User input > 0 for number of clients
		if (Integer.parseInt(text_box.getText()) > 0) {
			CardLayout c1 = (CardLayout) (cards.getLayout());
			c1.show(cards, TEXT_PANEL);
			server.max_clients = Integer.parseInt(text_box.getText());
		}
	}

	// Initialize components of the Jpanel
	private void initComponents() {

		room = new javax.swing.JLabel();
		clients = new javax.swing.JLabel();
		button = new javax.swing.JButton();
		text_box = new javax.swing.JTextField();

		room.setText("Multi Client Chat Room");

		clients.setText("Set Num Clients (1+)");

		button.setText("OK");

		// Set up components and various properties, sizes, etc.
		GroupLayout layout = new GroupLayout(card1);
		card1.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup().addGap(75, 75, 75).addComponent(clients)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(text_box, GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(button).addGap(43, 43, 43))
				.addGroup(layout.createSequentialGroup().addGap(164, 164, 164).addComponent(room)
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(27, 27, 27).addComponent(room).addGap(37, 37, 37)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(button)
								.addComponent(clients).addComponent(text_box, GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(199, 199, 199)));
	}

	// Constructor for custom card layout
	public CustomCardLayout(Server s, JTextArea jta) {
		// initialize GUI
		chat_box = jta;
		server = s;
		ip_address_label.setText("Server IP Address: " + server.getIPAddress());
		initComponents();
		button.addActionListener(this);

		card1.add(room);
		card1.add(clients);
		card1.add(text_box);
		card1.add(button);

		// Set up text area for displaying messages on server
		jta.setColumns(40);
		jta.setLineWrap(true);
		jta.setRows(20);
		jta.setWrapStyleWord(true);
		jta.setEditable(false);

		// Make text area scrollable
		JPanel card2 = new JPanel(new BorderLayout());
		card2.setBorder(new TitledBorder("Server IP Address: " + server.getIPAddress()));
		card2.add(new JScrollPane(jta));

		// display server's ip address
		// card2.add(ip_address_label, BorderLayout.SOUTH);

		// outer JPanel
		cards = new JPanel(new CardLayout());
		cards.add(card1, BUTTON_PANEL);
		cards.add(card2, TEXT_PANEL);

		add(cards);
	}
}
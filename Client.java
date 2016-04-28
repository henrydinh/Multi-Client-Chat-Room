
/*
 * Henry Dinh
 * CS 6390.001 Advanced Computer Networks
 * Final Project - Multi-client chat program using sockets
 * Client class - run sever first and set the number clients.
 * Clients connect to the server via the server's ip address
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame {
	private final static String DISCONNECT_MESSAGE = "0DISCONNECT0_0FROM0_0SERVER0_0NOW0";

	// text field to enter ip address of server
	private JTextField enter_address = new JTextField();

	String server_address = "";

	// Text area to display contents and messages
	private JTextArea chat_box = new JTextArea();

	// Connect to server button
	private JButton connect_button = new JButton("Connect");
	// Button to disconnect from the server
	private JButton disconnect_button = new JButton("Disconnect");

	// Text field to type in message to send
	private JTextField type_message = new JTextField();

	// socket for client
	private Socket socket;

	// IO Streams for sending/receiving to/from server
	private ObjectOutputStream to_server;
	private ObjectInputStream from_server;

	public static void main(String[] args) {
		// Create a new client object
		new Client();
	}

	// Constructor for the client
	public Client() {
		// Panel to hold the top "connect to server" portion
		JPanel top_panel = new JPanel();
		top_panel.setLayout(new BorderLayout());
		top_panel.add(new JLabel("Enter Address: "), BorderLayout.WEST);
		top_panel.add(enter_address, BorderLayout.CENTER);

		// Panel to hold the connect and disconnect buttons
		JPanel button_panel = new JPanel();
		button_panel.setLayout(new BorderLayout());
		button_panel.add(connect_button, BorderLayout.NORTH);
		button_panel.add(disconnect_button, BorderLayout.SOUTH);

		// add button panel to top_panel
		top_panel.add(button_panel, BorderLayout.EAST);

		enter_address.setHorizontalAlignment(JTextField.RIGHT);
		connect_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// if server address is empty
				if (server_address.equals("")) {
					server_address = enter_address.getText();
					enter_address.setText("");
				}
			}
		});
		disconnect_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					to_server.writeObject(DISCONNECT_MESSAGE);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		// Panel for holding lower "sending message" portion
		JPanel bottom_panel = new JPanel();
		bottom_panel.setLayout(new BorderLayout());
		bottom_panel.add(new JLabel("Enter your message"), BorderLayout.NORTH);
		bottom_panel.add(type_message, BorderLayout.CENTER);
		type_message.setHorizontalAlignment(JTextField.RIGHT);
		type_message.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					to_server.writeObject(type_message.getText());
					type_message.setText("");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		// make chat box uneditable
		chat_box.setEditable(false);

		// Set up more GUI stuff
		setLayout(new BorderLayout());
		add(top_panel, BorderLayout.NORTH);
		add(new JScrollPane(chat_box), BorderLayout.CENTER);
		add(bottom_panel, BorderLayout.SOUTH);

		setTitle("Client");
		setSize(400, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);

		// wait for ip address input from user
		while (server_address.equals("")) {
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			// Create a socket to connect to the server
			socket = new Socket(server_address, 8000);
			print("Connected to server.");

			// Create an input stream to receive data from the server
			from_server = new ObjectInputStream(socket.getInputStream());

			// Create an output stream to send data to the server
			to_server = new ObjectOutputStream(socket.getOutputStream());

			while (true) {
				// Display message from the server
				String message = from_server.readObject().toString();
				print(message);

				// Make sure chat_box displays newest messages
				chat_box.selectAll();
			}
		} catch (IOException ex) {
			print("Disconnected from server.");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// display message on chat box
	private void print(String message) {
		chat_box.append(message + "\n");
	}
}

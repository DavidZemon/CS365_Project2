

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/10/13
 * Time: 11:32 PM
 */
public class Client {

	private static final int TIMEOUT = 5;
	//GUI
	//----
	JFrame f = new JFrame("Client");
	JButton setupButton = new JButton("Setup");
	JButton playButton = new JButton("Play");
	JButton pauseButton = new JButton("Pause");
	JButton tearButton = new JButton("Teardown");
	JPanel mainPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	JLabel iconLabel = new JLabel();
	ImageIcon icon;
	static int WIDTH = 500;


	//RTP variables:
	//----------------
	DatagramPacket rcvdp; //UDP packet received from the server
	DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
	static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

	Timer timer; //timer used to receive data from the UDP socket
	byte[] buf; //buffer used to store data received from the server

	//RTSP variables
	//----------------
	//rtsp states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	static int state; //RTSP state == INIT or READY or PLAYING
	Socket RTSPSocket; //socket used to send/receive RTSP messages
	//input and output stream filters
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;
	static String VideoFileName; //video file to request to the server
	int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
	int RTSPid = 0; //ID of the RTSP session (given by the RTSP Server)

	final static String CRLF = "\r\n";

	//Video constants:
	//------------------
	static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

	/**
	 * Constructor
	 */
	public Client () {

		//build GUI
		//--------------------------

		//Frame
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing (WindowEvent e) {
				System.exit(0);
			}
		});

		//Buttons
		buttonPanel.setLayout(new GridLayout(1, 0));
		buttonPanel.add(setupButton);
		buttonPanel.add(playButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(tearButton);
		setupButton.addActionListener(new setupButtonListener());
		playButton.addActionListener(new playButtonListener());
		pauseButton.addActionListener(new pauseButtonListener());
		tearButton.addActionListener(new tearButtonListener());

		//Image display label
		iconLabel.setIcon(null);

		//frame layout
		mainPanel.setLayout(null);
		mainPanel.add(iconLabel);
		mainPanel.add(buttonPanel);
		iconLabel.setBounds(0, 0, WIDTH, 280);
		buttonPanel.setBounds(0, 280, WIDTH - 4, 50);

		f.getContentPane().add(mainPanel, BorderLayout.CENTER);
		f.setSize(new Dimension(WIDTH, 370));
		f.setVisible(true);

		//init timer
		//--------------------------
		timer = new Timer(20, new timerListener());
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		//allocate enough memory for the buffer used to receive data from the server
		buf = new byte[15000];
	}

	/**
	 * Main
	 * @param argv Terminal arguments
	 * @throws Exception
	 */
	public static void main (String argv[]) throws Exception {
		//Create a Client object
		Client theClient = new Client();

		//get server RTSP port and IP address from the command line
		//------------------
		int RTSP_server_port = Integer.parseInt(argv[1]);
		String ServerHost = argv[0];
		InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);

		//get video filename to request:
		VideoFileName = argv[2];

		//Establish a TCP connection with the server to exchange RTSP messages
		//------------------
		theClient.RTSPSocket = new Socket(ServerIPAddr, RTSP_server_port);

		//Set input and output stream filters:
		RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPSocket.getInputStream()));
		RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPSocket.getOutputStream()));

		//init RTSP state:
		state = INIT;
	}


	/**
	 * Handler for setup button
	 */
	class setupButtonListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {

			System.out.println("Setup Button pressed !");

			if (state == INIT) {
				// Init non-blocking RTPSocket that will be used to receive data
				try {
					// Wait to receive RTP packets from the server on port RTP_RCV_PORT
					RTPsocket = new DatagramSocket(Client.RTP_RCV_PORT);
					RTPsocket.setSoTimeout(Client.TIMEOUT);
				} catch (SocketException se) {
					System.out.println("Socket exception: " + se);
					System.exit(0);
				}

				// Init RTSP sequence number
				RTSPSeqNb = 1;

				// Send SETUP message to the server
				sendRTSPRequest("SETUP");

				// Wait for the response
				if (parseServerResponse() != 200) {
					System.out.println("Invalid Server Response");
				} else {
					//change RTSP state and print new state
					state = READY;
					System.out.println("New RTSP state: ....");
				}
			}//else if state != INIT then do nothing
		}
	}

	/**
	 * Handler for play button
	 */
	class playButtonListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {

			//System.out.println("Play Button pressed !");

			if (state == READY) {
				//increase RTSP sequence number
				RTSPSeqNb = RTSPSeqNb++;


				//Send PLAY message to the server
				sendRTSPRequest("PLAY");

				//Wait for the response
				if (parseServerResponse() != 200) System.out.println("Invalid Server Response");
				else {
					//change RTSP state and print out new state
					state = PLAYING;
					// System.out.println("New RTSP state: ...")

					//start the timer
					timer.start();
				}
			}//else if state != READY then do nothing
		}
	}


	/**
	 * Handler for pause button
	 */
	class pauseButtonListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {

			System.out.println("Pause Button pressed !");

			if (state == PLAYING) {
				//increase RTSP sequence number
				RTSPSeqNb = RTSPSeqNb++;

				//Send PAUSE message to the server
				sendRTSPRequest("PAUSE");

				//Wait for the response
				if (parseServerResponse() != 200) System.out.println("Invalid Server Response");
				else {
					//change RTSP state and print out new state
					state = READY;
					System.out.println("New RTSP state: ...");

					//stop the timer
					timer.stop();
				}
			}
			//else if state != PLAYING then do nothing
		}
	}

	/**
	 * Handler for teardown button
	 */
	class tearButtonListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {

			System.out.println("Teardown Button pressed !");

			//increase RTSP sequence number
			RTSPSeqNb = RTSPSeqNb++;


			//Send TEARDOWN message to the server
			sendRTSPRequest("TEARDOWN");

			//Wait for the response
			if (parseServerResponse() != 200) System.out.println("Invalid Server Response");
			else {
				//change RTSP state and print out new state
				state = INIT;
				System.out.println("New RTSP state: ...");

				//stop the timer
				timer.stop();

				//exit
				System.exit(0);
			}
		}
	}


	/**
	 * Handler for timer
	 */
	class timerListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {

			//Construct a DatagramPacket to receive data from the UDP socket
			rcvdp = new DatagramPacket(buf, buf.length);

			try {
				//receive the DP from the socket:
				RTPsocket.receive(rcvdp);

				//create an RTPPacket object from the DP
				RTPPacket rtp_packet = new RTPPacket(rcvdp.getData(), rcvdp.getLength());

				//print important header fields of the RTP packet received:
				System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getSequenceNumber() + " TimeStamp " +
								   rtp_packet.getTimestamp() + " ms, of type " + rtp_packet.getPayloadType());

				//print header bitstream:
				rtp_packet.printHeader();

				//get the payload bitstream from the RTPPacket object
				int payload_length = rtp_packet.getPayload_length();
				byte[] payload = new byte[payload_length];
				rtp_packet.getpayload(payload);

				//get an Image object from the payload bitstream
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = toolkit.createImage(payload, 0, payload_length);

				//display the image as an ImageIcon object
				icon = new ImageIcon(image);
				iconLabel.setIcon(icon);
			} catch (InterruptedIOException iioe) {
				//System.out.println("Nothing to read");
			} catch (IOException ioe) {
				System.out.println("Exception caught: " + ioe);
			}
		}
	}

	/**
	 * Parse server response
	 * @return Returns the server's response
	 */
	private int parseServerResponse () {
		int reply_code = 0;

		try {
			//parse status line and extract the reply_code:
			String StatusLine = RTSPBufferedReader.readLine();
			System.out.println("RTSP Client - Received from Server:");
			System.out.println(StatusLine);

			StringTokenizer tokens = new StringTokenizer(StatusLine);
			tokens.nextToken(); //skip over the RTSP version
			reply_code = Integer.parseInt(tokens.nextToken());

			//if reply code is OK get and print the 2 other lines
			if (reply_code == 200) {
				String SeqNumLine = RTSPBufferedReader.readLine();
				System.out.println(SeqNumLine);

				String SessionLine = RTSPBufferedReader.readLine();
				System.out.println(SessionLine);

				//if state == INIT gets the Session Id from the SessionLine
				tokens = new StringTokenizer(SessionLine);
				tokens.nextToken(); //skip over the Session:
				RTSPid = Integer.parseInt(tokens.nextToken());
			}
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}

		return reply_code;
	}

	/**
	 * Send RTSP request
	 * @param requestType Type of the request to send
	 */
	private void sendRTSPRequest (String requestType) {
		try {
			//Use the RTSPBufferedWriter to write to the RTSP socket

			// Send out the request
			RTSPBufferedWriter.write(requestType + " " + VideoFileName + " RTSP/1.0" + CRLF);
			System.out.print(requestType + " " + VideoFileName + " RTSP/1.0" + CRLF);

			// Send out the CSeq
			RTSPBufferedWriter.write("CSeq " + RTSPSeqNb + CRLF);
			System.out.print("CSeq " + RTSPSeqNb + CRLF);

			// Check if the request type is SETUP...
			if (requestType.equals("SETUP")) {
				// And act accordingly
				RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
				System.out.print("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
			}
			// If not, send the session ID
			else {
				RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
				System.out.print("Session: " + RTSPid + CRLF);
			}

			// Clear the buffer
			try {
				RTSPBufferedWriter.flush();
			} catch (IOException e) {
				System.out.println("Exception caught a: " + e.toString());
			}
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}

}//end of Class Client


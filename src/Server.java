import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/10/13
 * Time: 11:33 PM
 */

public class Server extends JFrame implements ActionListener {

	//RTP variables:
	//----------------
	DatagramSocket RTPSocket; //socket to be used to send and receive UDP packets
	DatagramPacket sendDataPacket; //UDP packet containing the video frames

	InetAddress ClientIPAddr; //Client IP address
	int RTPDestPort = 0; //destination port for RTP packets  (given by the RTSP Client)

	//GUI:
	//----------------
	JLabel label;

	//Video variables:
	//----------------
	int imageNum = 0; //image nb of the image currently transmitted
	VideoStream video; //VideoStream object used to access video frames
	static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
	static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
	static int VIDEO_LENGTH = 500; //length of the video in frames

	Timer timer; //timer used to send the images at the video frame rate
	byte[] buf; //buffer used to store the images to send to the client

	//RTSP variables
	//----------------
	//rtsp states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	//rtsp message types
	final static int SETUP = 3;
	final static int PLAY = 4;
	final static int PAUSE = 5;
	final static int TEARDOWN = 6;

	static int state; //RTSP Server state == INIT or READY or PLAY
	Socket RTSPSocket; //socket used to send/receive RTSP messages
	//input and output stream filters
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;
	static String VideoFileName; //video file requested from the client
	static int RTSP_ID = 123456; //ID of the RTSP session
	int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

	final static String CRLF = "\r\n";

	/**
	 * Constructor
	 */
	public Server () {

		//init Frame
		super("Server");

		//init Timer
		timer = new Timer(FRAME_PERIOD, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		//allocate memory for the sending buffer
		buf = new byte[15000];

		//Handler to close the main window
		addWindowListener(new WindowAdapter() {
			public void windowClosing (WindowEvent e) {
				//stop the timer and exit
				timer.stop();
				System.exit(0);
			}
		});

		//GUI:
		label = new JLabel("Send frame #        ", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);
	}

	/**
	 * Main
	 * @param argv Arguments from the terminal
	 * @throws Exception
	 */
	public static void main (String argv[]) throws Exception {
		//create a Server object
		Server theServer = new Server();

		//show GUI:
		theServer.pack();
		theServer.setVisible(true);

		//get RTSP socket port from the command line
		int RTSPPort = Integer.parseInt(argv[0]);

		//Initiate TCP connection with the client for the RTSP session
		ServerSocket listenSocket = new ServerSocket(RTSPPort);
		theServer.RTSPSocket = listenSocket.accept();
		listenSocket.close();

		//Get Client IP address
		theServer.ClientIPAddr = theServer.RTSPSocket.getInetAddress();

		//Initiate RTSPstate
		state = INIT;

		//Set input and output stream filters:
		RTSPBufferedReader = new BufferedReader(new InputStreamReader(theServer.RTSPSocket.getInputStream()));
		RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.RTSPSocket.getOutputStream()));

		//Wait for the SETUP message from the client
		int request_type;
		boolean done = false;
		while (!done) {
			request_type = theServer.parseRTSPRequest(); //blocking

			if (request_type == SETUP) {
				done = true;

				//update RTSP state
				state = READY;
				System.out.println("New RTSP state: READY");

				//Send response
				theServer.sendRTSPResponse();

				//init the VideoStream object:
				theServer.video = new VideoStream(VideoFileName);

				//init RTP socket
				theServer.RTPSocket = new DatagramSocket();
			}
		}

		//loop to handle RTSP requests
		while (true) {
			//parse the request
			request_type = theServer.parseRTSPRequest(); //blocking

			if ((request_type == PLAY) && (state == READY)) {
				//send back response
				theServer.sendRTSPResponse();
				//start timer
				theServer.timer.start();
				//update state
				state = PLAYING;
				System.out.println("New RTSP state: PLAYING");
			} else if ((request_type == PAUSE) && (state == PLAYING)) {
				//send back response
				theServer.sendRTSPResponse();
				//stop timer
				theServer.timer.stop();
				//update state
				state = READY;
				System.out.println("New RTSP state: READY");
			} else if (request_type == TEARDOWN) {
				//send back response
				theServer.sendRTSPResponse();
				//stop timer
				theServer.timer.stop();
				//close sockets
				theServer.RTSPSocket.close();
				theServer.RTPSocket.close();

				System.exit(0);
			}
		}
	}


	/**
	 * Handler for the timer
	 * @param e Event to act upon
	 */
	public void actionPerformed (ActionEvent e) {

		//if the current image nb is less than the length of the video
		if (imageNum < VIDEO_LENGTH) {
			//update current imageNum
			imageNum++;

			try {
				//get next frame to send from the video, as well as its size
				int image_length = video.getNextFrame(buf);

				//Builds an RTPPacket object containing the frame
				RTPPacket rtp_packet = new RTPPacket(MJPEG_TYPE, imageNum, imageNum * FRAME_PERIOD, buf, image_length);

				//get to total length of the full rtp packet to send
				int packet_length = rtp_packet.getLength();

				//retrieve the packet bitstream and store it in an array of bytes
				byte[] packet_bits = new byte[packet_length];
				rtp_packet.getPacket(packet_bits);

				//send the packet as a DatagramPacket over the UDP socket
				sendDataPacket = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTPDestPort);
				RTPSocket.send(sendDataPacket);

				System.out.println("Send frame #" + imageNum);
				//print the header bitstream
				rtp_packet.printHeader();

				//update GUI
				label.setText("Send frame #" + imageNum);
			} catch (Exception ex) {
				System.out.println("Exception caught: " + ex);
				System.exit(0);
			}
		} else {
			//if we have reached the end of the video file, stop the timer
			timer.stop();
		}
	}

	/**
	 * Parse RTSP request
	 * @return request type
	 */
	private int parseRTSPRequest () {
		int request_type = -1;
		try {
			//parse request line and extract the request_type:
			String RequestLine = RTSPBufferedReader.readLine();
			System.out.println("RTSP Server - Received from Client:");
			System.out.println(RequestLine);

			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String request_type_string = tokens.nextToken();

			//convert to request_type structure:
			if ((request_type_string).compareTo("SETUP") == 0) request_type = SETUP;
			else if ((request_type_string).compareTo("PLAY") == 0) request_type = PLAY;
			else if ((request_type_string).compareTo("PAUSE") == 0) request_type = PAUSE;
			else if ((request_type_string).compareTo("TEARDOWN") == 0) request_type = TEARDOWN;

			if (request_type == SETUP) {
				//extract VideoFileName from RequestLine
				VideoFileName = tokens.nextToken();
			}

			//parse the SeqNumLine and extract CSeq field
			String SeqNumLine = RTSPBufferedReader.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			RTSPSeqNb = Integer.parseInt(tokens.nextToken());

			//get LastLine
			String LastLine = RTSPBufferedReader.readLine();
			System.out.println(LastLine);

			if (request_type == SETUP) {
				//extract RTPDestPort from LastLine
				tokens = new StringTokenizer(LastLine);
				for (int i = 0; i < 3; i++)
					tokens.nextToken(); //skip unused stuff
				RTPDestPort = Integer.parseInt(tokens.nextToken());
			}
			//else LastLine will be the SessionId line ... do not check for now.
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
		return (request_type);
	}

	/**
	 * Send RTSP response
	 */
	private void sendRTSPResponse () {
		try {
			RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
			RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
			RTSPBufferedWriter.flush();
			System.out.println("RTSP Server - Sent response to Client.");
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}
}

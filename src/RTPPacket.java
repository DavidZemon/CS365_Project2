/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/10/13
 * Time: 11:34 PM
 */

public class RTPPacket {

	// Size of the RTP header:
	static int HEADER_SIZE = 12;

	// Fields that compose the RTP header
	public int Version;
	public int Padding;
	public int Extension;
	public int CC;
	public int Marker;
	public int PayloadType;
	public int SequenceNumber;
	public int TimeStamp;
	public int srcID;

	// Bitstream of the RTP header
	public byte[] header;

	// size of the RTP payload
	public int payload_size;
	// Bitstream of the RTP payload
	public byte[] payload;


	//--------------------------
	// Constructor of an RTPPacket object from header fields and payload bitstream
	//--------------------------
	public RTPPacket (int PType, int frameNum, int Time, byte[] data, int data_length) {
		//fill by default header fields:
		Version = 2;
		Padding = 0;
		Extension = 0;
		CC = 0;
		Marker = 0;
		srcID = 0;

		//fill changing header fields:
		SequenceNumber = frameNum;
		TimeStamp = Time;
		PayloadType = PType;

		//build the header bitstream:
		//--------------------------
		header = new byte[HEADER_SIZE];

		//.............
		//TO COMPLETE
		//.............
		//fill the header array of byte with RTP header fields

		//header[0] = ...
		// .....


		//fill the payload bitstream:
		//--------------------------
		payload_size = data_length;
		payload = new byte[data_length];

		//fill payload array of byte from data (given in parameter of the constructor)
		//......

		// ! Do not forget to uncomment method printHeader() below !

	}

	//--------------------------
	//Constructor of an RTPPacket object from the packet bitstream
	//--------------------------
	public RTPPacket (byte[] packet, int packet_size) {
		//fill default fields:
		Version = 2;
		Padding = 0;
		Extension = 0;
		CC = 0;
		Marker = 0;
		srcID = 0;

		//check if total packet size is lower than the header size
		if (packet_size >= HEADER_SIZE) {
			//get the header bitsream:
			header = new byte[HEADER_SIZE];
			for (int i = 0; i < HEADER_SIZE; i++)
				header[i] = packet[i];

			//get the payload bitstream:
			payload_size = packet_size - HEADER_SIZE;
			payload = new byte[payload_size];
			for (int i = HEADER_SIZE; i < packet_size; i++)
				payload[i - HEADER_SIZE] = packet[i];

			//interpret the changing fields of the header:
			PayloadType = header[1] & 127;
			SequenceNumber = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
			TimeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) +
						16777216 * unsigned_int(header[4]);
		}
	}

	//--------------------------
	//getpayload: return the payload bistream of the RTPPacket and its size
	//--------------------------
	public int getpayload (byte[] data) {

		System.arraycopy(payload, 0, data, 0, payload_size);

		return (payload_size);
	}

	//--------------------------
	//getPayload_length: return the length of the payload
	//--------------------------
	public int getPayload_length () {
		return (payload_size);
	}

	//--------------------------
	//getLength: return the total length of the RTP packet
	//--------------------------
	public int getLength () {
		return (payload_size + HEADER_SIZE);
	}

	//--------------------------
	//getPacket: returns the packet bitstream and its length
	//--------------------------
	public int getPacket (byte[] packet) {
		//construct the packet = header + payload
		for (int i = 0; i < HEADER_SIZE; i++)
			packet[i] = header[i];
		for (int i = 0; i < payload_size; i++)
			packet[i + HEADER_SIZE] = payload[i];

		//return total size of the packet
		return (payload_size + HEADER_SIZE);
	}

	//--------------------------
	//getTimestamp
	//--------------------------

	public int getTimestamp () {
		return (TimeStamp);
	}

	//--------------------------
	//getSequenceNumber
	//--------------------------
	public int getSequenceNumber () {
		return (SequenceNumber);
	}

	//--------------------------
	//getPayloadType
	//--------------------------
	public int getPayloadType () {
		return (PayloadType);
	}


	//--------------------------
	//print headers without the SSRC
	//--------------------------
	public void printHeader () {
		//TO DO: uncomment
	/*
    for (int i=0; i < (HEADER_SIZE-4); i++)
      {
	for (int j = 7; j>=0 ; j--)
	  if (((1<<j) & header[i] ) != 0)
	    System.out.print("1");
	else
	  System.out.print("0");
	System.out.print(" ");
      }

    System.out.println();
    */
	}

	//return the unsigned value of 8-bit integer nb
	static int unsigned_int (int nb) {
		if (nb >= 0) return (nb);
		else return (256 + nb);
	}

}

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/10/13
 * Time: 11:34 PM
 */

public class RTPPacket 
{

	// Size of the RTP header:
	static int HEADERSIZE = 12;

	// Fields that compose the RTP header
	public int versionNum;
	public int padding;
	public int extension;
	public int contribSrc;
	public int marker;
	public int payloadType;
	public int seqNum;
	public int timeStamp;
	public int srcID;

	// Bitstream of the RTP header
	public byte[] header;

	// size of the RTP payload
	public int payloadSize;
	// Bitstream of the RTP payload
	public byte[] payload;


	//--------------------------
	// Constructor of an RTPPacket object from header fields and payload bitstream
	//--------------------------
	public RTPPacket (int PType, int frameNum, int Time, byte[] data, int dataLength) 
	{
		//fill by default header fields:
		versionNum = 2;
		padding = 0;
		extension = 0;
		contribSrc = 0;
		marker = 0;
		srcID = 0;

		//fill changing header fields:
		seqNum = frameNum;
		timeStamp = Time;
		payloadType = PType;

		//build the header bitstream:
		//--------------------------
		header = new byte[HEADERSIZE];

		//.............
		//TO COMPLETE
		//.............
		//fill the header array of byte with RTP header fields

	    header[0] = (byte) ((versionNum << 6 | padding << 5 | extension << 4 | contribSrc));
	    
	    header[1] = (byte) (marker << 7 | payloadType);
	    
	    header[2] = (byte) (seqNum >> 8);
	    header[3] = (byte) (seqNum & 0xFF);

	    header[4] = (byte) (timeStamp >> 24);
	    header[5] = (byte) ((timeStamp <<8) >> 24);
	    header[6] = (byte) ((timeStamp <<16) >> 24);
	    header[7] = (byte) ((timeStamp <<24) >> 24);
	   
	    header[8] = (byte) (srcID >> 24);
	    header[9] = (byte) ((srcID <<8) >> 24);
	    header[10] = (byte) ((srcID <<16) >> 24);
	    header[11] = (byte) ((srcID <<24) >> 24);


		//fill the payload bitstream:
		//--------------------------
		payloadSize = dataLength;
		payload = new byte[dataLength];

		//fill payload array of byte from data (given in parameter of the constructor)
	    for (int i=0; i<dataLength; i++)
	    {
	        payload[i] = data[i];
	    }

	}

	//--------------------------
	//Constructor of an RTPPacket object from the packet bitstream
	//--------------------------
	public RTPPacket (byte[] packet, int packetSize)
	{
		//fill default fields:
		versionNum = 2;
		padding = 0;
		extension = 0;
		contribSrc = 0;
		marker = 0;
		srcID = 0;

		//check if total packet size is lower than the header size
		if (packetSize >= HEADERSIZE) {
			//get the header bitsream:
			header = new byte[HEADERSIZE];
			for (int i = 0; i < HEADERSIZE; i++)
				header[i] = packet[i];

			//get the payload bitstream:
			payloadSize = packetSize - HEADERSIZE;
			payload = new byte[payloadSize];
			for (int i = HEADERSIZE; i < packetSize; i++)
				payload[i - HEADERSIZE] = packet[i];

			//interpret the changing fields of the header:
			payloadType = header[1] & 127;
			seqNum = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
			timeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) +
						16777216 * unsigned_int(header[4]);
		}
	}

	/**
	*	getPayload - returns the bitstream of the packet and the size of said bitstream
	*/
	public int getPayload (byte[] data) 
	{

		System.arraycopy(payload, 0, data, 0, payloadSize);

		return (payloadSize);
	}

	/**
	*	getPayloadLength - returns the length of the payload
	*/
	public int getPayloadLength () 
	{
		return (payloadSize);
	}

	/**
	*	getLength - returns total length of packet
	*/
	public int getLength () 
	{
		return (payloadSize + HEADERSIZE);
	}

	/**
	*	getPacket - returns the packet's bitstream and the length of the packet
	*/
	public int getPacket (byte[] packet) 
	{
		//construct the packet = header + payload
		for (int i = 0; i < HEADERSIZE; i++)
			packet[i] = header[i];
		for (int i = 0; i < payloadSize; i++)
			packet[i + HEADERSIZE] = payload[i];

		//return total size of the packet
		return (payloadSize + HEADERSIZE);
	}

	/**
	*	getTimestamp - Getter function for the timestamp
	*/

	public int getTimestamp () 
	{
		return (timeStamp);
	}

	/**
	*	getSequenceNumber - Getter function for the sequence number
	*/
	public int getSequenceNumber () 
	{
		return (seqNum);
	}

	/**
	*	getPayloadType - Getter function for the payload type
	*/
	public int getPayloadType () 
	{
		return (payloadType);
	}


	/**
	*	printHeader - print the packet header w/o the srcId
	*/
	public void printHeader () {
    	for (int i=0; i < (HEADERSIZE-4); i++)
      	{
			for (int j = 7; j>=0 ; j--)
			{
	 	 		if (((1<<j) & header[i] ) != 0)
		    		System.out.print("1");
				else
		 			 System.out.print("0");
				System.out.print(" ");
	      }
	    System.out.println();
	}

	//return the unsigned value of 8-bit integer nb
	static int unsigned_int (int nb) {
		if (nb >= 0) return (nb);
		else return (256 + nb);
	}

}

import java.io.FileInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/10/13
 * Time: 11:34 PM
 */
public class VideoStream {

	FileInputStream fis; //video file
	int frame_nb; //current frame nb

	//-----------------------------------
	//constructor
	//-----------------------------------
	public VideoStream (String filename) throws Exception {

		//init variables
		fis = new FileInputStream(filename);
		frame_nb = 0;
	}

	//-----------------------------------
	// getNextFrame
	//returns the next frame as an array of byte and the size of the frame
	//-----------------------------------
	public int getNextFrame (byte[] frame) throws Exception {
		int length;
		String length_string;
		byte[] frame_length = new byte[5];

		//read current frame length
		//noinspection ResultOfMethodCallIgnored
		fis.read(frame_length, 0, 5);

		//transform frame_length to integer
		length_string = new String(frame_length);
		length = Integer.parseInt(length_string);

		return (fis.read(frame, 0, length));
	}
}

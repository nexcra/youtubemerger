package video;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;

public class Cutter {
	/**
	 * Infile should be a youtube id!
	 * 
	 * @param inFile
	 * @param from
	 * @param to
	 */
	public static long cut(String inFile, long from, long to,
			IMediaWriter writer, long offset, int width, int height, int audioChannels, int audioRate) {		
		inFile = getFileUrl(inFile);
		IContainer readContainer = IContainer.make();
		readContainer.open(inFile, IContainer.Type.READ, null);
		
		IStreamCoder vCoder = readContainer.getStream(0).getStreamCoder();
		IStreamCoder aCoder = readContainer.getStream(1).getStreamCoder();
		
		//Preparing resamplers
		IVideoResampler videoResampler = IVideoResampler.make(width, height, vCoder
				.getPixelType(), vCoder.getWidth(), vCoder.getHeight(), vCoder
				.getPixelType());
		IAudioResampler audioResampler=IAudioResampler.make(audioChannels, aCoder.getChannels(), audioRate, aCoder.getSampleRate());
		
		long firstTimeStamp = -1;
		IPacket packet = IPacket.make();
		vCoder.open();
		aCoder.open();		
		
		boolean writtenKeyFrame=false;
		//Write to the file!
		long lastPacketStamp=0;
		while (readContainer.readNextPacket(packet) == 0) {
			if (packet.getTimeStamp() > from && packet.getTimeStamp() < to) {
				if(!writtenKeyFrame){
					if(packet.isKeyPacket() && packet.getStreamIndex()==0){
						writtenKeyFrame=true;
					}else{
						continue;
					}
				}
				
				//Fixing timestamp
				if (firstTimeStamp == -1) {
					firstTimeStamp = packet.getTimeStamp();
					packet.setTimeStamp(offset);
					packet.setDts(packet.getTimeStamp());
					packet.setPts(packet.getTimeStamp());
				} else {
					long oldTime = packet.getTimeStamp();
					packet.setTimeStamp(oldTime - firstTimeStamp + offset);
					packet.setDts(packet.getTimeStamp());
					packet.setPts(packet.getTimeStamp());
				}
				System.out.println(packet.getTimeStamp());//Nice while developing//debugging
				if (packet.getStreamIndex() == 0) {//Video packet
					IVideoPicture picture = IVideoPicture.make(vCoder.getPixelType(), vCoder.getWidth(), vCoder.getHeight());
					int packetOffset = 0;
					while (packetOffset < packet.getSize()) {
						int bytesDecoded = vCoder.decodeVideo(picture, packet, packetOffset);
						if (bytesDecoded < 0){
							 throw new RuntimeException("Error decoding video!");
						 }
						packetOffset += bytesDecoded;			
						if (picture.isComplete()){
							IVideoPicture newPic = IVideoPicture.make(videoResampler.getOutputPixelFormat(), width, height);
							videoResampler.resample(newPic, picture);
							writer.encodeVideo(0, newPic);
						}
					}
				}else if (packet.getStreamIndex() == 1) {//Audio packet			
					 IAudioSamples samples = IAudioSamples.make(1024, aCoder.getChannels()); 
					 int packetOffset = 0;
					 while(packetOffset < packet.getSize()){
						 int bytesDecoded = aCoder.decodeAudio(samples, packet, packetOffset);
						 if (bytesDecoded < 0){
							 throw new RuntimeException("Error decoding audio!");
						 }
						 packetOffset += bytesDecoded; 
						 if (samples.isComplete()){
							 IAudioSamples newAudio=IAudioSamples.make(1024, audioChannels);
							 audioResampler.resample(newAudio, samples, samples.getNumSamples());
							 writer.encodeAudio(1, newAudio); 
						 }
					 }
				}
			} else if (packet.getTimeStamp() > to) {//We're done!
				long oldTime = packet.getTimeStamp();
				packet.setTimeStamp(oldTime - firstTimeStamp + offset);
				lastPacketStamp=packet.getTimeStamp();
				break;
			}
		}
		vCoder.close();
		aCoder.close();
		return lastPacketStamp-firstTimeStamp;
	}

	/**
	 * Finding the direct link to the flv file for a youtube video ID...
	 * @param videoId
	 * @return
	 */
	public static String getFileUrl(String videoId) {
		// We have to get the url for the flv file at the given youtube video id
		String requestUrl = "http://www.youtube.com/get_video_info?&video_id="
				+ videoId;

		try {
			// We're making an Http request to get enough information to get the
			// URL of the video file.
			URL url = new URL(requestUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn
					.getInputStream()));

			String line = rd.readLine();
			int from = line.indexOf("&token=") + 7;
			int to = line.indexOf("&thumbnail_url=");
			String id = line.substring(from, to);
			String tmp = "http://www.youtube.com/get_video?video_id=" + videoId
					+ "&t=" + id;
			// We're then creating a connection to that url to get redirected to
			// the real file url! (ugly)
			url = new URL(tmp);
			conn = (HttpURLConnection) url.openConnection();
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			rd.readLine();
			tmp = conn.getURL().toString();

			return tmp;
		} catch (Exception e) {
			System.out.println("Could not find flv-url " + videoId + "! "
					+ e.getMessage());
		}
		return "";
	}
}

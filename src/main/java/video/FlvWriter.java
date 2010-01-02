package video;

import java.util.ArrayList;
import java.util.List;

import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;


public class FlvWriter {
	private long offset=0;
	private int maxWidth=0, maxHeight=0;
	
	public FlvWriter() throws Exception {
		List<Object[]> videos=new ArrayList<Object[]>();
		videos.add(new Object[]{"http://www.youtube.com/watch?v=WJEAeRdgqKA", 60000, 70000});
		videos.add(new Object[]{"http://www.youtube.com/watch?v=1QP-SIW6iKY", 5000, 35000});
		
		//Finding max height, width, have to rescale the other videos to this
		IStreamCoder audioCoder=null, videoCoder=null;
		
		for(Object[] vid:videos){
			String video=vid[0].toString();
			String inFile = Cutter.getFileUrl(video.substring(video.indexOf("?v=") + 3, video.indexOf("?v=") + 14));
			
			IContainer readContainer = IContainer.make();
			readContainer.open(inFile, IContainer.Type.READ, null);
			
			IStreamCoder vCoder = readContainer.getStream(0).getStreamCoder();
			IStreamCoder aCoder = readContainer.getStream(1).getStreamCoder();
			if(vCoder.getWidth()>maxWidth){
				maxWidth=vCoder.getWidth();
				maxHeight=vCoder.getHeight();
				audioCoder=aCoder;
				videoCoder=vCoder;
			}
			System.out.println("Dimensions: " + vCoder.getWidth() + "x" + vCoder.getHeight());
		}
		
		//Preparing stuff!
		IContainer writeContainer = IContainer.make();
		writeContainer.open("temp.flv", IContainer.Type.WRITE, null);
		

		IStream outVideoStream = writeContainer.addNewStream(0);
		outVideoStream.setStreamCoder(videoCoder);

		IStream outAudioStream = writeContainer.addNewStream(1);
		outAudioStream.setStreamCoder(audioCoder);

		writeContainer.writeHeader();
		
		//ToolFactory.setTurboCharged(true);
			
		for(Object[] vid:videos){
			write(vid[0].toString(), Long.parseLong(vid[1].toString()), Long.parseLong(vid[2].toString()), writeContainer);
		}

		writeContainer.writeTrailer();
		writeContainer.close();
	}

	private void write(String video, long from, long to, IContainer writeContainer) throws Exception {
		String id = video.substring(video.indexOf("?v=") + 3, video.indexOf("?v=") + 14);
		System.out.println("Downloading ID " + video + " from " + from + " to " + to + " with offset " + offset);
		Cutter.cut(id, from, to, writeContainer, offset, maxWidth, maxHeight); 
		offset+=(to-from);
		System.out.println(offset);
	}

	public static void main(String[] args) throws Exception {
		new FlvWriter();
	}
}

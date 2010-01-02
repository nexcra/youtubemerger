package video;

import java.util.ArrayList;
import java.util.List;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStreamCoder;


public class FlvWriter {
	private long offset=0;
	private int maxWidth=0, maxHeight=0, audioChannels=0, audioSampleRate=0;//Data about the output file
	
	public FlvWriter() throws Exception {
		//The clips we want to merge
		List<Object[]> videos=new ArrayList<Object[]>();
		videos.add(new Object[]{"http://www.youtube.com/watch?v=ZNGe7iK1O-4", 0, 46000});
		videos.add(new Object[]{"http://www.youtube.com/watch?v=WJEAeRdgqKA", 170000, 230000});
		
		//Finding max height, width, have to rescale the other videos to this
		IStreamCoder audioCoder=null, videoCoder=null;
		for(Object[] vid:videos){
			String video=vid[0].toString();
			String inFile = Cutter.getFileUrl(video.substring(video.indexOf("?v=") + 3, video.indexOf("?v=") + 14));
			
			IContainer readContainer = IContainer.make();
			readContainer.open(inFile, IContainer.Type.READ, null);
			
			IStreamCoder vCoder = readContainer.getStream(0).getStreamCoder();
			IStreamCoder aCoder = readContainer.getStream(1).getStreamCoder();
			System.out.println(vCoder);//Hmm...
			if(vCoder.getWidth()>maxWidth){
				maxWidth=vCoder.getWidth();
				maxHeight=vCoder.getHeight();
				audioCoder=aCoder;
				videoCoder=vCoder;
			}
		}
		audioChannels=audioCoder.getChannels();
		audioSampleRate=audioCoder.getSampleRate();

		if(audioCoder.getCodec()==null || videoCoder.getCodec()==null){
			throw new RuntimeException("Couldn't get codecs, try again!");
		}
		
		//Preparing the writer
		IMediaWriter writer = ToolFactory.makeWriter("target/writer.flv");
		writer.addVideoStream(0, 0, maxWidth, maxHeight);
		writer.addAudioStream(1, 0, audioCoder.getChannels(), audioCoder.getSampleRate());
			
		for(Object[] vid:videos){
			write(vid[0].toString(), Long.parseLong(vid[1].toString()), Long.parseLong(vid[2].toString()), writer);
		}

		writer.close();
	}

	private void write(String video, long from, long to, IMediaWriter writeContainer) throws Exception {
		String id = video.substring(video.indexOf("?v=") + 3, video.indexOf("?v=") + 14);//Ugly, finding video ID
		System.out.println("Downloading ID " + video + " from " + from + " to " + to + " with offset " + offset);
		Cutter.cut(id, from, to, writeContainer, offset, maxWidth, maxHeight, audioChannels, audioSampleRate); 
		offset+=(to-from);
	}

	public static void main(String[] args) throws Exception {
		new FlvWriter();
	}
}

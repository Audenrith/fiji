package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.Map;

import net.imglib2.img.ImgPlus;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import ij.ImagePlus;
import ij.ImageStack;

public class JostSegmenter implements Segmenter{
	
	
	public static final String SEGMENTER_KEY = "JOST_SEGMENTER";
	public static final String NAME = "Jost Segmenter";
	public static final String INFO_TEXT = "<html>" +
			"This segmenter is based on the paper of Jost et al. \"3D-Image analysis platform <br>" +
			"monitoring relocation of pluripotency genes during reprogramming\" 2011, Nucleic <br>" +
			"Acid Research. <br>" +
			"It does a global thresholding operation with the average image value." +
			"</html>";

	private final static String BASE_ERROR_MESSAGE = "JOST_SEGMENTER: ";
	private String errorMessage = "";
	
	/** Logger used to echo progress on tracking. */
	protected final Logger logger;
	
	/** Data model of the Segmentation */
	protected final Model model;
	
	/** Image */
	protected ImagePlus regionImage;
	protected ImagePlus image;
	
	public JostSegmenter(Model model, ImagePlus image) {
		this.logger = model.getLogger();
		this.model = model;
		this.image = image;
	}

	/**
	 * Divides the Image in two parts. These parts should be segmented afterwards
	 * @param image to binarize
	 * @return binarized Image
	 */
	private ImagePlus getBinarizedImage() {
		ImageStack stack = image.getStack();
		
		int width = stack.getWidth();
		int height = stack.getHeight();

		
		Object pixelsChr = stack.getPixels(1);
		
		double[] imgChr = null;
		
		if (pixelsChr instanceof byte[]) {
			byte[] dataChr = (byte[])pixelsChr;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
			
		} else if (pixelsChr instanceof short[]) {
			short[] dataChr = (short[])pixelsChr;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof int[]) {
			int[] dataChr = (int[])pixelsChr;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof float[]) {
			float[] dataChr = (float[])pixelsChr;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof double[]) {
			imgChr = (double[])pixelsChr;
		} else {
			throw new IllegalArgumentException("Unsuproted data type");
		}
				
		
		double[] imgAv = new double[imgChr.length];
		int[] imgThresh = new int[imgChr.length];
		double avChr=0;
		int numAvChr=0;
		double avAv=0;
		int numAvAv = 0;
		
		
		// Determine Average
		for (int t = 0; t<imgChr.length; t++) {
			
			if (imgChr[t]>0) {
				avChr+=imgChr[t];
				numAvChr++;
			}
		}
		
		avChr /= numAvChr;
		
		// Create Average Image
		// Create Average Image Average
		
		for (int t=0; t<imgChr.length; t++) {
			imgAv[t] = imgChr[t]/avChr ;//+ img2[t]/av2;
			if (imgAv[t]>0){
				avAv += imgAv[t];
				numAvAv++;
			}				
		}
		avAv /= numAvAv;
		
		// Thresholding via Average
		for (int t=0; t<imgChr.length; t++) {
			if (imgAv[t]<avAv) {
				imgAv[t] = 0;
			}
		}
		
		// Do Pixel count
		for (int x=5; x<width-5; x++) {
			for (int y=5; y<height-5; y++) {
				
				int threshCounter = 0;
				
				for (int ii = -5; ii<6; ii++) {
					for (int jj = -5; jj<6; jj++) {
						if (imgAv[(y+jj)*width+(x+ii)]>0) {
							threshCounter++;
						}
					}
				}
				
				if (threshCounter>50) {
					imgThresh[(y*width+x)] = 255;
				} else {
					imgThresh[(y*width+x)] = 0;
				}
			}
		}
		
		ImageStack segStack = new ImageStack(width,height);
		segStack.addSlice("binImg", imgThresh);
		
		return new ImagePlus("Binarized Image",segStack); 
		
	}
	
	private ImagePlus divideInRegions(ImagePlus binImage) {
				
		ImageStack stack = binImage.getStack();
		
		int width = stack.getWidth();
		int height = stack.getHeight();
		
		int[] pixel = (int[])stack.getPixels(1);
		
		ArrayList<Integer> usedValues = new ArrayList<Integer>();

		
		usedValues.add(0);
		usedValues.add(255);
		
		model.getLogger().setProgress(0.5);
		pixel = segmentAllPixelsWithValue(pixel, 0, usedValues, width, height);
		model.getLogger().setProgress(0.6);
		pixel = segmentAllPixelsWithValue(pixel, 255, usedValues, width, height);
		model.getLogger().setProgress(0.7);
		
		ImageStack segStack = new ImageStack(width,height);
		segStack.addSlice("segmImg", pixel);
		
		return new ImagePlus("Segmented Image",segStack); 
	}

	private int[] segmentAllPixelsWithValue(int[] pixel, int value, ArrayList<Integer> usedValues, int width, int height) {
			
		
		int seed = valueEquals(pixel, value);
		while (seed!=-1) {
			
			ArrayList<Integer> testPoints = new ArrayList<Integer>();
			testPoints.add(seed);
			
			int targetValue = findUsableValue(usedValues, 0, 255);
			usedValues.add(targetValue);
			pixel[seed] = targetValue;
			
			while (testPoints.size()>0) {
				int point =testPoints.get(0); 
				
				int x = point%width;
				int y = (point-x)/width; 
				
				if (x-1>0 && pixel[point-1] == value) {
					testPoints.add(point-1);
					pixel[point-1] = targetValue;
				} 
				if (x+1 < width && pixel[point+1] == value) {
					testPoints.add(point+1);
					pixel[point+1] = targetValue;
				} 
				if (y-1 > 0 && pixel[(y-1)*width+x] == value) {
					testPoints.add((y-1)*width+x);
					pixel[(y-1)*width+x] = targetValue;
				}
				if (y+1 < height && pixel[(y+1)*width+x] == value) {
					testPoints.add((y+1)*width+x);
					pixel[(y+1)*width+x] = targetValue;
				}
				
				testPoints.remove(0);
				
				
				
			}
			seed = valueEquals(pixel, value);
			
		}
		
		return pixel;
	}
	
	/** Finds a value between minRange and maxRange which is not in usedValues
	 * 
	 * @param usedValues
	 * @param minRange
	 * @param maxRange
	 * @return
	 */
	private int findUsableValue(ArrayList<Integer> usedValues, int minRange, int maxRange) {
		for (int i=minRange; i<=maxRange; i++) {
			if (!usedValues.contains(i)) {
				return i;
			}
		}
		return minRange-1;
	}
	
	/** 
	 * 
	 * @param pixel int[] of the pixelValues
	 * @param value Value to search for
	 * @return One Pixel with value or -1
	 */
	private int valueEquals(int[] pixel, int value){
		for (int t=0; t<pixel.length; t++) {
			if (pixel[t]==value) {
				return t;
			}
		}
		return -1;
	}
	
	@Override
	public boolean checkInput() {
		return model!=null;
	}



	@Override
	public boolean process() {
		
		
		ImagePlus binImage = getBinarizedImage();
		model.getLogger().setProgress(0.4);
		
		regionImage = divideInRegions(binImage);
				
		return true;
	}



	@Override
	public String getErrorMessage() {
		return BASE_ERROR_MESSAGE+errorMessage;
	}



	@Override
	public String getKey() {
		return SEGMENTER_KEY;
	}


	@Override
	public ImagePlus getResult() {
		return regionImage;
	}
	
	
	
}

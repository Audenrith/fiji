package fiji.plugin.trackmate.segmentation;

import ij.ImagePlus;
import net.imglib2.algorithm.OutputAlgorithm;



/**
 * This interface should be used when writing a new segmentation algorithm for dividing
 * a picture into different regions.
 * <p>
 * The spots now can be selected by the region they lie in. 
 * 
 * @author Benjamin Audenrith
 *
 */

public interface Segmenter extends OutputAlgorithm<ImagePlus> {
		
	public int BACKGROUND_REGION = 0;
	
	/** @return a unique String identifier for this tracker. */
	public String getKey();

	
}

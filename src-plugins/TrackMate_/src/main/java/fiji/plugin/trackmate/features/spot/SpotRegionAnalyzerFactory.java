package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;

public class SpotRegionAnalyzerFactory<T extends RealType<T> & NativeType<T>> implements SpotAnalyzerFactory<T> {

	/*
	 * FIELDS
	 */
	
	/** The single feature key name that this analyzer computes. */
	public static final String						KEY = "Spot Regionnumbers and Regionnames";
	
	public static final String						REGIONNUMBER = "REGIONNUMBER";
	public static final String						REGIONNAME = "REGIONNAME";
	
	public static final ArrayList<String> 			FEATURES = new ArrayList<String>(1);
	public static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(1);
	public static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	public static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	static {
		FEATURES.add(REGIONNUMBER);
		//FEATURES.add(REGIONNAME);
		
		FEATURE_NAMES.put(REGIONNUMBER, "Regionnumber");
		//FEATURE_NAMES.put(REGIONNAME, "Regionname");
		
		FEATURE_SHORT_NAMES.put(REGIONNUMBER, "Region");
		//FEATURE_SHORT_NAMES.put(REGIONNAME, "Region");
		
		FEATURE_DIMENSIONS.put(REGIONNUMBER, Dimension.NONE);
		//FEATURE_DIMENSIONS.put(REGIONNAME, Dimension.NONE);
	}
	
	private final Model model;
	private ImgPlus<T> img;
	
	public static Settings settings;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public SpotRegionAnalyzerFactory(final Model model, ImgPlus<T> img) {
		this.model = model;
		this.img = img;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public final SpotRegionAnalyzer<T> getAnalyzer(final int frame, final int channel) {
		
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, channel);
		final ImgPlus<T> imgCT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		final Iterator<Spot> spots = model.getSpots().iterator(frame, false);
				
		SpotRegionAnalyzer<T> sra = new SpotRegionAnalyzer<T>(imgCT, spots);
		
		if (settings!=null) {
			sra.setRegionPixels((int[])(settings.regionImage.getStack().getPixels(1)));
			sra.setWidth(settings.regionImage.getWidth());
		} else {
			int[] zeroImage = new int[(int) (img.calibration(0)*img.calibration(1))];
			for (int i=0; i<zeroImage.length; i++) {
				zeroImage[i] = 0;
			}
			sra.setRegionPixels(zeroImage);
			sra.setWidth((int) img.calibration(0));
		}
		
		
		model.getLogger().log("SpotRegionAnaylizer was created\n");
				
		return sra;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public List<String> getFeatures() {
		return FEATURES;
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	}

}

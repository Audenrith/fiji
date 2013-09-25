package fiji.plugin.trackmate.features.spot;


import static fiji.plugin.trackmate.features.spot.SpotRegionAnalyzerFactory.REGIONNUMBER;

import java.util.Iterator;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;

public class SpotRegionAnalyzer<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	//TODO START
	
	//private Map<String, List<Integer>> regions;
	private int[] pixels;
	private int width;
	
	public SpotRegionAnalyzer(final ImgPlus<T> img, final Iterator<Spot> spots) {
		super(img, spots);
	}
	
	/*public void setRegions(final Map<String, List<Integer>> regions) {
		this.regions = regions;
	}*/
	
	public void setRegionPixels(final int[] pixels) {
		this.pixels = pixels;
	}
	
	public void setWidth(final int width) {
		this.width = width;
	}
	
	public final void process(final Spot spot) {
		
		double x = spot.getFeature(Spot.POSITION_X);
		double y = spot.getFeature(Spot.POSITION_Y);
		
		System.out.println(width+" "+(y*width+x));

		
		int regionNumber = pixels[(int) (y*width+x)];
		/*
		String[] posibleRegions = RegionSelectPanel.REGION_STRINGS; 
		
		String regionName="NONE";
		for (int i=0; i<posibleRegions.length; i++) {
			if (regions.containsKey(posibleRegions[i])){
				if (regions.get(posibleRegions[i]).contains(regionNumber)) {
					regionName = posibleRegions[i];
				}
			}
			
		}*/
		
		//spot.putFeature(REGIONNAME, regionName);
		spot.putFeature(REGIONNUMBER, new Double(regionNumber));
	}
}

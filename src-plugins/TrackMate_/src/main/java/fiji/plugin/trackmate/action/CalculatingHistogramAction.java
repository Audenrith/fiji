package fiji.plugin.trackmate.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.HistogramGrapher;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.descriptors.RegionSelectDescriptor;

public class CalculatingHistogramAction extends AbstractTMAction {


	public static final String COMPLETE_HISTO = "Histogram of all tracks";
	public static final String STAY_INSIDE = "Stay inside";
	public static final String STAY_OUTSIDE = "Stay outside";
	public static final String STAY_INTERSECTING = "Stay in the intersection";
	public static final String INSIDE_OUT = "Move from inside to outside";
	public static final String OUTSIDE_IN = "Move from outside to inside";
	public static final String NOCODE = "No Code";
	
	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/lightbulb.png"));
	public static final String NAME = "Calculating Histogram";
	public static final String INFO_TEXT =  "<html>" +
				"This action calculates the "+COMPLETE_HISTO+"."+
				"<p>" +
				"There are also some histograms of special region(changes)." +
				"The following region changes (First spot-> Last spot)" +
				"are combined: "+
				"<p>"+
				"     - "+STAY_INSIDE+" IN->IN, INTER->IN, IN->INTER"+
				"<p>"+
				"     - "+STAY_OUTSIDE+" OUT->OUT, INTER->OUT, OUT->INTER"+
				"<p>"+
				"     - "+STAY_INTERSECTING+" INTER->INTER"+
				"<p>"+
				"     - "+INSIDE_OUT+" IN->OUT"+
				"<p>"+
				"     - "+OUTSIDE_IN+" OUT->IN"+
				"<p>"+
				"</html>" ;
	
	
	public CalculatingHistogramAction(TrackMate trackmate, TrackMateGUIController controller) {
		super(trackmate, controller);
		this.icon = ICON;
	}
	
	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public void execute() {
		
		logger.log("Calculating histogram of all tracks.\n");
		int ntracks = trackmate.getModel().getSpots().getNSpots(false);
		if (ntracks == 0) {
			logger.error("No visible track found. Aborting.\n");
			return;
		}
				
		//logger.log(model.getSettings().imp.getCalibration().getUnits()+"\n"+model.getSettings().imp.getCalibration().frameInterval+"\n");
		combineRegionChanges(trackmate.getSettings());
				
		HistogramGrapher grapher = new HistogramGrapher(trackmate, getHistogramKeys());
		
		grapher.render();
		
		logger.setProgress(1);
		
		logger.log("done.\n");
	}
	
	private void combineRegionChanges(Settings settings) {
		
		String IN_STRING = RegionSelectDescriptor.IN_STRING;
		String BACKGROUND_STRING = RegionSelectDescriptor.BACKGROUND_STRING;
		String INTERSECTION_STRING = RegionSelectDescriptor.INTERSECTION_STRING;
		
		Map<String, List<Integer[]>> histograms = new HashMap<String, List<Integer[]>>(6);
		Map<String, List<Integer>> regions = settings.regions;
		
		// Complete Histo
		histograms.put(COMPLETE_HISTO, null);
		
		// Load all regions
		List<Integer> inRegions = regions.get(IN_STRING);
		List<Integer> backgroundRegions = regions.get(BACKGROUND_STRING);
		List<Integer> intersectionRegions = regions.get(INTERSECTION_STRING);
		
		if (inRegions==null) {
			inRegions = new ArrayList<Integer>();
		}
		if (backgroundRegions==null) {
			backgroundRegions = new ArrayList<Integer>();
		}
		if (intersectionRegions==null) {
			intersectionRegions = new ArrayList<Integer>();
		}
		
		
		
		// Stay Inside
		List<Integer[]> histoList = new ArrayList<Integer[]>();
		{
			// IN->IN
			for (int i=0; i<inRegions.size(); i++) {
				Integer[] intArray = {inRegions.get(i), inRegions.get(i)};
				histoList.add(intArray);
			}
			// IN->INTERSECTION
			// INTERSECTION->IN
			for (int i=0; i<inRegions.size(); i++) {
				for (int s=0; s<intersectionRegions.size(); s++) {
					Integer[] intArray = {inRegions.get(i), intersectionRegions.get(s)};
					histoList.add(intArray);
					intArray = new Integer[]{intersectionRegions.get(s), inRegions.get(i)};
					histoList.add(intArray);
				}
				
			}
		}
		histograms.put(STAY_INSIDE, histoList);
		
		
		
		
		// Stay Outside
		histoList = new ArrayList<Integer[]>();
		{
			// OUT->OUT
			for (int b1=0; b1<backgroundRegions.size(); b1++) {
				for (int b2=0; b2<backgroundRegions.size(); b2++) {
					Integer[] intArray = {backgroundRegions.get(b1), backgroundRegions.get(b2)};
					histoList.add(intArray);
				}
			}
			// OUT->INTERSECTION
			// INTERSECTION->OUT
			for (int b=0; b<backgroundRegions.size(); b++) {
				for (int s=0; s<intersectionRegions.size(); s++) {
					Integer[] intArray = {backgroundRegions.get(b), intersectionRegions.get(s)};
					histoList.add(intArray);
					intArray = new Integer[]{intersectionRegions.get(s), backgroundRegions.get(b)};
					histoList.add(intArray);
				}
				
			}
		}
		histograms.put(STAY_OUTSIDE, histoList);

		
		
		
		
		// Stay Intersecting
		histoList = new ArrayList<Integer[]>();
		{
			// INTERSECTION->INTERSECTION
			for (int s1=0; s1<intersectionRegions.size(); s1++) {
				for (int s2=0; s2<intersectionRegions.size(); s2++) {
					Integer[] intArray = {intersectionRegions.get(s1), intersectionRegions.get(s2)};
					histoList.add(intArray);
				}
			}
		}
		histograms.put(STAY_INTERSECTING, histoList);
		
		
		
		
		// Inside Out
		histoList = new ArrayList<Integer[]>();
		{
			// IN->OUT
			for (int i=0; i<inRegions.size(); i++) {
				for (int b=0; b<backgroundRegions.size(); b++) {
					Integer[] intArray = {inRegions.get(i), backgroundRegions.get(b)};
					histoList.add(intArray);
				}
			}
		}
		histograms.put(INSIDE_OUT, histoList);
				
		
		
		
		// Outside In
		histoList = new ArrayList<Integer[]>();
		{
			// OUT->IN
			for (int i=0; i<inRegions.size(); i++) {
				for (int b=0; b<backgroundRegions.size(); b++) {
					Integer[] intArray = {backgroundRegions.get(b), inRegions.get(i)};
					histoList.add(intArray);
				}
			}
		}
		histograms.put(OUTSIDE_IN, histoList);
				
		
		
		
		settings.histograms = histograms;
				
	}

	private static String[] getHistogramKeys() {
		
		String[] returnString ={COMPLETE_HISTO, STAY_INSIDE, STAY_OUTSIDE ,STAY_INTERSECTING , INSIDE_OUT , OUTSIDE_IN }; 
		
		return returnString;
		
	}

}

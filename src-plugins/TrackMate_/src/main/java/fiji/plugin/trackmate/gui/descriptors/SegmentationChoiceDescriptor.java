package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.ListChooserRadioButtonPanel;
import fiji.plugin.trackmate.providers.SegmentationProvider;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;

public class SegmentationChoiceDescriptor implements WizardPanelDescriptor {

	//public static final String DESCRIPTOR = "SegmentationChoiceDescriptor";
	public static final String KEY = "SegmentationChoiceDescriptor";
	
	private static final String LOADIMAGE = "Load an Image";
	private static final String USEAVERAGE = "Use the Average of the Imagestack";
	
	private ListChooserRadioButtonPanel component;
	private TrackMateGUIController controller;
	protected final LogPanel logPanel;
	protected final TrackMate trackmate;
	protected SegmentationProvider provider;
	
	private List<String> segmentationImageSources;
	
	public SegmentationChoiceDescriptor(SegmentationProvider segmentationProvider, TrackMate trackmate, final TrackMateGUIController controller) {
		this.controller = controller;
		this.logPanel = controller.getGUI().getLogPanel();
		this.trackmate = trackmate;
		provider = segmentationProvider;

		List<String> segmenterNames =  provider.getNames();
		List<String> infoTexts = provider.getInfoTexts();
		segmentationImageSources = new ArrayList<String>();
		segmentationImageSources.add(LOADIMAGE);
		segmentationImageSources.add(USEAVERAGE);
		
		this.component = new ListChooserRadioButtonPanel(segmenterNames, infoTexts, "segmenter",
				"Select the Segmentation Image Source", segmentationImageSources);
	}
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public void aboutToHidePanel() {
		
		// Set Region Image
		String actionString = segmentationImageSources.get(component.getRadioButtonChoice());

		if (actionString==LOADIMAGE) {
			// Load a new Image as Region Image
			trackmate.getSettings().segmentationImage = trackmate.loadImage(controller, "Load Segmentation Image");
		} else if (actionString==USEAVERAGE) {
			// Use the average Image of the Stack as RegionImage
			trackmate.getSettings().segmentationImage = trackmate.getImageAverage(trackmate.getSettings().imp);
			
		} else {
			// False Choice
			throw new IllegalArgumentException();
		}
		
		SegmentationProvider provider = controller.getSegmentationProvider();
		int index = component.getListChoice();
		String key = provider.getKeys().get(index);
		
		// Set Segmenter
		if (trackmate.getSettings().segmenter == null || !trackmate.getSettings().segmenter.getKey().equals(key)) {
			provider.select(key);
			logPanel.getLogger().log("Selected Segmenter: "+key+"\n");
			
			trackmate.getSettings().segmenter = provider.getSegmenter(key, trackmate.getSettings().segmentationImage);
			trackmate.getSettings().regions = new HashMap<String, List<Integer>>();
			
		}
		
		
	}

	@Override
	public void displayingPanel() {

		// Reset of previously set Settings if return from Segmentation Descriptor
		if (trackmate.getSettings().segmenter != null &&
				trackmate.getSettings().segmenter.getKey().equals(ManualSegmenter.SEGMENTER_KEY)) {
			((ManualSegmenter)(trackmate.getSettings().segmenter)).closeSegmentationWindow();
		}		
		trackmate.getSettings().segmentationImage = null;
		trackmate.getSettings().segmenter = null;
		trackmate.getSettings().regions = null;
						
		controller.getGUI().setNextButtonEnabled(true);
		
	}

	@Override
	public void aboutToDisplayPanel() { 
		setCurrentChoiceFromPlugin();
		controller.getGUI().setNextButtonEnabled(true);
	}
		

	private void setCurrentChoiceFromPlugin() {
		String key;
		if (null != trackmate.getSettings().segmenter) {
			key = trackmate.getSettings().segmenter.getKey();
		} else {
			key = controller.getSegmentationProvider().getCurrentKey(); // back to default 
		}
		int index = controller.getSegmentationProvider().getKeys().indexOf(key);
		
		if (index < 0) {
			logPanel.getLogger().error("[SegmenterDescriptor] Cannot find segmenter named "+key+" in current plugin.");
			return;
		}
		component.setListChoice(index);
	}

}

package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;

public class SegmentationDescriptor implements WizardPanelDescriptor {
	
	public static final String KEY = "SegmentationPanel";
	protected LogPanel logger;
	protected TrackMateGUIController controller;
	protected final TrackMate trackmate;
	
	public SegmentationDescriptor(TrackMateGUIController controller) {
		this.controller = controller;
		this.logger = controller.getGUI().getLogPanel();
		this.trackmate = controller.getPlugin();
	}


	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public Component getComponent() {
		return logger;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		controller.getGUI().setNextButtonEnabled(false);
		
		// Hide Region Images if visible
		if (trackmate.getSettings().segmentationWindow!=null) {
			trackmate.getSettings().segmentationWindow.setVisible(false);
			trackmate.getSettings().segmentationWindow.close();
			trackmate.getSettings().segmentationWindow = null;
		}
		controller.hideRegionWindow();

		
		
		final Settings settings = trackmate.getSettings();
		logger.getLogger().log("Starting segmentation using "+settings.segmenter.getKey()+"\n", Logger.BLUE_COLOR);
		new Thread("SegmentationThread") {					
			public void run() {
				long start = System.currentTimeMillis();
				//try {
					trackmate.execSegmentation();
				//} catch (Exception e) {
				//	logger.getLogger().error("An error occured:\n"+e+'\n');
				//	e.printStackTrace(logger.getLogger());
				//} finally {
					controller.getGUI().setNextButtonEnabled(true);
					long end = System.currentTimeMillis();
					logger.getLogger().log(String.format("Segmentation done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				//}
			}
		}.start();
	}

	@Override
	public void aboutToHidePanel() {
		
	}
	
	
}

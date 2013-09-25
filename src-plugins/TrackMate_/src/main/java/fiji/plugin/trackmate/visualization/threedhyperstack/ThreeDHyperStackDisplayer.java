package fiji.plugin.trackmate.visualization.threedhyperstack;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.LUT;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.SpotEditTool;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import fiji.util.gui.OverlayedImageCanvas;

public class ThreeDHyperStackDisplayer extends AbstractTrackMateModelView  {

	private static final boolean DEBUG = false;
	public static final String NAME = "3D HyperStack Displayer";
	public static final String INFO_TEXT = 
			"<html>" +
			"This displayer has the normal HyperStack Displayer but also the <br>" +
			"maximum projection of the Stack.<br>" +
			"<p> " +
			"Additionally to the editing of Spots, it also alows editing of <br>" +
			"tracks. <br>" +
			/*"<p>" +
			"Double-clicking in a spot toggles the editing mode: The spot can <br> " +
			"be moved around in a XY plane by mouse dragging. To move it in Z <br>" +
			"or in time, simply change the current plane and time-point by <br>" +
			"using the hyperstack sliders. To change its radius, hold the <br>" +
			"<tt>alt</tt> key down and rotate the mouse-wheel. Holding the <br>" +
			"<tt>shift</tt> key on top changes it faster. " +
			"<p>" +
			"Alternatively, keyboard can be used to edit spots: " +
			"<ul>" +
			"	<li><b>A</b> creates a new spot under the mouse" +
			"	<li><b>D</b> deletes the spot under the mouse" +
			"	<li><b>Q</b> and <b>E</b> decreases and increases the radius of the spot " +
			"under the mouse (shift to go faster)" +
			"	<li><b>Space</b> + mouse drag moves the spot under the mouse" +
			"</ul>" +*/
			"</html>";
	protected final ImagePlus imp;
	protected ImagePlus maximumImp;
	
	protected SpotOverlay spotOverlay;
	protected MaximumSpotOverlay maximumSpotOverlay;
	protected TrackOverlay trackOverlay;
	protected MaximumTrackOverlay maximumTrackOverlay;

	private HyperstackSpotEditTool editTool;
	private Roi initialROI;
	private Roi maximumROI;
	
	
	
	
	OverlayedImageCanvas hyperstackCanvas;
	OverlayedImageCanvas maximumCanvas;
		


	/*
	 * CONSTRUCTORS
	 */

	public ThreeDHyperStackDisplayer(final Model model, final SelectionModel selectionModel, final ImagePlus imp) {	
		super(model, selectionModel);
		if (null != imp) {
			this.imp = imp;
			maximumImp = getMaximumProjection(imp);
		} else {
			this.imp = ViewUtils.makeEmpytImagePlus(model);
			maximumImp = ViewUtils.makeEmpytImagePlus(model);
		}
		this.spotOverlay = createSpotOverlay();
		this.maximumSpotOverlay = createMaximumSpotOverlay();
		this.trackOverlay = createTrackOverlay(); 
		this.maximumTrackOverlay = createMaximumTrackOverlay();
	}

	public ThreeDHyperStackDisplayer(final Model model, final SelectionModel selectionModel) {
		this(model, selectionModel, null);
	}

	
	

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return the spot overlay
	 */
	protected SpotOverlay createSpotOverlay() {
		return new SpotOverlay(model, imp, displaySettings);
	}

	protected MaximumSpotOverlay createMaximumSpotOverlay() {
		return new MaximumSpotOverlay(model, maximumImp, displaySettings);
	}
	
	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected TrackOverlay createTrackOverlay() {
		TrackOverlay to = new TrackOverlay(model, imp, displaySettings);
		TrackColorGenerator colorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
		to.setTrackColorGenerator(colorGenerator);
		return to;
	}

	protected MaximumTrackOverlay createMaximumTrackOverlay() {
		MaximumTrackOverlay to = new MaximumTrackOverlay(model, maximumImp, displaySettings);
		TrackColorGenerator colorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);
		to.setTrackColorGenerator(colorGenerator);
		return to;
	}
	

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[HyperStackDisplayer] Received model changed event ID: " 
					 + event.getEventID() +" from "+event.getSource());
		boolean redoOverlay = false;

		switch (event.getEventID()) {

		case ModelChangeEvent.MODEL_MODIFIED:
			// Rebuild track overlay only if edges were added or removed, or if at least one spot was removed. 
			final Set<DefaultWeightedEdge> edges = event.getEdges();
			if (edges != null && edges.size() > 0) {
				redoOverlay = true;				
			}
			break;
			
		case ModelChangeEvent.SPOTS_FILTERED:
			redoOverlay = true;
			break;

		case ModelChangeEvent.SPOTS_COMPUTED:
			redoOverlay = true;
			break;

		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case ModelChangeEvent.TRACKS_COMPUTED:
			redoOverlay = true;
			break;
		}

		if (redoOverlay)
			refresh();
	}
	

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		// Highlight selection
		trackOverlay.setHighlight(selectionModel.getEdgeSelection());
		maximumTrackOverlay.setHighlight(selectionModel.getEdgeSelection());
		spotOverlay.setSpotSelection(selectionModel.getSpotSelection());
		maximumSpotOverlay.setSpotSelection(selectionModel.getSpotSelection());
		// Center on last spot
		super.selectionChanged(event);
		// Redraw
		imp.updateAndDraw();	
		maximumImp.updateAndDraw();
	}
	
	@Override
	public void centerViewOn(Spot spot) {
		int frame = spot.getFeature(Spot.FRAME).intValue();
		double dz = imp.getCalibration().pixelDepth;
		long z = Math.round(spot.getFeature(Spot.POSITION_Z) / dz  ) + 1;
		imp.setPosition(1, (int) z, frame+1);
	}
	
	@Override
	public void render() {
		maximumROI = maximumImp.getRoi();
		if (maximumROI!= null) {
			maximumImp.killRoi();
		}
		
		initialROI = imp.getRoi();
		if (initialROI != null) {
			imp.killRoi();
		}

		
		
		clear();
		imp.setOpenAsHyperStack(true);
		if (!imp.isVisible()) {
			imp.show();
		}

		addOverlay(spotOverlay);
		addOverlay(trackOverlay);
		imp.updateAndDraw();
		registerEditTool();
		
		maximumImp.setOpenAsHyperStack(true);
		if (!maximumImp.isVisible()) {
			maximumImp.show();
		}

		addMaximumOverlay(maximumSpotOverlay);
		addMaximumOverlay(maximumTrackOverlay);
		maximumImp.updateAndDraw();
		//TODO registerMaximumEditTool();
	}

	@Override
	public void refresh() { 
		if (null != imp) {
			imp.updateAndDraw();
		}
		if (null != maximumImp) {
			maximumImp.updateAndDraw();
		}
	}

	@Override
	public void clear() {
		Overlay overlay = imp.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}
		overlay.clear();
		if (initialROI != null) {
			imp.getOverlay().add(initialROI);
		}
		
		overlay = maximumImp.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			maximumImp.setOverlay(overlay);
		}
		overlay.clear();
		if (maximumROI != null) {
			maximumImp.getOverlay().add(maximumROI);
		}
		
		refresh();
	}	

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String getKey() {
		return NAME;
	}

	public void addOverlay(Roi overlay) {
		imp.getOverlay().add(overlay);
	}

	public void addMaximumOverlay(Roi overlay) {
		maximumImp.getOverlay().add(overlay);
	}
	
	public SelectionModel getSelectionModel() {
		return selectionModel;
	}
	
	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = HyperstackSpotEditTool.getInstance();
		if (!SpotEditTool.isLaunched())
			editTool.run("");
		else {
			editTool.imageOpened(imp);
		}
		editTool.register(imp, this);
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		boolean dorefresh = false;
		
		if (key == TrackMateModelView.KEY_SPOT_COLORING) {
			dorefresh = true;
			
		} else if (key == TrackMateModelView.KEY_TRACK_COLORING) {
			// pass the new one to the track overlay - we ignore its spot coloring and keep the spot coloring
			TrackColorGenerator colorGenerator = (TrackColorGenerator) value;
			trackOverlay.setTrackColorGenerator(colorGenerator);
			maximumTrackOverlay.setTrackColorGenerator(colorGenerator);
			dorefresh = true;
		}
		
		super.setDisplaySettings(key, value);
		if (dorefresh) {
			refresh();
		}
	}
	
	private ImagePlus getMaximumProjection(ImagePlus image) {
		ImageStack stack = image.getStack();
		
		int width = stack.getWidth();
		int slices = stack.getSize();
		int height = stack.getHeight();

		
		int[] maxPixel = new int[width*slices];
		int max=0;
		
		LUT sliceLUT = stack.getProcessor(1).getLut();
				
		// 8 Bit Pixel
		if (image.getBitDepth() == 8) {

			for (int t = 1; t<=slices; t++) {
				
				int[][] slice = stack.getProcessor(t).getIntArray();
				
				
				for (int col = 0; col < width; col++) {
					max = 0;
					
					for (int row = 0; row<height; row++) {
						if (max<slice[col][row]){
							max = slice[col][row];
						}
					}

					
					maxPixel[col+(t-1)*width] = sliceLUT.getRGB(max);
					//maxPixel[col+(t-1)*width] = max;
				}		
			}
		}
		
		ImageStack maxStack = new ImageStack(width,slices);
		maxStack.addSlice("maxProj", maxPixel);
		
		return new ImagePlus("Maximum Projection",maxStack);
	}
	
}

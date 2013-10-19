package fiji.plugin.trackmate.visualization.orthogonalview;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.LUT;

import java.util.ArrayList;
import java.util.List;
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
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import fiji.util.gui.OverlayedImageCanvas;

public class OrthogonalView extends AbstractTrackMateModelView{

	private static final boolean DEBUG = false;
	public static final String NAME = "OrthogonalView Displayer";
	public static final String INFO_TEXT = "<html>" +
			"This displayer overlays the spots and tracks on the current <br>" +
			"ImageJ hyperstack window. <br>" +
			"Additional to that it opens an orthogonal view to view the spots <br>" +
			"and tracks in the surrounding <br>" +
			"<p> " +
			"This displayer allows manual editing of spots, thanks to the spot <br> " +
			"edit tool that appear in ImageJ toolbar." +
			"<p>" +
			"Double-clicking in a spot toggles the editing mode: The spot can <br> " +
			"be moved around in a XY plane by mouse dragging. To move it in Z <br>" +
			"or in time, simply change the current plane and time-point by <br>" +
			"using the hyperstack sliders. To change its radius, hold the <br>" +
			"<tt>alt</tt> key down and rotate the mouse-wheel. Holding the <br>" +
			"<tt>shift</tt> key on top changes it faster. " +
			"<p>" +
			"Alternatively, keyboard can be used to edit spots:<br/>" +
			" - <b>A</b> creates a new spot under the mouse.<br/>" +
			" - <b>D</b> deletes the spot under the mouse.<br/>" +
			" - <b>Q</b> and <b>E</b> decreases and increases the radius of the spot " +
			"under the mouse (shift to go faster).<br/>" +
			" - <b>Space</b> + mouse drag moves the spot under the mouse.<br/>" +
			"<p>" +
			"To toggle links between two spots, select two spots (Shift+Click), <br>" +
			"then press <b>L</b>. "
			+ "<p>"
			+ "<b>Shift+L</b> toggle the auto-linking mode on/off. <br>"
			+ "If on, every spot created will be automatically linked with the spot <br>"
			+ "currently selected, if they are in subsequent frames." +
			"</html>";
	
	protected final ImagePlus imp;
	protected ImagePlus verticalImage;
	protected ImagePlus horizontalImage;
	
	protected SpotOverlay spotOverlay;
	protected PositionOverlay verticalOverlay, horizontalOverlay, originalOverlay;
	protected List<PositionOverlay> positionOverlayList = new ArrayList<PositionOverlay>();
	
	protected TrackOverlay trackOverlay;

	//private CentralWindowSpotEditTool editTool;
	private Roi initialROI;
	private Roi verticalROI, horizontalROI;
	
	private CentralWindowSpotEditTool editTool;
	
	
	OverlayedImageCanvas hyperstackCanvas;
	OverlayedImageCanvas maximumCanvas;
		


	/*
	 * CONSTRUCTORS
	 */

	public OrthogonalView(final Model model, final SelectionModel selectionModel, final ImagePlus imp) {	
		super(model, selectionModel);
		if (null != imp) {
			this.imp = imp;
			updateOrthogonalImages(imp);
		} else {
			this.imp = ViewUtils.makeEmpytImagePlus(model);
			verticalImage = ViewUtils.makeEmpytImagePlus(model);
			horizontalImage = ViewUtils.makeEmpytImagePlus(model);
		}
				
		this.spotOverlay = createSpotOverlay();
		this.originalOverlay = createOriginalOverlay();
		this.verticalOverlay = createVerticalOverlay();
		this.horizontalOverlay = createHorizontalOverlay();

		positionOverlayList.add(originalOverlay);
		positionOverlayList.add(verticalOverlay);
		positionOverlayList.add(horizontalOverlay);
		
		this.trackOverlay = createTrackOverlay(); 
		
		
		//controller.getGuimodel().addView(mainView);
		
		/*if (SpotEditTool.isLaunched()) {
			SpotEditTool.getInstance().imageClosed(imp);
		}
		if (this.imp.isVisible()) {
			this.imp.hide();
		}*/
	}

	public OrthogonalView(final Model model, final SelectionModel selectionModel) {
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

	protected PositionOverlay createOriginalOverlay() {
		return new PositionOverlay(model, imp, displaySettings, PositionOverlay.Direction.ORIGINAL);
	}

	protected PositionOverlay createHorizontalOverlay() {
		return new PositionOverlay(model, horizontalImage, displaySettings, PositionOverlay.Direction.HORIZONTAL);
	}

	protected PositionOverlay createVerticalOverlay() {
		return new PositionOverlay(model, verticalImage, displaySettings, PositionOverlay.Direction.VERTICAL);
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
	
	
	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[OrthogonalView] Received model changed event ID: " 
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
		spotOverlay.setSpotSelection(selectionModel.getSpotSelection());
		
		// Center on last spot
		super.selectionChanged(event);
		
		
		// Redraw
		imp.updateAndDraw();	
		verticalImage.updateAndDraw();
		horizontalImage.updateAndDraw();
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
		initialROI = imp.getRoi();
		if (initialROI != null) {
			imp.killRoi();
		}
		
		verticalROI = verticalImage.getRoi();
		if (verticalROI!= null) {
			verticalImage.killRoi();
		}
		
		horizontalROI = horizontalImage.getRoi();
		if (horizontalROI!= null) {
			horizontalImage.killRoi();
		}
		
		

		
		
		clear();
		imp.setOpenAsHyperStack(true);
		if (!imp.isVisible()) {
			imp.show();
		}

		addOverlay(originalOverlay);
		addOverlay(spotOverlay);
		addOverlay(trackOverlay);
		imp.updateAndDraw();
		
		
		verticalImage.setOpenAsHyperStack(true);
		if (!verticalImage.isVisible()) {
			verticalImage.show();
		}
		
		horizontalImage.setOpenAsHyperStack(true);
		if (!horizontalImage.isVisible()) {
			horizontalImage.show();
		}

		addPositionOverlay();
		
		verticalImage.updateAndDraw();
		horizontalImage.updateAndDraw();
		
		registerEditTool();
	}




	@Override
	public void refresh() { 
		if (null != imp) {
			imp.updateAndDraw();
		}
		if (null != verticalImage) {
			verticalImage.updateAndDraw();
		}
		if (null != horizontalImage) {
			horizontalImage.updateAndDraw();
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
		//	imp.getOverlay().add(originalOverlay);
		}
		
		overlay = verticalImage.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			verticalImage.setOverlay(overlay);
		}
		overlay.clear();
		if (verticalROI != null) {
			verticalImage.getOverlay().add(verticalROI);
		}
		
		overlay = horizontalImage.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			horizontalImage.setOverlay(overlay);
		}
		overlay.clear();
		if (horizontalROI != null) {
			horizontalImage.getOverlay().add(horizontalROI);
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

	public void addPositionOverlay() {
		verticalImage.getOverlay().add(verticalOverlay);
		horizontalImage.getOverlay().add(horizontalOverlay);
	}
	
	public SelectionModel getSelectionModel() {
		return selectionModel;
	}
	
	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = CentralWindowSpotEditTool.getInstance();
		if (!CentralWindowSpotEditTool.isLaunched())
			editTool.run("");
		else {
			editTool.imageOpened(imp);
		}
		editTool.register(imp, this);
		editTool.register(verticalImage, this);
		editTool.register(horizontalImage, this);
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
			dorefresh = true;
		}
		
		super.setDisplaySettings(key, value);
		if (dorefresh) {
			refresh();
		}
	}
	
	
	
	private void updateOrthogonalImages(ImagePlus image) {
		
		ImageStack stack = image.getStack();
		
		int width = stack.getWidth();
		int slices = stack.getSize();
		int height = stack.getHeight();

		int[][] verticalPixel = new int[height][width*slices];
		int[][] horizontalPixel = new int[width][height*slices];
		
		LUT sliceLUT = stack.getProcessor(1).getLut();
				
		// 8 Bit Pixel
		if (image.getBitDepth() == 8) {

			for (int t = 1; t<=slices; t++) {
				
				int[][] slice = stack.getProcessor(t).getIntArray();
				
				for (int col = 0; col < width; col++) {
					for (int row = 0; row<height; row++) {
						verticalPixel[row][col+(t-1)*width] = sliceLUT.getRGB(slice[col][row]);
						horizontalPixel[col][(t-1)+row*slices] = sliceLUT.getRGB(slice[col][row]);
					}
				}		
			}
		}
		
		ImageStack verticalStack = new ImageStack(width,slices);
		ImageStack horizontalStack = new ImageStack(slices,height);
		
		for (int i=0; i<height; i++) {
			verticalStack.addSlice("VerticalImage "+i, verticalPixel[i]);
		}
		for (int i=0; i<width; i++) {
			horizontalStack.addSlice("HorizontalImage "+i, horizontalPixel[i]);
		}
		
		verticalImage = new ImagePlus("Vertical Image Stack", verticalStack);
		horizontalImage = new ImagePlus("Horizontal Image Stack", horizontalStack);
		
	}
	
	public void setOverlayPosition(double x, double y, double z) {
		for (PositionOverlay po: positionOverlayList) {
			po.setPosition((int)x, (int)y, (int)z);
		}
		
		if (DEBUG) {			
			System.out.println("verticalOverlay: "+verticalOverlay.focusX+", "+verticalOverlay.focusY+", "+verticalOverlay.focusZ);
			System.out.println("horizontalOverlay: "+horizontalOverlay.focusX+", "+horizontalOverlay.focusY+", "+horizontalOverlay.focusZ);
			System.out.println("originalOverlay: "+originalOverlay.focusX+", "+originalOverlay.focusY+", "+originalOverlay.focusZ);
		}
		
		updatePosition();
	}
	
	public void updatePosition() {
		editTool.update = false;
		for (PositionOverlay po: positionOverlayList) {
			po.update();
		}
		editTool.update = true;
	}
	
	public void setSlice(double z) {
		
	}

}

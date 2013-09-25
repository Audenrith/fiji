package fiji.plugin.trackmate.gui.descriptors;

import static fiji.plugin.trackmate.gui.TrackMateWizard.TRACKMATE_ICON;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import ij.process.LUT;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;



import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.RegionCombinationOptionPanel;
import fiji.plugin.trackmate.gui.panels.components.NumberOverlay;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.segmentation.Segmenter;
import fiji.plugin.trackmate.util.ExportAsXLSX;
import fiji.util.gui.OverlayedImageCanvas;

public class RegionSelectDescriptor implements WizardPanelDescriptor {

	public static final String IN_STRING = "Inside";
	public static final String IN_INDEX = "i";
	public static final String BACKGROUND_STRING = "Background";
	public static final String BACKGROUND_INDEX = "b";
	public static final String INTERSECTION_STRING = "Intersection";
	public static final String INTERSECTION_INDEX = "s";
	
	public static final String[] REGION_STRINGS = {IN_STRING, BACKGROUND_STRING, INTERSECTION_STRING};
	public static final String[] REGION_INDEXES = {IN_INDEX, BACKGROUND_INDEX, INTERSECTION_INDEX};
	
	public static final String KEY = "RegionSelectPanel";
	
	private static final String SAVEREGIONIMAGE = "Save region image";
	private static final String SAVEREGIONMATRIX = "Save matrix with regionnumbers";

	private RegionCombinationOptionPanel component;
	private StackWindow regionWindow;
	private OverlayedImageCanvas numberCanvas;
	private NumberOverlay numberOverlay;
	private ImagePlus displayRegionImage;
	private TrackMateGUIController controller;
	private List<String> options;
	private ImagePlus maskedImage;
	
	protected final LogPanel logPanel;
	protected final TrackMate trackmate;
	protected Settings settings;
	

	public RegionSelectDescriptor(TrackMateGUIController controller) {
		this.controller = controller;
		this.logPanel = controller.getGUI().getLogPanel();
		this.trackmate = controller.getPlugin();
		this.settings = trackmate.getSettings();
		
		options = new ArrayList<String>();
		options.add(SAVEREGIONIMAGE);
		options.add(SAVEREGIONMATRIX);
	}




	private void refresh() {	
		if (displayRegionImage == null) {			
			displayRegionImage = copyOfRegionImage();
		}
		
		
		if (null != displayRegionImage) {
						
			ImageStack stack = displayRegionImage.getStack();			
			int width = stack.getWidth();
			int height = stack.getHeight();	
						
			ImageStack overlayStack = new ImageStack(width,height);
			
			// TODO konfigure LUT? 
			double[] maskPixel = equalizeStack(stack);			
			int[] bytePixel = null;
			LUT sliceLUT = settings.imp.getStack().getProcessor(1).getLut();
						
			// Overlay Images
			bytePixel = new int[maskPixel.length];			
			for (int t=0; t<maskPixel.length; t++) {
				bytePixel[t] = sliceLUT.getRGB((byte)maskPixel[t]);
			}
			
			overlayStack.addSlice("Mask", bytePixel);
			
			maskedImage =new ImagePlus("Mask Image",overlayStack);	
			
			maskedImage.setOpenAsHyperStack(true);
			
			if (regionWindow != null) {
				System.out.println(regionWindow.toString());
				regionWindow.setVisible(false);
				regionWindow.close();
			}
			
			if (numberOverlay== null) {
				numberOverlay = createNumberOverlay(settings.regionImage);
			}
			numberCanvas = new OverlayedImageCanvas(maskedImage);
			regionWindow = new StackWindow(maskedImage, numberCanvas);
			regionWindow.setVisible(true);
			numberCanvas.addOverlay(numberOverlay);
			maskedImage.updateAndDraw();
			
			int[] pixels = (int[])settings.regionImage.getImageStack().getPixels(1);

			List<String> regions = new ArrayList<String>();
			// TODO add i/o/s to Region Name
			
			for (int i=0; i<pixels.length; i++) {
				if (!regions.contains(""+pixels[i])) {
					regions.add(""+pixels[i]);
				}
			}
						
			Map<String, List<Integer>> finalRegions = settings.regions;

			// Create List of Target Areas
			List<String> regionNames = new ArrayList<String>();
			for (int i=0; i<REGION_STRINGS.length; i++) {
				if (finalRegions.containsKey(REGION_STRINGS[i])) {
					regionNames.add(REGION_STRINGS[i]+" ("+finalRegions.get(REGION_STRINGS[i])+")");
					if (regions.contains(""+finalRegions.get(REGION_STRINGS[i]))) {
						regions.remove(""+finalRegions.get(REGION_STRINGS[i]));
					}
				} else {
					regionNames.add(REGION_STRINGS[i]);
				}
			}
			
			component = new RegionCombinationOptionPanel(regionNames.get(0), regionNames, regions, "Combine Regions", options);
			component.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					combineRegions();
				}
			});
			
		}
		
		
		
	}
	
	private void combineRegions() {
		
		// Cast Regions(Y) to Int
		String[] yFeatureList = component.getYKeys().toArray(new String[0]);
		List<Integer> yFeatures = new ArrayList<Integer>(yFeatureList.length);

		String targetString = component.getXKey().split(" ")[0];
		Map<String, List<Integer>> finalRegions = settings.regions;
		
		// Create List<Integer> of Regions to add to targetRegion
		logPanel.getLogger().log("Add Regions to "+targetString+":\n");
		System.out.println("Add Regions to "+targetString+":\n");
		for (int i=0; i<yFeatureList.length; i++) {
			Integer yValue =Integer.parseInt(yFeatureList[i].split(" ")[0]);
			yFeatures.add(yValue);
			logPanel.getLogger().log("     Region "+yFeatureList[i]+"\n");
			System.out.println("     Region "+yFeatureList[i]+"\n");

			// Test if yFeatures are in a Region. If so, then remove them
			for (int j=0; j<REGION_STRINGS.length; j++) {
				if (finalRegions.containsKey(REGION_STRINGS[j]) && finalRegions.get(REGION_STRINGS[j]).contains(yValue)) {
					finalRegions.get(REGION_STRINGS[j]).remove(yValue);
				}
			}
			
		}

		// Add yFeatures to the Region
		List<Integer> region = yFeatures;
		if (finalRegions.containsKey(targetString)) {
			region = finalRegions.get(targetString);
			region.addAll(yFeatures);
		} else {
			finalRegions.put(targetString, yFeatures);
		}
		
		
		displayRegionImage = copyOfRegionImage();	
		
		int[] pixels = (int[])displayRegionImage.getImageStack().getPixels(1);
		
		// Combine Regions
		for (int i=0; i<pixels.length; i++) {
			
			for (int j=0; j<REGION_STRINGS.length; j++) {
				if (finalRegions.containsKey(REGION_STRINGS[j]) && 
						finalRegions.get(REGION_STRINGS[j])!=null && 
						finalRegions.get(REGION_STRINGS[j]).contains(pixels[i])) {
					pixels[i] = finalRegions.get(REGION_STRINGS[j]).get(0);
					break;
				} 
			}
			
		}
		
		
		
		
		// Refresh Image
		ImageStack segStack = new ImageStack(displayRegionImage.getWidth(),displayRegionImage.getHeight());
		segStack.addSlice("segmImg", pixels);
		
		displayRegionImage = new ImagePlus("SegmentedImage", segStack);
		
		// Refresh Panel
		numberOverlay.update(finalRegions);
		refresh();
		
		controller.getGUI().show(this);
		
	}
	
	private NumberOverlay createNumberOverlay(ImagePlus regionImage) {
		return new NumberOverlay(logPanel, regionImage);
	}
	
	
	
	
	
	
	private double[] equalizeStack(ImageStack stack) {
		
		Object pixelsChr = stack.getPixels(1);
		
		double[] maskPixel = null;
		
		if (pixelsChr instanceof byte[]) {
			byte[] dataChr = (byte[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
			
		} else if (pixelsChr instanceof short[]) {
			short[] dataChr = (short[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof int[]) {
			int[] dataChr = (int[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof float[]) {
			float[] dataChr = (float[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else {
			JFrame jf = new JFrame();
			jf.setSize(300,400);
			jf.add(new JLabel("unsuported data type"));
			jf.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			jf.setVisible(true);
			throw new IllegalArgumentException("Unsuported data type");
		}
		
		
		
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (int t=0; t<maskPixel.length; t++) {
			if (maskPixel[t]>max) max = maskPixel[t];
			if (maskPixel[t]<min) min = maskPixel[t];
		}
		
		for (int t=0; t<maskPixel.length; t++) {
			maskPixel[t] = (maskPixel[t]-min)*255/(max-min);
		}
		
		
		return maskPixel;
	}
	
	private ImagePlus copyOfRegionImage() {
		
		if (settings.regionImage== null) {
			return null;
		}
		
		ImageStack stack = settings.regionImage.getStack();			
		int width = stack.getWidth();
		int height = stack.getHeight();	
					
		ImageStack overlayStack = new ImageStack(width,height);
		
		int[] pixels = (int[]) stack.getPixels(1);
		int[] newPixels = new int[pixels.length];
		for (int i=0; i<pixels.length; i++) {
			newPixels[i] = pixels[i];
		}
		
		overlayStack.addSlice("CopyOfRegionImage", newPixels);
		
		return new ImagePlus("Copy of regionImage", overlayStack);
		
	}

	

	/*
	 * WIZARDPANELDESCRIPTOR METHODS	
	 */
	
	@Override
	public String getKey() {
		return KEY;
	}


	@Override
	public Component getComponent() {
		return component;
	}


	@Override
	public void aboutToDisplayPanel() { 

		// create Masked Image and update MaskImage
		Segmenter activeSegmenter = settings.segmenter;
		displayRegionImage = null;		
		
		ImagePlus regionImage = activeSegmenter.getResult();
		settings.regionImage = regionImage;
		ImagePlus overlayImage = overlay(settings.imp, regionImage);
		
		regionImage.setOpenAsHyperStack(true);
				
		settings.segmentationWindow = new StackWindow(overlayImage);
		settings.segmentationWindow.setVisible(true);
		regionImage.updateAndDraw();
						
		
		refresh();
		
		// Hide Segmentation Window if Manualy Segmented
		if (trackmate.getSettings().segmenter != null &&
				trackmate.getSettings().segmenter.getKey().equals(ManualSegmenter.SEGMENTER_KEY)) {
			((ManualSegmenter)(trackmate.getSettings().segmenter)).hideSegmentationWindow();
		}		
		
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void displayingPanel() {
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		boolean[] choose = component.getOptions();
		
		
		for (int i=0; i<choose.length; i++) {
			if (choose[i] && options.get(i).equals(SAVEREGIONIMAGE)) {
				// Save RegionImage
				BufferedImage bi = maskedImage.getBufferedImage();
				numberOverlay.paint(bi.getGraphics(), 0, 0, 1);
				ImagePlus saveImage = new ImagePlus();
				saveImage.setImage(bi);
				
				trackmate.saveImage(controller, saveImage, "Save Region Image");
			} else if (choose[i] && options.get(i).equals(SAVEREGIONMATRIX)){
				// SAVE MATRIX OF REGIONS
				File file;
				JFileChooser fileChooser = new JFileChooser() {
					private static final long serialVersionUID = 1L;
					@Override
				    protected JDialog createDialog( Component parent ) throws HeadlessException {
				        JDialog dialog = super.createDialog( parent );
				        dialog.setIconImage( TRACKMATE_ICON.getImage() );
				        return dialog;
				    }
				};
				fileChooser.setName("RegionImageXLSX");
				fileChooser.setSelectedFile(new File(this.settings.imageFolder));
				FileNameExtensionFilter filter = new FileNameExtensionFilter("RegionImageXLSX", "xlsx");
				fileChooser.setFileFilter(filter);

				int returnVal = fileChooser.showSaveDialog(controller.getGUI());
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					file = fileChooser.getSelectedFile();
					ExportAsXLSX.exportImageAsXlsx(copyOfRegionImage(), file);
				} else {
					controller.getGUI().getLogger().log("Export Regionimage aborted.\n");	
				}
				
				
			} else if (choose[i]){
				throw new IllegalArgumentException();
			}
			
		}
		
		
		// Hide SegmentationImage if visible
		if (trackmate.getSettings().segmentationWindow!=null) {
			trackmate.getSettings().segmentationWindow.setVisible(false);
			trackmate.getSettings().segmentationWindow.close();
			trackmate.getSettings().segmentationWindow = null;
		}
	}
	
	private ImagePlus overlay(ImagePlus img, ImagePlus mask) {
		//TODO Abfrage bzgl. Datentyp der Images
		//TODO Abfrage ob Maske und Bild gleich groß
		
		ImageStack stack = img.getStack();
		ImageStack maskStack = mask.getStack();
		
		int width = stack.getWidth();
		int height = stack.getHeight();
		int stackSize = stack.getSize();
		

		ImageStack overlayStack = new ImageStack(width,height);
		
		int imageType = getTypeOf(stack);
		

		double[] pixel = null;
		double[] averagePixel = getDoubleOfPixels(stack,1,imageType);
				
		
		// Average Image
		for (int i=2; i<stackSize; i++) {
			pixel = getDoubleOfPixels(stack, i, imageType);
			for (int t=0; t<pixel.length; t++) {
				averagePixel[t]+=pixel[t];
			}
		}
		
		
		
		// Equalize Mask
		double[] maskPixel = edgeImage(maskStack);
		
		
		int[] bytePixel = null;
		LUT sliceLUT = stack.getProcessor(1).getLut();
						
		
		
		// Overlay Images
		bytePixel = new int[averagePixel.length];
		
		for (int t=0; t<averagePixel.length; t++) {
			averagePixel[t] = (3*(averagePixel[t]/stackSize) + maskPixel[t]) / 4;
			bytePixel[t] = sliceLUT.getRGB((byte)averagePixel[t]);
		}
		
		overlayStack.addSlice("Overlay", bytePixel);
			
		ImagePlus maskedImage =new ImagePlus("Masked Image",overlayStack);		
		trackmate.getModel().getLogger().log("  - Masked Image "+maskedImage.toString()+"\n");
		return maskedImage;
	}
	
	private double[] edgeImage(ImageStack stack) {
		
		Object pixelsChr = stack.getPixels(1);
		
		double[] maskPixel = null;
		
		if (pixelsChr instanceof byte[]) {
			byte[] dataChr = (byte[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
			
		} else if (pixelsChr instanceof short[]) {
			short[] dataChr = (short[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof int[]) {
			int[] dataChr = (int[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else if (pixelsChr instanceof float[]) {
			float[] dataChr = (float[])pixelsChr;
			
			maskPixel = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				maskPixel[t] = dataChr[t];
			}
		} else {
			throw new IllegalArgumentException("Unsuported data type");
		}
		
		
		
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (int t=0; t<maskPixel.length; t++) {
			if (maskPixel[t]>max) max = maskPixel[t];
			if (maskPixel[t]<min) min = maskPixel[t];
		}
		
		for (int t=0; t<maskPixel.length; t++) {
			maskPixel[t] = (maskPixel[t]-min)*255/(max-min);
		}
		
		// Kantenbild
		int width = stack.getWidth();
		int height = stack.getHeight();
		
		double[] edgeMask = new double[maskPixel.length];
		
		for (int i=0; i<width; i++) {
			edgeMask[i] = 0;
			edgeMask[height*width-1-i] = 0;
		}
		for (int i=0; i<height; i++) {
			edgeMask[i*width] = 0;
			edgeMask[i*(width-1)] = 0;
		}
		
		for (int col = 1; col < width-1; col++) {
			
			for (int row = 1; row<height-1; row++) {
				
				// Sobel
				edgeMask[col+row*width] = 
						Math.abs(maskPixel[col-1+(row-1)*width] + 2*maskPixel[col-1+row*width] + maskPixel[col-1+(row+1)*width]
					  - maskPixel[col+1+(row-1)*width] - 2*maskPixel[col+1+row*width] - maskPixel[col+1+(row+1)*width]) +
					  
					  	Math.abs(maskPixel[col-1+(row-1)*width] + 2*maskPixel[col+(row-1)*width] + maskPixel[col+1+(row-1)*width]
					  - maskPixel[col-1+(row+1)*width] - 2*maskPixel[col+(row+1)*width] - maskPixel[col+1+(row+1)*width]);
				
				if (edgeMask[col+row*width]>0) {
					edgeMask[col+row*width] = 255;
				}
				
			}

		}	
		
		return edgeMask;
	}
	
	private double[] getDoubleOfPixels(ImageStack stack, int i, int imageType) {
		
		Object pixels = stack.getPixels(i);
		
		double[] doublePixel = null;
							
		switch (imageType) {
			case 0:
				byte[] byteData = (byte[])pixels;
				
				doublePixel = new double[byteData.length];
				
				for (int t=0; t<byteData.length; t++) {
					if (byteData[t]>0) {
						doublePixel[t] = byteData[t];
					} else {
						doublePixel[t] = 256+byteData[t];
					}
				}
				break;
			case 1:
				short[] shortData = (short[])pixels;
				
				doublePixel = new double[shortData.length];
				
				for (int t=0; t<shortData.length; t++) {
					doublePixel[t] = shortData[t];
				}
				break;
			case 2:
				int[] intData = (int[])pixels;
				
				doublePixel = new double[intData.length];
				
				for (int t=0; t<intData.length; t++) {
					doublePixel[t] = intData[t];
				}
				break;
			case 3:
				float[] floatData = (float[])pixels;
				
				doublePixel = new double[floatData.length];
				
				for (int t=0; t<floatData.length; t++) {
					doublePixel[t] = floatData[t];
				}
				break;
			default:
				throw new IllegalArgumentException("Unsuproted data type");
		}
		return doublePixel;
				
	}

	private int getTypeOf(ImageStack stack) {
		// TEST DATA-TYPE OF IMAGE
		Object pixelsTest = stack.getPixels(1);
		
		double[] imgChr = null;
		int type = -1;
		
		if (pixelsTest instanceof byte[]) {
			type = 0;
			
			byte[] dataChr = (byte[])pixelsTest;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
			
		} else if (pixelsTest instanceof short[]) {
			type = 1;
			
			short[] dataChr = (short[])pixelsTest;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else if (pixelsTest instanceof int[]) {
			type = 2;
			
			int[] dataChr = (int[])pixelsTest;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else if (pixelsTest instanceof float[]) {
			type = 3;
			
			float[] dataChr = (float[])pixelsTest;
			
			imgChr = new double[dataChr.length];
			
			for (int t=0; t<dataChr.length; t++) {
				imgChr[t] = dataChr[t];
			}
		} else {
			throw new IllegalArgumentException("Unsuported data type");
		}
		return type;
	}
	
	public void hideRegionImage() {
		if (regionWindow != null) {
			regionWindow.setVisible(false);
			regionWindow.close();
			regionWindow = null;
		}
	}
	
}

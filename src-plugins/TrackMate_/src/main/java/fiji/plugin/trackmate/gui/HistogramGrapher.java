package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.TRACK_SCHEME_ICON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.util.ExportableChartPanel;

public class HistogramGrapher {
	
	protected final InterpolatePaintScale paints = InterpolatePaintScale.Jet; 
	protected final Settings settings;
	protected final Model model;
	private final String[] histogramKeys;
	private final int[] regionImage;
	private final int imageWidth;
	private Logger logger;
		
	public HistogramGrapher(final TrackMate trackmate, final String[] histogramKeys) {
		this.settings = trackmate.getSettings();
		this.model = trackmate.getModel();
		this.histogramKeys = histogramKeys;
		
		this.logger = model.getLogger();		
		
		
		regionImage = (int[]) settings.regionImage.getStack().getPixels(1);
		imageWidth = settings.regionImage.getWidth();
		
	}
	
	/**
	 * Draw and render the graph.
	 */
	public void render() {

		// X label
		String xAxisLabel = "Track Length in "+model.getTimeUnits();

		// Y label
		String yAxisLabel = "Number of Tracks";
		
		// Title
		String title = "Histogram of Track Length";
		
		// Generate panels for every Histogram
		Map<String, List<Integer[]>> histograms = settings.histograms;
		ArrayList<ExportableChartPanel> chartPanels = new ArrayList<ExportableChartPanel>();
		
		
		logger.setProgress(0.05);
		
		// Create Combined Histogram
		HistogramDataset combinedDataset = combinedHistogram();
		
		// The chart
		JFreeChart combinedChart = ChartFactory.createHistogram(title, xAxisLabel, yAxisLabel, 
				combinedDataset, PlotOrientation.VERTICAL, true, true, false);
		combinedChart.getTitle().setFont(FONT);
		combinedChart.getLegend().setItemFont(SMALL_FONT);
		
				
		// The plot
		XYPlot combinedPlot = combinedChart.getXYPlot();
		combinedPlot.getRangeAxis().setLabelFont(FONT);
		combinedPlot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		combinedPlot.getDomainAxis().setLabelFont(FONT);
		combinedPlot.getDomainAxis().setTickLabelFont(SMALL_FONT);

		
		// The panel
		ExportableChartPanel combinedChartPanel = new ExportableChartPanel(combinedChart);
		combinedChartPanel.setPreferredSize(new java.awt.Dimension(500, 540));
		chartPanels.add(combinedChartPanel);
		
		
		// Create individual Histograms
		for (int i=0; i<histogramKeys.length; i++) {
			// Build Histogram
			HistogramDataset dataset = buildHistogram(histogramKeys[i], histograms.get(histogramKeys[i]));
			logger.setProgress(0.3+0.6*i/histogramKeys.length);
			
			if (dataset != null) {
								
				// The chart
				JFreeChart chart = ChartFactory.createHistogram(title+": "+histogramKeys[i], xAxisLabel, yAxisLabel, 
						dataset, PlotOrientation.VERTICAL, true, true, false);
				chart.getTitle().setFont(FONT);
				chart.getLegend().setItemFont(SMALL_FONT);
				
		
				// The plot
				XYPlot plot = chart.getXYPlot();
				plot.getRangeAxis().setLabelFont(FONT);
				plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
				plot.getDomainAxis().setLabelFont(FONT);
				plot.getDomainAxis().setTickLabelFont(SMALL_FONT);
		
				// The panel
				ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
				chartPanel.setPreferredSize(new java.awt.Dimension(500, 540));
				chartPanels.add(chartPanel);
			} else {
				logger.log("No track for: "+histogramKeys[i]+"\n");
			}
		}
		
		
		
		
		
		
		// The Panel
		JPanel panel = new JPanel();
		BoxLayout panelLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(panelLayout);
		for(ExportableChartPanel cp : chartPanels)  {
			panel.add(cp);
			panel.add(Box.createVerticalStrut(5));
		}
		
		// Scroll pane
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(panel);

		// The frame
		JFrame frame = new JFrame();
		frame.setTitle("Histogram");
		frame.setIconImage(TRACK_SCHEME_ICON.getImage());
		frame.getContentPane().add(scrollPane);
		frame.validate();
		frame.setSize(new java.awt.Dimension(520, 320));
		frame.setVisible(true);
		
	}



	/**
	 * @return a new dataset that contains the histogram
	 */
	private HistogramDataset buildHistogram(String histogramKey, List<Integer[]> regionCommands) {
		
		// Calculating Histogram
		Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
		final FeatureModel fm = model.getFeatureModel();
		
		int maxHistogramValue = 0;
		double[] histogramData = new double[trackIDs.size()];
		int counter = 0;
		
		double frameInterval = settings.imp.getCalibration().frameInterval;
				
		// Considre all Tracks 
		if (regionCommands==null) {
			for (Integer trackID : trackIDs) {
				
				//Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);
	
				Double spotNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPOTS);
				Double gapNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_GAPS);
				if (null == spotNumber || null == gapNumber) {
					continue;
				} 
				
				Double length = (spotNumber + gapNumber)*frameInterval;//*model.getSettings().dt/1000000; 
				
				if (length>maxHistogramValue) {
					maxHistogramValue= length.intValue();
				}
				
				histogramData[counter] = length;
				counter++;
			}
		} else {

			List<Double> tempList = new ArrayList<Double>();
			
			// Filter Tracks by regionCommand
			for (Integer trackID : trackIDs) {
								
				Set<Spot> track = model.getTrackModel().trackSpots(trackID);				
				Spot[] spots = track.toArray(new Spot[0]);
												
				double startX = spots[0].getFeature(Spot.POSITION_X);
				double startY = spots[0].getFeature(Spot.POSITION_Y);
				
				double endX = spots[spots.length-1].getFeature(Spot.POSITION_X);
				double endY = spots[spots.length-1].getFeature(Spot.POSITION_Y);
				
				int[] change = {regionImage[(int) (startY)*imageWidth+(int)(startX)], regionImage[(int) (endY)*imageWidth+(int)(endX)]};
								
				for (int k = 0; k<regionCommands.size(); k++) {
					if (regionCommands.get(k)!=null && regionCommands.get(k)[0]!=null && regionCommands.get(k)[1]!=null &&
							regionCommands.get(k)[0] == change[0] && regionCommands.get(k)[1] == change[1]) {
						Double spotNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPOTS);
						Double gapNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_GAPS);
						if (null == spotNumber || null == gapNumber) {
							continue;
						} 
						
						Double length = (spotNumber + gapNumber)*frameInterval;
						
						if (length>maxHistogramValue) {
							maxHistogramValue= length.intValue();
						}
												
						tempList.add(length);
						counter++;
						break;
					}
				}	
			}
			
			histogramData = new double[counter];
			for (int i=0; i<counter; i++) {
				histogramData[i] = tempList.get(i);
			}
		}
		
		if (histogramData.length == 0) {
			return null;
		}

		
		// Creating Dataset
		HistogramDataset dataset = new HistogramDataset();	
		dataset.setType(HistogramType.FREQUENCY);
		dataset.addSeries(histogramKey, histogramData, maxHistogramValue);
		return dataset;
	}

	/**
	 * @return a new dataset that contains the histogram
	 */
	private HistogramDataset combinedHistogram() {
		
		logger.log("Start Combined Histogram\n");
		
		// Calculating Histogram
		Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
		final FeatureModel fm = model.getFeatureModel();
		
		double frameInterval = settings.imp.getCalibration().frameInterval;

		Map<String, List<Integer[]>> histograms = settings.histograms;
		
		//logger.log("Creating Temp List\n");
		
		int maxHistogramValue = 0;
		ArrayList<ArrayList<Double>> tempList = new ArrayList<ArrayList<Double>>();
		
		for (int i=0; i<histogramKeys.length; i++) {
			if (histograms.get(histogramKeys[i])!=null) {
				tempList.add(new ArrayList<Double>());
			}
		}
		
		String[] tempHistogramKeys = new String[tempList.size()];
		
		int counter=0; 
		for (int i=0; i<histogramKeys.length; i++) {
			if (histograms.get(histogramKeys[i])!=null) {
				tempHistogramKeys[counter] = histogramKeys[i];
				counter++;
			}
		}
		//logger.log("Start Filtering Tracks\n");
			
		// TrackCounter for calculating progress 
		int trackCounter = 0;

		try{
		// Filter Tracks by regionCommand
		for (Integer trackID : trackIDs) {
			trackCounter++;
			logger.setProgress(0.05+0.20*trackCounter/trackIDs.size());
			
			Set<Spot> track = model.getTrackModel().trackSpots(trackID);			
			if (track!= null) { 
				
				
				Spot[] spots = track.toArray(new Spot[0]);
												
				double startX = spots[0].getFeature(Spot.POSITION_X);
				double startY = spots[0].getFeature(Spot.POSITION_Y);
				
				double endX = spots[spots.length-1].getFeature(Spot.POSITION_X);
				double endY = spots[spots.length-1].getFeature(Spot.POSITION_Y);
				
				int[] change = {regionImage[(int) (startY)*imageWidth+(int)(startX)], regionImage[(int) (endY)*imageWidth+(int)(endX)]};
				
				for (int i=0; i<tempHistogramKeys.length; i++) {
					List<Integer[]>regionCommands = null; 
					if (histograms.containsKey(tempHistogramKeys[i])) {
						regionCommands = histograms.get(tempHistogramKeys[i]);
					} 
					
					if (regionCommands!=null) {
						for (int k = 0; k<regionCommands.size(); k++) {
														
							
							if (regionCommands.get(k)!=null && regionCommands.get(k)[0]!=null && regionCommands.get(k)[1]!=null &&
									regionCommands.get(k)[0] == change[0] && regionCommands.get(k)[1] == change[1]) {
								Double spotNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPOTS);
								Double gapNumber = fm.getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_GAPS);
								if (null == spotNumber || null == gapNumber) {
									continue;
								} 
								
								Double length = (spotNumber + gapNumber)*frameInterval+0.05*frameInterval*k;
								
								if (length>maxHistogramValue) {
									maxHistogramValue= length.intValue();
								}
														
								tempList.get(i).add(length);
								break;
							}
						}	
					}
					
					
					
					
				}
			}
		}
		}catch(Exception e) {
			logger.error("Exception TrackID:\n");
			logger.error(e.getMessage()+"\n");
			for (int l=0; l<e.getStackTrace().length; l++) {
				logger.error(e.getStackTrace()[l].getFileName()+", "+e.getStackTrace()[l].getClassName()+": "+
							e.getStackTrace()[l].getMethodName()+"("+e.getStackTrace()[l].getLineNumber()+")\n");
			}
		}
		
		//logger.log("Finished Filter Tracks\n");

		// Creating Dataset
		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.FREQUENCY);
		
	//	logger.log("Rewriting Histograms\n");
		
		try{
		for (int i=0; i<tempHistogramKeys.length; i++) {
			if (histograms.get(tempHistogramKeys[i])!=null) {
				double[] histogramData = new double[tempList.get(i).size()];
				for (int k=0; k<histogramData.length; k++) {
					histogramData[k] = tempList.get(i).get(k);
				}
				
				if (histogramData.length != 0) {
					dataset.addSeries(tempHistogramKeys[i], histogramData, maxHistogramValue);
				}
				logger.setProgress(0.25+0.5*i/tempHistogramKeys.length);
				//logger.log(tempHistogramKeys[i]+": "+histogramData.length+" Tracks\n");
			}
		}
		}catch(Exception e) {
			logger.error("Exception Histogram Keys:\n");
			logger.error(e.getMessage()+"\n");
			logger.error(e.getLocalizedMessage()+"\n");
			for (int l=0; l<e.getStackTrace().length; l++) {
				logger.error(e.getStackTrace()[l].getFileName()+", "+e.getStackTrace()[l].getClassName()+": "+
							e.getStackTrace()[l].getMethodName()+"("+e.getStackTrace()[l].getLineNumber()+")\n");
			}
		}
		
		//logger.log("Finished Rewriting Histograms\n");
		logger.log("Finished combined Histogram\n");
		
		return dataset;
	}
}
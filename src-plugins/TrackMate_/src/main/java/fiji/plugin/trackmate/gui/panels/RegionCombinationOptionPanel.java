package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class RegionCombinationOptionPanel extends ActionListenablePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -278038714641290344L;
	private RegionCombinationPanel rcp;
	private JPanel optionPanel;
	private JLabel titleLabel;
	private List<JCheckBox> checkBoxOptions;
	private List<String> options;
	private String title; 
		
	public RegionCombinationOptionPanel(String xKey, List<String> regionNames, List<String> regions, String title, List<String> options) {
		rcp = new RegionCombinationPanel(xKey, regionNames, regions);
		
		this.title = title;
		this.options = options;
		this.checkBoxOptions = new ArrayList<JCheckBox>();
		
		initGui();
	}
	
	
	/*
	 * PUBLIC METHODS 
	 */
	
	public boolean[] getOptions() {
		boolean[] choices = new boolean[options.size()];
		
		for (int i=0; i<choices.length; i++) {
			choices[i] = checkBoxOptions.get(i).isSelected();
		}
		return choices;
	}
	
	
	
	
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGui() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		optionPanel = new JPanel(new GridLayout(options.size()+1,1));
		
		{
			// Title
			titleLabel = new JLabel();
			titleLabel.setFont(BIG_FONT);
			titleLabel.setText(title);
			optionPanel.add(titleLabel);			
		}
		{
			// Options
			for (String s : options) {
				JCheckBox jcb = new JCheckBox();
				jcb.setText(s);
				optionPanel.add(jcb);
				checkBoxOptions.add(jcb);
			}
		}
		{
			this.add(optionPanel);
			this.add(rcp);
		}
	}
	
	
	
	
	
	/*
	 * RegionCombinationPanel Methods
	 */

	/**
	 * Return the enum constant selected in the X combo-box feature.
	 */
	public String getXKey() {
		return rcp.getXKey();
	}

	/**
	 * Return a set of the keys selected in the Y feature panel. Since we
	 * use a {@link Set}, duplicates are trimmed.
	 */
	public Set<String> getYKeys() {
		return rcp.getYKeys();
	}
	
	/*
	 * Forwarding Methods
	 * 
	 */
	
	@Override
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
		rcp.addActionListener(listener);
	}
	
	/**
	 * Removes an ActionListener from this panel. 
	 * @return true if the listener was in the ActionListener collection of this instance.
	 */
	@Override
	public boolean removeActionListener(ActionListener listener) {
		return actionListeners.remove(listener) && rcp.removeActionListener(listener);
	}
	
	@Override
	public Collection<ActionListener> getActionListeners() {
		return actionListeners;
	}
	

	/** 
	 * Forwards the given {@link ActionEvent} to all the {@link ActionListener} of this panel.
	 */
	@Override
	protected void fireAction(ActionEvent e) {
		for (ActionListener l : actionListeners)
			l.actionPerformed(e);
		rcp.fireAction(e);
	}
	
	
	
	
	
}

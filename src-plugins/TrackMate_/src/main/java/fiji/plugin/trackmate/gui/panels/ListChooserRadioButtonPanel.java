package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

public class ListChooserRadioButtonPanel extends ActionListenablePanel {

	

	/**
	 * 
	 */
	private static final long serialVersionUID = -162350466878669387L;
	private ListChooserPanel lcp;
	protected JLabel jLabelHeader;
	private JPanel radioButtonPanel;
	private ButtonGroup radioButtonGroup;
	private List<JRadioButton> radioButtons;
	private List<String> radioButtonOptions;
	private String headline;
			
	public ListChooserRadioButtonPanel(List<String> items, List<String> infoTexts, String typeName, String headline, List<String> radioButtonOptions) {
		this.lcp = new ListChooserPanel(items, infoTexts, typeName);
		this.radioButtonOptions = radioButtonOptions;
		this.headline = headline;
		
		radioButtons = new ArrayList<JRadioButton>(radioButtonOptions.size());
		
		initGui();
	}
	
	
	/*
	 * PUBLIC METHODS
	 */

	public int getListChoice() {
		return lcp.getChoice();
	}

	public void setListChoice(int index) {
		lcp.setChoice(index);
	}
	
	public int getRadioButtonChoice() {
		for (int i=0; i<radioButtons.size(); i++) {
			if (radioButtons.get(i).isSelected())
				return i;
		}
		return -1;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGui() {
		SpringLayout springLayout = new SpringLayout();
		setLayout(springLayout);
		
		{
			jLabelHeader = new JLabel();
			springLayout.putConstraint(SpringLayout.NORTH, jLabelHeader, 20, SpringLayout.NORTH, this);
			springLayout.putConstraint(SpringLayout.WEST, jLabelHeader, 20, SpringLayout.WEST, this);
			springLayout.putConstraint(SpringLayout.SOUTH, jLabelHeader, 36, SpringLayout.NORTH, this);
			springLayout.putConstraint(SpringLayout.EAST, jLabelHeader, 290, SpringLayout.WEST, this);
			this.add(jLabelHeader);
			jLabelHeader.setFont(BIG_FONT);
			jLabelHeader.setText(headline);
		}
		
		{
			radioButtonPanel = new JPanel();
			radioButtonPanel.setLayout(new GridLayout(radioButtonOptions.size(),1));
			springLayout.putConstraint(SpringLayout.NORTH, radioButtonPanel, 48, SpringLayout.NORTH, this);
			springLayout.putConstraint(SpringLayout.WEST, radioButtonPanel, 10, SpringLayout.WEST, this);
			springLayout.putConstraint(SpringLayout.SOUTH, radioButtonPanel, 80, SpringLayout.NORTH, this);
			springLayout.putConstraint(SpringLayout.EAST, radioButtonPanel, -10, SpringLayout.EAST, this);
			this.add(radioButtonPanel);
			radioButtonPanel.setFont(FONT);
		}
		{

			radioButtonGroup = new ButtonGroup();
						
			for (String s : radioButtonOptions) {
				JRadioButton rb = new JRadioButton(s);
				radioButtons.add(rb);
				radioButtonGroup.add(rb);
				radioButtonPanel.add(rb);
			}
			radioButtons.get(0).setSelected(true);
		}
		{
			springLayout.putConstraint(SpringLayout.NORTH, lcp, 85, SpringLayout.NORTH, this);
			springLayout.putConstraint(SpringLayout.SOUTH, lcp, -24, SpringLayout.SOUTH, this);
			springLayout.putConstraint(SpringLayout.WEST, lcp, 10, SpringLayout.WEST, this);
			springLayout.putConstraint(SpringLayout.EAST, lcp, -10, SpringLayout.EAST, this);
			this.add(lcp);
		}
		
	}
	
}

package fiji.plugin.trackmate.segmentation;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;

public class CreateRegionsToolConfigPanel extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private final static ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private final static ImageIcon SELECT_TRACK_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_updown.png"));

	private final Logger logger;
	private JNumericTextField jNFDistanceTolerance;
	private final CreateRegionsTool parent;

	public CreateRegionsToolConfigPanel(final CreateRegionsTool parent) {
		this.parent = parent;
		
		System.out.println("Create CreateRegionsToolConfigPanel");
		
		/*
		 * Listeners
		 */
		
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateParamsFromTextFields();
			}
		};
		FocusListener fl = new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				updateParamsFromTextFields();
			}
			@Override public void focusGained(FocusEvent arg0) {}
		};
		
		
		/*
		 * GUI
		 */

		
		setTitle("TrackMate tools");
		setIconImage(ICON.getImage());
		setResizable(false);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		
		JPanel mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		mainPanel.setLayout(null);
		
		JLabel lblTitle = new JLabel("TrackMate tools");
		lblTitle.setBounds(6, 6, 395, 33);
		lblTitle.setFont(BIG_FONT);
		lblTitle.setIcon(ICON);
		mainPanel.add(lblTitle);
		
		JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		panelSemiAutoParams.setBounds(6, 51, 192, 108);
		mainPanel.add(panelSemiAutoParams);
		panelSemiAutoParams.setLayout(null);
		
		JLabel lblDistanceTolerance = new JLabel("Distance tolerance");
		lblDistanceTolerance.setToolTipText("<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>");
		lblDistanceTolerance.setBounds(6, 86, 119, 16);
		lblDistanceTolerance.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblDistanceTolerance);
		
		jNFDistanceTolerance = new JNumericTextField(parent.pointClickRadius);
		jNFDistanceTolerance.setHorizontalAlignment(SwingConstants.CENTER);
		jNFDistanceTolerance.setFont(SMALL_FONT);
		jNFDistanceTolerance.setBounds(137, 84, 49, 18);
		jNFDistanceTolerance.addActionListener(al);
		jNFDistanceTolerance.addFocusListener(fl);
		panelSemiAutoParams.add(jNFDistanceTolerance);
				
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(210, 51, 264, 237);
		mainPanel.add(scrollPane);
		
		final JTextPane textPane = new JTextPane();
		textPane.setFont(SMALL_FONT);
		textPane.setEditable(false);
		textPane.setBackground(this.getBackground());
		scrollPane.setViewportView(textPane);
		
		JPanel panelButtons = new JPanel();
		panelButtons.setBounds(6, 171, 192, 117);
		panelButtons.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		mainPanel.add(panelButtons);
		panelButtons.setLayout(null);
		
		JLabel lblSelectionTools = new JLabel("Selection tools");
		lblSelectionTools.setFont(FONT.deriveFont(Font.BOLD));
		lblSelectionTools.setBounds(10, 11, 172, 14);
		panelButtons.add(lblSelectionTools);
		
		JButton buttonSelectTrack = new JButton(SELECT_TRACK_ICON);
		buttonSelectTrack.setBounds(10, 36, 33, 23);
		buttonSelectTrack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
		panelButtons.add(buttonSelectTrack);

		JLabel lblSelectTrack = new JLabel("Select track");
		lblSelectTrack.setBounds(53, 36, 129, 23);
		lblSelectTrack.setFont(SMALL_FONT);
		lblSelectTrack.setToolTipText("Select the whole tracks selected spots belong to.");
		panelButtons.add(lblSelectTrack);



		
		
		logger = new Logger() {

			@Override
			public void error(String message) {
				log(message, Logger.ERROR_COLOR);				
			}

			@Override
			public void log(final String message, final Color color) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						textPane.setEditable(true);
						StyleContext sc = StyleContext.getDefaultStyleContext();
						AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
						int len = textPane.getDocument().getLength();
						textPane.setCaretPosition(len);
						textPane.setCharacterAttributes(aset, false);
						textPane.replaceSelection(message);
						textPane.setEditable(false);
					}
				});
			}

			@Override
			public void setStatus(final String status) {
				log(status, Logger.GREEN_COLOR);
			}
			
			@Override
			public void setProgress(double val) {}
		};	
		
		setSize(480, 318);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setVisible(true);
	}
	
	/**
	 * Returns the {@link Logger} that outputs on this config panel.
	 * @return  the {@link Logger} instance of this panel.
	 */
	public Logger getLogger() {
		return logger;
	}

	private void updateParamsFromTextFields() {
		parent.pointClickRadius = (int)jNFDistanceTolerance.getValue();
	}
	

}

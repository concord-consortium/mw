/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */

package org.concord.modeler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.PrintParameters;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.DataQueue;

class PreferencesDialog extends JDialog {

	final static String DEFAULT_HOME_PAGE = "Default home page";
	final static String HOME_PAGE = "Home page";
	final static String LAST_VISITED_PAGE = "Last visited page";

	private JTextField homePageTextField;
	private String startPageType = DEFAULT_HOME_PAGE;
	private JRadioButton[] rButton;
	private Modeler modeler;
	private JRadioButton[] proxyButton;
	private JTextField proxyAddressField;
	private IntegerTextField proxyPortField;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private IntegerTextField connectTimeoutField;
	private IntegerTextField readTimeoutField;
	private IntegerTextField tapeLengthField;
	private IntegerTextField dayField;
	private static JLabel addressLabel;
	private static JLabel portLabel;
	private static JLabel nameLabel;
	private static JLabel passwordLabel;

	PreferencesDialog(Modeler modeler0) {

		super(modeler0, "Preferences", false);
		String s = Modeler.getInternationalText("Preference");
		if (s != null)
			setTitle(s);

		if (addressLabel == null) {
			s = Modeler.getInternationalText("ProxyAddress");
			addressLabel = new JLabel((s != null ? s : "Address") + ":");
		}
		if (portLabel == null) {
			s = Modeler.getInternationalText("PortNumber");
			portLabel = new JLabel((s != null ? s : "Port") + ":");
		}
		if (nameLabel == null) {
			s = Modeler.getInternationalText("UserName");
			nameLabel = new JLabel((s != null ? s : "User name") + ":");
		}
		if (passwordLabel == null) {
			s = Modeler.getInternationalText("Password");
			passwordLabel = new JLabel((s != null ? s : "Password") + ":");
		}

		setResizable(true);
		setSize(new Dimension(400, 400));

		this.modeler = modeler0;

		getContentPane().setLayout(new BorderLayout(5, 5));

		JTabbedPane tabbedPane = new JTabbedPane();

		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		s = Modeler.getInternationalText("GeneralTab");
		tabbedPane.addTab(s != null ? s : "General", createHomePagePanel());

		s = Modeler.getInternationalText("ConnectionTab");
		tabbedPane.addTab(s != null ? s : "Connection", createConnectionPanel());

		s = Modeler.getInternationalText("RecorderTab");
		tabbedPane.addTab(s != null ? s : "Recorder", createTapePanel());

		s = Modeler.getInternationalText("PrintingTab");
		tabbedPane.addTab(s != null ? s : "Printing", createPrintPanel());

		s = Modeler.getInternationalText("CacheTab");
		tabbedPane.addTab(s != null ? s : "Cache", createCachePanel());

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HistoryManager.sharedInstance().setDays(dayField.getValue());
				if (Modeler.tapeLength != tapeLengthField.getValue()) {
					if (okToRestart()) {
						Modeler.tapeLength = tapeLengthField.getValue();
						restart();
					}
					else {
						tapeLengthField.setValue(Modeler.tapeLength);
					}
				}
				else {
					dispose();
				}
			}
		});
		panel.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PreferencesDialog.this.dispose();
			}
		});
		panel.add(button);

		getContentPane().add(panel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(modeler);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				PreferencesDialog.this.dispose();
			}
		});

	}

	private JPanel createPrintPanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel p = new JPanel(new GridLayout(4, 2, 5, 5));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20), BorderFactory
				.createTitledBorder("Scaling Factors")));

		final PrintParameters pp = Page.getPrintParameters();

		String s = Modeler.getInternationalText("ScaleCharacter");
		p.add(new JLabel("  " + (s != null ? s : "Scale characters") + " (%):"));
		IntegerTextField tf = new IntegerTextField(Math.round(pp.getCharacterScale() * 100), 50, 100, 10);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setCharacterScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Characters", x);
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setCharacterScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Characters", x);
			}
		});
		p.add(tf);

		s = Modeler.getInternationalText("ScaleImage");
		p.add(new JLabel("  " + (s != null ? s : "Scale images") + " (%):"));
		tf = new IntegerTextField(Math.round(pp.getImageScale() * 100), 50, 100, 10);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setImageScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Images", x);
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setImageScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Images", x);
			}
		});
		p.add(tf);

		s = Modeler.getInternationalText("ScaleComponent");
		p.add(new JLabel("  " + (s != null ? s : "Scale components") + " (%):"));
		tf = new IntegerTextField(Math.round(pp.getComponentScale() * 100), 50, 100, 10);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setComponentScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Components", x);
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setComponentScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Components", x);
			}
		});
		p.add(tf);

		s = Modeler.getInternationalText("ScaleIndent");
		p.add(new JLabel("  " + (s != null ? s : "Scale paragraph indents") + " (%):"));
		tf = new IntegerTextField(Math.round(pp.getIndentScale() * 100), 10, 100, 10);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setIndentScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Paragraph Indents", x);
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				float x = 0.01f * getWithinBounds((IntegerTextField) e.getSource());
				pp.setIndentScale(x);
				Initializer.sharedInstance().getPreferences().putFloat("Scale Paragraph Indents", x);
			}
		});
		p.add(tf);

		total.add(p, BorderLayout.NORTH);

		s = Modeler.getInternationalText("PrintSettingInfo");
		JLabel label = new JLabel(
				"<html>"
						+ (s != null ? s
								: "The sizes of text, images and components on a page will<br>be different on a piece of paper than on a computer screen.<br>Therefore, they have to be rescaled to fit the size of<br>paper before printing. The scaling factors for different<br>types of elements may be different. You may change them in<br>the above fields.")
						+ "</html>");
		label.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 20));
		total.add(label, BorderLayout.CENTER);

		p = new JPanel(new GridLayout(2, 4, 5, 5));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 20, 10, 20), BorderFactory
				.createTitledBorder("Paper Margins")));
		total.add(p, BorderLayout.SOUTH);

		p.add(new JLabel("  Top :"));
		tf = new IntegerTextField(pp.getTopMargin(), 10, 100, 5);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pp.setTopMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				pp.setTopMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		p.add(tf);

		p.add(new JLabel("  Bottom :"));
		tf = new IntegerTextField(pp.getBottomMargin(), 10, 100, 5);
		tf.setPreferredSize(new Dimension(100, 25));
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pp.setBottomMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				pp.setBottomMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		p.add(tf);

		p.add(new JLabel("  Left :"));
		tf = new IntegerTextField(pp.getLeftMargin(), 10, 100, 5);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pp.setLeftMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				pp.setLeftMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		p.add(tf);

		p.add(new JLabel("  Right :"));
		tf = new IntegerTextField(pp.getRightMargin(), 10, 100, 5);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pp.setRightMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				pp.setRightMargin(getWithinBounds((IntegerTextField) e.getSource()));
			}
		});
		p.add(tf);

		return total;

	}

	private static int getWithinBounds(IntegerTextField t) {
		int x = t.getValue();
		if (x > t.getMaxValue())
			x = t.getMaxValue();
		else if (x < t.getMinValue())
			x = t.getMinValue();
		t.setValue(x);
		return x;
	}

	private JPanel createConnectionPanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel pt = new JPanel(new BorderLayout(5, 5));
		total.add(pt, BorderLayout.NORTH);

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		String s = Modeler.getInternationalText("SetTimeOut");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "Set timeout") + ":"));
		pt.add(panel, BorderLayout.NORTH);

		JPanel p = new JPanel(new GridLayout(2, 1, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		s = Modeler.getInternationalText("ConnectTimeOut");
		p.add(new JLabel((s != null ? s : "Connection opening timeout") + " (in seconds)", SwingConstants.LEFT));
		s = Modeler.getInternationalText("ReadTimeOut");
		p.add(new JLabel((s != null ? s : "Read timeout") + " (in seconds)", SwingConstants.LEFT));
		panel.add(p, BorderLayout.WEST);

		p = new JPanel(new GridLayout(2, 1, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		connectTimeoutField = new IntegerTextField(10, 1, 60);
		p.add(connectTimeoutField);
		readTimeoutField = new IntegerTextField(60, 1, 120);
		p.add(readTimeoutField);
		panel.add(p, BorderLayout.CENTER);

		connectTimeoutField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int x = getWithinBounds(connectTimeoutField);
				ConnectionManager.setConnectTimeout(x * 1000);
				Initializer.sharedInstance().getPreferences().putInt("Connect Timeout", x);
			}
		});
		connectTimeoutField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				int x = getWithinBounds(connectTimeoutField);
				ConnectionManager.setConnectTimeout(x * 1000);
				Initializer.sharedInstance().getPreferences().putInt("Connect Timeout", x);
			}
		});

		readTimeoutField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int x = getWithinBounds(readTimeoutField);
				ConnectionManager.setReadTimeout(x * 1000);
				Initializer.sharedInstance().getPreferences().putInt("Read Timeout", x);
			}
		});
		readTimeoutField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				int x = getWithinBounds(readTimeoutField);
				ConnectionManager.setReadTimeout(x * 1000);
				Initializer.sharedInstance().getPreferences().putInt("Read Timeout", x);
			}
		});

		panel = new JPanel(new BorderLayout(5, 5));
		s = Modeler.getInternationalText("SetProxy");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "Set proxy to access the Internet") + ":"));
		pt.add(panel, BorderLayout.CENTER);

		ButtonGroup bg = new ButtonGroup();

		proxyButton = new JRadioButton[2];

		s = Modeler.getInternationalText("DirectConnectionToWeb");
		proxyButton[0] = new JRadioButton(s != null ? s : "Direct connection to the Web");
		proxyButton[0].setSelected(true);
		proxyButton[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setUseProxy(false);
			}
		});
		panel.add(proxyButton[0], BorderLayout.NORTH);
		bg.add(proxyButton[0]);

		s = Modeler.getInternationalText("UseHTTPProxyServer");
		proxyButton[1] = new JRadioButton(s != null ? s : "Use HTTP proxy server");
		proxyButton[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setUseProxy(true);
			}
		});
		panel.add(proxyButton[1], BorderLayout.CENTER);
		bg.add(proxyButton[1]);

		p = new JPanel(new BorderLayout(5, 5));
		panel.add(p, BorderLayout.SOUTH);

		JPanel p1 = new JPanel();
		p.add(p1, BorderLayout.NORTH);

		addressLabel.setEnabled(false);
		p1.add(addressLabel);

		proxyAddressField = new PastableTextField(20);
		proxyAddressField.setEnabled(false);
		proxyAddressField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = proxyAddressField.getText();
				if (s == null || s.trim().equals("")) {
					Initializer.sharedInstance().getPreferences().remove("Proxy Address");
				}
				else {
					Initializer.sharedInstance().getPreferences().put("Proxy Address", s);
				}
			}
		});
		proxyAddressField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				String s = proxyAddressField.getText();
				if (s == null || s.trim().equals("")) {
					Initializer.sharedInstance().getPreferences().remove("Proxy Address");
				}
				else {
					Initializer.sharedInstance().getPreferences().put("Proxy Address", s);
				}
			}
		});
		p1.add(proxyAddressField);

		portLabel.setEnabled(false);
		p1.add(portLabel);

		proxyPortField = new IntegerTextField(8080, 0, 65535);
		proxyPortField.setColumns(6);
		proxyPortField.setEnabled(false);
		proxyPortField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = proxyPortField.getText();
				if (s == null || s.trim().equals("")) {
					Initializer.sharedInstance().getPreferences().remove("Proxy Port");
				}
				else {
					Initializer.sharedInstance().getPreferences().putInt("Proxy Port", proxyPortField.getValue());
				}
			}
		});
		proxyPortField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				String s = proxyPortField.getText();
				if (s == null || s.trim().equals("")) {
					Initializer.sharedInstance().getPreferences().remove("Proxy Port");
				}
				else {
					Initializer.sharedInstance().getPreferences().putInt("Proxy Port", proxyPortField.getValue());
				}
			}
		});
		p1.add(proxyPortField);

		p1 = new JPanel();
		s = Modeler.getInternationalText("ProxyAuthentication");
		p1.setBorder(BorderFactory.createTitledBorder("    "
				+ (s != null ? s : "Proxy authentication (If required)" + ":")));
		p.add(p1, BorderLayout.CENTER);

		nameLabel.setEnabled(false);
		p1.add(nameLabel);

		usernameField = new JTextField(10);
		usernameField.setEnabled(false);
		p1.add(usernameField);

		passwordLabel.setEnabled(false);
		p1.add(passwordLabel);

		passwordField = new JPasswordField(10);
		passwordField.setEnabled(false);
		p1.add(passwordField);

		return total;

	}

	private JPanel createTapePanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		String s = Modeler.getInternationalText("LengthOfRecorder");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "Length of Recorder") + ":"));
		total.add(panel, BorderLayout.NORTH);

		s = Modeler.getInternationalText("RecorderSettingInfo");
		JLabel a = new JLabel(s != null ? s
				: "<html>Set the number of frames of the recorder here. After changing<br>it, please restart the "
						+ Modeler.NAME
						+ " for the setting to<br>come into effect, if it does not automatically restart.<html>");
		a.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(a, BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		panel.add(p, BorderLayout.NORTH);

		tapeLengthField = new IntegerTextField(Modeler.tapeLength, 100, 5000, 10);
		// tapeLengthField.setPreferredSize(new Dimension(100, 24));
		tapeLengthField.setHorizontalAlignment(JTextField.RIGHT);
		p.add(tapeLengthField);

		s = Modeler.getInternationalText("Reset");
		JButton button = new JButton(s != null ? s : "Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.tapeLength = DataQueue.DEFAULT_SIZE;
				tapeLengthField.setValue(Modeler.tapeLength);
			}
		});
		p.add(button);

		return total;

	}

	private JPanel createCachePanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		String s = Modeler.getInternationalText("CacheWebFiles");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "Caching Web files") + ":"));
		total.add(panel, BorderLayout.NORTH);

		String s2 = ConnectionManager.sharedInstance().getCacheDirectory().toString();
		s = Modeler.getInternationalText("CacheInfo");
		String s3 = Modeler.getInternationalText("CacheDirectory");
		int n = s2.length() / 3 * 2;
		JLabel a = new JLabel(
				"<html>"
						+ (s != null ? s
								: "Cached Web pages are stored in a special folder on your computer<br>for quick viewing later or offline browsing.")
						+ "<br><br>" + (s3 != null ? s3 : "The cache directory is") + ":<br><br><tt>"
						+ s2.substring(0, n) + "<br>" + s2.substring(n, s2.length()) + "</tt></html>");
		a.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(a, BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = Modeler.getInternationalText("ClearCache");
		JButton button = new JButton(s != null ? s : "Clear Cache");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(PreferencesDialog.this,
						"Do you really want to remove all cached files?\n(you normally should not do this)",
						"Clear cache?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					ConnectionManager.sharedInstance().clear();
				}
			}
		});
		p.add(button);

		panel.add(p, BorderLayout.SOUTH);

		panel = new JPanel(new BorderLayout(5, 5));
		s = Modeler.getInternationalText("VisitHistory");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "History") + ":"));
		total.add(panel, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.CENTER);
		s = Modeler.getInternationalText("RememberVisitedPagesFor");
		p.add(new JLabel(s != null ? s : "Remember visited pages for the last "));

		dayField = new IntegerTextField(HistoryManager.sharedInstance().getDays(), 1, 30, 3);
		p.add(dayField);

		s = Modeler.getInternationalText("Day");
		p.add(new JLabel(s != null ? s : " days."));

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(p, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("ClearVisitHistory");
		button = new JButton(s != null ? s : "Clear History");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HistoryManager.sharedInstance().clear();
			}
		});
		p.add(button);

		return total;

	}

	private JPanel createHomePagePanel() {

		JPanel total = new JPanel(new BorderLayout(8, 8));
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
		String s = Modeler.getInternationalText("MWStartPage");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s : "When the " + Modeler.NAME
				+ " starts up, display")
				+ ":"));

		ButtonGroup bg = new ButtonGroup();

		rButton = new JRadioButton[3];

		s = Modeler.getInternationalText("DefaultHomePage");
		rButton[0] = new JRadioButton(s != null ? s : "Default home page");
		rButton[0].setSelected(true);
		rButton[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startPageType = DEFAULT_HOME_PAGE;
			}
		});
		panel.add(rButton[0]);
		bg.add(rButton[0]);

		s = Modeler.getInternationalText("MyHomePage");
		rButton[1] = new JRadioButton(s != null ? s : "My home page");
		rButton[1].setSelected(false);
		rButton[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startPageType = HOME_PAGE;
			}
		});
		panel.add(rButton[1]);
		bg.add(rButton[1]);

		s = Modeler.getInternationalText("LastVisitedPage");
		rButton[2] = new JRadioButton(s != null ? s : "Last page visited");
		rButton[2].setSelected(false);
		rButton[2].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startPageType = LAST_VISITED_PAGE;
			}
		});
		panel.add(rButton[2]);
		bg.add(rButton[2]);

		total.add(panel, BorderLayout.NORTH);

		panel = new JPanel(new BorderLayout(5, 5));
		s = Modeler.getInternationalText("HomePage");
		panel.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Home page"));

		s = Modeler.getInternationalText("SetHomePageForMW");
		JLabel label = new JLabel("  " + (s != null ? s : "Set a home page for the " + Modeler.NAME));
		panel.add(label, BorderLayout.NORTH);

		s = Modeler.getInternationalText("PageLocation");
		label = new JLabel("  " + (s != null ? s : "Location") + ":  ");
		panel.add(label, BorderLayout.WEST);

		homePageTextField = new JTextField(20);
		homePageTextField.setText(modeler.navigator.getHomePage());
		panel.add(homePageTextField, BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = Modeler.getInternationalText("UseCurrentPage");
		JButton button = new JButton(s != null ? s : "Use Current Page");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startPageType = HOME_PAGE;
				setHome(modeler.navigator.getSelectedLocation());
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("Browse");
		button = new JButton(s != null ? s : "Browse");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ModelerUtilities.fileChooser.setAcceptAllFileFilterUsed(false);
				ModelerUtilities.fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("cml"));
				ModelerUtilities.fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				ModelerUtilities.fileChooser.setDialogTitle("Select page");
				ModelerUtilities.fileChooser.setApproveButtonText("Select");
				ModelerUtilities.fileChooser.setApproveButtonMnemonic('S');
				String latestPath = ModelerUtilities.fileChooser.getLastVisitedPath();
				if (latestPath != null)
					ModelerUtilities.fileChooser.setCurrentDirectory(new File(latestPath));
				if (ModelerUtilities.fileChooser.showOpenDialog(PreferencesDialog.this) == JFileChooser.APPROVE_OPTION) {
					setHome(ModelerUtilities.fileChooser.getSelectedFile().getAbsolutePath());
				}
				ModelerUtilities.fileChooser.resetChoosableFileFilters();
			}
		});
		p.add(button);

		panel.add(p, BorderLayout.SOUTH);

		total.add(panel, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		s = Modeler.getInternationalText("OtherSettings");
		panel.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Other settings"));

		p = new JPanel(new SpringLayout());
		panel.add(p);

		s = Modeler.getInternationalText("LanguageOptions");
		p.add(new JLabel((s != null ? s : "Set language") + ":"));
		s = Modeler.getInternationalText("SimplifiedChinese");
		String s1 = Modeler.getInternationalText("TraditionalChinese");
		final JComboBox languageComboBox = new JComboBox(new String[] { "English (United States)",
				s != null ? s : "Simplied Chinese (PRC)", s1 != null ? s1 : "Traditional Chinese (Taiwan)", "Russian" });
		languageComboBox.setToolTipText("This sets character encoding for saving page too.");
		setLanguageComboBox(languageComboBox);
		languageComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (languageComboBox.getSelectedIndex()) {
					case 0:
						Modeler.preference.put("Locale", "en_US");
						break;
					case 1:
						Modeler.preference.put("Locale", "zh_CN");
						break;
					case 2:
						Modeler.preference.put("Locale", "zh_TW");
						break;
					case 3:
						Modeler.preference.put("Locale", "ru");
						break;
					}
					if (okToRestart())
						restart();
					else setLanguageComboBox(languageComboBox);
				}
			}
		});
		p.add(languageComboBox);

		ModelerUtilities.makeCompactGrid(p, 1, 2, 4, 4, 4, 4);

		total.add(panel, BorderLayout.SOUTH);

		p = new JPanel(new BorderLayout());
		p.add(total, BorderLayout.NORTH);

		return p;

	}

	private void setLanguageComboBox(JComboBox comboBox) {
		ItemListener[] il = comboBox.getItemListeners();
		for (ItemListener i : il)
			comboBox.removeItemListener(i);
		if (Locale.getDefault().equals(Locale.US))
			comboBox.setSelectedIndex(0);
		else if (Locale.getDefault().equals(Locale.CHINA))
			comboBox.setSelectedIndex(1);
		else if (Locale.getDefault().equals(Locale.TAIWAN))
			comboBox.setSelectedIndex(2);
		else if (Locale.getDefault().equals(new Locale("ru")))
			comboBox.setSelectedIndex(3);
		for (ItemListener i : il)
			comboBox.addItemListener(i);
	}

	private boolean okToRestart() {
		String s1 = Modeler.getInternationalText("RestartMolecularWorkbenchNotice");
		String s2 = Modeler.getInternationalText("RestartMolecularWorkbench");
		if (JOptionPane.showConfirmDialog(modeler, s1 != null ? s1
				: "The Molecular Workbench has to be restarted for the\n" + "new settings to come into effect.\n\n"
						+ "(If it does not restart automatically, please click the\noriginal launcher to restart it.)",
				s2 != null ? s2 : "Restart", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			dispose();
			return true;
		}
		dispose();
		return false;
	}

	private void restart() {
		Modeler.restart = true;
		if (Modeler.launchedByJWS) {
			modeler.navigator.visitLocation(Initializer.sharedInstance().getResetJnlpAddress());
		}
		else {
			modeler.dispatchEvent(new WindowEvent(modeler, WindowEvent.WINDOW_CLOSING));
		}
	}

	void setPreferences(Preferences pref) {

		dayField.setValue(HistoryManager.sharedInstance().getDays());

		if (pref == null)
			return;

		connectTimeoutField.setValue(pref.getInt("Connect Timeout", 5));
		readTimeoutField.setValue(pref.getInt("Read Timeout", 30));

		String s = pref.get("Proxy Address", null);
		if (s != null) {
			int i = pref.getInt("Proxy Port", -1);
			if (i != -1) {
				setUseProxy(true);
				proxyPortField.setValue(i);
				proxyAddressField.setText(s);
				s = pref.get("Proxy Username", null);
				if (s != null)
					usernameField.setText(s);
				s = pref.get("Proxy Password", null);
				if (s != null)
					passwordField.setText(s);
			}
		}
		else {
			setUseProxy(false);
		}

		s = pref.get(HOME_PAGE, null);
		setHome(s != null ? s : modeler.navigator.getHomePage());

		s = pref.get("Start From", null);
		if (s != null)
			setStartPageType(s);

	}

	private void setHome(String s) {
		homePageTextField.setText(s);
		modeler.navigator.setHomePage(s);
	}

	String getHome() {
		return homePageTextField.getText();
	}

	void setStartPageType(String s) {
		startPageType = s;
		if (s.equals(DEFAULT_HOME_PAGE)) {
			rButton[0].setSelected(true);
		}
		else if (s.equals(HOME_PAGE)) {
			rButton[1].setSelected(true);
		}
		else if (s.equals(LAST_VISITED_PAGE)) {
			rButton[2].setSelected(true);
		}
	}

	String getStartPageType() {
		return startPageType;
	}

	void setUseProxy(boolean b) {
		proxyAddressField.setEnabled(b);
		proxyPortField.setEnabled(b);
		addressLabel.setEnabled(b);
		portLabel.setEnabled(b);
		usernameField.setEnabled(b);
		passwordField.setEnabled(b);
		nameLabel.setEnabled(b);
		passwordLabel.setEnabled(b);
		proxyButton[1].setSelected(b);
	}

	boolean getUseProxy() {
		return proxyButton[1].isSelected();
	}

	String getProxyAddress() {
		if (!proxyAddressField.isEnabled())
			return null;
		return proxyAddressField.getText();
	}

	int getProxyPortNumber() {
		if (!proxyPortField.isEnabled())
			return -1;
		return proxyPortField.getValue();
	}

	String getProxyUserName() {
		if (!usernameField.isEnabled())
			return null;
		return usernameField.getText();
	}

	char[] getProxyPassword() {
		if (!passwordField.isEnabled())
			return null;
		return passwordField.getPassword();
	}

}
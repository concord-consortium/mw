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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;

class PagePropertiesDialog extends JDialog {

	private JTextField urlField;
	private JLabel protocolLabel, encodingLabel;
	private JTextField titleField, additionalResourceField;
	private BackgroundComboBox bgComboBox;
	private Page page;
	private String imageURL;
	private JPanel dynamicPanel;
	private JButton okButton, cancelButton;

	private static Dimension mediumField = new Dimension(120, 20);
	private static Dimension longField = new Dimension(200, 20);
	private static Border border = BorderFactory.createEmptyBorder(0, 0, 0, 10);

	PagePropertiesDialog(final Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Properties", false);
		String s = Modeler.getInternationalText("Properties");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.page = page0;

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(BorderLayout.CENTER, tabbedPane);

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		s = Modeler.getInternationalText("GeneralTab");
		tabbedPane.addTab(s != null ? s : "General", total);

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		total.add(panel, BorderLayout.CENTER);

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;

		s = Modeler.getInternationalText("Protocol");
		JLabel label = new JLabel((s != null ? s : "Protocol") + ":");
		label.setBorder(border);
		label.setPreferredSize(mediumField);
		c.gridx = 0;
		c.gridy = 0;
		panel.add(label, c);
		protocolLabel = new JLabel(page.getAddress().indexOf("http://") == -1 ? "File Protocol"
				: "Hypertext Transfer Protocol");
		protocolLabel.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(protocolLabel, c);

		s = Modeler.getInternationalText("FileType");
		label = new JLabel((s != null ? s : "Type") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 1;
		panel.add(label, c);
		label = new JLabel("Extensible Markup Language 1.0");
		label.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(label, c);

		s = Modeler.getInternationalText("CharacterEncoding");
		label = new JLabel((s != null ? s : "Character Encoding") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 2;
		panel.add(label, c);
		encodingLabel = new JLabel(page.getCharacterEncoding());
		encodingLabel.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(encodingLabel, c);

		s = Modeler.getInternationalText("PageTitle");
		label = new JLabel((s != null ? s : "Title") + ":");
		label.setBorder(border);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 1.0;
		panel.add(label, c);
		titleField = new PastableTextField();
		titleField.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(titleField, c);

		s = Modeler.getInternationalText("AdditionalResourceFiles");
		label = new JLabel((s != null ? s : "Additional Resources") + ":");
		label.setBorder(border);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 1.0;
		panel.add(label, c);
		additionalResourceField = new PastableTextField();
		additionalResourceField.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(additionalResourceField, c);

		s = Modeler.getInternationalText("Background");
		label = new JLabel((s != null ? s : "Background") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 5;
		panel.add(label, c);
		bgComboBox = new BackgroundComboBox(page, ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.getColorMenu().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				bgComboBox.getColorMenu().setColor(page.getBackground());
			}
		});
		bgComboBox.getColorMenu().addNoFillListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				page.changeFillMode(fm);
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addColorArrayListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(new FillMode.ColorFill(bgComboBox.getColorMenu().getColor()));
			}
		});
		bgComboBox.getColorMenu().addMoreColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				page.changeFillMode(fm);
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						page.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) page.getFillMode())
								.getColor() : null);
				if (c == null)
					return;
				FillMode fm = new FillMode.ColorFill(c);
				page.changeFillMode(fm);
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addFillEffectListeners(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(bgComboBox.getColorMenu().getFillEffectChooser().getFillMode());
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, page.getFillMode());
			}
		}, null);
		bgComboBox.setToolTipText("Background");
		bgComboBox.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(bgComboBox, c);

		s = Modeler.getInternationalText("PageLocation");
		label = new JLabel((s != null ? s : "Location") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 6;
		panel.add(label, c);
		urlField = new PastableTextField(page.getAddress());
		urlField.setPreferredSize(longField);
		urlField.setEditable(false);
		urlField.setBackground(label.getBackground());
		urlField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(urlField, c);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(panel, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = page.getTitle();
				if (s == null || !s.equals(titleField.getText())) {
					page.setTitle(titleField.getText());
					if (page.isEditable())
						page.getSaveReminder().setChanged(true);
				}
				s = page.getAdditionalResourceFiles();
				if (s == null || !s.equals(additionalResourceField.getText())) {
					page.setAdditionalResourceFiles(additionalResourceField.getText());
					if (page.isEditable())
						page.getSaveReminder().setChanged(true);
				}
				if (imageURL != null) {
					page.changeBackgroundImage(imageURL);
					imageURL = null;
				}
				page.repaint();
				PagePropertiesDialog.this.dispose();
			}
		});
		panel.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PagePropertiesDialog.this.dispose();
			}
		});
		panel.add(cancelButton);

		dynamicPanel = new JPanel(new GridBagLayout());
		dynamicPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		total.add(dynamicPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				page.requestFocusInWindow();
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				// transfer focus to the input URL field
				if (page.isEditable()) {
					titleField.selectAll();
					titleField.requestFocusInWindow();
				}
			}
		});

	}

	public void destroy() {

		bgComboBox.destroy();

		ActionListener[] al = okButton.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				okButton.removeActionListener(i);
		}
		al = cancelButton.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				cancelButton.removeActionListener(i);
		}
		WindowListener[] wl = getWindowListeners();
		if (wl != null) {
			for (WindowListener i : wl)
				removeWindowListener(i);
		}

		getContentPane().removeAll();

		try {
			finalize();
		}
		catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		dispose();

		bgComboBox = null;
		page = null;

	}

	public void setCurrentValues() {

		titleField.setText(page.getTitle());
		additionalResourceField.setText(page.getAdditionalResourceFiles());
		urlField.setText(page.getAddress());
		protocolLabel.setText(page.getAddress().indexOf("http://") == -1 ? "File Protocol"
				: "Hypertext Transfer Protocol");
		encodingLabel.setText(page.getCharacterEncoding());
		bgComboBox.setFillMode(page.getFillMode());

		boolean b = page.isEditable();
		titleField.setEditable(b);
		additionalResourceField.setEditable(b);
		bgComboBox.setEnabled(b);

		dynamicPanel.removeAll();
		if (!page.getProperties().isEmpty())
			createPanel();

	}

	private void createPanel() {

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;

		JLabel label;
		int i = 0;

		String s = Modeler.getInternationalText("EmbeddedModel");
		for (Iterator it = page.getComponentPool().getModels().iterator(); it.hasNext();) {
			String s2 = (String) page.getProperties().get(it.next());
			if (s2 != null) {
				s2 = FileUtilities.getFileName(s2);
				label = new JLabel((s != null ? s : "Embedded Model") + ":");
				label.setPreferredSize(mediumField);
				label.setBorder(border);
				c.gridx = 0;
				c.gridy = i;
				dynamicPanel.add(label, c);
				label = new JLabel(s2);
				label.setPreferredSize(longField);
				c.gridx = 1;
				c.weightx = 1.0;
				c.gridwidth = 3;
				c.fill = GridBagConstraints.HORIZONTAL;
				dynamicPanel.add(label, c);
				i++;
			}
		}

		s = Modeler.getInternationalText("ContentLength");
		label = new JLabel((s != null ? s : "Content Length") + ":");
		label.setPreferredSize(mediumField);
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = i;
		dynamicPanel.add(label, c);
		Long size = (Long) (page.getProperties().get("Size"));
		label = new JLabel(size == null ? "Unknown" : new DecimalFormat("######.#").format(size.longValue() / 1024.0)
				+ " KB (" + new DecimalFormat("###,###,###").format(size) + " bytes)");
		label.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		dynamicPanel.add(label, c);
		i++;

		s = Modeler.getInternationalText("LastModified");
		label = new JLabel((s != null ? s : "Last Modified") + ":");
		label.setPreferredSize(mediumField);
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = i;
		dynamicPanel.add(label, c);
		Date date = (Date) page.getProperties().get("Last modified");
		label = new JLabel(date == null ? "Unknown" : date.toString());
		label.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		dynamicPanel.add(label, c);
		i++;

	}

}
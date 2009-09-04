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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class ActivityButtonMaker extends ComponentMaker {

	private static Font smallFont;
	private ActivityButton activityButton;
	private JDialog dialog;
	private JTextField uidField;
	private JComboBox actionComboBox, borderComboBox;
	private JCheckBox transparentCheckBox, autoSizeCheckBox;
	private ColorComboBox bgComboBox;
	private IntegerTextField widthField, heightField;
	private JTextField nameField, toolTipField;
	private JTextField imageFileNameField;
	private JLabel pageGroupLabel;
	private JTextField pageGroupField;
	private JLabel descriptionLabel;
	private JTextField descriptionField;
	private JLabel areaLabel;
	private JTextArea textArea;
	private JPanel contentPane;
	private JComponent focusComponent;

	private ItemListener actionSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			Action a = (Action) actionComboBox.getSelectedItem();
			activityButton.setAction(a);
			boolean b = needPageGroupInput(activityButton.getAction().toString());
			pageGroupField.setEnabled(b);
			pageGroupLabel.setEnabled(b);
			String s = null;
			if (activityButton.pageNameGroup != null) {
				s = activityButton.pageNameGroup;
			}
			else {
				s = FileUtilities.getFileName(activityButton.page.getAddress());
			}
			pageGroupField.setText(b ? s : null);
			descriptionLabel.setEnabled(b);
			descriptionField.setEnabled(b);
			descriptionField.setText(b ? activityButton.reportTitle : null);
			nameField.setText(activityButton.getAction().toString());
			toolTipField.setText(activityButton.getAction().toString());
			setTextArea(false);
		}
	};

	ActivityButtonMaker(ActivityButton ab) {
		setObject(ab);
	}

	void setObject(ActivityButton ab) {
		activityButton = ab;
	}

	private boolean confirm() {
		if (!checkAndSetUid(uidField.getText(), activityButton, dialog))
			return false;
		if (!Page.isNativeLookAndFeelUsed()) {
			activityButton.setOpaque(!transparentCheckBox.isSelected());
			activityButton.setBorderType((String) borderComboBox.getSelectedItem());
		}
		activityButton.setBackground(bgComboBox.getSelectedColor());
		Action action = (Action) actionComboBox.getSelectedItem();
		Icon actionIcon = (Icon) action.getValue(Action.SMALL_ICON);
		activityButton.setAction(action);
		activityButton.getAction().putValue("source", this);
		activityButton.setText(nameField.getText());
		String imageFileName = imageFileNameField.getText();
		if (imageFileName != null && !imageFileName.trim().equals("")) {
			ImageIcon image = loadLocalImage(activityButton.page, imageFileName);
			if (image == null) {
				JOptionPane.showMessageDialog(dialog, "File " + imageFileName + " does not exist.", "File Error",
						JOptionPane.ERROR_MESSAGE);
				focusComponent = imageFileNameField;
				return false;
			}
			activityButton.setIcon(image);
		}
		else {
			activityButton.setIcon(actionIcon);
		}
		String toolTip = toolTipField.getText();
		if (toolTip != null && !toolTip.trim().equals(""))
			activityButton.setToolTipText(toolTip);
		String s = pageGroupField.getText();
		if (s.trim().equals("")) {
			s = null;
		}
		else if (!s.trim().toLowerCase().endsWith(".cml")) {
			s = s.substring(0, s.toLowerCase().lastIndexOf(".cml") + 4);
		}
		activityButton.setPageNameGroup(s);
		s = descriptionField.getText();
		if (s.trim().equals(""))
			s = null;
		activityButton.setReportTitle(s);
		if (!activityButton.autoSize) {
			activityButton.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		}
		if (activityButton.isActionHint())
			activityButton.putClientProperty("hint", textArea.getText());
		else if (activityButton.isActionScript())
			activityButton.putClientProperty("script", textArea.getText());
		else if (activityButton.isActionGrade())
			activityButton.putClientProperty("grade_uri", textArea.getText());
		activityButton.page.getSaveReminder().setChanged(true);
		activityButton.page.settleComponentSize();
		return true;
	}

	private boolean needPageGroupInput(String s) {
		return s.indexOf("page group") != -1 || s.indexOf("grade") != -1;
	}

	void invoke(Page page) {

		activityButton.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {

			String s = Modeler.getInternationalText("CustomizeButtonDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize activity button", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					if (focusComponent == null) {
						nameField.selectAll();
						nameField.requestFocusInWindow();
					}
					else {
						focusComponent.requestFocusInWindow();
					}
				}
			});

		}

		actionComboBox.removeItemListener(actionSelectionListener);
		fillActionComboBox();
		if (activityButton.getAction() != null) {
			actionComboBox.setSelectedItem(activityButton.getAction());
			boolean b = needPageGroupInput(activityButton.getAction().toString());
			pageGroupLabel.setEnabled(b);
			pageGroupField.setEnabled(b);
			descriptionLabel.setEnabled(b);
			descriptionField.setEnabled(b);
		}
		actionComboBox.addItemListener(actionSelectionListener);

		uidField.setText(activityButton.getUid());
		String t = activityButton.getText();
		nameField.setText(t != null ? t : (actionComboBox.getSelectedItem() != null ? actionComboBox.getSelectedItem()
				.toString() : null));
		Icon icon = activityButton.getIcon();
		if (icon instanceof ImageIcon) {
			String s = ((ImageIcon) icon).getDescription();
			if (s != null && s.indexOf(":") != -1) {
				imageFileNameField.setText(null);
			}
			else {
				imageFileNameField.setText(s);
			}
		}
		else {
			imageFileNameField.setText(null);
		}
		toolTipField.setText(activityButton.getToolTipText());
		if (needPageGroupInput(actionComboBox.getSelectedItem().toString())) {
			pageGroupField.setText(activityButton.pageNameGroup);
		}
		else {
			pageGroupField.setText(null);
		}
		descriptionField.setText(activityButton.reportTitle);
		if (!Page.isNativeLookAndFeelUsed()) {
			transparentCheckBox.setSelected(!activityButton.isOpaque());
			borderComboBox.setSelectedItem(activityButton.getBorderType());
		}
		bgComboBox.setColor(activityButton.getBackground());
		if (activityButton.isPreferredSizeSet()) {
			widthField.setValue(activityButton.getWidth());
			heightField.setValue(activityButton.getHeight());
		}
		autoSizeCheckBox.setSelected(activityButton.autoSize);
		widthField.setEnabled(!activityButton.autoSize);
		heightField.setEnabled(!activityButton.autoSize);

		setTextArea(activityButton.getAction() == null);

		dialog.setVisible(true);

	}

	private void fillActionComboBox() {
		actionComboBox.removeAllItems();
		Map activityActions = activityButton.page.getActivityActions();
		if (activityActions == null)
			return;
		synchronized (activityActions) {
			for (Object o : activityActions.values()) {
				actionComboBox.addItem(o);
			}
		}
		Object scriptAction = getScriptAction(activityActions);
		if (scriptAction != null) {
			actionComboBox.setSelectedItem(scriptAction);
		}
	}

	private void setTextArea(boolean scriptDefault) {
		boolean isHintButton = activityButton.isActionHint();
		boolean isScriptButton = activityButton.isActionScript();
		boolean isGradeButton = activityButton.isActionGrade();
		if (scriptDefault || isHintButton || isScriptButton || isGradeButton) {
			textArea.setEnabled(true);
			areaLabel.setEnabled(true);
			textArea.requestFocusInWindow();
			textArea.setCaretPosition(0);
		}
		else {
			textArea.setEnabled(false);
			areaLabel.setEnabled(false);
		}
		if (isHintButton) {
			areaLabel.setText("Enter hint (support plain text and HTML) :");
			textArea.setText(ModelerUtilities.deUnicode((String) activityButton.getClientProperty("hint")));
		}
		else if (isScriptButton || scriptDefault) {
			areaLabel
					.setText("<html>Enter scripts in the following format:<br><font face=\"Courier New\" size=\"-2\">(native)script:[component type]:[component index]:[script body]</font></html>");
			textArea.setText((String) activityButton.getClientProperty("script"));
		}
		else if (isGradeButton) {
			areaLabel.setText("Enter the URI at which the report will be graded:");
			textArea.setText((String) activityButton.getClientProperty("grade_uri"));
		}
		else {
			areaLabel.setText(null);
			textArea.setText(null);
		}
	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					dialog.dispose();
					cancel = false;
				}
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectActionLabel");
		p.add(new JLabel(s != null ? s : "Select an action", SwingConstants.LEFT));
		actionComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(actionComboBox.getFont().getFamily(), actionComboBox.getFont().getStyle(), 10);
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select an action that this button will perform.");
		p.add(actionComboBox);

		// row 2
		s = Modeler.getInternationalText("UniqueIdentifier");
		p.add(new JLabel(s != null ? s : "Unique identifier", SwingConstants.LEFT));
		uidField = new JTextField();
		uidField.setToolTipText("Type in a string to be used as the unique identifier of this button.");
		uidField.addActionListener(okListener);
		p.add(uidField);

		// row 3
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Text", SwingConstants.LEFT));
		nameField = new JTextField();
		nameField.setToolTipText("Type in the text that will appear on this button.");
		nameField.addActionListener(okListener);
		p.add(nameField);

		// row 4
		s = Modeler.getInternationalText("ImageFileName");
		p.add(new JLabel(s != null ? s : "Image file name", SwingConstants.LEFT));
		imageFileNameField = new JTextField();
		imageFileNameField.setToolTipText("Type in the file name of the image that will appear on this button.");
		imageFileNameField.addActionListener(okListener);
		p.add(imageFileNameField);

		// row 5
		s = Modeler.getInternationalText("ToolTipLabel");
		p.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p.add(toolTipField);

		// row 6
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(activityButton.getWidth() <= 0 ? 100 : activityButton.getWidth(), 10, 400);
		widthField.setEnabled(false);
		widthField.setToolTipText("Type in an integer to set the width of this button, if it will not be auto-sized.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 7
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(activityButton.getHeight() <= 0 ? 24 : activityButton.getHeight(), 10, 400);
		heightField.setEnabled(false);
		heightField
				.setToolTipText("Type in an integer to set the height of this button, if it will not be auto-sized.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 8
		s = Modeler.getInternationalText("BackgroundColorLabel");
		JLabel label = new JLabel(s != null ? s : "Background color", SwingConstants.LEFT);
		label.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(label);
		bgComboBox = new ColorComboBox(activityButton);
		bgComboBox.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color for this button, if it is not transparent.");
		bgComboBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(bgComboBox);

		// row 9
		s = Modeler.getInternationalText("BorderLabel");
		label = new JLabel(s != null ? s : "Border", SwingConstants.LEFT);
		label.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(label);
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this button.");
		borderComboBox.setPreferredSize(new Dimension(250, 24));
		p.add(borderComboBox);
		borderComboBox.setEnabled(!Page.isNativeLookAndFeelUsed());

		// row 10
		s = Modeler.getInternationalText("CollectDataFromPagesLabel");
		pageGroupLabel = new JLabel(s != null ? s : "Collect data from pages:", SwingConstants.LEFT);
		pageGroupLabel.setEnabled(false);
		p.add(pageGroupLabel);
		pageGroupField = new JTextField();
		pageGroupField.setEnabled(false);
		pageGroupField
				.setToolTipText("Type in the correct order the file names of the pages that a multipage report will collect data from, separated by comma.");
		pageGroupField.addActionListener(okListener);
		p.add(pageGroupField);

		// row 11
		s = Modeler.getInternationalText("MultipageReportTitleLabel");
		descriptionLabel = new JLabel(s != null ? s : "Title on multipage report:", SwingConstants.LEFT);
		descriptionLabel.setEnabled(false);
		p.add(descriptionLabel);
		descriptionField = new JTextField();
		descriptionField.setEnabled(false);
		descriptionField.setToolTipText("Set a title for a multipage report.");
		descriptionField.addActionListener(okListener);
		p.add(descriptionField);

		ModelerUtilities.makeCompactGrid(p, 11, 2, 5, 5, 10, 2);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new GridLayout(1, 2));
		p1.setBorder(BorderFactory.createEtchedBorder());
		p.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		transparentCheckBox.setSelected(false);
		transparentCheckBox.setToolTipText("Select to set this button to be transparent.");
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("AutosizeCheckBox");
		autoSizeCheckBox = new JCheckBox(s != null ? s : "Set Size Automatically");
		autoSizeCheckBox.setSelected(activityButton.autoSize);
		autoSizeCheckBox
				.setToolTipText("<html>Select to make this button auto-size itself according to the text and image on it.<br>Deselect to set a custom size.</html>");
		autoSizeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				activityButton.autoSize = autoSizeCheckBox.isSelected();
				widthField.setEnabled(!activityButton.autoSize);
				heightField.setEnabled(!activityButton.autoSize);
			}
		});
		p1.add(autoSizeCheckBox);

		p1 = new JPanel(new BorderLayout(5, 5));
		p.add(p1, BorderLayout.CENTER);

		areaLabel = new JLabel();
		p1.add(areaLabel, BorderLayout.NORTH);
		textArea = new PastableTextArea(5, 10);
		textArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(textArea), BorderLayout.CENTER);

	}

}
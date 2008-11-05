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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageScriptConsoleMaker extends ComponentMaker {

	private PageScriptConsole pageScriptConsole;
	private JDialog dialog;
	private JComboBox modelComboBox;
	private JComboBox borderComboBox;
	private IntegerTextField widthField, heightField;
	private JButton okButton;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageScriptConsole);
			}
			else {
				if (m instanceof MDModel) {
					pageScriptConsole.setModelID(pageScriptConsole.page.getComponentPool().getIndex(m));
				}
				else if (m instanceof Embeddable) {
					pageScriptConsole.setModelID(((Embeddable) m).getIndex());
				}
				m.addModelListener(pageScriptConsole);
			}
		}
	};

	PageScriptConsoleMaker(PageScriptConsole psc) {
		setObject(psc);
	}

	void setObject(PageScriptConsole psc) {
		pageScriptConsole = psc;
	}

	private void confirm() {
		pageScriptConsole.setChangable(true);
		Object o = modelComboBox.getSelectedItem();
		BasicModel m = (BasicModel) o;
		m.addModelListener(pageScriptConsole);
		pageScriptConsole.setModelClass(m.getClass().getName());
		if (o instanceof MDModel) {
			pageScriptConsole.setModelID(pageScriptConsole.page.getComponentPool().getIndex(m));
			((MDModel) o).addScriptListener(pageScriptConsole);
		}
		else if (o instanceof Embeddable) {
			pageScriptConsole.setModelID(((Embeddable) o).getIndex());
			if (o instanceof PageMd3d) {
				((PageMd3d) o).getMolecularModel().addScriptListener(pageScriptConsole);
			}
		}
		pageScriptConsole.setBorderType((String) borderComboBox.getSelectedItem());
		pageScriptConsole.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageScriptConsole.page.getSaveReminder().setChanged(true);
		pageScriptConsole.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageScriptConsole.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeScriptConsoleDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize script console", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocusInWindow();
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();

		// add legacy MD models to the model list
		final ComponentPool componentPool = page.getComponentPool();
		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getContainer().getModel());
				}
			}
		}

		// add target models to the model list
		for (Class c : ModelCommunicator.targetClass) {
			Map map = page.getEmbeddedComponent(c);
			if (map != null && !map.isEmpty()) {
				for (Object o : map.keySet()) {
					modelComboBox.addItem(map.get(o));
				}
			}
		}

		if (pageScriptConsole.isTargetClass()) {
			if (pageScriptConsole.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageScriptConsole.modelClass),
							pageScriptConsole.modelID);
					if (o != null)
						modelComboBox.setSelectedItem(o);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				if (m instanceof Embeddable)
					pageScriptConsole.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageScriptConsole.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageScriptConsole.modelID);
				modelComboBox.setSelectedItem(mc.getContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageScriptConsole.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		borderComboBox.setSelectedItem(pageScriptConsole.getBorderType());
		widthField.setValue(pageScriptConsole.getWidth() <= 0 ? 300 : pageScriptConsole.getWidth());
		heightField.setValue(pageScriptConsole.getHeight() <= 0 ? 300 : pageScriptConsole.getHeight());
		okButton.setEnabled(modelComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.dispose();
				cancel = false;
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(okListener);
		p.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this script console will interact with.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(300, 100, 1000);
		widthField.setToolTipText("Type in an integer to set the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 3
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(300, 100, 800);
		heightField.setToolTipText("Type in an integer to set the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 4
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setPreferredSize(new Dimension(200, 24));
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this button.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 4, 2, 5, 5, 15, 5);

	}

}
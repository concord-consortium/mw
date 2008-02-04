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

package org.concord.mw2d.models;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.concord.modeler.process.Loadable;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.mw2d.MDView;
import org.concord.mw2d.models.MDModel;

class AutomaticalReminderControlPanel extends JPanel {

	private MDModel model;

	private JCheckBox pauseSwitch;
	private JCheckBox repeatButton;
	private IntegerTextField intervalField;
	private JLabel label1, label2;
	private JTextArea reminderArea;

	AutomaticalReminderControlPanel() {

		super(new BorderLayout());

		JPanel p = new JPanel();
		add(p, BorderLayout.CENTER);

		String s = MDView.getInternationalText("InvokeAfter");
		label1 = new JLabel(s != null ? s : "Invoke after");
		p.add(label1);
		intervalField = new IntegerTextField(2000, 0, 10000000);
		intervalField.setPreferredSize(new Dimension(80, 20));
		p.add(intervalField);
		s = MDView.getInternationalText("Femtosecond");
		label2 = new JLabel(s != null ? s : "femtoseconds");
		p.add(label2);

		s = MDView.getInternationalText("Periodically");
		repeatButton = new JCheckBox(s != null ? s : "periodically");
		p.add(repeatButton);

		s = MDView.getInternationalText("SetMessageBelow");
		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Set message below:"));
		add(p, BorderLayout.SOUTH);

		reminderArea = new PastableTextArea();
		reminderArea.setBorder(BorderFactory.createLoweredBevelBorder());
		reminderArea.setPreferredSize(new Dimension(300, 200));
		p.add(reminderArea, BorderLayout.CENTER);

		s = MDView.getInternationalText("Apply");
		pauseSwitch = new JCheckBox(s != null ? s : "Apply");
		pauseSwitch.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (model == null)
					return;
				final boolean b = e.getStateChange() == ItemEvent.SELECTED;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						enableEditor(b);
						model.enableReminder(b);
					}
				});
			}
		});

	}

	int getIntervalTime() {
		return intervalField.getValue();
	}

	boolean isRepeatable() {
		return repeatButton.isSelected();
	}

	String getMessage() {
		return reminderArea.getText();
	}

	private void enableEditor(boolean b) {
		label1.setEnabled(b);
		label2.setEnabled(b);
		repeatButton.setEnabled(b);
		intervalField.setEnabled(b);
		reminderArea.setEnabled(b);
	}

	private void setup(MDModel model) {
		if (model.isReminderEnabled()) {
			selectWithoutNotifyingListeners(pauseSwitch, true);
			enableEditor(true);
		}
		else {
			selectWithoutNotifyingListeners(pauseSwitch, false);
			enableEditor(false);
		}
		intervalField.setValue((int) (model.reminder.getInterval() * model.getTimeStep()));
		selectWithoutNotifyingListeners(repeatButton, model.reminder.getLifetime() == Loadable.ETERNAL);
		reminderArea.setText(model.reminderMessage);
	}

	public JDialog createDialog(Component parent, MDModel model) {

		this.model = model;

		if (model == null)
			return null;

		String s = MDView.getInternationalText("AutomaticReminder");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s : "Automatic Reminder",
				true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.getContentPane().add(this, BorderLayout.CENTER);

		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		};
		intervalField.setAction(action);

		setup(model);

		final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		d.getContentPane().add(p, BorderLayout.SOUTH);

		p.add(pauseSwitch);

		JButton b = new JButton(action);
		s = MDView.getInternationalText("CloseButton");
		b.setText(s != null ? s : "Close");
		p.add(b);

		d.pack();

		Point point = parent.getLocationOnScreen();
		Dimension dim0 = parent.getPreferredSize();
		Dimension dim1 = d.getPreferredSize();
		d.setLocation(point.x + (dim0.width - dim1.width) / 2, point.y + (dim0.height - dim1.height) / 2);

		d.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (intervalField.isEnabled()) {
							intervalField.selectAll();
							intervalField.requestFocus();
						}
					}
				});
			}
		});

		return d;

	}

	static void selectWithoutNotifyingListeners(AbstractButton ab, boolean selected) {

		if (ab == null)
			return;

		ItemListener[] il = ab.getItemListeners();
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				ab.removeItemListener(il[i]);
		}
		ActionListener[] al = ab.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				ab.removeActionListener(al[i]);
		}

		ab.setSelected(selected);

		if (il != null) {
			for (int i = 0; i < il.length; i++)
				ab.addItemListener(il[i]);
		}
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				ab.addActionListener(al[i]);
		}

	}

}
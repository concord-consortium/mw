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
package org.concord.modeler.process;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;

/**
 * @author Charles Xie
 * 
 */
class TaskCreator {

	private final static byte NAME_OK = 0;
	private final static byte NAME_ERROR = 1;
	private final static byte NAME_EXISTS = 2;

	private JPanel contentPane;
	private JTextArea scriptArea;
	private JTextField nameField, descriptionField;
	private IntegerTextField intervalField, lifetimeField;
	private JCheckBox permanentCheckBox;
	private JLabel lifetimeLabel;
	private JSpinner prioritySpinner;
	private JDialog dialog;
	private Job job;
	private Loadable task;
	private JTable table;
	private int row;

	TaskCreator(Job j) {

		job = j;

		contentPane = new JPanel(new BorderLayout(5, 5));

		scriptArea = new PastableTextArea();
		String s = JobTable.getInternationalText("TaskScripts");
		scriptArea.setBorder(BorderFactory.createTitledBorder((s != null ? s : "Scripts") + ":"));
		JScrollPane scroller = new JScrollPane(scriptArea);
		scroller.setPreferredSize(new Dimension(600, 400));
		contentPane.add(scroller, BorderLayout.CENTER);

		JPanel topPanel = new JPanel(new BorderLayout());
		contentPane.add(topPanel, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		topPanel.add(p1, BorderLayout.CENTER);

		s = JobTable.getInternationalText("TaskName");
		p1.add(new JLabel((s != null ? s : "Name") + ": "));
		nameField = new JTextField("Untitled");
		nameField.setColumns(10);
		p1.add(nameField);

		s = JobTable.getInternationalText("TaskDescription");
		p1.add(new JLabel((s != null ? s : "Description") + ": "));
		descriptionField = new JTextField();
		descriptionField.setColumns(50);
		p1.add(descriptionField);

		final JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		topPanel.add(p2, BorderLayout.SOUTH);

		s = JobTable.getInternationalText("Priority");
		p2.add(new JLabel((s != null ? s : "Priority") + ": "));
		prioritySpinner = new JSpinner(new SpinnerNumberModel(Thread.NORM_PRIORITY, 1, 5, 1));
		p2.add(prioritySpinner);

		s = JobTable.getInternationalText("Interval");
		p2.add(new JLabel((s != null ? s : "Interval") + ": "));
		intervalField = new IntegerTextField(10, 1, 100000, 6);
		p2.add(intervalField);

		s = JobTable.getInternationalText("Permanent");
		permanentCheckBox = new JCheckBox(s != null ? s : "Permanent");
		permanentCheckBox.setSelected(true);
		permanentCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					p2.remove(lifetimeLabel);
					p2.remove(lifetimeField);
				}
				else {
					p2.add(lifetimeLabel);
					p2.add(lifetimeField);
					lifetimeField.setValue(100000);
				}
				p2.validate();
				p2.repaint();
			}
		});
		p2.add(permanentCheckBox);

		s = JobTable.getInternationalText("Lifetime");
		lifetimeLabel = new JLabel((s != null ? s : "Lifetime") + ": ");
		lifetimeField = new IntegerTextField(10000, 1, Loadable.ETERNAL, 6);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		s = JobTable.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (ok()) {
				case NAME_OK:
					job.notifyChange();
					dialog.dispose();
					break;
				case NAME_EXISTS:
					JOptionPane.showMessageDialog(dialog, "A task with the name \"" + nameField.getText()
							+ "\" already exists.", "Duplicate Task Name", JOptionPane.ERROR_MESSAGE);
					break;
				case NAME_ERROR:
					JOptionPane.showMessageDialog(dialog,
							"A task name must contain at least four characters in [a-zA-Z_0-9] (no space allowed): \""
									+ nameField.getText() + "\".", "Task Name Error", JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
		});
		buttonPanel.add(button);

		s = JobTable.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		buttonPanel.add(button);

	}

	private byte ok() {

		String name = nameField.getText();
		if (!name.matches("\\w{4,}")) {
			return NAME_ERROR;
		}
		if (task == null) {
			if (job.containsName(name))
				return NAME_EXISTS;
			Loadable l = new AbstractLoadable(intervalField.getValue()) {
				public void execute() {
					job.runScript(getScript());
					if (job.getIndexOfStep() >= getLifetime()) {
						setCompleted(true);
					}
				}
			};
			l.setSystemTask(false);
			l.setPriority((Integer) prioritySpinner.getValue());
			l.setLifetime(permanentCheckBox.isSelected() ? Loadable.ETERNAL : lifetimeField.getValue());
			l.setName(name);
			l.setDescription(descriptionField.getText());
			l.setScript(scriptArea.getText());
			job.add(l);
			job.processPendingRequests();
		}
		else {
			task.setPriority((Integer) prioritySpinner.getValue());
			task.setInterval(intervalField.getValue());
			task.setLifetime(permanentCheckBox.isSelected() ? Loadable.ETERNAL : lifetimeField.getValue());
			task.setDescription(descriptionField.getText());
			task.setScript(scriptArea.getText());
			if (!task.getName().equals(name)) {
				task.setName(name);
				table.setValueAt(name, row, 2);
			}
		}
		return NAME_OK;

	}

	void show(JTable table, Loadable l, int row) {
		this.table = table;
		this.row = row;
		if (dialog == null) {
			dialog = new JDialog(JOptionPane.getFrameForComponent(table), "Creating a Task", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(table);
		}
		if (l != null) {
			dialog.setTitle("Edit a Task");
			nameField.setText(l.getName());
			descriptionField.setText(l.getDescription());
			scriptArea.setText(l.getScript());
			scriptArea.setCaretPosition(0);
			prioritySpinner.setValue(l.getPriority());
			permanentCheckBox.setSelected(l.getLifetime() == Loadable.ETERNAL);
			lifetimeField.setValue(l.getLifetime());
			intervalField.setValue(l.getInterval());
		}
		else {
			dialog.setTitle("Create a Task");
			nameField.setText("Untitled");
			descriptionField.setText(null);
			scriptArea.setText(null);
			prioritySpinner.setValue(Thread.NORM_PRIORITY);
			lifetimeField.setValue(Loadable.ETERNAL);
			intervalField.setValue(10);
			permanentCheckBox.setSelected(true);
		}
		task = l;
		dialog.setVisible(true);
	}

}

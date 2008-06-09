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
package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.BitSet;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ListOrMapTransferHandler;
import org.concord.modeler.ui.PastableTextArea;
import org.myjmol.api.Scene;

/**
 * @author Charles Xie
 * 
 */
class ItineraryManager {

	boolean returnHome;
	private BitSet deletion;
	private int selectedIndex;
	private JmolContainer jmolContainer;

	// GUI
	private JTextArea departInfoArea;
	private JTextArea arriveInfoArea;
	private JTextArea departScriptArea;
	private JTextArea arriveScriptArea;
	private JSpinner transitionTimeSpinner;
	private JSpinner stopTimeSpinner;
	private JCheckBox deleteCheckBox;

	ItineraryManager(JmolContainer jmolContainer) {
		this.jmolContainer = jmolContainer;
	}

	JDialog getEditor() {

		if (jmolContainer.scenes.isEmpty())
			return null;

		String s = JmolContainer.getInternationalText("EditItinerary");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(jmolContainer), s != null ? s : "Itinerary",
				true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		d.setContentPane(contentPane);

		selectedIndex = Math.max(0, jmolContainer.getCurrentSceneIndex());
		int n = jmolContainer.scenes.size();
		deletion = new BitSet(n);
		s = JmolContainer.getInternationalText("DeleteAtClosingWindow");
		deleteCheckBox = new JCheckBox(s != null ? s : "Delete at closing window");
		transitionTimeSpinner = new JSpinner(new SpinnerNumberModel(new Integer(1), new Integer(0), new Integer(60),
				new Integer(1)));
		stopTimeSpinner = new JSpinner(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(60),
				new Integer(1)));
		departInfoArea = new PastableTextArea();
		arriveInfoArea = new PastableTextArea();
		departScriptArea = new PastableTextArea();
		arriveScriptArea = new PastableTextArea();

		DefaultListModel listModel = new DefaultListModel();
		s = JmolContainer.getInternationalText("Scene");
		for (int i = 0; i < n; i++)
			listModel.addElement((s != null ? s : "Scene") + " #" + (i + 1));
		final JList sceneList = new JList(listModel);
		sceneList.setSelectedIndex(selectedIndex);
		sceneList.setCellRenderer(new MyListCellRenderer());
		sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sceneList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				if (selectedIndex >= 0) { // save the current entries in the text areas
					setDepartInfo();
					setArriveInfo();
					setDepartScript();
					setArriveScript();
				}
				selectedIndex = sceneList.getSelectedIndex();
				if (selectedIndex >= 0) {
					populate();
				}
			}
		});
		// drag and drop support
		sceneList.setTransferHandler(new ListOrMapTransferHandler(jmolContainer.scenes));
		sceneList.setDragEnabled(true);
		JScrollPane scroller = new JScrollPane(sceneList);
		scroller.setPreferredSize(new Dimension(150, 200));
		contentPane.add(scroller, BorderLayout.WEST);

		JPanel editPanel = new JPanel(new BorderLayout(5, 5));
		contentPane.add(editPanel, BorderLayout.CENTER);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		editPanel.add(panel, BorderLayout.NORTH);

		s = JmolContainer.getInternationalText("TransitionTime");
		panel.add(new JLabel(s != null ? s : "Transition time (s)"));
		transitionTimeSpinner.setToolTipText("Time in seconds to fly to this scene");
		transitionTimeSpinner.setValue(new Integer((jmolContainer.scenes.get(selectedIndex)).getTransitionTime()));
		transitionTimeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				short i = ((Integer) transitionTimeSpinner.getValue()).shortValue();
				jmolContainer.scenes.get(selectedIndex).setTransitionTime(i);
			}
		});
		panel.add(transitionTimeSpinner);

		s = JmolContainer.getInternationalText("StopTime");
		panel.add(new JLabel(s != null ? s : "Stop time (s)"));
		stopTimeSpinner.setToolTipText("Time in seconds to stop at this scene");
		stopTimeSpinner.setValue(new Integer((jmolContainer.scenes.get(selectedIndex)).getStopTime()));
		stopTimeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				short i = ((Integer) stopTimeSpinner.getValue()).shortValue();
				jmolContainer.scenes.get(selectedIndex).setStopTime(i);
			}
		});
		panel.add(stopTimeSpinner);

		deleteCheckBox.setToolTipText("Delete this scene from the itinerary");
		deleteCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				deletion.set(selectedIndex, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		panel.add(deleteCheckBox);

		JTabbedPane tabbedPane = new JTabbedPane();
		editPanel.add(tabbedPane, BorderLayout.CENTER);

		panel = new JPanel(new SpringLayout());
		s = JmolContainer.getInternationalText("Information");
		tabbedPane.addTab(s != null ? s : "Information", panel);

		s = JmolContainer.getInternationalText("ArrivalInformation");
		panel.add(new JLabel(s != null ? s : "Information displayed at arrival:"));
		arriveInfoArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setArriveInfo();
			}
		});
		scroller = new JScrollPane(arriveInfoArea);
		scroller.setPreferredSize(new Dimension(350, 100));
		panel.add(scroller);
		s = JmolContainer.getInternationalText("DepartureInformation");
		panel.add(new JLabel(s != null ? s : "Information displayed at departure:"));
		departInfoArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setDepartInfo();
			}
		});
		scroller = new JScrollPane(departInfoArea);
		scroller.setPreferredSize(new Dimension(300, 100));
		panel.add(scroller);
		ModelerUtilities.makeCompactGrid(panel, 4, 1, 5, 5, 10, 2);

		panel = new JPanel(new SpringLayout());
		s = JmolContainer.getInternationalText("Scripts");
		tabbedPane.addTab(s != null ? s : "Scripts", panel);

		s = JmolContainer.getInternationalText("ArrivalScripts");
		panel.add(new JLabel(s != null ? s : "Jmol scripts to run when arriving:"));
		arriveScriptArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setArriveScript();
			}
		});
		scroller = new JScrollPane(arriveScriptArea);
		scroller.setPreferredSize(new Dimension(300, 100));
		panel.add(scroller);

		s = JmolContainer.getInternationalText("DepartureScripts");
		panel.add(new JLabel(s != null ? s : "Jmol scripts to run when departing:"));
		departScriptArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setDepartScript();
			}
		});
		scroller = new JScrollPane(departScriptArea);
		scroller.setPreferredSize(new Dimension(350, 100));
		panel.add(scroller);

		ModelerUtilities.makeCompactGrid(panel, 4, 1, 5, 5, 10, 2);

		panel = new JPanel(new BorderLayout(5, 5));
		contentPane.add(panel, BorderLayout.SOUTH);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.NORTH);
		s = JmolContainer.getInternationalText("DragAndDropToReOrderScenes");
		p.add(new JLabel(s != null ? s : "Drag and drop an item on the left panel to re-order scenes."));

		p = new JPanel();
		panel.add(p, BorderLayout.CENTER);
		s = JmolContainer.getInternationalText("StayWithThisScene");
		JButton button = new JButton(s != null ? s : "Stay with this scene");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				returnHome = false;
				if (deletion.cardinality() > 0) {
					String s = "";
					for (int i = 0; i < deletion.length(); i++) {
						if (deletion.get(i))
							s += (i + 1) + ", ";
					}
					s = s.substring(0, s.length() - 2);
					if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(d),
							"Are you sure you want to delete #" + s + "?", "Deletion confirmation",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
						return;
				}
				finalizeDeal();
				d.dispose();
			}
		});
		p.add(button);

		s = JmolContainer.getInternationalText("BackToStartingScene");
		button = new JButton(s != null ? s : "Back to the starting scene");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				returnHome = true;
				if (deletion.cardinality() > 0) {
					String s = "";
					for (int i = 0; i < deletion.length(); i++) {
						if (deletion.get(i))
							s += (i + 1) + ", ";
					}
					s = s.substring(0, s.length() - 2);
					if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(d),
							"Are you sure you want to delete #" + s + "?", "Deletion confirmation",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
						return;
				}
				finalizeDeal();
				d.dispose();
			}
		});
		p.add(button);

		populate();
		d.pack();

		return d;

	}

	private void setDepartInfo() {
		Scene scene = jmolContainer.scenes.get(selectedIndex);
		String s = departInfoArea.getText();
		if (s == null || s.trim().equals("")) {
			scene.setDepartInformation(null);
		}
		else {
			scene.setDepartInformation(s);
		}
	}

	private void setArriveInfo() {
		Scene scene = jmolContainer.scenes.get(selectedIndex);
		String s = arriveInfoArea.getText();
		if (s == null || s.trim().equals("")) {
			scene.setArriveInformation(null);
		}
		else {
			scene.setArriveInformation(s);
		}
	}

	private void setDepartScript() {
		Scene scene = jmolContainer.scenes.get(selectedIndex);
		String s = departScriptArea.getText();
		if (s == null || s.trim().equals("")) {
			scene.setDepartScript(null);
		}
		else {
			scene.setDepartScript(s);
		}
	}

	private void setArriveScript() {
		Scene scene = jmolContainer.scenes.get(selectedIndex);
		String s = arriveScriptArea.getText();
		if (s == null || s.trim().equals("")) {
			scene.setArriveScript(null);
		}
		else {
			scene.setArriveScript(s);
		}
	}

	private void populate() {
		Scene scene = jmolContainer.scenes.get(selectedIndex);
		ModelerUtilities.setWithoutNotifyingListeners(deleteCheckBox, deletion.get(selectedIndex));
		transitionTimeSpinner.setValue(new Integer(scene.getTransitionTime()));
		stopTimeSpinner.setValue(new Integer(scene.getStopTime()));
		departInfoArea.setText(scene.getDepartInformation());
		arriveInfoArea.setText(scene.getArriveInformation());
		departScriptArea.setText(scene.getDepartScript());
		arriveScriptArea.setText(scene.getArriveScript());
		jmolContainer.moveToScene(selectedIndex, true);
	}

	private void finalizeDeal() {

		int n = jmolContainer.scenes.size();
		if (n == 0)
			return;

		for (int i = n - 1; i >= 0; i--) {
			if (deletion.get(i)) {
				jmolContainer.scenes.remove(i);
			}
		}

	}

}
/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelCommunicator;
import org.concord.modeler.PageButton;
import org.concord.modeler.PageCheckBox;
import org.concord.modeler.PageComboBox;
import org.concord.modeler.PagePlugin;
import org.concord.modeler.PageRadioButton;
import org.concord.modeler.PageSlider;
import org.concord.modeler.PageSpinner;
import org.concord.modeler.event.AbstractChange;

class PluginConnector {

	private List<PagePlugin> pluginList;
	private Map<Integer, List<ModelCommunicator>> listenerMap;

	PluginConnector() {
		pluginList = Collections.synchronizedList(new ArrayList<PagePlugin>());
		listenerMap = new LinkedHashMap<Integer, List<ModelCommunicator>>();
	}

	boolean isEmpty() {
		return pluginList.isEmpty();
	}

	void connect() {
		if (isEmpty())
			return;
		if (!listenerMap.isEmpty())
			connectListeners();
	}

	private void connectListeners() {

		PagePlugin model;
		List<ModelCommunicator> listenerList;
		String name;

		for (Integer id : listenerMap.keySet()) {

			model = pluginList.get(id.intValue());
			listenerList = listenerMap.get(id);

			for (ModelCommunicator listener : listenerList) {

				if (listener instanceof PageSlider) {
					PageSlider slider = (PageSlider) listener;
					name = slider.getName();
					if (name != null) {
						ChangeListener cl = model.getChanges().get(name);
						if (cl instanceof AbstractChange) {
							String tooltip = slider.getToolTipText();
							slider.addChangeListener(cl);
							model.addModelListener(slider);
							if (tooltip != null && !tooltip.trim().equals("")) {
								slider.setToolTipText(tooltip);
							}
							else {
								if (cl instanceof AbstractChange) {
									slider.setToolTipText((String) ((AbstractChange) cl)
											.getProperty(AbstractChange.SHORT_DESCRIPTION));
								}
							}
						}
					}
				}

				else if (listener instanceof PageSpinner) {
					PageSpinner spinner = (PageSpinner) listener;
					name = spinner.getName();
					if (name != null) {
						ChangeListener cl = model.getChanges().get(name);
						if (cl instanceof AbstractChange) {
							String tooltip = spinner.getToolTipText();
							spinner.addChangeListener(cl);
							model.addModelListener(spinner);
							if (tooltip != null && !tooltip.trim().equals("")) {
								spinner.setToolTipText(tooltip);
							}
							else {
								if (cl instanceof AbstractChange) {
									spinner.setToolTipText((String) ((AbstractChange) cl)
											.getProperty(AbstractChange.SHORT_DESCRIPTION));
								}
							}
						}
					}
				}

				else if (listener instanceof PageButton) {
					PageButton button = (PageButton) listener;
					name = button.getName();
					if (name != null) {
						Action a = model.getActions().get(name);
						if (a != null) {
							String text = button.getText();
							String tooltip = button.getToolTipText();
							button.setAction(a);
							button.setText(text);
							if (tooltip != null && !tooltip.trim().equals(""))
								button.setToolTipText(tooltip);
							model.addModelListener(button);
						}
					}
				}

				else if (listener instanceof PageCheckBox) {
					PageCheckBox checkBox = (PageCheckBox) listener;
					name = checkBox.getName();
					if (name != null) {
						Action a = model.getSwitches().get(name);
						if (a != null) {
							String text = checkBox.getText();
							String tooltip = checkBox.getToolTipText();
							checkBox.setAction(a);
							checkBox.setText(text);
							if (tooltip != null && !tooltip.trim().equals(""))
								checkBox.setToolTipText(tooltip);
							model.addModelListener(checkBox);
						}
					}
				}

				else if (listener instanceof PageComboBox) {
					PageComboBox comboBox = (PageComboBox) listener;
					name = comboBox.getName();
					if (name != null) {
						Action a = model.getChoices().get(name);
						if (a != null) {
							String tooltip = comboBox.getToolTipText();
							a.setEnabled(false);
							comboBox.setAction(a);
							Object o = null;
							if (name.equals("Execute native script") || name.equals("Execute MW script")) {
								o = comboBox.getClientProperty("Script");
								if (o instanceof String) {
									comboBox.setupScripts((String) o);
								}
							}
							o = comboBox.getClientProperty("Options");
							if (o instanceof String)
								comboBox.setOptionGroup((String) o);
							model.addModelListener(comboBox);
							a.setEnabled(true);
							if (tooltip != null && !tooltip.trim().equals(""))
								comboBox.setToolTipText(tooltip);
						}
					}
				}

				else if (listener instanceof PageRadioButton) {
					PageRadioButton radioButton = (PageRadioButton) listener;
					name = radioButton.getName();
					if (name != null) {
						Action a = model.getMultiSwitches().get(name);
						if (a != null) {
							String text = radioButton.getText();
							String tooltip = radioButton.getToolTipText();
							radioButton.setAction(a);
							radioButton.setText(text);
							if (tooltip != null && !tooltip.trim().equals(""))
								radioButton.setToolTipText(tooltip);
							model.addModelListener(radioButton);
						}
					}
				}

			}

		}

	}

	void enroll(PagePlugin p) {
		if (p == null)
			return;
		pluginList.add(p);
	}

	void start() {
		if (pluginList.isEmpty())
			return;
		synchronized (pluginList) {
			for (PagePlugin a : pluginList) {
				a.start();
			}
		}
	}

	void linkModelCommunicator(int id, ModelCommunicator mc) {
		List<ModelCommunicator> x = listenerMap.get(id);
		if (x != null) {
			x.add(mc);
		}
		else {
			x = new ArrayList<ModelCommunicator>();
			x.add(mc);
			listenerMap.put(id, x);
		}
	}

	void clear() {
		pluginList.clear();
		for (List l : listenerMap.values()) {
			l.clear();
		}
		listenerMap.clear();
	}

}
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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.event.ChangeListener;

import org.concord.modeler.PageButton;
import org.concord.modeler.PageCheckBox;
import org.concord.modeler.PageComboBox;
import org.concord.modeler.PageMd3d;
import org.concord.modeler.PageRadioButton;
import org.concord.modeler.PageScriptConsole;
import org.concord.modeler.PageSlider;
import org.concord.modeler.PageSpinner;
import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.event.ModelListener;
import org.concord.modeler.util.FileUtilities;

class Mw3dConnector {

	private Page page;
	private List<PageMd3d> mdList;
	private Map<Integer, List<ModelListener>> listenerMap;

	Mw3dConnector(Page page) {
		listenerMap = new LinkedHashMap<Integer, List<ModelListener>>();
		this.page = page;
	}

	boolean isEmpty() {
		return mdList == null || mdList.isEmpty();
	}

	void loadResources() {
		if (isEmpty())
			return;
		String address = null;
		synchronized (mdList) {
			for (PageMd3d md : mdList) {
				address = FileUtilities.removeSuffix(page.getAddress()) + "$" + md.getIndex() + ".mdd";
				md.input(address, false);
			}
		}
	}

	void finishLoading() {
		if (isEmpty())
			return;
		if (!listenerMap.isEmpty()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					connectListeners();
				}
			});
		}
	}

	private void connectListeners() {

		PageMd3d model;
		List<ModelListener> listenerList;
		String name;

		for (Integer id : listenerMap.keySet()) {

			if (id.intValue() < 0)
				continue;
			model = mdList.get(id.intValue());
			listenerList = listenerMap.get(id);

			for (ModelListener listener : listenerList) {

				if (listener instanceof PageSlider) {
					PageSlider slider = (PageSlider) listener;
					name = slider.getName();
					if (name != null) {
						ChangeListener cl = model.getChanges().get(name);
						if (cl instanceof AbstractChange) {
							slider.addChangeListener(cl);
							model.addModelListener(slider);
							String tooltip = slider.getToolTipText();
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
							spinner.addChangeListener(cl);
							model.addModelListener(spinner);
							String tooltip = spinner.getToolTipText();
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
							model.addModelListener(button);
							if (tooltip != null && !tooltip.trim().equals(""))
								button.setToolTipText(tooltip);
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
							model.addModelListener(checkBox);
							if (tooltip != null && !tooltip.trim().equals(""))
								checkBox.setToolTipText(tooltip);
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
							model.addModelListener(radioButton);
							if (tooltip != null && !tooltip.trim().equals(""))
								radioButton.setToolTipText(tooltip);
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
							if (name.equals("Execute MW script") || name.equals("Execute Jmol script")) {
								o = comboBox.getClientProperty("Script");
								if (o instanceof String) {
									comboBox.setupScripts((String) o);
								}
							}
							else if (!name.equals("Import a model")) {
								o = comboBox.getClientProperty("Selected Index");
								if (o instanceof Integer)
									comboBox.setSelectedIndex(((Integer) o).intValue());
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

				else if (listener instanceof PageScriptConsole) {
					model.addModelListener(listener);
					model.getMolecularModel().addScriptListener((PageScriptConsole) listener);
				}

			}

		}

	}

	void enroll(PageMd3d md) {
		if (md == null)
			return;
		if (mdList == null)
			mdList = Collections.synchronizedList(new ArrayList<PageMd3d>());
		mdList.add(md);
	}

	void linkModelListener(int id, ModelListener ml) {
		List<ModelListener> x = listenerMap.get(id);
		if (x != null) {
			x.add(ml);
		}
		else {
			x = new ArrayList<ModelListener>();
			x.add(ml);
			listenerMap.put(new Integer(id), x);
		}
	}

	void clear() {
		if (mdList != null)
			mdList.clear();
		for (List l : listenerMap.values()) {
			l.clear();
		}
		listenerMap.clear();
	}

}
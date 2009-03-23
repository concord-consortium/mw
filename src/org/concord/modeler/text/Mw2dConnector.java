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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ComponentMaker;
import org.concord.modeler.Model;
import org.concord.modeler.ModelCanvas;
import org.concord.modeler.PageBarGraph;
import org.concord.modeler.PageButton;
import org.concord.modeler.PageCheckBox;
import org.concord.modeler.PageComboBox;
import org.concord.modeler.PageDiffractionInstrument;
import org.concord.modeler.PageDNAScroller;
import org.concord.modeler.PageElectronicStructureViewer;
import org.concord.modeler.PageGauge;
import org.concord.modeler.PageNumericBox;
import org.concord.modeler.PagePhotonSpectrometer;
import org.concord.modeler.PagePotentialHill;
import org.concord.modeler.PagePotentialWell;
import org.concord.modeler.PageRadioButton;
import org.concord.modeler.PageScriptConsole;
import org.concord.modeler.PageSlider;
import org.concord.modeler.PageSpinner;
import org.concord.modeler.PageXYGraph;
import org.concord.modeler.SlideMovie;
import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.ModelListener;
import org.concord.mw2d.StyleConstant;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.ReactionModel;
import org.concord.mw2d.ui.AtomContainer;
import org.concord.mw2d.ui.MDContainer;

class Mw2dConnector {

	private List<ModelCanvas> mdList;
	private Map<Integer, List<ModelListener>> listenerMap;
	private Page page;

	Mw2dConnector(Page page) {
		listenerMap = new LinkedHashMap<Integer, List<ModelListener>>();
		this.page = page;
	}

	boolean isEmpty() {
		return mdList == null || mdList.isEmpty();
	}

	void loadResources() {
		if (isEmpty())
			return;
		MDContainer c;
		synchronized (mdList) {
			for (ModelCanvas mc : mdList) {
				c = mc.getMdContainer();
				removeOldListeners(c.getModel());
				c.setMenuEnabled(false);
				c.setLoading(true);
				mc.loadCurrentResource();
			}
		}
	}

	void finishLoading() {
		if (isEmpty())
			return;
		synchronized (mdList) {
			for (ModelCanvas mc : mdList) {
				mc.loadToolBarButtons();
				mc.getMdContainer().setMenuEnabled(true);
				mc.getMdContainer().setLoading(false);
				if (mc.getMdContainer() instanceof AtomContainer) {
					final AtomContainer ac = (AtomContainer) mc.getMdContainer();
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							if (ac.hasDNAScroller()) {
								ac.getView().setRestraintStyle(StyleConstant.RESTRAINT_GHOST_STYLE);
							}
						}
					});
				}
			}
		}
		if (!listenerMap.isEmpty())
			// NOTE: listeners must be connected as late as possible to wait for other things to complete
			// e.g. a DNA scroller to be installed and the actions related to it be added to the list
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					connectListeners();
				}
			});
	}

	private void removeOldListeners(Model model) {
		List list = model.getMovie().getMovieListeners();
		for (Iterator it = list.iterator(); it.hasNext();) {
			if (it.next() != ((SlideMovie) (model.getMovie())).getMovieSlider())
				it.remove();
		}
		list = model.getModelListeners();
		if (list != null)
			list.clear();
	}

	private void connectListeners() {

		List<ModelListener> listenerList;
		String name;
		Model model;

		for (Integer id : listenerMap.keySet()) {

			if (id == -1)
				continue;
			model = page.getComponentPool().get(id).getMdContainer().getModel();
			listenerList = listenerMap.get(id);

			for (ModelListener listener : listenerList) {

				if (listener instanceof PageSlider) {
					PageSlider slider = (PageSlider) listener;
					name = slider.getName();
					if (name != null) {
						ChangeListener cl = model.getChanges().get(name);
						if (cl != null) {
							slider.addChangeListener(cl);
							model.addModelListener(slider);
							String tooltip = slider.getToolTipText(); // store the tool tip
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
						if (cl != null) {
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
						Action act = model.getActions().get(name);
						if (act != null) {
							/* restore the original text because setAction() may change the text */
							String text = button.getText();
							String tooltip = button.getToolTipText();
							button.setAction(act);
							button.setText(text);
							if (tooltip != null && !tooltip.trim().equals("")) {
								button.setToolTipText(tooltip);
							}
							Object increment = button.getClientProperty("increment");
							if (increment != null)
								act.putValue("increment", increment);
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
							if (tooltip != null && !tooltip.trim().equals("")) {
								checkBox.setToolTipText(tooltip);
							}
							model.addModelListener(checkBox);
						}
					}
				}

				else if (listener instanceof PageRadioButton) {
					PageRadioButton radioButton = (PageRadioButton) listener;
					name = radioButton.getName();
					if (name != null) {
						Action act = model.getMultiSwitches().get(name);
						if (act != null) {
							String text = radioButton.getText();
							String tooltip = radioButton.getToolTipText();
							radioButton.setAction(act);
							radioButton.setText(text);
							if (tooltip != null && !tooltip.trim().equals("")) {
								radioButton.setToolTipText(tooltip);
							}
							model.addModelListener(radioButton);
						}
					}
				}

				else if (listener instanceof PageComboBox) {
					PageComboBox comboBox = (PageComboBox) listener;
					name = comboBox.getName();
					if (name != null) {
						Action a = model.getChoices().get(name);
						if (a != null) {
							a.setEnabled(false);
							String tooltip = comboBox.getToolTipText();
							comboBox.setAction(a);
							if (tooltip != null && !tooltip.trim().equals(""))
								comboBox.setToolTipText(tooltip);
							Object o = null;
							if (ComponentMaker.isScriptActionKey(name)) {
								o = comboBox.getClientProperty("Script");
								if (o instanceof String) {
									comboBox.setupScripts((String) o);
								}
							}
							else if (!name.equals("Import a model")) {
								o = comboBox.getClientProperty("Selected Index");
								if (o instanceof Integer) {
									comboBox.setSelectedIndex(((Integer) o).intValue());
								}
							}
							o = comboBox.getClientProperty("Options");
							if (o instanceof String)
								comboBox.setOptionGroup((String) o);
							model.addModelListener(comboBox);
							a.setEnabled(true);
						}
					}
				}

				else if (listener instanceof PageNumericBox) {
					PageNumericBox box = (PageNumericBox) listener;
					model.addModelListener(box);
					if (!model.getRecorderDisabled())
						model.getMovie().addMovieListener(box);
				}

				else if (listener instanceof PageBarGraph) {
					PageBarGraph bg = (PageBarGraph) listener;
					model.addModelListener(bg);
					if (!model.getRecorderDisabled())
						model.getMovie().addMovieListener(bg);
				}

				else if (listener instanceof PageGauge) {
					PageGauge g = (PageGauge) listener;
					model.addModelListener(g);
					if (!model.getRecorderDisabled())
						model.getMovie().addMovieListener(g);
				}

				else if (listener instanceof PageXYGraph) {
					final PageXYGraph xyg = (PageXYGraph) listener;
					model.addModelListener(xyg);
					// xyg.modelUpdate(new ModelEvent(model, ModelEvent.MODEL_CHANGED));
					xyg.modelUpdate(new ModelEvent(model, ModelEvent.MODEL_INPUT));
					if (!model.getRecorderDisabled())
						model.getMovie().addMovieListener(xyg);
					// INTERESTING!! putting the repaint request at the end of the ATW queue fixes
					// the problem of incomplete painting.
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							xyg.repaint();
						}
					});
				}

				else if (listener instanceof PagePotentialWell) {
					model.addModelListener(listener);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
				}

				else if (listener instanceof PagePotentialHill) {
					PagePotentialHill h = (PagePotentialHill) listener;
					model.addModelListener(h);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
					(((ReactionModel) model).getType()).addPropertyChangeListener(h);
				}

				else if (listener instanceof PageDNAScroller) {
					PageDNAScroller dnaScroller = (PageDNAScroller) listener;
					model.addModelListener(dnaScroller);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
				}

				else if (listener instanceof PageElectronicStructureViewer) {
					PageElectronicStructureViewer s = (PageElectronicStructureViewer) listener;
					model.addModelListener(s);
					MolecularModel mm = (MolecularModel) model;
					s.setElement(mm.getElement(s.getElementID()));
					s.addParameterChangeListener(mm);
					s.scaleViewer();
					mm.addUpdateListener(s);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
				}

				else if (listener instanceof PageDiffractionInstrument) {
					PageDiffractionInstrument diffractionInstrument = (PageDiffractionInstrument) listener;
					model.addModelListener(diffractionInstrument);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
				}

				else if (listener instanceof PagePhotonSpectrometer) {
					PagePhotonSpectrometer spectrometer = (PagePhotonSpectrometer) listener;
					model.addModelListener(spectrometer);
					model.notifyModelListeners(new ModelEvent(model, ModelEvent.MODEL_INPUT));
				}

				else if (listener instanceof PageScriptConsole) {
					model.addModelListener(listener);
					((MDModel) model).addScriptListener((PageScriptConsole) listener);
				}

			}

		}

	}

	void enroll(ModelCanvas mc) {
		if (mc == null)
			return;
		if (mdList == null)
			mdList = Collections.synchronizedList(new ArrayList<ModelCanvas>());
		mdList.add(mc);
	}

	void linkModelListener(int id, ModelListener ml) {
		List<ModelListener> x = listenerMap.get(id);
		if (x != null) {
			x.add(ml);
		}
		else {
			x = new ArrayList<ModelListener>();
			x.add(ml);
			listenerMap.put(id, x);
		}
	}

	void clear() {
		if (mdList != null)
			mdList.clear();
		listenerMap.clear();
	}

}
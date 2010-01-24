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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.concord.modeler.text.Page;
import org.concord.mw2d.ui.AtomContainer;
import org.concord.mw2d.ui.ChemContainer;
import org.concord.mw2d.ui.GBContainer;
import org.concord.mw2d.ui.MDContainer;
import org.concord.mw2d.models.MDModel;

/**
 * <p>
 * This implementation is stupid, but here it is ......
 * </p>
 * 
 * <p>
 * In general, page components that are expensive to create should be created only when they are going to be needed, and
 * once created, the instances should be reused as much as possible. The user should never be allowed to create too many
 * expensive components. Components that use threads to run must turn off their threads when the user leaves a page.
 * </p>
 * 
 * <p>
 * The <code>ComponentPool</code> provides these functionalities.
 * </p>
 * 
 * <p>
 * In order for a component to be reused by different pages, its state must be storable and restorable. The contract is
 * that the state of a component must be stored in its <code>toString()</code> method.
 * </p>
 * 
 * @author Charles Xie
 */

public class ComponentPool {

	private final static int MOLECULAR_MODEL_1 = 0;
	private final static int MOLECULAR_MODEL_2 = 1;
	private final static int GB_MODEL_1 = 2;
	private final static int GB_MODEL_2 = 3;
	private final static int REACTION_MODEL_1 = 4;
	private final static int REACTION_MODEL_2 = 5;

	private static Color selectionColor = Color.blue;
	private Map<Integer, ModelCanvas> map;
	private AtomContainer[] atomContainer;
	private ChemContainer[] chemContainer;
	private GBContainer[] gbContainer;
	private boolean lazyInit = true;
	private Page page;

	class NotModelException extends IllegalArgumentException {
		NotModelException() {
			super("Input must be a ModelCanvas instance");
		}
	}

	public ComponentPool(Page page) {

		// MUST use a tree map to maintain order???
		map = Collections.synchronizedMap(new TreeMap<Integer, ModelCanvas>());

		MDContainer.setApplet(Page.isApplet());
		if (!Page.isApplet())
			MDContainer.setPreferences(Initializer.sharedInstance().getPreferences());

		this.page = page;
		atomContainer = new AtomContainer[2];
		gbContainer = new GBContainer[2];
		chemContainer = new ChemContainer[2];

		if (!lazyInit) {
			for (int i = 0; i < 2; i++) {
				initAtomContainer(i);
				initGBContainer(i);
				initChemContainer(i);
			}
			map.put(MOLECULAR_MODEL_1, new ModelCanvas(atomContainer[0]));
			map.put(MOLECULAR_MODEL_2, new ModelCanvas(atomContainer[1]));
			map.put(GB_MODEL_1, new ModelCanvas(gbContainer[0]));
			map.put(GB_MODEL_2, new ModelCanvas(gbContainer[1]));
			map.put(REACTION_MODEL_1, new ModelCanvas(chemContainer[0]));
			map.put(REACTION_MODEL_2, new ModelCanvas(chemContainer[1]));
		}

	}

	/** When the current instance is closed, destroy this component to prevent memory leaks. */
	public void destroy() {
		synchronized (map) {
			for (ModelCanvas mc : map.values()) {
				mc.recycle();
			}
		}
		map.clear();
		for (int i = 0; i < 2; i++) {
			if (atomContainer[i] != null) {
				if (page.getEditor() != null)
					atomContainer[i].getModel().removePageComponentListener(page.getEditor());
				if (page.getSaveReminder() != null)
					atomContainer[i].getModel().removePageComponentListener(page.getSaveReminder());
				atomContainer[i].destroy();
			}
			if (chemContainer[i] != null) {
				if (page.getEditor() != null)
					chemContainer[i].getModel().removePageComponentListener(page.getEditor());
				if (page.getSaveReminder() != null)
					chemContainer[i].getModel().removePageComponentListener(page.getSaveReminder());
				chemContainer[i].destroy();
			}
			if (gbContainer[i] != null) {
				if (page.getEditor() != null)
					gbContainer[i].getModel().removePageComponentListener(page.getEditor());
				if (page.getSaveReminder() != null)
					gbContainer[i].getModel().removePageComponentListener(page.getSaveReminder());
				gbContainer[i].destroy();
			}
		}
		page = null;
	}

	/**
	 * This method is called upon inserting a new container before an existing one, or removing the first container when
	 * there are two (hence the containers need to be re-indexified).
	 */
	public void processInsertionOrRemoval(boolean insert, String type) {
		if (type == null)
			return;
		if (type.endsWith("AtomContainer")) {
			ModelCanvas mc1 = map.get(MOLECULAR_MODEL_1);
			ModelCanvas mc2 = map.get(MOLECULAR_MODEL_2);
			if (insert) {
				if (mc1 != null && mc2 == null) { // where only one container has been initialized
					map.put(MOLECULAR_MODEL_2, mc1);
					map.remove(MOLECULAR_MODEL_1);
				}
				else if (mc1 != null && mc2 != null) { // where two containers have been initialized
					map.put(MOLECULAR_MODEL_1, mc2);
					map.put(MOLECULAR_MODEL_2, mc1);
				}
			}
			else {
				if (mc1 != null && mc2 != null) {
					map.put(MOLECULAR_MODEL_1, mc2);
					map.remove(MOLECULAR_MODEL_2);
				}
			}
		}
		else if (type.endsWith("GBContainer")) {
			ModelCanvas mc1 = map.get(GB_MODEL_1);
			ModelCanvas mc2 = map.get(GB_MODEL_2);
			if (insert) {
				if (mc1 != null && mc2 == null) { // where only one container has been initialized
					map.put(GB_MODEL_2, mc1);
					map.remove(GB_MODEL_1);
				}
				else if (mc1 != null && mc2 != null) { // where two containers have been initialized
					map.put(GB_MODEL_1, mc2);
					map.put(GB_MODEL_2, mc1);
				}
			}
			else {
				if (mc1 != null && mc2 != null) {
					map.put(GB_MODEL_1, mc2);
					map.remove(GB_MODEL_2);
				}
			}
		}
		else if (type.endsWith("ChemContainer")) {
			ModelCanvas mc1 = map.get(REACTION_MODEL_1);
			ModelCanvas mc2 = map.get(REACTION_MODEL_2);
			if (insert) {
				if (mc1 != null && mc2 == null) { // where only one container has been initialized
					map.put(REACTION_MODEL_2, mc1);
					map.remove(REACTION_MODEL_1);
				}
				else if (mc1 != null && mc2 != null) { // where two containers have been initialized
					map.put(REACTION_MODEL_1, mc2);
					map.put(REACTION_MODEL_2, mc1);
				}
			}
			else {
				if (mc1 != null && mc2 != null) {
					map.put(REACTION_MODEL_1, mc2);
					map.remove(REACTION_MODEL_2);
				}
			}
		}
	}

	private void initAtomContainer(int i) {
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		atomContainer[i] = new AtomContainer(Modeler.tapeLength);
		if (page.getEditor() != null) {
			atomContainer[i].setProgressBar(page.getEditor().getStatusBar().getProgressBar());
			atomContainer[i].getModel().addPageComponentListener(page.getEditor());
		}
		if (page.getSaveReminder() != null)
			atomContainer[i].getModel().addPageComponentListener(page.getSaveReminder());
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void initGBContainer(int i) {
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		gbContainer[i] = new GBContainer(Modeler.tapeLength);
		if (page.getEditor() != null) {
			gbContainer[i].setProgressBar(page.getEditor().getStatusBar().getProgressBar());
			gbContainer[i].getModel().addPageComponentListener(page.getEditor());
		}
		if (page.getSaveReminder() != null)
			gbContainer[i].getModel().addPageComponentListener(page.getSaveReminder());
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void initChemContainer(int i) {
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		chemContainer[i] = new ChemContainer(Modeler.tapeLength);
		if (page.getEditor() != null) {
			chemContainer[i].setProgressBar(page.getEditor().getStatusBar().getProgressBar());
			chemContainer[i].getModel().addPageComponentListener(page.getEditor());
		}
		if (page.getSaveReminder() != null)
			chemContainer[i].getModel().addPageComponentListener(page.getSaveReminder());
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void clear() {
		map.clear();
	}

	public ModelCanvas get(Object key) {
		if (!lazyInit)
			return map.get(key);
		ModelCanvas value = map.get(key);
		if (value == null) {
			if (key.equals(MOLECULAR_MODEL_1)) {
				initAtomContainer(0);
				value = new ModelCanvas(atomContainer[0]);
				map.put(MOLECULAR_MODEL_1, value);
			}
			else if (key.equals(MOLECULAR_MODEL_2)) {
				initAtomContainer(1);
				value = new ModelCanvas(atomContainer[1]);
				map.put(MOLECULAR_MODEL_2, value);
			}
			else if (key.equals(GB_MODEL_1)) {
				initGBContainer(0);
				value = new ModelCanvas(gbContainer[0]);
				map.put(GB_MODEL_1, value);
			}
			else if (key.equals(GB_MODEL_2)) {
				initGBContainer(1);
				value = new ModelCanvas(gbContainer[1]);
				map.put(GB_MODEL_2, value);
			}
			else if (key.equals(REACTION_MODEL_1)) {
				initChemContainer(0);
				value = new ModelCanvas(chemContainer[0]);
				map.put(REACTION_MODEL_1, value);
			}
			else if (key.equals(REACTION_MODEL_2)) {
				initChemContainer(1);
				value = new ModelCanvas(chemContainer[1]);
				map.put(REACTION_MODEL_2, value);
			}
			if (value != null)
				value.setPage(page);
		}
		return value;
	}

	public Collection<ModelCanvas> getModels() {
		return map.values();
	}

	public void setSelectionColor(Color c) {
		selectionColor = c;
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	/**
	 * Reset this pool and remove all the model listeners hooked up with the models in this pool. This method should be
	 * called before loading a page to prevent memory leak.
	 */
	public void resetStatus() {
		if (map.isEmpty())
			return;
		synchronized (map) {
			for (ModelCanvas mc : map.values()) {
				if (!mc.isUsed())
					continue;
				final MDContainer c = mc.getMdContainer();
				c.getModel().stop();
				c.getModel().clearMouseScripts();
				c.getModel().clearKeyScripts();
				mc.setUsed(false);
				mc.showBorder(true);
				mc.getMdContainer().setStatusBarShown(true);
				List list = c.getModel().getModelListeners();
				if (list != null)
					list.clear();
				list = c.getModel().getMovie().getMovieListeners();
				if (list != null) {
					for (Iterator j = list.iterator(); j.hasNext();) {
						if (j.next() != ((SlideMovie) (c.getModel().getMovie())).getMovieSlider())
							j.remove();
					}
				}
				// CAUTION: Free-up memory causes problem of inserting a new model in a new page.
				// c.getModel().freeUpMemory();
				c.getModel().clear();
				/*
				 * leave this job to when it is time to repopulate the tool bar EventQueue.invokeLater(new Runnable(){
				 * public void run(){ if(c.getToolBar()!=null) c.getToolBar().removeAll(); if(c.getExpandMenu()!=null)
				 * c.getExpandMenu().removeAll(); c.removeToolbar(); } });
				 */
				if (c instanceof AtomContainer) {
					((AtomContainer) c).disableGridMode();
				}
			}
		}
	}

	/**
	 * get the index of the given model in the store. Return -1 if the model is null, or not contained in this store.
	 */
	public int getIndex(BasicModel m) {
		if (m == null)
			return -1;
		synchronized (map) {
			for (Integer key : map.keySet()) {
				ModelCanvas mc = map.get(key);
				if (mc.getMdContainer().getModel() == m)
					return key.intValue();
			}
		}
		return -1;
	}

	/**
	 * request an unused model of the specified type. Returns null if there is no such type, or all model handles for
	 * this type have been taken.
	 */
	public ModelCanvas request(String type) {

		int count = 0;
		synchronized (map) {
			for (ModelCanvas mc : map.values()) {
				if (mc.getMdContainer().getRepresentationName().equals(type)) {
					if (!mc.isUsed())
						return mc;
					count++;
				}
			}
		}
		if (lazyInit) {
			ModelCanvas mc = null;
			if (type.endsWith("AtomContainer")) {
				if (count == 0) {
					initAtomContainer(0);
					mc = new ModelCanvas(atomContainer[0]);
					mc.setPage(page);
					map.put(MOLECULAR_MODEL_1, mc);
					return mc;
				}
				else if (count == 1) {
					initAtomContainer(1);
					mc = new ModelCanvas(atomContainer[1]);
					mc.setPage(page);
					if (map.get(MOLECULAR_MODEL_2) == null)
						map.put(MOLECULAR_MODEL_2, mc);
					else if (map.get(MOLECULAR_MODEL_1) == null)
						map.put(MOLECULAR_MODEL_1, mc);
					return mc;
				}
			}
			else if (type.endsWith("GBContainer")) {
				if (count == 0) {
					initGBContainer(0);
					mc = new ModelCanvas(gbContainer[0]);
					mc.setPage(page);
					map.put(GB_MODEL_1, mc);
					return mc;
				}
				else if (count == 1) {
					initGBContainer(1);
					mc = new ModelCanvas(gbContainer[1]);
					mc.setPage(page);
					if (map.get(GB_MODEL_2) == null)
						map.put(GB_MODEL_2, mc);
					else if (map.get(GB_MODEL_1) == null)
						map.put(GB_MODEL_1, mc);
					return mc;
				}
			}
			else if (type.endsWith("ChemContainer")) {
				if (count == 0) {
					initChemContainer(0);
					mc = new ModelCanvas(chemContainer[0]);
					mc.setPage(page);
					map.put(REACTION_MODEL_1, mc);
					return mc;
				}
				else if (count == 1) {
					initChemContainer(1);
					mc = new ModelCanvas(chemContainer[1]);
					mc.setPage(page);
					if (map.get(REACTION_MODEL_2) == null)
						map.put(REACTION_MODEL_2, mc);
					else if (map.get(REACTION_MODEL_1) == null)
						map.put(REACTION_MODEL_1, mc);
					return mc;
				}
			}
		}

		return null;

	}

	/** return true if one or more models in this store is used on the page. */
	public boolean isUsed() {
		synchronized (map) {
			for (ModelCanvas mc : map.values()) {
				if (mc.isUsed())
					return true;
			}
		}
		return false;
	}

	/** stop all current inputs to the models in this store */
	public void killCurrentInputs() {
		if (map.isEmpty())
			return;
		synchronized (map) {
			for (ModelCanvas canvas : map.values()) {
				Model model = canvas.getMdContainer().getModel();
				if (model != null)
					model.stopInput();
			}
		}
	}

	/** stop all models that are currently running in this store */
	public void stopAllRunningModels() {
		if (map.isEmpty())
			return;
		synchronized (map) {
			for (ModelCanvas mc : map.values()) {
				if (mc.getMdContainer() instanceof AtomContainer) {
					if (((AtomContainer) mc.getMdContainer()).hasDNAScroller())
						((AtomContainer) mc.getMdContainer()).stopDNAAnimation();
				}
				Model model = mc.getMdContainer().getModel();
				model.haltScriptExecution();
				if (model.getJob() != null) {
					if (!model.getJob().isStopped()) {
						if (model instanceof MDModel) {
							((MDModel) model).stopImmediately();
						}
						else {
							model.stop();
						}
					}
				}
			}
		}
	}

}
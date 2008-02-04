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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.ModelListener;

public interface BasicModel {

	/** input a model from a file. */
	public void input(File f);

	/** input a model from a URL. */
	public void input(URL url);

	/** output a model to a file. */
	public void output(File f);

	/** run this model */
	public void run();

	/** stop running this model */
	public void stop();

	public boolean isRunning();

	/** return the view of this view */
	public JComponent getView();

	public void addModelListener(ModelListener ml);

	public void removeModelListener(ModelListener ml);

	public List<ModelListener> getModelListeners();

	public void notifyModelListeners(ModelEvent e);

	/**
	 * return a set of actions of this model that can be called by an external source. Usually rendered through a
	 * regular button.
	 */
	public Map<String, Action> getActions();

	/**
	 * return a set of changes of this model that can be called by an external source. Usually rendered through a spin
	 * button or slider.
	 */
	public Map<String, ChangeListener> getChanges();

	/**
	 * return a set of boolean switches of this model that can be called by an external source. Usually rendered through
	 * a check box.
	 */
	public Map<String, Action> getSwitches();

	/**
	 * return a set of multi-state switches of this model that can be called by an external source. Usually rendered
	 * through a group of radio buttons.
	 */
	public Map<String, Action> getMultiSwitches();

	/**
	 * return a set of multiple choices of this model that can be called by an external source. Usually rendered through
	 * a combo box.
	 */
	public Map<String, Action> getChoices();

	/**
	 * if this model supports scripts, run the script.
	 * 
	 * @return a description about the result.
	 */
	public String runScript(String script);

	/** if this model supports scripts, halt the current script. */
	public void haltScriptExecution();

}
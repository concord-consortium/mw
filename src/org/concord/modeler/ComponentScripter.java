/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import org.concord.modeler.script.Compiler;

/**
 * @author Charles Xie
 * 
 */
public abstract class ComponentScripter {

	private Thread scriptThread;
	private String name;
	private boolean runInEDT;

	/** You must specify whether or not scripts will be executed in the Event-Dispatching Thread. */
	public ComponentScripter(boolean runInEDT) {
		this.runInEDT = runInEDT;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String runScript(final String script) {
		if (runInEDT) {
			executeScript(script);
		}
		else {
			if (scriptThread != null) {
				scriptThread.interrupt();
			}
			scriptThread = new Thread(new Runnable() {
				public void run() {
					executeScript(script);
				}
			});
			if (name != null)
				scriptThread.setName(name);
			scriptThread.setPriority(Thread.NORM_PRIORITY - 1);
			scriptThread.start();
		}
		return null;
	}

	private void executeScript(String script) {
		String[] command = Compiler.COMMAND_BREAK.split(script);
		if (command.length < 1)
			return;
		for (String ci : command) {
			ci = ci.trim();
			if (ci.equals(""))
				continue;
			if (Compiler.COMMENT.matcher(ci).find())
				continue; // comments
			evalCommand(ci);
		}
	}

	protected abstract void evalCommand(String ci);

}

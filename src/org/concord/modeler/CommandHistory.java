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

import java.util.LinkedList;

/**
 * Implements a queue for a bash-like command history.
 * 
 * 
 * @author Agust\u00ED S\u00E1nchez Furrasola
 * @version $Revision: 1.10 $ 2003-07-28
 * 
 */

final class CommandHistory {

	private LinkedList<String> commandList = new LinkedList<String>();
	private int maxSize;
	private int pos = 0;

	/**
	 * Creates a new instance.
	 * 
	 * @param maxSize
	 *            maximum size for the command queue
	 */
	CommandHistory(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Retrieves the following command from the bottom of the list, updates list position.
	 * 
	 * @return the String value of a command
	 */
	String getCommandUp() {
		if (commandList.size() > 0)
			pos--;
		return getCommand();
	}

	/**
	 * Retrieves the following command from the top of the list, updates list position.
	 * 
	 * @return the String value of a command
	 */
	String getCommandDown() {
		if (commandList.size() > 0)
			pos++;
		return getCommand();
	}

	/**
	 * Calculates the command to return.
	 * 
	 * @return the String value of a command
	 */
	private String getCommand() {
		if (pos == 0)
			return "";
		int size = commandList.size();
		if (size > 0) {
			if (pos == (size + 1)) {
				return ""; // just beyond last one: ""
			}
			else if (pos > size) {
				pos = 1; // roll around to first command
			}
			else if (pos < 0) {
				pos = size; // roll around to last command
			}
			return commandList.get(pos - 1);
		}
		return "";
	}

	/**
	 * Adds a new command to the bottom of the list, resets list position.
	 * 
	 * @param command
	 *            the String value of a command
	 */
	void addCommand(String command) {
		pos = 0;
		commandList.addLast(command);
		if (commandList.size() > maxSize) {
			commandList.removeFirst();
		}
	}

	/**
	 * Resets maximum size of command queue. Cuts off extra commands.
	 * 
	 * @param maxSize
	 *            maximum size for the command queue
	 */
	void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		while (maxSize < commandList.size()) {
			commandList.removeFirst();
		}
	}

	/**
	 * Resets instance.
	 * 
	 * @param maxSize
	 *            maximum size for the command queue
	 */
	void reset(int maxSize) {
		this.maxSize = maxSize;
		commandList = new LinkedList<String>();
	}

}
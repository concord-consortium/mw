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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;

import javax.swing.text.BadLocationException;

import org.concord.modeler.ui.PastableTextPane;

class ConsoleTextPane extends PastableTextPane {

	private CommandHistory commandHistory = new CommandHistory(20);
	private ConsoleDocument consoleDoc;
	private EnterListener enterListener;

	ConsoleTextPane(EnterListener enterListener) {
		super(new ConsoleDocument());
		consoleDoc = (ConsoleDocument) getDocument();
		consoleDoc.setConsoleTextPane(this);
		this.enterListener = enterListener;
	}

	public String getCommandString() {
		String cmd = consoleDoc.getCommandString();
		commandHistory.addCommand(cmd);
		return cmd;
	}

	public void setPrompt() {
		consoleDoc.setPrompt();
	}

	public void appendNewline() {
		consoleDoc.appendNewline();
	}

	public void outputError(final String strError) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				consoleDoc.outputError(strError);
			}
		});
	}

	public void outputErrorForeground(final String strError) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				consoleDoc.outputErrorForeground(strError);
			}
		});
	}

	public void outputEcho(final String strEcho) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				consoleDoc.outputEcho(strEcho);
			}
		});
	}

	public void outputStatus(final String strStatus) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				consoleDoc.outputStatus(strStatus);
			}
		});
	}

	public void enterPressed() {
		if (enterListener != null)
			enterListener.enterPressed();
	}

	public void clearContent() {
		consoleDoc.clearContent();
	}

	/**
	 * Custom key event processing for command history implementation. Captures key up and key down * strokes to call
	 * command history and redefines the same events with control down to allow caret * vertical shift.
	 */
	protected void processKeyEvent(KeyEvent ke) {
		// Id Control key is down, captures events does command history recall and inhibits caret vertical shift.
		if (ke.getKeyCode() == KeyEvent.VK_UP && ke.getID() == KeyEvent.KEY_PRESSED && !ke.isControlDown()) {
			recallCommand(true);
		}
		else if (ke.getKeyCode() == KeyEvent.VK_DOWN && ke.getID() == KeyEvent.KEY_PRESSED && !ke.isControlDown()) {
			recallCommand(false);
		}
		// If Control key is down, redefines the event as if it where a key up or key down stroke without
		// modifiers. This allows to move the caret up and down with no command history recall.
		else if ((ke.getKeyCode() == KeyEvent.VK_DOWN || ke.getKeyCode() == KeyEvent.VK_UP)
				&& ke.getID() == KeyEvent.KEY_PRESSED && ke.isControlDown()) {
			super.processKeyEvent(new KeyEvent((Component) ke.getSource(), ke.getID(), ke.getWhen(), 0, // No modifiers
					ke.getKeyCode(), ke.getKeyChar(), ke.getKeyLocation()));
		}
		// Standard processing for other events.
		else {
			super.processKeyEvent(ke);
		}
	}

	/**
	 * Recall command histoy.
	 * 
	 * @param up -
	 *            history up or down
	 */
	private final void recallCommand(boolean up) {
		String cmd = up ? commandHistory.getCommandUp() : commandHistory.getCommandDown();
		try {
			consoleDoc.replaceCommand(cmd);
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
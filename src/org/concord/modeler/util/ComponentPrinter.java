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

package org.concord.modeler.util;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.PrintableComponent;

public class ComponentPrinter extends AbstractAction {

	private PrintableComponent component;
	private static PrinterJob printerJob;

	public ComponentPrinter(PrintableComponent c, String jobName) {
		super();
		if (printerJob == null) {
			printerJob = PrinterJob.getPrinterJob();
			printerJob.setCopies(1);
		}
		if (jobName != null)
			printerJob.setJobName(jobName);
		setComponent(c);
		putValue(NAME, "Print");
		putValue(SHORT_DESCRIPTION, "Print component");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		putValue(SMALL_ICON, IconPool.getIcon("printer"));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_P, KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

	public void setComponent(PrintableComponent c) {
		component = c;
		printerJob.setJobName(c.getPrintingInfo());
	}

	public PrintableComponent getComponent() {
		return component;
	}

	/** set up the printer page. */
	public void pageSetup() {
		component.setCurrentPageFormat(printerJob.pageDialog(component.getCurrentPageFormat()));
	}

	public void actionPerformed(ActionEvent e) {
		component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		component.setCurrentPageFormat(printerJob.validatePage(component.getCurrentPageFormat()));
		printerJob.setPrintable(component, component.getCurrentPageFormat());
		printerJob.setPageable(component);
		if (printerJob.printDialog()) {
			try {
				printerJob.print();
			}
			catch (PrinterException pe) {
				pe.printStackTrace();
				printerJob.cancel();
			}
		}
		component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		firePropertyChange("printing finished", Boolean.FALSE, Boolean.TRUE);
	}

}
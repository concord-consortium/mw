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

package org.concord.modeler.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;

import javax.swing.JComponent;

import org.concord.modeler.util.PrinterUtilities;

/** A subclass of this abstract class can be printed on a single page. */

public abstract class PrintableComponent extends JComponent implements Pageable, Printable {

	private String printingInfo = "Printing a Component";
	private PageFormat currentPageFormat;

	public PrintableComponent() {
		super();
		currentPageFormat = new PageFormat();
	}

	public void setCurrentPageFormat(PageFormat pageFormat) {
		currentPageFormat = pageFormat;
	}

	public PageFormat getCurrentPageFormat() {
		return currentPageFormat;
	}

	/**
	 * print this component. The graphics is automatically rescaled to fit the size of the selected paper. Always print
	 * when calling this method, even when the component is blank.
	 */
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		Dimension d = PrinterUtilities.scaleToPaper(pageFormat.getOrientation(), pageFormat.getPaper(), getSize());
		BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		paint(bi.createGraphics());
		Graphics2D g2d = (Graphics2D) g;
		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		g2d.drawImage(bi.getScaledInstance(d.width, d.height, Image.SCALE_SMOOTH), 0, 25, d.width, d.height, this);
		g2d.setStroke(new BasicStroke(1.0f));
		g2d.setColor(Color.black);
		g2d.drawRoundRect(0, 25, d.width, d.height, 5, 5);
		g2d.setFont(new Font("Arial", Font.PLAIN, 12));
		g2d.drawString(printingInfo, 0, 20);
		return PAGE_EXISTS;
	}

	/**
	 * always print a component on a single page.
	 * 
	 * @return 1
	 */
	public int getNumberOfPages() {
		return 1;
	}

	public PageFormat getPageFormat(int pageIndex) {
		return currentPageFormat;
	}

	/** this component is printable and is laid out on a single page */
	public Printable getPrintable(int pageIndex) {
		return this;
	}

	public void setPrintingInfo(String s) {
		printingInfo = s;
	}

	public String getPrintingInfo() {
		return printingInfo;
	}

}

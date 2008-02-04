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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.swing.JEditorPane;

/**
 * Render a printable HTML document (portrait layout only). This is a component that knows how to split content into a
 * sequence of pages.
 * 
 * @author Qian Xie
 */

public class PrintableEditor extends JEditorPane implements Pageable, Printable {

	private PageFormat currentPageFormat;
	private Vector<BufferedImage> pages;

	/** creates a new PrintableEditor. The document model is set to null. */
	public PrintableEditor() {
		super();
		currentPageFormat = new PageFormat();
	}

	/**
	 * creates a PrintableEditor based on a string containing a URL specification.
	 * 
	 * @throws java.io.IOException
	 *             if if the URL is null or cannot be accessed
	 */
	public PrintableEditor(String url) throws IOException {
		super(url);
		currentPageFormat = new PageFormat();
	}

	/**
	 * creates a PrintableEditor that has been initialized to the given text. This is a convenience constructor that
	 * calls the setContentType and setText methods.
	 * 
	 * @param type
	 *            MIME type of the given text
	 * @param text
	 *            the text to initialize with
	 */
	public PrintableEditor(String type, String text) {
		super(type, text);
		currentPageFormat = new PageFormat();
	}

	/**
	 * creates a PrintableEditor based on a specified URL for input.
	 * 
	 * @throws java.io.IOException
	 *             if if the URL is null or cannot be accessed
	 */
	public PrintableEditor(URL initialPage) throws IOException {
		super(initialPage);
		currentPageFormat = new PageFormat();
	}

	public void setCurrentPageFormat(PageFormat pageFormat) {
		currentPageFormat = pageFormat;
	}

	public PageFormat getCurrentPageFormat() {
		return currentPageFormat;
	}

	public void split() {
		pages = new Vector<BufferedImage>();
		Dimension size = getSize();
		BufferedImage bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		print(bi.createGraphics());
		int ihpp = imageHeightPerPage();
		int numberOfPages = getNumberOfPages();
		if (numberOfPages <= 1) {
			pages.add(bi);
		}
		else {
			BufferedImage sub;
			for (int i = 0; i < numberOfPages - 1; i++) {
				sub = bi.getSubimage(0, ihpp * i, bi.getWidth(), ihpp);
				pages.add(sub);
			}
			int end = (numberOfPages - 1) * ihpp;
			sub = bi.getSubimage(0, end, bi.getWidth(), bi.getHeight() - end);
			pages.add(sub);
		}
	}

	public void noPage() {
		pages = null;
	}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {

		if (pages == null || pages.isEmpty())
			return NO_SUCH_PAGE;
		int numberOfPages = getNumberOfPages();
		if (pageIndex >= numberOfPages || pageIndex < 0)
			return NO_SUCH_PAGE;

		BufferedImage bi = pages.get(pageIndex);
		Graphics2D g2d = (Graphics2D) g;
		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

		int iw = (int) pageFormat.getImageableWidth();
		int ih = (int) pageFormat.getImageableHeight();

		if (numberOfPages == 1) {
			Dimension d = rescaledDimension();
			g2d.drawImage(bi.getScaledInstance(d.width, d.height - 20, Image.SCALE_SMOOTH), 0, 0, d.width,
					d.height - 20, this);
		}
		else {
			if (pageIndex == numberOfPages - 1) {
				g2d.drawImage(bi.getScaledInstance(iw, (int) (bi.getHeight() / xScale()), Image.SCALE_SMOOTH), 0, 0,
						iw, (int) (bi.getHeight() / xScale()), this);
			}
			else {
				g2d.drawImage(bi.getScaledInstance(iw, ih - 20, Image.SCALE_SMOOTH), 0, 0, iw, ih - 20, this);
			}
		}

		g2d.setFont(new Font("Arial", Font.PLAIN, 10));
		g2d.drawString((pageIndex + 1) + " of " + numberOfPages, iw / 2 - 5, ih - 5);
		System.out.println("Printing p." + (pageIndex + 1) + "...");

		return PAGE_EXISTS;

	}

	public int getNumberOfPages() {
		return (rescaledDimension().height) / (int) currentPageFormat.getImageableHeight() + 1;
	}

	public PageFormat getPageFormat(int pageIndex) {
		return currentPageFormat;
	}

	public Printable getPrintable(int pageIndex) {
		return this;
	}

	private Dimension rescaledDimension() {
		Dimension d;
		float w0 = getSize().width;
		float wp = (float) currentPageFormat.getImageableWidth();
		float h = getSize().height;
		if (w0 <= wp) {
			d = new Dimension((int) w0, (int) h);
		}
		else {
			h *= wp / w0;
			d = new Dimension((int) wp, (int) h);
		}
		return d;
	}

	private float xScale() {
		float w0 = getSize().width;
		float wp = (float) currentPageFormat.getImageableWidth();
		float r = 1.0f;
		if (w0 > wp)
			r = w0 / wp;
		return r;
	}

	private int imageHeightPerPage() {
		return (int) (xScale() * currentPageFormat.getImageableHeight());
	}

}

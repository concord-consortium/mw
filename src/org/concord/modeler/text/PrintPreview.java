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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.IllegalComponentStateException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.IconPool;

class PrintPreview {

	static PageFormat pageFormat;
	private static PrinterJob printerJob;
	private final static Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

	private Page page;
	private JComboBox scaleComboBox;
	private PreviewContainer previewContainer;
	private JButton closeButton;
	private Container contentPane;
	private JDialog dialog;

	public PrintPreview(Page target) {
		page = target;
		previewContainer = new PreviewContainer();
		contentPane = new Container();
		contentPane.setLayout(new BorderLayout());
		JScrollPane scroller = new JScrollPane(previewContainer);
		contentPane.add(scroller, BorderLayout.CENTER);
		if (Modeler.user != null && !Modeler.user.isEmpty()) {
			PrintPage.setFooter(Modeler.user.getFullName() + ", " + Modeler.user.getInstitution());
		}
	}

	private static void init(String jobName) {
		if (printerJob == null) {
			printerJob = PrinterJob.getPrinterJob();
			printerJob.setCopies(1);
			pageFormat = printerJob.defaultPage();
			PrintPage.setPageFormat(pageFormat);
		}
		printerJob.setJobName(jobName);
	}

	public static void invokePageFormatDialog() {
		init("None");
		pageFormat = printerJob.pageDialog(PrintPreview.getPageFormat());
		PrintPage.setPageFormat(pageFormat);
	}

	public static PageFormat getPageFormat() {
		return pageFormat;
	}

	public void print() {
		previewContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		setPrintUIEnabled(false);
		generate(100);
		Book book = new Book();
		int count = previewContainer.getComponentCount();
		PrintPage pp = null;
		for (int i = 0; i < count; i++) {
			pp = (PrintPage) previewContainer.getComponent(i);
			book.append(pp, pageFormat);
		}
		printerJob.setPageable(book);
		if (printerJob.printDialog()) {
			try {
				printerJob.print();
			}
			catch (PrinterException pe) {
				printerJob.cancel();
			}
			catch (Exception e) {
				e.printStackTrace();
				// FIXME: On Mac OS X with Java 1.5.0_06, printing for the first page doesn't work.
				// The workaround in the follow sends one more request. Watch this in the future
				// Java version on Mac OS X, which might fix this problem.
				if (Page.OS.startsWith("Mac")) {
					try {
						printerJob.print(); // mac os x, try again
					}
					catch (PrinterException pe) {
						printerJob.cancel();
					}
				}
			}
		}
		setPrintUIEnabled(true);
		page.getProgressBar().setString(page.getAddress() + " was sent to print.");
		previewContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/*
	 * generate a set of pages for the given document according to the current page format. This method should be called
	 * before proceeding to printing, even if a print preview is not wanted.
	 * 
	 * @param ratio the scale of preview page's size, when printing, this parameter <b>should be</b> set 100, which
	 * represents the full size of a piece of paper.
	 */
	private void generate(int ratio) {
		init("Printing " + page.getAddress());
		PrintPage.reset();
		if (ratio != 100) {
			int w = (int) (pageFormat.getWidth() * ratio * 0.01);
			int h = (int) (pageFormat.getHeight() * ratio * 0.01);
			Component[] comps = previewContainer.getComponents();
			RescaledView rv = null;
			for (int k = 0; k < comps.length; k++) {
				if (comps[k] instanceof RescaledView) {
					rv = (RescaledView) comps[k];
					rv.setScaledSize(w, h);
				}
				else if (comps[k] instanceof PrintPage) {
					previewContainer.remove(comps[k]);
					rv = new RescaledView(w, h, (PrintPage) comps[k]);
					previewContainer.add(rv);
				}
				else {
					throw new IllegalComponentStateException("Preview container exception");
				}
			}
		}
		else {
			previewContainer.removeAll();
			int pageIndex = 0;
			PrintPage pp = new PrintPage(page, pageIndex);
			do {
				previewContainer.add(pp);
				pageIndex++;
				pp = new PrintPage(page, pageIndex);
			} while (pp.getComponentCount() != 0);
		}
		previewContainer.doLayout();
		previewContainer.repaint();
		contentPane.validate();
		contentPane.addNotify();
	}

	private void setPrintUIEnabled(final boolean b) {
		if (page == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				page.getAction("Print").setEnabled(b);
				page.getAction("Preview").setEnabled(b);
				page.getAction("Page Setup").setEnabled(b);
			}
		});
	}

	public void showPreviewScreen() {
		createFullScreen();
		generate(100);
		ItemListener[] listeners = scaleComboBox.getItemListeners();
		for (ItemListener x : listeners)
			scaleComboBox.removeItemListener(x);
		scaleComboBox.setSelectedIndex(scaleComboBox.getItemCount() - 1);
		for (ItemListener x : listeners)
			scaleComboBox.addItemListener(x);
		dialog.setVisible(true);
	}

	private void createFullScreen() {

		if (dialog != null)
			return;

		dialog = new JDialog(JOptionPane.getFrameForComponent(page), page.getAddress() + " - Print preview", true);
		dialog.setResizable(true);
		dialog.setUndecorated(true);
		dialog.setSize(screen.width, screen.height);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setContentPane(contentPane);

		JToolBar tb = new JToolBar();

		Dimension buttonDimension = new Dimension(24, 24);

		JButton button = new JButton(IconPool.getIcon("printer"));
		button.setToolTipText("Send to printer");
		button.setPreferredSize(buttonDimension);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print();
			}
		});
		tb.add(button);

		closeButton = new JButton(IconPool.getIcon("exit"));
		closeButton.setToolTipText("Close");
		closeButton.setPreferredSize(buttonDimension);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		tb.add(closeButton);
		tb.addSeparator();

		String[] scales = { "10 %", "25 %", "33 %", "50 %", "67 %", "75 %", "90 %", "100 %" };
		scaleComboBox = new JComboBox(scales);
		scaleComboBox.setFont(new Font("Arial", Font.PLAIN, 11));
		scaleComboBox.setToolTipText("Scale");
		scaleComboBox.setEditable(false);
		scaleComboBox.setSelectedIndex(scales.length - 1);
		FontMetrics fm = scaleComboBox.getFontMetrics(scaleComboBox.getFont());
		int w = fm.stringWidth(scales[scales.length - 1]);
		int h = fm.getHeight();
		scaleComboBox.setPreferredSize(new Dimension(w + 40, h + 4));
		scaleComboBox.setMaximumSize(scaleComboBox.getPreferredSize());
		scaleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					previewContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					String str = scaleComboBox.getSelectedItem().toString();
					if (str.endsWith("%")) {
						str = str.substring(0, str.length() - 1);
						str = str.trim();
					}
					int scale = 0;
					try {
						scale = Integer.parseInt(str);
					}
					catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(dialog, "This is an invalid input.", "Input format error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (scale < 10 || scale > 100) {
						JOptionPane.showMessageDialog(dialog, "The number must be between 10 and 100.",
								"Input range error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					generate(scale);
					previewContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});

		tb.add(scaleComboBox);

		contentPane.add(tb, BorderLayout.NORTH);

		JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel(dialog.getTitle());
		label.setFont(new Font("Arial", Font.ITALIC, 10));
		sb.add(label);

		contentPane.add(sb, BorderLayout.SOUTH);

	}

}
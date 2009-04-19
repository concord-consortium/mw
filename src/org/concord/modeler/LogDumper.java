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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.concord.modeler.ui.PastableTextArea;

public class LogDumper {

	private String newline = System.getProperty("line.separator");
	private PastableTextArea textArea;
	private JScrollPane scrollPane;
	private static LogDumper sharedInstance;

	private LogDumper() {
		textArea = new PastableTextArea(10, 50);
		textArea.setFont(new Font("Verdana", Font.PLAIN, 9));
		textArea.setEditable(false);
		scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(600, 250));
	}

	public final static LogDumper sharedInstance() {
		if (sharedInstance == null)
			sharedInstance = new LogDumper();
		return sharedInstance;
	}

	public void redirectSystemOutput() {
		PrintStream ps = new PrintStream(new OutputStream() {
			private List<Byte> byteList;
			{
				byteList = new ArrayList<Byte>();
			}

			public void write(int b) {
				byteList.add((byte) b);
			}

			public void flush() {
				final byte[] bytes = new byte[byteList.size()];
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = byteList.get(i).byteValue();
				}
				byteList.clear();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						textArea.append(new String(bytes));
					}
				});
			}
		}, true);
		System.setOut(ps);
		System.setErr(ps);
	}

	public void show(Frame frame) {

		String s = Modeler.getInternationalText("ViewSessionLog");
		final JDialog dialog = new JDialog(frame, s != null ? s : "Session Log", false);
		dialog.setLocation(200, 200);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);

		JPanel p = new JPanel();

		s = Modeler.getInternationalText("CloseButton");
		final JButton b = new JButton(s != null ? s : "Close");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.getContentPane().remove(scrollPane);
				dialog.dispose();
			}
		});
		p.add(b);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().remove(scrollPane);
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				b.requestFocusInWindow();
			}
		});

		dialog.setVisible(true);

	}

	public void dump(final String s) {
		if (textArea == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textArea.append(s + newline);
			}
		});
	}

	public void output() {
		if (textArea == null)
			return;
		textArea.append("<Client> Closed : " + new Date().toString() + newline);
		File f = new File(Initializer.sharedInstance().getPropertyDirectory(), "log.txt");
		if (f.exists())
			f.delete();
		FileWriter fw = null;
		try {
			f.createNewFile();
			fw = new FileWriter(f);
			textArea.write(fw);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fw != null) {
				try {
					fw.close();
				}
				catch (IOException iox) {
				}
			}
		}
	}

}
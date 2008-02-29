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
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.Modeler;
import org.concord.modeler.Upload;
import org.concord.modeler.text.Page;
import org.concord.modeler.util.SwingWorker;

/**
 * @author Charles Xie
 * 
 */
final class PostSubmissionHandler {

	private JLabel label;
	private Page page;
	private JDialog dialog;

	PostSubmissionHandler(Page page) {
		this.page = page;
		String s = Modeler.getInternationalText("YourSubmissionIsBeingTransmitted");
		label = new JLabel("<html><font face=\"Verdana\">"
				+ (s != null ? s : "Your submission is being transmitted.......") + "</font><br><br></html>");
		label.setIcon(new ImageIcon(Modeler.class.getResource("images/upload.gif")));
		label.setIconTextGap(10);
		s = Modeler.getInternationalText("Server");
		dialog = new JDialog(JOptionPane.getFrameForComponent(page), s != null ? s : "Server", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(label, BorderLayout.CENTER);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
	}

	void open(final URLConnection connect, final byte taskType) {

		new SwingWorker() {

			public Object construct() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						dialog.pack();
						dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
						dialog.setVisible(true);
					}
				});
				String serverMessage = "";
				BufferedReader in = null;
				String inputLine;
				try {
					in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
					while ((inputLine = in.readLine()) != null)
						serverMessage += inputLine;
				}
				catch (IOException e) {
					e.printStackTrace(System.err);
					serverMessage = "Error: " + e;
				}
				finally {
					if (in != null) {
						try {
							in.close();
						}
						catch (IOException iox) {
						}
					}
				}
				// try { Thread.sleep(5000); } catch(Exception e){} /* mimick server delay */
				return serverMessage;
			}

			public void finished() {
				label.setText((String) get());
				new SwingWorker() {
					public Object construct() {
						try {
							Thread.sleep(2000);
						}
						catch (InterruptedException e) {
						}
						return null;
					}

					public void finished() {
						dialog.dispose();
						page.getNavigator().visitLocation(
								taskType == Upload.UPLOAD_REPORT ? Modeler.getMyReportAddress() : Modeler
										.getMyModelSpaceAddress());
					}
				}.start();
			}

		}.start();
	}

}
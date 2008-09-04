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

import static java.util.regex.Pattern.compile;

import java.awt.EventQueue;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import org.concord.modeler.script.Compiler;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class TextBoxScripter extends ComponentScripter {

	private BasicPageTextBox textBox;
	private final static Pattern ENABLE_COMPONENT = compile("(^(?i)enablecomponent\\b){1}");
	private final static Pattern SELECT_COMPONENT = compile("(^(?i)selectcomponent\\b){1}");
	private final static Pattern SELECT_COMBOBOX = compile("(^(?i)selectcombobox\\b){1}");
	private final static Pattern SNAPSHOT = compile("(^(?i)snapshot\\b){1}");

	TextBoxScripter(BasicPageTextBox textBox) {
		super(false);
		this.textBox = textBox;
		setName("Text Box Script Runner #" + textBox.getIndex());
	}

	protected void evalCommand(String ci) {
		// load
		Matcher matcher = Compiler.LOAD.matcher(ci);
		if (matcher.find()) {
			if (textBox instanceof PageTextBox) {
				((PageTextBox) textBox).load(ci.substring(matcher.end()).trim(), false);
			}
			return;
		}
		// snapshot
		matcher = SNAPSHOT.matcher(ci);
		if (matcher.find()) {
			String s = ci.substring(matcher.end()).trim();
			s = textBox.getPage().getPathBase() + s;
			ImageIcon image = null;
			if (FileUtilities.isRemote(s)) {
				URL u = null;
				try {
					u = new URL(s);
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
				if (u != null)
					image = ConnectionManager.sharedInstance().loadImage(u);
			}
			else {
				image = new ImageIcon(s);
			}
			SnapshotGallery.sharedInstance().takeSnapshot(textBox.getPage().getAddress(), image);
			return;
		}
		// set <t>text</t>
		matcher = Compiler.SET.matcher(ci);
		if (matcher.find()) {
			String s = ci.substring(matcher.end()).trim();
			if (s.startsWith("<t>") || s.startsWith("<T>")) {
				String t;
				if (s.endsWith("</t>") || s.endsWith("</T>")) {
					t = s.substring(3, s.length() - 4);
				}
				else {
					t = s.substring(3);
				}
				if (!t.toLowerCase().startsWith("<html>"))
					t = "<html>" + t + "</html>";
				if (EventQueue.isDispatchThread()) {
					textBox.setText(t);
				}
				else {
					final String text = t;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							textBox.setText(text);
							textBox.repaint();
						}
					});
				}
			}
			return;
		}
		// delay
		matcher = Compiler.DELAY.matcher(ci);
		if (matcher.find()) {
			delay(ci.substring(matcher.end()).trim());
			return;
		}
		// enable component
		matcher = ENABLE_COMPONENT.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length == 1) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				if (i >= 0) {
					final int i2 = i;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							textBox.setEmbeddedComponentEnabled(i2, true);
						}
					});
				}
			}
			else if (s.length == 2) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				if (i >= 0) {
					final int i2 = i;
					final boolean b = "true".equalsIgnoreCase(s[1]);
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							textBox.setEmbeddedComponentEnabled(i2, b);
						}
					});
				}
			}
			return;
		}
		// select component
		matcher = SELECT_COMPONENT.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length >= 1) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				if (i >= 0) {
					boolean a = true;
					if (s.length >= 2)
						a = "true".equalsIgnoreCase(s[1]);
					boolean b = false;
					if (s.length >= 3)
						b = "execute".equalsIgnoreCase(s[2]);
					final int i2 = i;
					final boolean a2 = a;
					final boolean b2 = b;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							textBox.setComponentSelected(i2, a2, b2);
						}
					});
				}
			}
			return;
		}
		// select index from a combo box
		matcher = SELECT_COMBOBOX.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length >= 2) {
				int i = -1, j = -1;
				try {
					i = Integer.parseInt(s[0]);
					j = Integer.parseInt(s[1]);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				if (i >= 0 && j >= 0) {
					boolean b = false;
					if (s.length >= 3)
						b = "execute".equalsIgnoreCase(s[2]);
					final int i2 = i;
					final int j2 = j;
					final boolean b2 = b;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							List comboBoxes = textBox.getEmbeddedComponents(JComboBox.class);
							if (comboBoxes == null || comboBoxes.isEmpty())
								return;
							if (i2 < comboBoxes.size()) {
								JComboBox cb = (JComboBox) comboBoxes.get(i2);
								if (j2 < cb.getItemCount())
									textBox.setSelectedIndex(cb, j2, b2);
							}
						}
					});
				}
			}
			return;
		}
	}

	private void delay(String str) {
		if (str.matches(Compiler.REGEX_NONNEGATIVE_DECIMAL)) {
			float sec = Float.valueOf(str).floatValue();
			int millis = (int) (sec * 1000);
			try {
				Thread.sleep(millis);
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}

}

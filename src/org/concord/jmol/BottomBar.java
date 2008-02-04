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
package org.concord.jmol;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class BottomBar extends JPanel implements NavigationListener {

	private JmolContainer container;
	private JButton toFirstButton;
	private JButton toLastButton;
	private JButton toNextButton;
	private JButton toPreviousButton;
	private JButton nonstopButton;
	private JButton landButton;
	private JLabel infoLabel;
	private JLabel sceneLabel;
	private final static Font SMALL_FONT = new Font(null, Font.PLAIN, 10);

	BottomBar(JmolContainer c) {

		super(new FlowLayout(FlowLayout.LEFT, 0, 1));
		setBorder(BorderFactory.createRaisedBevelBorder());

		container = c;

		int m = System.getProperty("os.name").startsWith("Mac") ? 6 : 2;
		Insets margin = new Insets(m, m, m, m);

		toFirstButton = new JButton(new ImageIcon(getClass().getResource("resources/FirstScene.gif")));
		toFirstButton.setMargin(margin);
		toFirstButton.setToolTipText("Go to the starting scene");
		toFirstButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (container.getSceneCount() == 0)
					return;
				if (container.getCurrentSceneIndex() == 0)
					return;
				container.moveToScene(0, false);
			}
		});
		// add(toFirstButton);

		toPreviousButton = new JButton(new ImageIcon(getClass().getResource("resources/PreviousScene.gif")));
		toPreviousButton.setMargin(margin);
		toPreviousButton.setToolTipText("Go to the previous scene");
		toPreviousButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.moveOneStep(false);
			}
		});
		// add(toPreviousButton);

		toNextButton = new JButton(new ImageIcon(getClass().getResource("resources/NextScene.gif")));
		toNextButton.setMargin(margin);
		toNextButton.setToolTipText("Go to the next scene");
		toNextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.moveOneStep(true);
			}
		});
		// add(toNextButton);

		nonstopButton = new JButton(new ImageIcon(getClass().getResource("resources/Nonstop.gif")));
		nonstopButton.setMargin(margin);
		nonstopButton.setToolTipText("Nonstop tour");
		nonstopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.moveNonstop();
			}
		});
		// add(nonstopButton);

		toLastButton = new JButton(new ImageIcon(getClass().getResource("resources/LastScene.gif")));
		toLastButton.setMargin(margin);
		toLastButton.setToolTipText("Go to the last scene");
		toLastButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int n = container.getSceneCount();
				if (n > 0 && container.getCurrentSceneIndex() < n - 1) {
					container.moveToScene(n - 1, false);
				}
			}
		});
		// add(toLastButton);

		landButton = new JButton(new ImageIcon(getClass().getResource("resources/Landing.gif")));
		landButton.setMargin(margin);
		landButton.setToolTipText("Request stop at the next scene");
		landButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.requestStopMoveTo();
			}
		});
		// add(landButton);

		sceneLabel = new JLabel();
		sceneLabel.setOpaque(true);
		sceneLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		sceneLabel
				.setToolTipText("<html>Current scene index / total number of scenes in itinerary.<br>Click to open the Itinerary Editor.</html>");
		sceneLabel.setFont(SMALL_FONT);
		sceneLabel.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (!container.isMoving())
					container.editItinerary();
			}

			public void mouseEntered(MouseEvent e) {
				if (!container.isMoving())
					sceneLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			public void mouseExited(MouseEvent e) {
				if (!container.isMoving())
					sceneLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		// add(sceneLabel);

		infoLabel = new JLabel();
		infoLabel.setToolTipText("File name and number of atoms");
		infoLabel.setOpaque(true);
		infoLabel.setBackground(Color.white);
		infoLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		infoLabel.setFont(SMALL_FONT);
		add(infoLabel);

	}

	void showAnimationControls(boolean b) {
		if (b) {
			add(sceneLabel, 0);
			add(landButton, 0);
			add(toLastButton, 0);
			add(nonstopButton, 0);
			add(toNextButton, 0);
			add(toPreviousButton, 0);
			add(toFirstButton, 0);
		}
		else {
			remove(toFirstButton);
			remove(toLastButton);
			remove(toNextButton);
			remove(toPreviousButton);
			remove(nonstopButton);
			remove(landButton);
			remove(sceneLabel);
		}
		validate();
		repaint();
	}

	boolean hasAnimationControls() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == toFirstButton)
				return true;
		}
		return false;
	}

	void setSceneIndex(final int currentIndex, final int count) {
		final String t = count == 0 ? null : "  " + (currentIndex + 1) + "/" + count + "  ";
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				sceneLabel.setText(t);
				boolean b = t != null;
				boolean b2 = b && currentIndex > 0;
				toFirstButton.setEnabled(b2);
				toPreviousButton.setEnabled(b2);
				b2 = b && currentIndex < count - 1;
				toNextButton.setEnabled(b2);
				toLastButton.setEnabled(b2);
				nonstopButton.setEnabled(b2);
			}
		});
	}

	void setResourceName(String s) {
		int n = Math.max(1, container.jmol.viewer.getModelCount());
		final String t = "  " + FileUtilities.getFileName(s) + ": " + container.jmol.viewer.getAtomCount() / n
				+ " atoms  ";
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				infoLabel.setText(t);
			}
		});
	}

	public void depart(NavigationEvent e) {
		int i = e.getCurrentSceneIndex() + 1;
		int j = e.getNextSceneIndex() + 1;
		final String t = "  " + i + " ... " + j + "/" + e.getSceneCount() + "  ";
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				sceneLabel.setText(t);
				toFirstButton.setEnabled(false);
				toLastButton.setEnabled(false);
				toPreviousButton.setEnabled(false);
				toNextButton.setEnabled(false);
				nonstopButton.setEnabled(false);
			}
		});
	}

	public void arrive(NavigationEvent e) {
		setSceneIndex(e.getCurrentSceneIndex(), e.getSceneCount());
	}

}
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

package org.concord.mw2d;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.ScreenshotSaver;
import org.concord.modeler.util.SwingWorker;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.StructureFactor;

public class DiffractionInstrument extends JPanel {

	private static Icon DIFFRACTION_ICON;
	private static Icon MORE_LOD;
	private static Icon SHARPEN_IMAGE;
	private static Icon BLUR_IMAGE;
	private static Icon INVERT_IMAGE;
	private static Icon CONTRAST_UP;
	private static Icon CONTRAST_DOWN;
	private static Icon BRIGHT_UP;
	private static Icon BRIGHT_DOWN;

	private final static float ONE_NINTH = 1.0f / 9.0f;
	private final static float[] BLUR_KERNEL = { ONE_NINTH, ONE_NINTH, ONE_NINTH, ONE_NINTH, ONE_NINTH, ONE_NINTH,
			ONE_NINTH, ONE_NINTH, ONE_NINTH };
	private final static float[] SHARP_KERNEL = { 0.0f, -1.0f, 0.0f, -1.0f, 5.0f, -1.0f, 0.0f, -1.0f, 0.0f };

	private static ConvolveOp blurOp;
	private static ConvolveOp sharpenOp;
	private static LookupOp invertOp;

	private Window owner;
	protected DiffractionPattern pattern;
	private MolecularModel model;
	private int type;
	private JButton scanButton;
	private JButton zoomInButton, zoomOutButton;
	private JToggleButton lodButton;

	public DiffractionInstrument(boolean addCloseButton) {

		super(new BorderLayout(5, 5));
		Dimension dim = new Dimension(250, 200);
		setPreferredSize(dim);
		setMaximumSize(dim);
		setMinimumSize(dim);

		pattern = new DiffractionPattern();
		pattern.setBorder(BorderFactory.createLoweredBevelBorder());
		add(pattern, BorderLayout.CENTER);

		if (DIFFRACTION_ICON == null)
			DIFFRACTION_ICON = new ImageIcon(DiffractionInstrument.class.getResource("images/diffraction.gif"));
		if (MORE_LOD == null)
			MORE_LOD = new ImageIcon(DiffractionInstrument.class.getResource("images/MoreLOD.gif"));
		if (SHARPEN_IMAGE == null)
			SHARPEN_IMAGE = new ImageIcon(DiffractionInstrument.class.getResource("images/sharpen.gif"));
		if (BLUR_IMAGE == null)
			BLUR_IMAGE = new ImageIcon(DiffractionInstrument.class.getResource("images/blur.gif"));
		if (INVERT_IMAGE == null)
			INVERT_IMAGE = new ImageIcon(DiffractionInstrument.class.getResource("images/invert.gif"));
		if (CONTRAST_UP == null)
			CONTRAST_UP = new ImageIcon(DiffractionInstrument.class.getResource("images/contrastUp.gif"));
		if (CONTRAST_DOWN == null)
			CONTRAST_DOWN = new ImageIcon(DiffractionInstrument.class.getResource("images/contrastDn.gif"));
		if (BRIGHT_UP == null)
			BRIGHT_UP = new ImageIcon(DiffractionInstrument.class.getResource("images/brightUp.gif"));
		if (BRIGHT_DOWN == null)
			BRIGHT_DOWN = new ImageIcon(DiffractionInstrument.class.getResource("images/brightDn.gif"));

		if (blurOp == null)
			blurOp = new ConvolveOp(new Kernel(3, 3, BLUR_KERNEL));
		if (sharpenOp == null)
			sharpenOp = new ConvolveOp(new Kernel(3, 3, SHARP_KERNEL), ConvolveOp.EDGE_NO_OP, null);

		if (invertOp == null) {
			short[] invert = new short[256];
			for (int i = 0; i < 256; i++)
				invert[i] = (short) (255 - i);
			invertOp = new LookupOp(new ShortLookupTable(0, invert), null);
		}

		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setPreferredSize(new Dimension(50, 200));

		JPanel p = new JPanel();
		buttonPanel.add(p, BorderLayout.NORTH);

		scanButton = new JButton(DIFFRACTION_ICON);
		scanButton.setToolTipText("Generate the diffraction pattern");
		scanButton.setPreferredSize(new Dimension(50, 24));
		scanButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scan();
			}
		});
		p.add(scanButton);

		p = new JPanel(new GridLayout(8, 2));
		buttonPanel.add(p, BorderLayout.CENTER);

		lodButton = new JToggleButton(MORE_LOD);
		lodButton.setToolTipText("Toggle this button to change the level of details of the image.");
		lodButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					setLevelOfDetails(StructureFactor.LOG_SCALING);
				}
				else {
					setLevelOfDetails(StructureFactor.LINEAR_SCALING);
				}
			}
		});
		p.add(lodButton);

		JButton button = new JButton(INVERT_IMAGE);
		button.setToolTipText("Invert");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pattern.getBufferedImage() == null)
					return;
				pattern.setBufferedImage(invertOp.filter(pattern.getBufferedImage(), null));
				pattern.repaint();
			}
		});
		p.add(button);

		button = new JButton(SHARPEN_IMAGE);
		button.setToolTipText("Sharpen");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pattern.getBufferedImage() == null)
					return;
				pattern.setBufferedImage(sharpenOp.filter(pattern.getBufferedImage(), null));
				pattern.repaint();
			}
		});
		p.add(button);

		button = new JButton(BLUR_IMAGE);
		button.setToolTipText("Blur");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pattern.getBufferedImage() == null)
					return;
				pattern.setBufferedImage(blurOp.filter(pattern.getBufferedImage(), null));
				pattern.repaint();
			}
		});
		p.add(button);

		button = new JButton(CONTRAST_UP);
		button.setToolTipText("Increase contrast");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contrast(1.1f);
			}
		});
		p.add(button);

		button = new JButton(CONTRAST_DOWN);
		button.setToolTipText("Decrease contrast");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				contrast(0.9f);
			}
		});
		p.add(button);

		button = new JButton(BRIGHT_UP);
		button.setToolTipText("Increase brightness");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				brighten(1.1f);
			}
		});
		p.add(button);

		button = new JButton(BRIGHT_DOWN);
		button.setToolTipText("Decrease brightness");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				brighten(0.9f);
			}
		});
		p.add(button);

		zoomInButton = new JButton(IconPool.getIcon("zoom_in"));
		zoomInButton.setToolTipText("Zoom in in the reciprocal space");
		zoomInButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pattern.zoomIn())
					scan();
			}
		});
		p.add(zoomInButton);

		zoomOutButton = new JButton(IconPool.getIcon("zoom_out"));
		zoomOutButton.setToolTipText("Zoom out in the reciprocal space");
		zoomOutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pattern.zoomOut())
					scan();
			}
		});
		p.add(zoomOutButton);

		if (!Page.isApplet()) {
			button = new JButton(IconPool.getIcon("camera"));
			button.setToolTipText("Take a snapshot");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (model != null)
						model.notifyPageComponentListeners(new PageComponentEvent(pattern,
								PageComponentEvent.SNAPSHOT_TAKEN));
				}
			});
			p.add(button);

			button = new JButton(new ScreenshotSaver(ModelerUtilities.fileChooser, "Output Image", pattern, false));
			button.setIcon(IconPool.getIcon("save"));
			button.setText(null);
			button.setToolTipText("Save this image to disk");
			p.add(button);
		}

		if (addCloseButton) {
			button = new JButton(IconPool.getIcon("exit"));
			button.setToolTipText("Close this window");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (owner != null)
						owner.dispose();
				}
			});
			p.add(button);
		}

		add(buttonPanel, BorderLayout.WEST);

	}

	public Insets getInsets() {
		return new Insets(5, 5, 5, 5);
	}

	public void setLevelOfDetails(final int i) {
		pattern.setLevelOfDetails(i);
		if (lodButton != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					lodButton.setSelected(i == StructureFactor.LOG_SCALING);
				}
			});
		}
	}

	public int getLevelOfDetails() {
		return pattern.getLevelOfDetails();
	}

	public void setScale(int i) {
		pattern.setZoom(i);
	}

	public int getScale() {
		return pattern.getZoom();
	}

	public void createImage() {
		pattern.computeImage(model, type);
		if (getLevelOfDetails() == StructureFactor.LINEAR_SCALING)
			brighten(4);
	}

	public void setModel(MolecularModel model) {
		this.model = model;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setOwner(Window owner) {
		this.owner = owner;
	}

	public void enableScan(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scanButton.setEnabled(b);
				zoomInButton.setEnabled(b);
				zoomOutButton.setEnabled(b);
			}
		});
	}

	protected void scan() {
		enableScan(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		pattern.setMessage("Calculating, please wait......");
		pattern.repaint();
		new SwingWorker("DiffractionInstrument:scan()") {
			public Object construct() {
				pattern.setMessage(null);
				createImage();
				return null;
			}

			public void finished() {
				pattern.repaint();
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				enableScan(true);
				model.getView().repaint();
			}
		}.start();
	}

	private void contrast(float gamma_contrast) {
		if (pattern.getBufferedImage() == null)
			return;
		short[] contrast = new short[256];
		float temp;
		for (int i = 0; i < 256; i++) {
			temp = (1.0f - gamma_contrast) * 128 + gamma_contrast * i;
			contrast[i] = (short) (temp > 0.0f ? Math.min(temp, 255) : Math.max(temp, 0));
		}
		LookupOp op = new LookupOp(new ShortLookupTable(0, contrast), null);
		pattern.setBufferedImage(op.filter(pattern.getBufferedImage(), null));
		pattern.repaint();
	}

	private void brighten(float gamma_brightness) {
		if (pattern.getBufferedImage() == null)
			return;
		short[] brightness = new short[256];
		for (int i = 0; i < 256; i++)
			brightness[i] = (short) (Math.min(gamma_brightness * i, 255));
		LookupOp op = new LookupOp(new ShortLookupTable(0, brightness), null);
		pattern.setBufferedImage(op.filter(pattern.getBufferedImage(), null));
		pattern.repaint();
	}

}
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

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.mw2d.models.AngularBond;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.EllipseComponent;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.LineComponent;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.MolecularObject;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RectangleComponent;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.TextBoxComponent;

public final class DialogFactory {

	public static void showDialog(final Object obj) {
		if (obj == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (obj instanceof Atom) {
					createDialog((Atom) obj).setVisible(true);
				}
				else if (obj instanceof RadialBond) {
					createDialog((RadialBond) obj).setVisible(true);
				}
				else if (obj instanceof AngularBond) {
					createDialog((AngularBond) obj).setVisible(true);
				}
				else if (obj instanceof Molecule) {
					if (obj instanceof MolecularObject) {
						createDialog((MolecularObject) obj).setVisible(true);
					}
					else {
						createDialog((Molecule) obj).setVisible(true);
					}
				}
				else if (obj instanceof RectangularObstacle) {
					createDialog((RectangularObstacle) obj).setVisible(true);
				}
				else if (obj instanceof GayBerneParticle) {
					createDialog((GayBerneParticle) obj).setVisible(true);
				}
				else if (obj instanceof ImageComponent) {
					createDialog((ImageComponent) obj).setVisible(true);
				}
				else if (obj instanceof TextBoxComponent) {
					createDialog((TextBoxComponent) obj).setVisible(true);
				}
				else if (obj instanceof LineComponent) {
					createDialog((LineComponent) obj).setVisible(true);
				}
				else if (obj instanceof RectangleComponent) {
					createDialog((RectangleComponent) obj).setVisible(true);
				}
				else if (obj instanceof EllipseComponent) {
					createDialog((EllipseComponent) obj).setVisible(true);
				}
			}
		});
	}

	/* create a dialog box for an atom */
	private static JDialog createDialog(final Atom atom) {
		String s = MDView.getInternationalText("ParticleProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(atom.getHostModel().getView()),
				s != null ? s : "Particle Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final AtomPropertiesPanel p = new AtomPropertiesPanel(atom);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(atom.getHostModel().getView());
		return dialog;
	}

	/* create a dialog for a radial bond */
	private static JDialog createDialog(final RadialBond rb) {
		String s = MDView.getInternationalText("RadialBondProperties");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(rb.getHostModel().getView()), s != null ? s
				: "Radial Bond Properties", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final RadialBondPropertiesPanel p = new RadialBondPropertiesPanel(rb);
		d.setContentPane(p);
		p.setDialog(d);
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				d.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		d.pack();
		d.setLocationRelativeTo(rb.getHostModel().getView());
		return d;
	}

	/* create a session dialog for this bond */
	private static JDialog createDialog(final AngularBond ab) {
		String s = MDView.getInternationalText("AngularBondProperties");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(ab.getHostModel().getView()), s != null ? s
				: "Angular Bond Properties", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final AngularBondPropertiesPanel p = new AngularBondPropertiesPanel(ab);
		d.setContentPane(p);
		p.setDialog(d);
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				d.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		d.pack();
		d.setLocationRelativeTo(ab.getHostModel().getView());
		return d;
	}

	/* create dialog box for an obstacle. */
	private static JDialog createDialog(final RectangularObstacle obs) {
		String s = MDView.getInternationalText("ObstacleProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(obs.getHostModel().getView()),
				s != null ? s : "Obstacle Properties", false);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final ObstaclePropertiesPanel p = new ObstaclePropertiesPanel(obs);
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(obs.getHostModel().getView());
		return dialog;
	}

	/* create a dialog box for an image component */
	private static JDialog createDialog(ImageComponent ic) {
		ImagePropertiesDialog ipd = new ImagePropertiesDialog((MDView) ic.getHostModel().getView());
		ipd.setImage(ic);
		ipd.pack();
		ipd.setLocationRelativeTo(ic.getHostModel().getView());
		return ipd;
	}

	/* create a dialog box for a text box component */
	private static JDialog createDialog(final TextBoxComponent c) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(c.getHostModel().getView()),
				"Text Box Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final TextBoxPropertiesPanel p = new TextBoxPropertiesPanel(c) {
			public int getIndex() {
				return ((MDView) c.getHostModel().getView()).getLayeredComponentIndex(c);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		String s = MDView.getInternationalText("TextBoxProperties");
		if (s != null)
			dialog.setTitle(s);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				TextBoxPropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		if (TextBoxPropertiesPanel.getOffset() == null) {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(c.getHostModel().getView()));
		}
		else {
			dialog.setLocation(TextBoxPropertiesPanel.getOffset());
		}
		return dialog;
	}

	/* create a dialog box for a line component */
	private static JDialog createDialog(final LineComponent c) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(c.getHostModel().getView()),
				"Line Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final LineComponentPropertiesPanel p = new LineComponentPropertiesPanel(c) {
			public int getIndex() {
				return ((MDView) c.getHostModel().getView()).getLayeredComponentIndex(c);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		String s = MDView.getInternationalText("LineProperties");
		if (s != null)
			dialog.setTitle(s);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				LineComponentPropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (LineComponentPropertiesPanel.getOffset() == null) {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(c.getHostModel().getView()));
		}
		else {
			dialog.setLocation(LineComponentPropertiesPanel.getOffset());
		}
		return dialog;
	}

	/* create a dialog box for a rectangle component */
	private static JDialog createDialog(final RectangleComponent c) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(c.getHostModel().getView()),
				"Rectangle Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final RectangleComponentPropertiesPanel p = new RectangleComponentPropertiesPanel(c) {
			public int getIndex() {
				return ((MDView) c.getHostModel().getView()).getLayeredComponentIndex(c);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		String s = MDView.getInternationalText("RectangleProperties");
		if (s != null)
			dialog.setTitle(s);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				RectangleComponentPropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (RectangleComponentPropertiesPanel.getOffset() == null) {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(c.getHostModel().getView()));
		}
		else {
			dialog.setLocation(RectangleComponentPropertiesPanel.getOffset());
		}
		return dialog;
	}

	/* create a dialog box for an ellipse component */
	private static JDialog createDialog(final EllipseComponent c) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(c.getHostModel().getView()),
				"Ellipse Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final EllipseComponentPropertiesPanel p = new EllipseComponentPropertiesPanel(c) {
			public int getIndex() {
				return ((MDView) c.getHostModel().getView()).getLayeredComponentIndex(c);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		String s = MDView.getInternationalText("EllipseProperties");
		if (s != null)
			dialog.setTitle(s);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				EllipseComponentPropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (EllipseComponentPropertiesPanel.getOffset() == null) {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(c.getHostModel().getView()));
		}
		else {
			dialog.setLocation(EllipseComponentPropertiesPanel.getOffset());
		}
		return dialog;
	}

	/* create a dialog box for a molecule */
	private static JDialog createDialog(final Molecule m) {
		String s = MDView.getInternationalText("MoleculeProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(m.getHostModel().getView()), s != null ? s
				: "Molecule Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final MoleculePropertiesPanel p = new MoleculePropertiesPanel(m);
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(m.getHostModel().getView());
		return dialog;
	}

	private static JDialog createDialog(final MolecularObject m) {
		String s = MDView.getInternationalText("MolecularSurfaceProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(m.getHostModel().getView()), s != null ? s
				: "Molecular Surface Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final MolecularObjectPropertiesPanel p = new MolecularObjectPropertiesPanel(m);
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(m.getHostModel().getView());
		return dialog;
	}

	/* create a dialog box for this Gay-Berne particle */
	private static JDialog createDialog(final GayBerneParticle gb) {
		String s = MDView.getInternationalText("MesoscaleParticleProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(gb.getHostModel().getView()), s != null ? s
				: "Mesoscale Particle Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final GayBernePropertiesPanel p = new GayBernePropertiesPanel(gb);
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(gb.getHostModel().getView());
		return dialog;
	}

}
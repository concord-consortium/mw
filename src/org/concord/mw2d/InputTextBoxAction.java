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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.concord.modeler.draw.FillMode;
import org.concord.mw2d.models.TextBoxComponent;

class InputTextBoxAction extends AbstractAction {

	private MDView view;

	InputTextBoxAction(MDView v) {
		super("Input Text Box");
		view = v;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		putValue(NAME, "Input Text Box");
		putValue(SHORT_DESCRIPTION, "Input text box");
		putValue(SMALL_ICON, new ImageIcon(MDView.class.getResource("images/textbox.gif")));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
	}

	public void actionPerformed(ActionEvent e) {
		int x = -1;
		int y = -1;
		Object o = getValue("x");
		if (o instanceof Integer) {
			x = ((Integer) o).intValue();
		}
		o = getValue("y");
		if (o instanceof Integer) {
			y = ((Integer) o).intValue();
		}
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(view), "Text Box Properties", true);
		TextBoxComponent c = new TextBoxComponent("Your text here");
		c.setBorderType((byte) 2);
		c.setShadowType((byte) 2);
		o = getValue("callout");
		c.setCallOut(o == Boolean.FALSE ? false : true);
		c.setFillMode(new FillMode.ColorFill(Color.green));
		if (x >= 0 && y >= 0) {
			c.setLocation(x, y);
		}
		view.addLayeredComponent(c);
		final TextBoxPropertiesPanel p = new TextBoxPropertiesPanel(c);
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.getContentPane().removeAll();
				TextBoxPropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		if (TextBoxPropertiesPanel.getOffset() == null) {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(view));
		}
		else {
			dialog.setLocation(TextBoxPropertiesPanel.getOffset());
		}
		dialog.setVisible(true);
		if (p.isCancelled())
			view.removeLayeredComponent(c);
	}

}
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
package org.concord.mw3d;

import java.awt.event.KeyEvent;

import static org.jmol.api.Navigator.UP_PRESSED;
import static org.jmol.api.Navigator.DOWN_PRESSED;
import static org.jmol.api.Navigator.LEFT_PRESSED;
import static org.jmol.api.Navigator.RIGHT_PRESSED;
import static org.jmol.api.Navigator.PGUP_PRESSED;
import static org.jmol.api.Navigator.PGDN_PRESSED;
import static org.jmol.api.Navigator.X_PRESSED;
import static org.jmol.api.Navigator.Y_PRESSED;
import static org.jmol.api.Navigator.Z_PRESSED;
import static org.jmol.api.Navigator.A_PRESSED;
import static org.jmol.api.Navigator.S_PRESSED;

/**
 * @author Charles Xie
 * 
 */
class KeyManager {

	private int keyCode;

	public int keyPressed(KeyEvent e) {
		boolean b = false;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			keyCode = keyCode | UP_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_DOWN:
			keyCode = keyCode | DOWN_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_LEFT:
			keyCode = keyCode | LEFT_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_RIGHT:
			keyCode = keyCode | RIGHT_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_PAGE_UP:
			keyCode = keyCode | PGUP_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_PAGE_DOWN:
			keyCode = keyCode | PGDN_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_X:
			keyCode = keyCode | X_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_Y:
			keyCode = keyCode | Y_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_Z:
			keyCode = keyCode | Z_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_A:
			keyCode = keyCode | A_PRESSED;
			b = true;
			break;
		case KeyEvent.VK_S:
			keyCode = keyCode | S_PRESSED;
			b = true;
			break;
		}
		return b ? keyCode : 0;
	}

	public int keyReleased(KeyEvent e) {
		if (!e.isControlDown()) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				keyCode = keyCode ^ UP_PRESSED;
				break;
			case KeyEvent.VK_DOWN:
				keyCode = keyCode ^ DOWN_PRESSED;
				break;
			case KeyEvent.VK_LEFT:
				keyCode = keyCode ^ LEFT_PRESSED;
				break;
			case KeyEvent.VK_RIGHT:
				keyCode = keyCode ^ RIGHT_PRESSED;
				break;
			case KeyEvent.VK_PAGE_UP:
				keyCode = keyCode ^ PGUP_PRESSED;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				keyCode = keyCode ^ PGDN_PRESSED;
				break;
			case KeyEvent.VK_X:
				keyCode = keyCode ^ X_PRESSED;
				break;
			case KeyEvent.VK_Y:
				keyCode = keyCode ^ Y_PRESSED;
				break;
			case KeyEvent.VK_Z:
				keyCode = keyCode ^ Z_PRESSED;
				break;
			case KeyEvent.VK_A:
				keyCode = keyCode ^ A_PRESSED;
				break;
			case KeyEvent.VK_S:
				keyCode = keyCode ^ S_PRESSED;
				break;
			}
		}
		return keyCode;
	}

}

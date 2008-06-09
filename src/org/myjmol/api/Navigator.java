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

package org.myjmol.api;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

// Flight dynamics reference: http://en.wikipedia.org/wiki/Flight_dynamics

public abstract class Navigator {

	public final static int UP_PRESSED = 1;
	public final static int DOWN_PRESSED = 2;
	public final static int LEFT_PRESSED = 4;
	public final static int RIGHT_PRESSED = 8;
	public final static int PGUP_PRESSED = 16;
	public final static int PGDN_PRESSED = 32;
	public final static int X_PRESSED = 64;
	public final static int Y_PRESSED = 128;
	public final static int Z_PRESSED = 256;
	public final static int A_PRESSED = 512;
	public final static int S_PRESSED = 1024;

	private static final byte HOME_POSITION = 0;
	private static final byte TRANSLATE_LEFT = 1;
	private static final byte TRANSLATE_RIGHT = 2;
	private static final byte TRANSLATE_UP = 3;
	private static final byte TRANSLATE_DOWN = 4;
	private static final byte TRANSLATE_FORWARD = 5;
	private static final byte TRANSLATE_BACK = 6;
	private static final byte YAW_CW = 7;
	private static final byte YAW_CCW = 8;
	private static final byte PITCH_UP = 9;
	private static final byte PITCH_DOWN = 10;
	private static final byte ROLL_CW = 11;
	private static final byte ROLL_CCW = 12;

	private static final int TRANSLATION_PERIOD = 50;
	private static final int ROTATION_PERIOD = 25;
	private static final int BUTTON_SIZE = 16;
	private static final int GAP = 2;

	// callbacks
	private Runnable engineOnCallback;
	private Runnable engineOffCallback;
	private Runnable steeringUpCallback;
	private Runnable steeringOffCallback;

	// multi-key steering using a thread-pool timer
	private ScheduledThreadPoolExecutor keyExecutor;
	private Runnable forwardRun, backRun, upRun, downRun, leftRun, rightRun;
	private Future forwardFuture, backFuture, upFuture, downFuture, leftFuture, rightFuture;
	private Runnable pitchUpRun, pitchDownRun, yawCwRun, yawCcwRun, rollCwRun, rollCcwRun;
	private Future pitchUpFuture, pitchDownFuture, yawCwFuture, yawCcwFuture, rollCwFuture, rollCcwFuture;
	private Runnable orbitUpRun, orbitDownRun, orbitLeftRun, orbitRightRun;
	private Future orbitUpFuture, orbitDownFuture, orbitLeftFuture, orbitRightFuture;
	private int keyCode;
	private final Object navigationLock = new Object();

	// navigation pad
	private volatile boolean buttonPressed;
	private volatile boolean enabled = true;
	private Color backgroundColor;
	private Color contrastBackgroundColor;
	private Rectangle homeButton;
	private Rectangle leftButton;
	private Rectangle rightButton;
	private Rectangle upButton;
	private Rectangle downButton;
	private Rectangle forwardButton;
	private Rectangle backButton;
	private Rectangle yawCwButton;
	private Rectangle yawCcwButton;
	private Rectangle pitchUpButton;
	private Rectangle pitchDownButton;
	private Rectangle rollCwButton;
	private Rectangle rollCcwButton;
	private Map<Rectangle, Boolean> selectionMap;
	private Thread thread;
	private final Object lock = new Object();
	private volatile byte flyMode = -1;

	// others
	private JmolViewer viewer;
	private int speed = 10;
	private float angularSpeed = 0.5f;
	private float rollingSpeed = 1;
	private volatile boolean steering;
	private Vector3f steeringDirection;
	private Vector3f rotateDirection;
	private Matrix3f tmp;
	private long steerStartTime;
	private int gearTime = 500;

	public Navigator(JmolViewer v) {
		viewer = v;
		selectionMap = new HashMap<Rectangle, Boolean>();
		setSpeed(speed);
		steeringDirection = new Vector3f();
		rotateDirection = new Vector3f();
		tmp = new Matrix3f();
		keyExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "Key navigation thread");
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
	}

	public void clear() {
		buttonPressed = false;
		steerStartTime = 0;
		flyMode = -1;
		if (steeringOffCallback != null)
			steeringOffCallback.run();
	}

	public Vector3f getSteeringDirection() {
		return steeringDirection;
	}

	public Vector3f getRotationDirection() {
		return rotateDirection;
	}

	public void engineOff() {
		steeringDirection.set(0, 0, 0);
		rotateDirection.set(0, 0, 0);
		if (engineOffCallback != null)
			engineOffCallback.run();
	}

	public void setSteering(boolean b) {
		steering = b;
	}

	public boolean isSteering() {
		return steering;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
		angularSpeed = (float) (0.01f * speed / Math.PI);
		rollingSpeed = 2 * angularSpeed;
	}

	public int getSpeed() {
		return speed;
	}

	/* callback support */

	public void setEngineOnCallback(Runnable r) {
		engineOnCallback = r;
	}

	public void setEngineOffCallback(Runnable r) {
		engineOffCallback = r;
	}

	public void setSteeringUpCallback(Runnable r) {
		steeringUpCallback = r;
	}

	public void setSteeringOffCallback(Runnable r) {
		steeringOffCallback = r;
	}

	/*
	 * navigation pad support: since the user can only click a button at a time, this is an exclusive mode (indicated by
	 * flyMode).
	 */

	public abstract void home();

	public boolean navigate(int x, int y) {
		if (!enabled)
			return false;
		if (homeButton.contains(x, y)) {
			flyMode = HOME_POSITION;
			home();
			return true;
		}
		buttonPressed = false;
		if (leftButton.contains(x, y)) {
			flyMode = TRANSLATE_LEFT;
			wakeUpNavigationThread();
			return true;
		}
		if (rightButton.contains(x, y)) {
			flyMode = TRANSLATE_RIGHT;
			wakeUpNavigationThread();
			return true;
		}
		if (upButton.contains(x, y)) {
			flyMode = TRANSLATE_UP;
			wakeUpNavigationThread();
			return true;
		}
		if (downButton.contains(x, y)) {
			flyMode = TRANSLATE_DOWN;
			wakeUpNavigationThread();
			return true;
		}
		if (forwardButton.contains(x, y)) {
			flyMode = TRANSLATE_FORWARD;
			wakeUpNavigationThread();
			return true;
		}
		if (backButton.contains(x, y)) {
			flyMode = TRANSLATE_BACK;
			wakeUpNavigationThread();
			return true;
		}
		if (yawCwButton.contains(x, y)) {
			flyMode = YAW_CW;
			wakeUpNavigationThread();
			return true;
		}
		if (yawCcwButton.contains(x, y)) {
			flyMode = YAW_CCW;
			wakeUpNavigationThread();
			return true;
		}
		if (pitchUpButton.contains(x, y)) {
			flyMode = PITCH_UP;
			wakeUpNavigationThread();
			return true;
		}
		if (pitchDownButton.contains(x, y)) {
			flyMode = PITCH_DOWN;
			wakeUpNavigationThread();
			return true;
		}
		if (rollCwButton.contains(x, y)) {
			flyMode = ROLL_CW;
			wakeUpNavigationThread();
			return true;
		}
		if (rollCcwButton.contains(x, y)) {
			flyMode = ROLL_CCW;
			wakeUpNavigationThread();
			return true;
		}
		if (steeringOffCallback != null)
			steeringOffCallback.run();
		flyMode = -1;
		viewer.setMeasurementEnabled(true);
		return false;
	}

	private void flyOneStep() {
		switch (flyMode) {
		case TRANSLATE_LEFT:
			flyLeft();
			break;
		case TRANSLATE_RIGHT:
			flyRight();
			break;
		case TRANSLATE_UP:
			flyUp();
			break;
		case TRANSLATE_DOWN:
			flyDown();
			break;
		case TRANSLATE_FORWARD:
			flyForward();
			break;
		case TRANSLATE_BACK:
			flyBack();
			break;
		case YAW_CW:
			yawCw();
			break;
		case YAW_CCW:
			yawCcw();
			break;
		case PITCH_UP:
			pitchUp();
			break;
		case PITCH_DOWN:
			pitchDown();
			break;
		case ROLL_CW:
			rollCw();
			break;
		case ROLL_CCW:
			rollCcw();
			break;
		}
		increaseSteeringForce();
	}

	/*
	 * button navigation is implemented earilier and therefore differently from key navigation. Let's keep it here for
	 * comparison.
	 */
	private void wakeUpNavigationThread() {
		viewer.setMeasurementEnabled(false);
		buttonPressed = true;
		if (thread == null) {
			thread = new Thread(new Runnable() {
				public void run() {
					while (true) {
						while (buttonPressed) {
							flyOneStep();
							try {
								Thread.sleep(100);
							}
							catch (InterruptedException e) {
								buttonPressed = false;
								return;
							}
						}
						synchronized (lock) {
							try {
								lock.wait();
							}
							catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});
			thread.setName("Button navigation thread");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		else {
			// move one step at least in case the user releases the mouse button too soon
			flyOneStep();
			// just in case there is a missed notification
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}

	/* keyboard navigation support */

	public int keyPressed(KeyEvent e) {
		boolean b = false;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_A:
			if ((keyCode & A_PRESSED) != A_PRESSED) {
				keyCode = keyCode | A_PRESSED;
				b = true;
				if (forwardRun == null) {
					forwardRun = new Runnable() {
						public void run() {
							flyForward();
							increaseSteeringForce();
						}
					};
				}
				forwardFuture = keyExecutor.scheduleAtFixedRate(forwardRun, 0, TRANSLATION_PERIOD,
						TimeUnit.MILLISECONDS);
			}
			break;
		case KeyEvent.VK_S:
			if ((keyCode & S_PRESSED) != S_PRESSED) {
				keyCode = keyCode | S_PRESSED;
				b = true;
				if (backRun == null) {
					backRun = new Runnable() {
						public void run() {
							flyBack();
							increaseSteeringForce();
						}
					};
				}
				backFuture = keyExecutor.scheduleAtFixedRate(backRun, 0, TRANSLATION_PERIOD, TimeUnit.MILLISECONDS);
			}
			break;
		case KeyEvent.VK_UP:
			if ((keyCode & UP_PRESSED) != UP_PRESSED) {
				keyCode = keyCode | UP_PRESSED;
				b = true;
				if (e.isShiftDown()) {
					if (upRun == null) {
						upRun = new Runnable() {
							public void run() {
								flyUp();
								increaseSteeringForce();
							}
						};
					}
					upFuture = keyExecutor.scheduleAtFixedRate(upRun, 0, TRANSLATION_PERIOD, TimeUnit.MILLISECONDS);
				}
				else if (e.isAltDown()) {
					if (orbitUpRun == null) {
						orbitUpRun = new Runnable() {
							public void run() {
								flyUp();
								pitchDown();
								increaseSteeringForce();
							}
						};
					}
					orbitUpFuture = keyExecutor.scheduleAtFixedRate(orbitUpRun, 0, TRANSLATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else {
					if (pitchUpRun == null) {
						pitchUpRun = new Runnable() {
							public void run() {
								pitchUp();
								increaseSteeringForce();
							}
						};
					}
					pitchUpFuture = keyExecutor.scheduleAtFixedRate(pitchUpRun, 0, ROTATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
			}
			break;
		case KeyEvent.VK_DOWN:
			if ((keyCode & DOWN_PRESSED) != DOWN_PRESSED) {
				keyCode = keyCode | DOWN_PRESSED;
				b = true;
				if (e.isShiftDown()) {
					if (downRun == null) {
						downRun = new Runnable() {
							public void run() {
								flyDown();
								increaseSteeringForce();
							}
						};
					}
					downFuture = keyExecutor.scheduleAtFixedRate(downRun, 0, TRANSLATION_PERIOD, TimeUnit.MILLISECONDS);
				}
				else if (e.isAltDown()) {
					if (orbitDownRun == null) {
						orbitDownRun = new Runnable() {
							public void run() {
								flyDown();
								pitchUp();
								increaseSteeringForce();
							}
						};
					}
					orbitDownFuture = keyExecutor.scheduleAtFixedRate(orbitDownRun, 0, TRANSLATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else {
					if (pitchDownRun == null) {
						pitchDownRun = new Runnable() {
							public void run() {
								pitchDown();
								increaseSteeringForce();
							}
						};
					}
					pitchDownFuture = keyExecutor.scheduleAtFixedRate(pitchDownRun, 0, ROTATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
			}
			break;
		case KeyEvent.VK_LEFT:
			if ((keyCode & LEFT_PRESSED) != LEFT_PRESSED) {
				keyCode = keyCode | LEFT_PRESSED;
				b = true;
				if (e.isShiftDown()) {
					if (leftRun == null) {
						leftRun = new Runnable() {
							public void run() {
								flyLeft();
								increaseSteeringForce();
							}
						};
					}
					leftFuture = keyExecutor.scheduleAtFixedRate(leftRun, 0, TRANSLATION_PERIOD, TimeUnit.MILLISECONDS);
				}
				else if (e.isAltDown()) {
					if (orbitLeftRun == null) {
						orbitLeftRun = new Runnable() {
							public void run() {
								flyLeft();
								yawCcw();
								increaseSteeringForce();
							}
						};
					}
					orbitLeftFuture = keyExecutor.scheduleAtFixedRate(orbitLeftRun, 0, TRANSLATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else {
					if (yawCwRun == null) {
						yawCwRun = new Runnable() {
							public void run() {
								yawCw();
								increaseSteeringForce();
							}
						};
					}
					yawCwFuture = keyExecutor.scheduleAtFixedRate(yawCwRun, 0, ROTATION_PERIOD, TimeUnit.MILLISECONDS);
				}
			}
			break;
		case KeyEvent.VK_RIGHT:
			if ((keyCode & RIGHT_PRESSED) != RIGHT_PRESSED) {
				keyCode = keyCode | RIGHT_PRESSED;
				b = true;
				if (e.isShiftDown()) {
					if (rightRun == null) {
						rightRun = new Runnable() {
							public void run() {
								flyRight();
								increaseSteeringForce();
							}
						};
					}
					rightFuture = keyExecutor.scheduleAtFixedRate(rightRun, 0, TRANSLATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else if (e.isAltDown()) {
					if (orbitRightRun == null) {
						orbitRightRun = new Runnable() {
							public void run() {
								flyRight();
								yawCw();
								increaseSteeringForce();
							}
						};
					}
					orbitRightFuture = keyExecutor.scheduleAtFixedRate(orbitRightRun, 0, TRANSLATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else {
					if (yawCcwRun == null) {
						yawCcwRun = new Runnable() {
							public void run() {
								yawCcw();
								increaseSteeringForce();
							}
						};
					}
					yawCcwFuture = keyExecutor
							.scheduleAtFixedRate(yawCcwRun, 0, ROTATION_PERIOD, TimeUnit.MILLISECONDS);
				}
			}
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
			if ((keyCode & Z_PRESSED) != Z_PRESSED) {
				keyCode = keyCode | Z_PRESSED;
				b = true;
				if (e.isShiftDown()) {
					if (rollCcwRun == null) {
						rollCcwRun = new Runnable() {
							public void run() {
								rollCcw();
								increaseSteeringForce();
							}
						};
					}
					rollCcwFuture = keyExecutor.scheduleAtFixedRate(rollCcwRun, 0, ROTATION_PERIOD,
							TimeUnit.MILLISECONDS);
				}
				else {
					if (rollCwRun == null) {
						rollCwRun = new Runnable() {
							public void run() {
								rollCw();
								increaseSteeringForce();
							}
						};
					}
					rollCwFuture = keyExecutor
							.scheduleAtFixedRate(rollCwRun, 0, ROTATION_PERIOD, TimeUnit.MILLISECONDS);
				}
			}
			break;
		}
		return b ? keyCode : 0;
	}

	private boolean isOtherPowerKeyStillPressed() {
		if ((keyCode & A_PRESSED) == A_PRESSED)
			return true;
		if ((keyCode & S_PRESSED) == S_PRESSED)
			return true;
		if ((keyCode & LEFT_PRESSED) == LEFT_PRESSED)
			return true;
		if ((keyCode & RIGHT_PRESSED) == RIGHT_PRESSED)
			return true;
		if ((keyCode & UP_PRESSED) == UP_PRESSED)
			return true;
		if ((keyCode & DOWN_PRESSED) == DOWN_PRESSED)
			return true;
		if ((keyCode & Z_PRESSED) == Z_PRESSED)
			return true;
		return false;
	}

	public int keyReleased(KeyEvent e) {
		boolean b = false;
		if (!e.isControlDown()) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				keyCode = keyCode ^ A_PRESSED;
				b = true;
				if (forwardFuture != null)
					forwardFuture.cancel(true);
				break;
			case KeyEvent.VK_S:
				keyCode = keyCode ^ S_PRESSED;
				b = true;
				if (backFuture != null)
					backFuture.cancel(true);
				break;
			case KeyEvent.VK_UP:
				keyCode = keyCode ^ UP_PRESSED;
				b = true;
				if (upFuture != null)
					upFuture.cancel(true);
				if (pitchUpFuture != null)
					pitchUpFuture.cancel(true);
				if (orbitUpFuture != null)
					orbitUpFuture.cancel(true);
				break;
			case KeyEvent.VK_DOWN:
				keyCode = keyCode ^ DOWN_PRESSED;
				b = true;
				if (downFuture != null)
					downFuture.cancel(true);
				if (pitchDownFuture != null)
					pitchDownFuture.cancel(true);
				if (orbitDownFuture != null)
					orbitDownFuture.cancel(true);
				break;
			case KeyEvent.VK_LEFT:
				keyCode = keyCode ^ LEFT_PRESSED;
				b = true;
				if (leftFuture != null)
					leftFuture.cancel(true);
				if (yawCwFuture != null)
					yawCwFuture.cancel(true);
				if (orbitLeftFuture != null)
					orbitLeftFuture.cancel(true);
				break;
			case KeyEvent.VK_RIGHT:
				keyCode = keyCode ^ RIGHT_PRESSED;
				b = true;
				if (rightFuture != null)
					rightFuture.cancel(true);
				if (yawCcwFuture != null)
					yawCcwFuture.cancel(true);
				if (orbitRightFuture != null)
					orbitRightFuture.cancel(true);
				break;
			case KeyEvent.VK_PAGE_UP:
				keyCode = keyCode ^ PGUP_PRESSED;
				b = true;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				keyCode = keyCode ^ PGDN_PRESSED;
				b = true;
				break;
			case KeyEvent.VK_X:
				keyCode = keyCode ^ X_PRESSED;
				b = true;
				break;
			case KeyEvent.VK_Y:
				keyCode = keyCode ^ Y_PRESSED;
				b = true;
				break;
			case KeyEvent.VK_Z:
				keyCode = keyCode ^ Z_PRESSED;
				b = true;
				if (rollCcwFuture != null)
					rollCcwFuture.cancel(true);
				if (rollCwFuture != null)
					rollCwFuture.cancel(true);
				break;
			}
		}
		if (b) {
			if (steering) {
				if (!isOtherPowerKeyStillPressed()) {
					if (steeringOffCallback != null)
						steeringOffCallback.run();
				}
			}
		}
		return keyCode;
	}

	private void increaseSteeringForce() {
		if (steering) {
			long t = System.currentTimeMillis();
			if (t - steerStartTime > gearTime) {
				steerStartTime = t;
				if (steeringUpCallback != null)
					steeringUpCallback.run();
			}
		}
	}

	private void fly(float x, float y, float z) {
		synchronized (navigationLock) {
			tmp.invert(viewer.getRotationMatrix());
			steeringDirection.set(x, y, z);
			tmp.transform(steeringDirection);
			if (steering) {
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				Point3f p = viewer.getCameraPosition();
				p.sub(steeringDirection);
				viewer.setCameraPosition(p.x, p.y, p.z);
				viewer.refresh();
			}
		}
	}

	private void flyForward() {
		if (viewer.getNavigationMode()) {
			fly(0, 0, 1);
		}
		else {
			synchronized (navigationLock) {
				viewer.setZoomPercent(viewer.getZoomPercent() + 2);
			}
		}
	}

	private void flyBack() {
		if (viewer.getNavigationMode()) {
			fly(0, 0, -1);
		}
		else {
			synchronized (navigationLock) {
				viewer.setZoomPercent(viewer.getZoomPercent() - 2);
			}
		}
	}

	private void flyUp() {
		if (viewer.getNavigationMode()) {
			fly(0, -1, 0);
		}
		else {
			synchronized (navigationLock) {
				viewer.translateBy(0, -1);
			}
		}
	}

	private void flyDown() {
		if (viewer.getNavigationMode()) {
			fly(0, 1, 0);
		}
		else {
			synchronized (navigationLock) {
				viewer.translateBy(0, 1);
			}
		}
	}

	private void flyLeft() {
		if (viewer.getNavigationMode()) {
			fly(1, 0, 0);
		}
		else {
			synchronized (navigationLock) {
				viewer.translateBy(-1, 0);
			}
		}
	}

	private void flyRight() {
		if (viewer.getNavigationMode()) {
			fly(-1, 0, 0);
		}
		else {
			synchronized (navigationLock) {
				viewer.translateBy(1, 0);
			}
		}
	}

	private void yawCw() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(0, -1, 0);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateYBy(-angularSpeed);
			}
		}
	}

	private void yawCcw() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(0, 1, 0);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateYBy(angularSpeed);
			}
		}
	}

	private void pitchUp() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(-1, 0, 0);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateXBy(-angularSpeed);
			}
		}
	}

	private void pitchDown() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(1, 0, 0);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateXBy(angularSpeed);
			}
		}
	}

	private void rollCw() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(0, 0, 1);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateZBy(rollingSpeed);
			}
		}
	}

	private void rollCcw() {
		synchronized (navigationLock) {
			if (steering) {
				rotateDirection.set(0, 0, -1);
				if (engineOnCallback != null)
					engineOnCallback.run();
			}
			else {
				viewer.rotateZBy(-rollingSpeed);
			}
		}
	}

	/* GUI support */

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setBackground(Color c) {
		backgroundColor = new Color(c.getRGB());
		contrastBackgroundColor = new Color(0xffffff ^ c.getRGB());
	}

	public int getLocationX() {
		return leftButton == null ? 0 : leftButton.x;
	}

	public int getLocationY() {
		return upButton == null ? 0 : upButton.y;
	}

	public void setLocation(int x, int y) {

		int j = y + BUTTON_SIZE + GAP;
		if (leftButton == null) {
			leftButton = new Rectangle(x, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			leftButton.x = x;
			leftButton.y = j;
		}

		int i = leftButton.x + leftButton.width + GAP;
		if (homeButton == null) {
			homeButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			homeButton.x = i;
			homeButton.y = j;
		}

		i = homeButton.x + homeButton.width + GAP;
		if (rightButton == null) {
			rightButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			rightButton.x = i;
			rightButton.y = j;
		}

		i = leftButton.x + leftButton.width + GAP;
		if (upButton == null) {
			upButton = new Rectangle(i, y, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			upButton.x = i;
			upButton.y = y;
		}

		j = upButton.y + upButton.height + 2 * GAP + BUTTON_SIZE;
		if (downButton == null) {
			downButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			downButton.x = i;
			downButton.y = j;
		}

		j = downButton.y + downButton.height + GAP;
		if (forwardButton == null) {
			forwardButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			forwardButton.x = i;
			forwardButton.y = j;
		}

		j = forwardButton.y + forwardButton.height + GAP;
		if (backButton == null) {
			backButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			backButton.x = i;
			backButton.y = j;
		}

		j = backButton.y + backButton.height + GAP + 10;
		if (yawCwButton == null) {
			yawCwButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			yawCwButton.x = i;
			yawCwButton.y = j;
		}

		j = yawCwButton.y + yawCwButton.height + GAP;
		if (yawCcwButton == null) {
			yawCcwButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			yawCcwButton.x = i;
			yawCcwButton.y = j;
		}

		j = yawCcwButton.y + yawCcwButton.height + GAP;
		if (pitchUpButton == null) {
			pitchUpButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			pitchUpButton.x = i;
			pitchUpButton.y = j;
		}

		j = pitchUpButton.y + pitchUpButton.height + GAP;
		if (pitchDownButton == null) {
			pitchDownButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			pitchDownButton.x = i;
			pitchDownButton.y = j;
		}

		j = pitchDownButton.y + pitchDownButton.height + GAP;
		if (rollCwButton == null) {
			rollCwButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			rollCwButton.x = i;
			rollCwButton.y = j;
		}

		j = rollCwButton.y + rollCwButton.height + GAP;
		if (rollCcwButton == null) {
			rollCcwButton = new Rectangle(i, j, BUTTON_SIZE, BUTTON_SIZE);
		}
		else {
			rollCcwButton.x = i;
			rollCcwButton.y = j;
		}

	}

	private void resetSelectionMap() {
		for (Rectangle r : selectionMap.keySet())
			selectionMap.put(r, Boolean.FALSE);
	}

	public boolean mouseHover(int x, int y) {
		if (!enabled)
			return false;
		resetSelectionMap();
		if (homeButton.contains(x, y)) {
			selectionMap.put(homeButton, Boolean.TRUE);
			return true;
		}
		if (leftButton.contains(x, y)) {
			selectionMap.put(leftButton, Boolean.TRUE);
			return true;
		}
		if (rightButton.contains(x, y)) {
			selectionMap.put(rightButton, Boolean.TRUE);
			return true;
		}
		if (upButton.contains(x, y)) {
			selectionMap.put(upButton, Boolean.TRUE);
			return true;
		}
		if (downButton.contains(x, y)) {
			selectionMap.put(downButton, Boolean.TRUE);
			return true;
		}
		if (forwardButton.contains(x, y)) {
			selectionMap.put(forwardButton, Boolean.TRUE);
			return true;
		}
		if (backButton.contains(x, y)) {
			selectionMap.put(backButton, Boolean.TRUE);
			return true;
		}
		if (yawCwButton.contains(x, y)) {
			selectionMap.put(yawCwButton, Boolean.TRUE);
			return true;
		}
		if (yawCcwButton.contains(x, y)) {
			selectionMap.put(yawCcwButton, Boolean.TRUE);
			return true;
		}
		if (pitchUpButton.contains(x, y)) {
			selectionMap.put(pitchUpButton, Boolean.TRUE);
			return true;
		}
		if (pitchDownButton.contains(x, y)) {
			selectionMap.put(pitchDownButton, Boolean.TRUE);
			return true;
		}
		if (rollCwButton.contains(x, y)) {
			selectionMap.put(rollCwButton, Boolean.TRUE);
			return true;
		}
		if (rollCcwButton.contains(x, y)) {
			selectionMap.put(rollCcwButton, Boolean.TRUE);
			return true;
		}
		return false;
	}

	public void paint(Graphics g) {

		if (!enabled)
			return;

		g.setColor(contrastBackgroundColor == null ? Color.white : contrastBackgroundColor);

		Graphics2D g2 = (Graphics2D) g;
		g2.draw(homeButton);
		g2.draw(leftButton);
		g2.draw(rightButton);
		g2.draw(upButton);
		g2.draw(downButton);
		g2.draw(forwardButton);
		g2.draw(backButton);
		g2.draw(yawCwButton);
		g2.draw(yawCcwButton);
		g2.draw(pitchUpButton);
		g2.draw(pitchDownButton);
		g2.draw(rollCwButton);
		g2.draw(rollCcwButton);

		FontMetrics fm = g.getFontMetrics();
		int fontHeight = fm.getHeight() >> 1;

		int x = leftButton.x + GAP;
		int y = leftButton.y + (leftButton.height >> 1);
		int x2, y2;
		String s;
		if (selectionMap.get(leftButton) == Boolean.TRUE) {
			g2.fill(leftButton);
			x2 = rightButton.x + rightButton.width * 3 / 2;
			y2 = y + fontHeight;
			s = "Go left (keyboard: Shift+Left Arrow)";
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x, y, x + leftButton.width - GAP * 2, y);
		g.drawLine(x, y, x + 5, y - 5);
		g.drawLine(x, y, x + 5, y + 5);
		g.drawLine(x + 1, y, x + 6, y - 5);
		g.drawLine(x + 1, y, x + 6, y + 5);

		x = homeButton.x + (homeButton.width >> 1);
		if (selectionMap.get(homeButton) == Boolean.TRUE) {
			g2.fill(homeButton);
			x2 = rightButton.x + rightButton.width * 3 / 2;
			y2 = y + fontHeight;
			s = "Return to starting position";
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawRect(x - 4, y - 1, 8, 6);
		g.drawRect(x - 1, y + 1, 2, 4);
		g.drawLine(x - 6, y - 1, x + 6, y - 1);
		g.drawLine(x - 6, y - 1, x, y - 5);
		g.drawLine(x + 6, y - 1, x, y - 5);

		x = rightButton.x + rightButton.width - GAP;
		if (selectionMap.get(rightButton) == Boolean.TRUE) {
			g2.fill(rightButton);
			x2 = rightButton.x + rightButton.width * 3 / 2;
			y2 = y + fontHeight;
			s = "Go right (keyboard: Shift+Right Arrow)";
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x, y, x - rightButton.width + 2 * GAP, y);
		g.drawLine(x, y, x - 5, y - 5);
		g.drawLine(x, y, x - 5, y + 5);
		g.drawLine(x - 1, y, x - 6, y - 5);
		g.drawLine(x - 1, y, x - 6, y + 5);

		x = upButton.x + (upButton.width >> 1);
		y = upButton.y + GAP;
		if (selectionMap.get(upButton) == Boolean.TRUE) {
			x2 = x + upButton.width;
			y2 = y + fontHeight;
			s = "Go up (keyboard: Shift+Up Arrow)";
			g2.fill(upButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x, y, x, y + upButton.height - 2 * GAP);
		g.drawLine(x, y, x + 5, y + 5);
		g.drawLine(x, y, x - 5, y + 5);
		g.drawLine(x, y + 1, x + 5, y + 6);
		g.drawLine(x, y + 1, x - 5, y + 6);

		y = downButton.y + downButton.height - GAP;
		if (selectionMap.get(downButton) == Boolean.TRUE) {
			x2 = x + downButton.width;
			y2 = downButton.y + (downButton.height >> 1) + fontHeight;
			s = "Go down (keyboard: Shift+Down Arrow)";
			g2.fill(downButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x, y, x, y - downButton.height + 2 * GAP);
		g.drawLine(x, y, x + 5, y - 5);
		g.drawLine(x, y, x - 5, y - 5);
		g.drawLine(x, y - 1, x + 5, y - 6);
		g.drawLine(x, y - 1, x - 5, y - 6);

		x = forwardButton.x + (forwardButton.width >> 1);
		y = forwardButton.y + (forwardButton.height >> 1);
		if (selectionMap.get(forwardButton) == Boolean.TRUE) {
			x2 = x + forwardButton.width;
			y2 = y + fontHeight;
			s = "Move forward (keyboard: \'A\')";
			g2.fill(forwardButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(forwardButton.x + GAP, y, forwardButton.x + forwardButton.width - GAP, y);
		g.drawLine(x, forwardButton.y + GAP, x, forwardButton.y + forwardButton.height - GAP);

		y = backButton.y + (backButton.height >> 1);
		if (selectionMap.get(backButton) == Boolean.TRUE) {
			x2 = x + backButton.width;
			y2 = y + fontHeight;
			s = "Reverse (keyboard: \'S\')";
			g2.fill(backButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(backButton.x + GAP, y, backButton.x + backButton.width - GAP, y);

		x = yawCwButton.x + (yawCwButton.width >> 1);
		y = yawCwButton.y + (yawCwButton.height >> 1);
		if (selectionMap.get(yawCwButton) == Boolean.TRUE) {
			x2 = x + yawCwButton.width;
			y2 = y + fontHeight;
			s = "Turn left (keyboard: Left Arrow)";
			g2.fill(yawCwButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x - 2, y - 5, x + 2, y + 3);
		g.drawLine(x - 5, y + 1, x + 5, y - 5);
		g.drawLine(x, y + 4, x + 4, y + 2);

		x = yawCcwButton.x + (yawCcwButton.width >> 1);
		y = yawCcwButton.y + (yawCcwButton.height >> 1);
		if (selectionMap.get(yawCcwButton) == Boolean.TRUE) {
			x2 = x + yawCcwButton.width;
			y2 = y + fontHeight;
			s = "Turn right (keyboard: Right Arrow)";
			g2.fill(yawCcwButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x + 2, y - 5, x - 2, y + 3);
		g.drawLine(x - 5, y - 5, x + 5, y + 1);
		g.drawLine(x, y + 4, x - 4, y + 2);

		x = pitchUpButton.x + (pitchUpButton.width >> 1);
		y = pitchUpButton.y + (pitchUpButton.height >> 1);
		if (selectionMap.get(pitchUpButton) == Boolean.TRUE) {
			x2 = x + pitchUpButton.width;
			y2 = y + fontHeight;
			s = "Pitch up (keyboard: Up Arrow)";
			g2.fill(pitchUpButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x - 5, y - 5, x + 3, y + 3);
		g.drawLine(x + 3, y + 3, x + 5, y + 1);
		g.drawLine(x - 3, y + 1, x + 1, y - 3);

		x = pitchDownButton.x + (pitchDownButton.width >> 1);
		y = pitchDownButton.y + (pitchDownButton.height >> 1);
		if (selectionMap.get(pitchDownButton) == Boolean.TRUE) {
			x2 = x + pitchDownButton.width;
			y2 = y + fontHeight;
			s = "Pitch down (keyboard: Down Arrow)";
			g2.fill(pitchDownButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawLine(x - 5, y + 5, x + 3, y - 3);
		g.drawLine(x + 3, y - 3, x + 1, y - 5);
		g.drawLine(x + 1, y + 3, x - 3, y - 1);

		x = rollCwButton.x + (rollCwButton.width >> 1);
		y = rollCwButton.y + (rollCwButton.height >> 1);
		if (selectionMap.get(rollCwButton) == Boolean.TRUE) {
			x2 = x + rollCwButton.width;
			y2 = y + fontHeight;
			s = "Roll right (keyboard: \'Z\')";
			g2.fill(rollCwButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawOval(x - 2, y - 2, 4, 4);
		g.drawLine(x - 6, y - 3, x + 6, y + 3);
		g.drawLine(x, y, x + 3, y - 6);

		x = rollCcwButton.x + (rollCcwButton.width >> 1);
		y = rollCcwButton.y + (rollCcwButton.height >> 1);
		if (selectionMap.get(rollCcwButton) == Boolean.TRUE) {
			x2 = x + rollCcwButton.width;
			y2 = y + fontHeight;
			s = "Roll left (keyboard: Shift+\'Z\')";
			g2.fill(rollCcwButton);
			g.fillRect(x2 - 5, y2 - fontHeight - 5, fm.stringWidth(s) + 15, fontHeight + 10);
			g.setColor(backgroundColor);
			g.drawString(s, x2, y2);
		}
		else {
			g.setColor(contrastBackgroundColor);
		}
		g.drawOval(x - 2, y - 2, 4, 4);
		g.drawLine(x - 6, y + 3, x + 6, y - 3);
		g.drawLine(x, y, x - 3, y - 6);

	}

}
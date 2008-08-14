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

package org.concord.mw2d.models;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;

import org.concord.modeler.math.Vector2D;

final class Statistics {

	private Statistics() {
	}

	public static double getMeanRx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					if (p[i].rQ == null || p[i].rQ.getPointer() <= 0) {
						average += p[i].rx;
					}
					else {
						average += p[i].rQ.getQueue1().getAverage();
					}
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].rx;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanRx2(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].rx * p[i].rx;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].rx * p[i].rx;
		return average / (toIndex - fromIndex);
	}

	public static double getRmsRx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double a = getMeanRx2(fromIndex, toIndex, p, selectedOnly);
		double b = getMeanRx(fromIndex, toIndex, p, selectedOnly);
		return Math.sqrt(a - b * b);
	}

	public static double getMeanRy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].ry;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].ry;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanRy2(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].ry * p[i].ry;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].ry * p[i].ry;
		return average / (toIndex - fromIndex);
	}

	public static double getRmsRy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double a = getMeanRy2(fromIndex, toIndex, p, selectedOnly);
		double b = getMeanRy(fromIndex, toIndex, p, selectedOnly);
		return Math.sqrt(a - b * b);
	}

	public static Point2D getCenterOfMass(int fromIndex, int toIndex, Particle[] p) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		for (int i = fromIndex; i < toIndex; i++) {
			mass += p[i].getMass();
			comx += p[i].getMass() * p[i].rx;
			comy += p[i].getMass() * p[i].ry;
		}
		return new Point2D.Double(comx / mass, comy / mass);
	}

	public static Vector2D getVelocityOfCenterOfMass(int fromIndex, int toIndex, Particle[] p) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		for (int i = fromIndex; i < toIndex; i++) {
			mass += p[i].getMass();
			comx += p[i].getMass() * p[i].vx;
			comy += p[i].getMass() * p[i].vy;
		}
		return new Vector2D(comx / mass, comy / mass);
	}

	public static double getRadiusOfGyration(int fromIndex, int toIndex, Atom[] atom) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double rx = getMeanRx(fromIndex, toIndex, atom, false);
		double ry = getMeanRy(fromIndex, toIndex, atom, false);
		double radg = 0.0;
		double mass = 0.0;
		for (int i = fromIndex; i < toIndex; i++) {
			mass += atom[i].getMass();
			radg += atom[i].getMass() * ((atom[i].rx - rx) * (atom[i].rx - rx) + (atom[i].ry - ry) * (atom[i].ry - ry));
		}
		return Math.sqrt(radg / mass);
	}

	public static double getMeanVx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vx;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vx;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanVy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vy;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vy;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanPx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vx * p[i].getMass();
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vx * p[i].getMass();
		return average / (toIndex - fromIndex);
	}

	public static double getMeanPy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vy * p[i].getMass();
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vy * p[i].getMass();
		return average / (toIndex - fromIndex);
	}

	public static double getMeanVx2(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vx * p[i].vx;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vx * p[i].vx;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanVy2(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vy * p[i].vy;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vy * p[i].vy;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanKx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vx * p[i].vx * p[i].getMass();
					count++;
				}
			}
			if (count == 0)
				return 0;
			return 0.5 * average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vx * p[i].vx * p[i].getMass();
		return 0.5 * average / (toIndex - fromIndex);
	}

	public static double getMeanKy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].vy * p[i].vy * p[i].getMass();
					count++;
				}
			}
			if (count == 0)
				return 0;
			return 0.5 * average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].vy * p[i].vy * p[i].getMass();
		return 0.5 * average / (toIndex - fromIndex);
	}

	public static double getMeanFx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].fx;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].fx;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanFy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].fy;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].fy;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanAx(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].ax;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].ax;
		return average / (toIndex - fromIndex);
	}

	public static double getMeanAy(int fromIndex, int toIndex, Particle[] p, boolean selectedOnly) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		if (selectedOnly) {
			int count = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				if (p[i].isSelected()) {
					average += p[i].ay;
					count++;
				}
			}
			if (count == 0)
				return 0;
			return average / count;
		}
		for (int i = fromIndex; i < toIndex; i++)
			average += p[i].ay;
		return average / (toIndex - fromIndex);
	}

	public static int getAverage(int fromIndex, int toIndex, int[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		float average = 0.0f;
		for (int i = fromIndex; i < toIndex; i++)
			average += input[i];
		return (int) (average / (toIndex - fromIndex));
	}

	public static float getAverage(int fromIndex, int toIndex, float[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		float average = 0.0f;
		for (int i = fromIndex; i < toIndex; i++)
			average += input[i];
		return average / (toIndex - fromIndex);
	}

	public static double getAverage(int fromIndex, int toIndex, double[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double average = 0.0;
		for (int i = fromIndex; i < toIndex; i++)
			average += input[i];
		return average / (toIndex - fromIndex);
	}

	public static double getRms(int fromIndex, int toIndex, double[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double rms = 0.0;
		double average = getAverage(fromIndex, toIndex, input);
		for (int i = fromIndex; i < toIndex; i++)
			rms += (input[i] - average) * (input[i] - average);
		return Math.sqrt(rms / (toIndex - fromIndex));
	}

	public static float getRms(int fromIndex, int toIndex, float[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double rms = 0.0;
		double average = getAverage(fromIndex, toIndex, input);
		for (int i = fromIndex; i < toIndex; i++)
			rms += (input[i] - average) * (input[i] - average);
		return (float) Math.sqrt(rms / (toIndex - fromIndex));
	}

	public static int getRms(int fromIndex, int toIndex, int[] input) {
		if (toIndex <= fromIndex)
			throw new IllegalArgumentException("toIndex<=fromIndex");
		double rms = 0.0;
		double average = getAverage(fromIndex, toIndex, input);
		for (int i = fromIndex; i < toIndex; i++)
			rms += (input[i] - average) * (input[i] - average);
		return (int) Math.sqrt(rms / (toIndex - fromIndex));
	}

	static double getMeanRx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).rx;
		return average / list.size();
	}

	static double getMeanRy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).ry;
		return average / list.size();
	}

	static double getMeanRx2(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.rx * p.rx;
		}
		return average / list.size();
	}

	static double getMeanRy2(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.ry * p.ry;
		}
		return average / list.size();
	}

	static double getRxRms(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double a = getMeanRx2(list);
		double b = getMeanRx(list);
		return Math.sqrt(a - b * b);
	}

	static double getRyRms(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double a = getMeanRy2(list);
		double b = getMeanRy(list);
		return Math.sqrt(a - b * b);
	}

	static Point2D getCenterOfMass(List list) {
		if (list == null || list.isEmpty())
			return null;
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			mass += p.getMass();
			comx += p.getMass() * p.rx;
			comy += p.getMass() * p.ry;
		}
		mass = 1.0 / mass;
		return new Point2D.Double(comx * mass, comy * mass);
	}

	static Vector2D getVelocityOfCenterOfMass(List list) {
		if (list == null || list.isEmpty())
			return null;
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		Particle p;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				p = (Particle) it.next();
				mass += p.getMass();
				comx += p.getMass() * p.vx;
				comy += p.getMass() * p.vy;
			}
		}
		mass = 1.0 / mass;
		return new Vector2D(comx * mass, comy * mass);
	}

	static Vector2D getMomentumOfCenterOfMass(List list) {
		if (list == null || list.isEmpty())
			return null;
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		Particle p;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				p = (Particle) it.next();
				mass += p.getMass();
				comx += p.getMass() * p.getMass() * p.vx;
				comy += p.getMass() * p.getMass() * p.vy;
			}
		}
		mass = 1.0 / mass;
		return new Vector2D(comx * mass, comy * mass);
	}

	static Vector2D getAccelerationOfCenterOfMass(List list) {
		if (list == null || list.isEmpty())
			return null;
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		Particle p;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				p = (Particle) it.next();
				mass += p.getMass();
				comx += p.getMass() * p.ax;
				comy += p.getMass() * p.ay;
			}
		}
		mass = 1.0 / mass;
		return new Vector2D(comx * mass, comy * mass);
	}

	static Vector2D getForceOfCenterOfMass(List list) {
		if (list == null || list.isEmpty())
			return null;
		double comx = 0.0, comy = 0.0;
		double mass = 0.0;
		Particle p;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				p = (Particle) it.next();
				mass += p.getMass();
				comx += p.getMass() * p.getMass() * p.ax;
				comy += p.getMass() * p.getMass() * p.ay;
			}
		}
		mass = 1.0 / mass;
		return new Vector2D(comx * mass, comy * mass);
	}

	static double getRadiusOfGyration(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double rx = getMeanRx(list);
		double ry = getMeanRy(list);
		double radg = 0.0;
		double mass = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			mass += p.getMass();
			radg += p.getMass() * ((p.rx - rx) * (p.rx - rx) + (p.ry - ry) * (p.ry - ry));
		}
		return Math.sqrt(radg / mass);
	}

	static double getMeanVx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).vx;
		return average / list.size();
	}

	static double getMeanVy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).vy;
		return average / list.size();
	}

	static double getMeanPx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.vx * p.getMass();
		}
		return average / list.size();
	}

	static double getMeanPy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.vy * p.getMass();
		}
		return average / list.size();
	}

	static double getMeanVx2(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		double v = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			v = ((Particle) it.next()).vx;
			average += v * v;
		}
		return average / list.size();
	}

	static double getMeanVy2(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		double v = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			v = ((Particle) it.next()).vy;
			average += v * v;
		}
		return average / list.size();
	}

	static double getMeanKx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.vx * p.vx * p.getMass();
		}
		return 0.5 * average / list.size();
	}

	static double getMeanKy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		Particle p;
		for (Iterator it = list.iterator(); it.hasNext();) {
			p = (Particle) it.next();
			average += p.vy * p.vy * p.getMass();
		}
		return 0.5 * average / list.size();
	}

	static double getMeanFx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).fx;
		return average / list.size();
	}

	static double getMeanFy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).fy;
		return average / list.size();
	}

	static double getMeanAx(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).ax;
		return average / list.size();
	}

	static double getMeanAy(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double average = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();)
			average += ((Particle) it.next()).ay;
		return average / list.size();
	}

}
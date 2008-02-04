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

package org.concord.modeler.util;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.modeler.g2d.AxisLabel;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.CurveFlavor;
import org.concord.modeler.g2d.CurveGroup;
import org.concord.modeler.g2d.Legend;
import org.concord.modeler.g2d.XYGrapher;

public final class DataQueueUtilities {

	private DataQueueUtilities() {
	}

	public static float getDotProduct(FloatQueue q1, FloatQueue q2) {
		float s = 0.0f;
		if (q1 != q2) {
			float[] a1 = (float[]) q1.getData();
			float[] a2 = (float[]) q2.getData();
			int pointer = Math.min(q1.getPointer(), q2.getPointer());
			for (int i = 0; i < pointer; i++)
				s += a1[i] * a2[i];
			s /= pointer;
		}
		else {
			float[] a = (float[]) q1.getData();
			int pointer = q1.getPointer();
			for (int i = 0; i < pointer; i++)
				s += a[i] * a[i];
			s /= pointer;
		}
		return s;
	}

	/**
	 * two queues are considered mismatching if they do not have the same length, sampling interval, and their pointers
	 * do not point at the same position.
	 */
	public static boolean mismatch(DataQueue q1, DataQueue q2) {
		if (q1 == null || q2 == null)
			throw new IllegalArgumentException("Null inputs");
		return q1.getLength() != q2.getLength() || q1.getPointer() != q2.getPointer()
				|| q1.getInterval() != q2.getInterval();
	}

	public static void showNoDataMessage(Frame owner) {
		JOptionPane.showMessageDialog(owner, "The selected data set is empty, as the simulation is not recorded.",
				"Empty Data Set", JOptionPane.ERROR_MESSAGE);
	}

	/** show a data queue as a time series in a dialog window. */
	public static void show(final DataQueue q, final Frame owner) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				show2(q, owner);
			}
		});
	}

	static void show2(DataQueue q, Frame owner) {

		if (q.pointer <= 0) {
			JOptionPane.showMessageDialog(owner, "No data to plot. Collect some and come back.", "Empty Data Set",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (q instanceof ObjectQueue)
			throw new RuntimeException("An object queue is generally not plottable");
		if (q.coordinateQ == null)
			throw new RuntimeException("A coordinate queue is needed in order to plot the data stored in this queue");
		if (q.coordinateQ.getLength() != q.getLength())
			throw new MismatchException("The coodinate queue does not have the same length");
		if (q.coordinateQ.getInterval() != q.getInterval())
			throw new MismatchException("The coodinate queue does not have the same sampling interval: "
					+ q.getInterval() + "!=" + q.coordinateQ.getInterval());

		XYGrapher graph = new XYGrapher();
		CurveGroup cg = new CurveGroup(q.name, new AxisLabel("Time (fs)"), new AxisLabel(q.name));
		if (q.coordinateQ instanceof FloatQueue && q instanceof FloatQueue) {
			cg.addCurve(new Curve(q.coordinateQ, q, new CurveFlavor(), new Legend(q.name)));
		}
		graph.input(cg);

		JDialog dialog = new JDialog(owner, cg.getTitle(), true);
		graph.setDialog(dialog);
		dialog.getContentPane().add(graph, BorderLayout.CENTER);
		dialog.setSize(300, 300);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);

	}

	/** show a set of data queues in a dialog window. */
	public static void show(final DataQueue[] q, final Frame owner) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				show3(q, owner);
			}
		});
	}

	static void show3(DataQueue[] q, Frame owner) {

		if (q.length == 0) {
			JOptionPane.showMessageDialog(owner, "The selected data set is empty.", "Empty Data Set",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (q[0].pointer <= 0) {
			JOptionPane.showMessageDialog(owner, "No data to plot. Collect some and come back.", "Empty Data Set",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		for (int i = 0; i < q.length; i++) {
			if (q[i] instanceof ObjectQueue)
				throw new RuntimeException("An object queue is generally not plottable");
			if (q[i].coordinateQ == null)
				throw new RuntimeException(
						"A coordinate queue is needed in order to plot the data stored in this queue");
			if (q[i].coordinateQ.getLength() != q[i].getLength())
				throw new MismatchException("The coodinate queue does not have the same length");
			if (q[i].coordinateQ.getInterval() != q[i].getInterval())
				throw new MismatchException("The coodinate queue does not have the same sampling interval");
		}

		XYGrapher graph = new XYGrapher();
		CurveGroup cg = new CurveGroup("Functions", new AxisLabel("x"), new AxisLabel("y"));
		for (int i = 0; i < q.length; i++) {
			if (q[i].coordinateQ instanceof FloatQueue && q[i] instanceof FloatQueue) {
				cg.addCurve(new Curve(q[i].coordinateQ, q[i], new CurveFlavor(i), new Legend(q[i].name)));
			}
		}
		graph.input(cg);
		graph.setLegendLocation(100, 100);

		JDialog dialog = new JDialog(owner, cg.getTitle(), false);
		graph.setDialog(dialog);
		dialog.getContentPane().add(graph, BorderLayout.CENTER);
		dialog.setSize(300, 300);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);

	}

}
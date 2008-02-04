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

/**
 * <p>
 * This class computes the electrostatic forces between a pair of united atoms of any type, including the Gay-Berne-type
 * particles. The electrostatic forces are decomposed into three parts for polar united atoms:
 * </p>
 * 
 * <p>
 * <ul>
 * <li>charge-charge</li>
 * <li>charge-dipole</li>
 * <li>dipole-dipole</li>
 * </ul>
 * </p>
 * 
 * <p>
 * To use this class, first instantiate it, check in the two united-atom particles using the <tt>checkin</tt> method,
 * then you can obtain the force and torque components on each particle by calling the corresponding methods.
 * </p>
 * 
 * @author Qian Xie
 */

final class ElectrostaticForce {

	private final static double ZERO = 0.000001;

	private boolean hasCharge_i, hasCharge_j;
	private boolean hasDipole_i, hasDipole_j;
	private double theta_i, theta_j, theta_ij;
	private double cosTheta_i, sinTheta_i;
	private double cosTheta_j, sinTheta_j;
	private double cosTheta_ij, sinTheta_ij;
	private double x_ij, y_ij, r_ij, rsq_ij;
	private double rijui, rijuj, uiuj;
	private double dip_ij, cou_ij;
	private double rCD = 50.0;
	private double piqj, pjqi;

	/**
	 * @param universe
	 *            the universe, this input is needed if you want to specify a dielectric constant etc.
	 * @param ua_i
	 *            the first united atom
	 * @param ua_j
	 *            the second united atom
	 * @param x_ij
	 *            the x component of the distance vector
	 * @param y_ij
	 *            the y component of the distance vector
	 * @param r_ij
	 *            the length of the distance vector (x_ij, y_ij and r_ij should be given because under periodic boundary
	 *            conditions they might be different from what would be calculated from the two passed united atom
	 *            objects.
	 */
	public void checkin(Universe universe, UnitedAtom ua_i, UnitedAtom ua_j, double x_ij, double y_ij, double r_ij) {

		if (universe != null)
			rCD = universe.getCoulombConstant() / universe.getDielectricConstant();

		hasCharge_i = Math.abs(ua_i.charge) > ZERO;
		hasCharge_j = Math.abs(ua_j.charge) > ZERO;
		hasDipole_i = Math.abs(ua_i.dipoleMoment) > ZERO;
		hasDipole_j = Math.abs(ua_j.dipoleMoment) > ZERO;

		rsq_ij = r_ij * r_ij;

		if (hasCharge_i && hasCharge_j)
			cou_ij = ua_i.charge * ua_j.charge / r_ij;

		if (hasDipole_i) {
			theta_i = ua_i.theta;
			cosTheta_i = Math.cos(theta_i);
			sinTheta_i = Math.sin(theta_i);
			rijui = cosTheta_i * x_ij + sinTheta_i * y_ij;
			rijui /= r_ij;
		}

		if (hasDipole_j) {
			theta_j = ua_j.theta;
			cosTheta_j = Math.cos(theta_j);
			sinTheta_j = Math.sin(theta_j);
			rijuj = cosTheta_j * x_ij + sinTheta_j * y_ij;
			rijuj /= r_ij;
		}

		if (hasDipole_i && hasDipole_j) {
			theta_ij = theta_i - theta_j;
			cosTheta_ij = Math.cos(theta_ij);
			sinTheta_ij = Math.sin(theta_ij);
			dip_ij = ua_i.dipoleMoment * ua_j.dipoleMoment / (r_ij * r_ij * r_ij);
			uiuj = cosTheta_ij;
		}

		if (hasDipole_i && hasCharge_j) {
			piqj = ua_i.dipoleMoment * ua_j.charge / rsq_ij;
		}

		if (hasDipole_j && hasCharge_i) {
			pjqi = ua_j.dipoleMoment * ua_i.charge / rsq_ij;
		}

		this.x_ij = x_ij;
		this.y_ij = y_ij;
		this.r_ij = r_ij;

	}

	/** return the x component of force on the i particle */
	public double fx_i() {
		double d = 0.0;
		if (hasCharge_i && hasCharge_j)
			d += chargeChargeVsX_i();
		if (hasDipole_i && hasDipole_j)
			d += dipoleDipoleVsX_i();
		if (hasCharge_i && hasDipole_j)
			d += pjqiVsX_i();
		if (hasDipole_i && hasCharge_j)
			d += piqjVsX_i();
		return d * rCD;
	}

	/** return the y component of force on the i particle */
	public double fy_i() {
		double d = 0.0;
		if (hasCharge_i && hasCharge_j)
			d += chargeChargeVsY_i();
		if (hasDipole_i && hasDipole_j)
			d += dipoleDipoleVsY_i();
		if (hasCharge_i && hasDipole_j)
			d += pjqiVsY_i();
		if (hasDipole_i && hasCharge_j)
			d += piqjVsY_i();
		return d * rCD;
	}

	/** return the torque on the i particle */
	public double torque_i() {
		double d = 0.0;
		if (hasDipole_i && hasDipole_j)
			d += dipoleDipoleVsTheta_i();
		if (hasDipole_i && hasCharge_j)
			d += piqjVsTheta_i();
		return d * rCD;
	}

	/** return the torque on the j particle */
	public double torque_j() {
		double d = 0.0;
		if (hasDipole_i && hasDipole_j)
			d += dipoleDipoleVsTheta_j();
		if (hasCharge_i && hasDipole_j)
			d += pjqiVsTheta_j();
		return d * rCD;
	}

	/** return the total electrostatic energy (charge-charge + charge-dipole + dipole-dipole, if any) */
	public double energy() {
		double d = 0.0;
		if (hasCharge_i && hasCharge_j)
			d += cou_ij;
		if (hasDipole_i && hasDipole_j)
			d += dipoleDipoleEnergy();
		if (hasCharge_i && hasDipole_j)
			d += pjqi * rijuj;
		if (hasDipole_i && hasCharge_j)
			d += piqj * rijui;
		return d * rCD;
	}

	private double dipoleDipoleEnergy() {
		return dip_ij * (uiuj - 3.0 * rijui * rijuj);
	}

	private double dipoleDipoleVsTheta_i() {
		return dip_ij * (-sinTheta_ij - 3.0 * rijuj / r_ij * (-x_ij * sinTheta_i + y_ij * cosTheta_i));
	}

	private double dipoleDipoleVsTheta_j() {
		return dip_ij * (sinTheta_ij - 3.0 * rijui / r_ij * (-x_ij * sinTheta_j + y_ij * cosTheta_j));
	}

	private double dipoleDipoleVsX_i() {
		return -3.0 * dip_ij * x_ij / rsq_ij * (uiuj - 3.0 * rijui * rijuj) + dip_ij
				* (6.0 * x_ij / rsq_ij * rijui * rijuj - 3.0 / r_ij * (cosTheta_i * rijuj + cosTheta_j * rijui));
	}

	private double dipoleDipoleVsY_i() {
		return -3.0 * dip_ij * y_ij / rsq_ij * (uiuj - 3.0 * rijui * rijuj) + dip_ij
				* (6.0 * y_ij / rsq_ij * rijui * rijuj - 3.0 / r_ij * (sinTheta_i * rijuj + sinTheta_j * rijui));
	}

	private double chargeChargeVsX_i() {
		return -x_ij / rsq_ij * cou_ij;
	}

	private double chargeChargeVsY_i() {
		return -y_ij / rsq_ij * cou_ij;
	}

	private double piqjVsX_i() {
		return piqj * (-3.0 * x_ij / rsq_ij * rijui + cosTheta_i / r_ij);
	}

	private double piqjVsY_i() {
		return piqj * (-3.0 * y_ij / rsq_ij * rijui + sinTheta_i / r_ij);
	}

	private double piqjVsTheta_i() {
		return piqj * (-sinTheta_i * x_ij + cosTheta_i * y_ij) / r_ij;
	}

	private double pjqiVsX_i() {
		return pjqi * (-3.0 * x_ij / rsq_ij * rijuj + cosTheta_j / r_ij);
	}

	private double pjqiVsY_i() {
		return pjqi * (-3.0 * y_ij / rsq_ij * rijuj + sinTheta_j / r_ij);
	}

	private double pjqiVsTheta_j() {
		return pjqi * (-sinTheta_j * x_ij + cosTheta_j * y_ij) / r_ij;
	}

}
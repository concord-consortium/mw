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
 * this collects methods for computing the intermolecular forces involving Gay-Berne particles. <b>Warning:</b> In
 * order to avoid repeating computations of same terms, many of the methods in this factory rely on shared data which is
 * hiden from client programmer. It is advised not to use these methods directly unless you know anything about the
 * Gay-Berne model.
 * 
 * <p>
 * The outputs of this class are:
 * <ol>
 * <li>potential energy of a pair of GB molecules</li>
 * <li>the x, y-components of the forces the centers of mass feel</li>
 * <li>the torques on the two GB molecules</li>
 * </ol>
 * 
 * @author Qian Xie
 */

final class GayBerneForce {

	private final static double INFINITESIMAL = 0.01;

	private boolean locked = true;

	/** this is the shared data zone for all the methods */
	private double length_i, breadth_i;
	private double length_j, breadth_j;
	private double sigma0, sigma;
	private double theta_i, theta_j, theta_ij;
	private double cosTheta_i, sinTheta_i;
	private double cosTheta_j, sinTheta_j;
	private double cosTheta_ij, sinTheta_ij;
	private double x_ij, y_ij, r_ij, invR_ij;
	private double chi, alp, invAlp;
	private double chi1;
	private double rijui, rijuj, uiuj;
	private double pij, qij, pdij, qdij;
	private double pij1, qij1, pdij1, qdij1;
	private double mu, nu;
	private double epsilon0;
	private double reduce6, reduce12;
	private double reduce7, reduce13;
	private double epsilon1, epsilon2;
	private double fourEpsilon, fourEpsilon1, fourEpsilon2;
	private double power_eps1_nu;
	private double power_eps1_nu1;
	private double power_eps2_mu;
	private double power_eps2_mu1;

	public GayBerneForce() {
	}

	/**
	 * create a factory with adjustable parameters.
	 * 
	 * @param mu
	 *            the power raised to epsilon2
	 * @param nu
	 *            the power raised to epsilon1
	 */
	public GayBerneForce(double mu, double nu) {
		this.mu = mu;
		this.nu = nu;
	}

	public synchronized void setMu(double mu) {
		this.mu = mu;
	}

	public synchronized void setNu(double nu) {
		this.nu = nu;
	}

	/**
	 * check in this factory
	 * 
	 * @param gb_i
	 *            the i-th GB
	 * @param gb_j
	 *            the j-th GB
	 * @param x_ij
	 *            the horizontal distance between i-th and j-th GB
	 * @param y_ij
	 *            the vertical distance between i-th and j-th GB
	 * @param r_ij
	 *            the distance between i-th and j-th GB
	 */
	public synchronized void checkin(GayBerneParticle gb_i, GayBerneParticle gb_j, double x_ij, double y_ij, double r_ij) {

		length_i = gb_i.length;
		length_j = gb_j.length;

		breadth_i = gb_i.breadth;
		breadth_j = gb_j.breadth;

		theta_i = gb_i.theta;
		theta_j = gb_j.theta;
		theta_ij = theta_i - theta_j;

		cosTheta_i = Math.cos(theta_i);
		sinTheta_i = Math.sin(theta_i);
		cosTheta_j = Math.cos(theta_j);
		sinTheta_j = Math.sin(theta_j);
		cosTheta_ij = Math.cos(theta_ij);
		sinTheta_ij = Math.sin(theta_ij);

		this.x_ij = x_ij;
		this.y_ij = y_ij;
		this.r_ij = r_ij;

		invR_ij = 1.0 / r_ij;

		rijui = cosTheta_i * x_ij + sinTheta_i * y_ij;
		rijui *= invR_ij;
		rijuj = cosTheta_j * x_ij + sinTheta_j * y_ij;
		rijuj *= invR_ij;
		uiuj = cosTheta_ij;

		sigma0 = Math.sqrt(0.5 * (breadth_i * breadth_i + breadth_j * breadth_j));

		chi = Math.sqrt(((length_i * length_i - breadth_i * breadth_i) * (length_j * length_j - breadth_j * breadth_j))
				/ ((length_j * length_j + breadth_i * breadth_i) * (length_i * length_i + breadth_j * breadth_j)));

		if (Math.abs(length_j - breadth_j) > 0.01) {
			alp = ((length_i * length_i - breadth_i * breadth_i) * (length_j * length_j + breadth_i * breadth_i))
					/ ((length_j * length_j - breadth_j * breadth_j) * (length_i * length_i + breadth_j * breadth_j));
			alp = Math.sqrt(Math.sqrt(alp));
		}
		else {
			alp = 1.0;
		}

		invAlp = 1.0 / alp;

		pij = (rijui * alp + rijuj * invAlp) * (rijui * alp + rijuj * invAlp) / (1.0 + chi * uiuj);
		qij = (rijui * alp - rijuj * invAlp) * (rijui * alp - rijuj * invAlp) / (1.0 - chi * uiuj);

		pdij = (rijui * alp + rijuj * invAlp) / (1.0 + chi * uiuj);
		qdij = (rijui * alp - rijuj * invAlp) / (1.0 - chi * uiuj);

		sigma = sigma0 / Math.sqrt(1.0 - 0.5 * chi * (pij + qij));

		double ratio = gb_i.eeVsEs * gb_j.eeVsEs;
		ratio = power(ratio, 0.5 / mu);
		chi1 = (1.0 - ratio) / (1.0 + ratio);

		/* orginal GB factors for epsilon2 */
		pij1 = (rijui + rijuj) * (rijui + rijuj) / (1.0 + chi1 * uiuj);
		qij1 = (rijui - rijuj) * (rijui - rijuj) / (1.0 - chi1 * uiuj);

		pdij1 = (rijui + rijuj) / (1.0 + chi1 * uiuj);
		qdij1 = (rijui - rijuj) / (1.0 - chi1 * uiuj);

		epsilon0 = Math.sqrt(gb_i.epsilon0 * gb_j.epsilon0);

		epsilon1 = 1.0 / Math.sqrt(1.0 - chi * chi * uiuj * uiuj);
		epsilon2 = 1.0 - 0.5 * chi1 * (pij1 + qij1);

		power_eps1_nu = power(epsilon1, nu);
		power_eps2_mu = power(epsilon2, mu);
		power_eps1_nu1 = power(epsilon1, nu - 1);
		power_eps2_mu1 = power(epsilon2, mu - 1);

		fourEpsilon = 4.0 * epsilon0 * power_eps1_nu * power_eps2_mu;
		fourEpsilon1 = fourEpsilon * 6.0 / sigma0;
		fourEpsilon2 = 4.0 * epsilon0 * mu * power_eps1_nu * power_eps2_mu1;

		locked = false;

	}

	/**
	 * unlocks the methods after checkin, lock them after force computataion for this pair of GB particles has finished.
	 * 
	 * @param value
	 *            true to lock, false to unlock
	 */
	public synchronized void lock(boolean value) {
		locked = value;
	}

	/**
	 * is this factory locked?
	 * 
	 * @return the current lock status
	 */
	public synchronized boolean isLocked() {
		return locked;
	}

	/** @return the distance between the centers of mass of this pair */
	public synchronized double distance() {
		return r_ij;
	}

	public synchronized double pij() {
		return pij;
	}

	public synchronized double qij() {
		return qij;
	}

	public synchronized double pij1() {
		return pij1;
	}

	public synchronized double qij1() {
		return qij1;
	}

	/** @return the geometric anisotropic ratio for this pair of GB particles. */
	public synchronized double chi() {
		return chi;
	}

	/** @return the energetic anisotropic ratio for this pair of GB particles. */
	public synchronized double chi1() {
		return chi1;
	}

	/** @return the orientation-dependent range parameter for this pair of GB particles. */
	public synchronized double sigma() {
		return sigma;
	}

	/** @return the first order derivative of sigma vs. theta_i */
	private synchronized double sigmaVsTheta_i() {
		double c = 2.0 * (-sinTheta_i * x_ij + cosTheta_i * y_ij) * alp * invR_ij;
		double deriv1 = pdij * c + pij / (1.0 + chi * uiuj) * chi * sinTheta_ij;
		double deriv2 = qdij * c - qij / (1.0 - chi * uiuj) * chi * sinTheta_ij;
		return 0.25 * sigma / (1.0 - 0.5 * chi * (pij + qij)) * chi * (deriv1 + deriv2);
	}

	/** @return the first order derivative of sigma vs. theta_j */
	private synchronized double sigmaVsTheta_j() {
		double c = 2.0 * (-sinTheta_j * x_ij + cosTheta_j * y_ij) * invAlp * invR_ij;
		double deriv1 = pdij * c - pij / (1.0 + chi * uiuj) * chi * sinTheta_ij;
		double deriv2 = -qdij * c + qij / (1.0 - chi * uiuj) * chi * sinTheta_ij;
		return 0.25 * sigma / (1.0 - 0.5 * chi * (pij + qij)) * chi * (deriv1 + deriv2);
	}

	/** @return the first order derivative of sigma vs. x_i */
	private synchronized double sigmaVsX_i() {
		return 0.5
				* sigma
				/ (1.0 - 0.5 * chi * (pij + qij))
				* chi
				* (pdij * (cosTheta_i * alp + cosTheta_j * invAlp) * invR_ij - pij * x_ij * invR_ij * invR_ij + qdij
						* (cosTheta_i * alp - cosTheta_j * invAlp) * invR_ij - qij * x_ij * invR_ij * invR_ij);
	}

	/** @return the first order derivative of sigma vs. y_i */
	private synchronized double sigmaVsY_i() {
		return 0.5
				* sigma
				/ (1.0 - 0.5 * chi * (pij + qij))
				* chi
				* (pdij * (sinTheta_i * alp + sinTheta_j * invAlp) * invR_ij - pij * y_ij * invR_ij * invR_ij + qdij
						* (sinTheta_i * alp - sinTheta_j * invAlp) * invR_ij - qij * y_ij * invR_ij * invR_ij);
	}

	/**
	 * calculate the first energy term
	 * 
	 * @return epsilon1
	 */
	public synchronized double epsilon1() {
		return epsilon1;
	}

	/** calculate the first order derivative of epsilon1 vs. theta_i */
	private synchronized double epsilon1VsTheta_i() {
		double a = 1.0 - chi * chi * cosTheta_ij * cosTheta_ij;
		return -1.0 / (Math.sqrt(a) * a) * chi * chi * cosTheta_ij * sinTheta_ij;
	}

	/** calculate the first order derivative of epsilon1 vs. theta_j */
	private synchronized double epsilon1VsTheta_j() {
		double a = 1.0 - chi * chi * cosTheta_ij * cosTheta_ij;
		return 1.0 / (Math.sqrt(a) * a) * chi * chi * cosTheta_ij * sinTheta_ij;
	}

	/**
	 * calculate the second energy term, in the original Gay-Berne form.
	 * 
	 * @return epsilon2
	 */
	public synchronized double epsilon2() {
		return epsilon2;
	}

	/** calculate the first order derivative of epsilon2 vs. theta_i, in the original Gay-Berne form. */
	private synchronized double epsilon2VsTheta_i() {
		double c = 2.0 * (-sinTheta_i * x_ij + cosTheta_i * y_ij) * invR_ij;
		double deriv1 = pdij1 * c + pij1 / (1.0 + chi1 * uiuj) * chi1 * sinTheta_ij;
		double deriv2 = qdij1 * c - qij1 / (1.0 - chi1 * uiuj) * chi1 * sinTheta_ij;
		return -0.5 * chi1 * (deriv1 + deriv2);
	}

	/** calculate the first order derivative of epsilon2 vs. theta_j, in the original Gay-Berne form. */
	private synchronized double epsilon2VsTheta_j() {
		double c = 2.0 * (-sinTheta_j * x_ij + cosTheta_j * y_ij) * invR_ij;
		double deriv1 = pdij1 * c - pij1 / (1.0 + chi1 * uiuj) * chi1 * sinTheta_ij;
		double deriv2 = -qdij1 * c + qij1 / (1.0 - chi1 * uiuj) * chi1 * sinTheta_ij;
		return -0.5 * chi1 * (deriv1 + deriv2);
	}

	/** calculate the first order derivate of epsilon2 vs. x_i, in the original Gay-Berne form. */
	private synchronized double epsilon2VsX_i() {
		return -chi1
				* (pdij1 * (cosTheta_i + cosTheta_j) * invR_ij - pij1 * x_ij * invR_ij * invR_ij + qdij1
						* (cosTheta_i - cosTheta_j) * invR_ij - qij1 * x_ij * invR_ij * invR_ij);
	}

	/** calculate the first order derivate of epsilon2 vs. y_i, in the original Gay-Berne form. */
	private synchronized double epsilon2VsY_i() {
		return -chi1
				* (pdij1 * (sinTheta_i + sinTheta_j) * invR_ij - pij1 * y_ij * invR_ij * invR_ij + qdij1
						* (sinTheta_i - sinTheta_j) * invR_ij - qij1 * y_ij * invR_ij * invR_ij);
	}

	/**
	 * calculate the potential energy.
	 * 
	 * @return the potential energy of this GB pair
	 */
	public synchronized double energy() {
		double temp = sigma0 / (r_ij - sigma + sigma0);
		double temp2 = temp * temp;
		reduce6 = temp2 * temp2 * temp2;
		reduce7 = temp * reduce6;
		reduce12 = reduce6 * reduce6;
		reduce13 = temp * reduce12;
		return fourEpsilon * (reduce12 - reduce6);
	}

	private synchronized double fourEpsilonVsTheta_i() {
		return 4.0
				* epsilon0
				* (nu * epsilon1VsTheta_i() * power_eps1_nu1 * power_eps2_mu + mu * epsilon2VsTheta_i()
						* power_eps2_mu1 * power_eps1_nu);
	}

	private synchronized double fourEpsilonVsTheta_j() {
		return 4.0
				* epsilon0
				* (nu * epsilon1VsTheta_j() * power_eps1_nu1 * power_eps2_mu + mu * epsilon2VsTheta_j()
						* power_eps2_mu1 * power_eps1_nu);
	}

	/** the torque the i-th particle feels */
	public synchronized double torque_i() {
		return fourEpsilonVsTheta_i() * (reduce12 - reduce6) + fourEpsilon1
				* (2.0 * reduce13 * sigmaVsTheta_i() - reduce7 * sigmaVsTheta_i());
	}

	/** the torque the j-th particle feels */
	public synchronized double torque_j() {
		return fourEpsilonVsTheta_j() * (reduce12 - reduce6) + fourEpsilon1
				* (2.0 * reduce13 * sigmaVsTheta_j() - reduce7 * sigmaVsTheta_j());
	}

	/** x-component force the center of mass of the i-th particle feels */
	public synchronized double fx_i() {
		return fourEpsilon2 * epsilon2VsX_i() * (reduce12 - reduce6) + fourEpsilon1 * (-2.0 * reduce13 + reduce7)
				* (x_ij * invR_ij - sigmaVsX_i());
	}

	/** y-component force the center of mass of the i-th particle feels */
	public synchronized double fy_i() {
		return fourEpsilon2 * epsilon2VsY_i() * (reduce12 - reduce6) + fourEpsilon1 * (-2.0 * reduce13 + reduce7)
				* (y_ij * invR_ij - sigmaVsY_i());
	}

	/**
	 * finish up. This object should be locked so that no one can call a computational method without checking in.
	 */
	public synchronized void finish() {
		locked = true;
	}

	private static double power(double x, double y) {
		double value;
		if (y < INFINITESIMAL && y > -INFINITESIMAL) {
			value = 1.0;
		}
		else if (y - 0.5 < INFINITESIMAL && y - 0.5 > -INFINITESIMAL) {
			value = Math.sqrt(x);
		}
		else if (y - 1.0 < INFINITESIMAL && y - 1.0 > -INFINITESIMAL) {
			value = x;
		}
		else if (y - 2.0 < INFINITESIMAL && y - 2.0 > -INFINITESIMAL) {
			value = x * x;
		}
		else {
			value = Math.pow(x, y);
		}
		return value;
	}

}
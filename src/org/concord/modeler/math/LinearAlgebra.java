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

package org.concord.modeler.math;

/**
 * Methods from Hang T. Lau, "A numerical library in Java for scientists and
 * engineers".*
 */

public final class LinearAlgebra {

	public static void dec(double a[][], int n, double aux[], int p[]) {

		int i, k, k1, pk, d;
		double r, s, eps;
		double v[] = new double[n + 1];

		pk = 0;
		r = -1.0;
		for (i = 1; i <= n; i++) {
			s = Math.sqrt(Basic.mattam(1, n, i, i, a, a));
			if (s > r)
				r = s;
			v[i] = 1.0 / s;
		}
		eps = aux[2] * r;
		d = 1;
		for (k = 1; k <= n; k++) {
			r = -1.0;
			k1 = k - 1;
			for (i = k; i <= n; i++) {
				a[i][k] -= Basic.matmat(1, k1, i, k, a, a);
				s = Math.abs(a[i][k]) * v[i];
				if (s > r) {
					r = s;
					pk = i;
				}
			}
			p[k] = pk;
			v[pk] = v[k];
			s = a[pk][k];
			if (Math.abs(s) < eps)
				break;
			if (s < 0.0)
				d = -d;
			if (pk != k) {
				d = -d;
				Basic.ichrow(1, n, k, pk, a);
			}
			for (i = k + 1; i <= n; i++)
				a[k][i] = (a[k][i] - Basic.matmat(1, k1, k, i, a, a)) / s;
		}
		aux[1] = d;
		aux[3] = k - 1;

	}

	public static void decinv(double a[][], int n, double aux[]) {
		int p[] = new int[n + 1];
		dec(a, n, aux, p);
		if (aux[3] == n)
			inv(a, n, p);
	}

	public static void inv(double a[][], int n, int p[]) {

		int j, k, k1;
		double r;
		double v[] = new double[n + 1];

		for (k = n; k >= 1; k--) {
			k1 = k + 1;
			for (j = n; j >= k1; j--) {
				a[j][k1] = v[j];
				v[j] = -Basic.matmat(k1, n, k, j, a, a);
			}
			r = a[k][k];
			for (j = n; j >= k1; j--) {
				a[k][j] = v[j];
				v[j] = -Basic.matmat(k1, n, j, k, a, a) / r;
			}
			v[k] = (1.0 - Basic.matmat(k1, n, k, k, a, a)) / r;
		}
		Basic.dupcolvec(1, n, 1, a, v);
		for (k = n - 1; k >= 1; k--) {
			k1 = p[k];
			if (k1 != k)
				Basic.ichcol(1, n, k, k1, a);
		}

	}

	public static void chldec2(double a[][], int n, double aux[]) {

		int k, j;
		double r = 0.0, epsnorm;
		for (k = 1; k <= n; k++)
			if (a[k][k] > r)
				r = a[k][k];
		epsnorm = aux[2] * r;
		for (k = 1; k <= n; k++) {
			r = a[k][k] - Basic.tammat(1, k - 1, k, k, a, a);
			if (r <= epsnorm) {
				aux[3] = k - 1;
				return;
			}
			a[k][k] = r = Math.sqrt(r);
			for (j = k + 1; j <= n; j++)
				a[k][j] = (a[k][j] - Basic.tammat(1, k - 1, j, k, a, a)) / r;
		}
		aux[3] = n;

	}

	public static void chlinv2(double a[][], int n) {

		int i, j, i1;
		double r;
		double u[] = new double[n + 1];

		for (i = n; i >= 1; i--) {
			r = 1.0 / a[i][i];
			i1 = i + 1;
			Basic.dupvecrow(i1, n, i, u, a);
			for (j = n; j >= i1; j--)
				a[i][j] = -(Basic.tamvec(i1, j, j, a, u) + Basic.matvec(j + 1, n, j, a, u)) * r;
			a[i][i] = (r - Basic.matvec(i1, n, i, a, u)) * r;
		}

	}

	public static void symeigimp(int n, double a[][], double vec[][], double val[], double lbound[], double ubound[],
			double aux[]) {

		boolean stop;
		int k, i, j, i0, i1, i01, iter, maxitp1, n1, i0m1, i1p1;
		double s, max, tol, mateps, relerra, reltolr, norma, eps2, dl, dr, m1, dtemp;
		int itmp[] = new int[1];
		int perm[] = new int[n + 1];
		double em[] = new double[6];
		double rq[] = new double[n + 1];
		double eps[] = new double[n + 1];
		double z[] = new double[n + 1];
		double val3[] = new double[n + 1];
		double eta[] = new double[n + 1];
		double r[][] = new double[n + 1][n + 1];
		double p[][] = new double[n + 1][n + 1];
		double y[][] = new double[n + 1][n + 1];

		max = 0.0;
		norma = Basic.infnrmmat(1, n, 1, n, itmp, a);
		i = itmp[0];
		relerra = aux[0];
		reltolr = aux[2];
		maxitp1 = (int) (aux[4] + 1.0);
		mateps = relerra * norma;
		tol = reltolr * norma;
		for (iter = 1; iter <= maxitp1; iter++) {
			if (iter == 1)
				stop = false;
			else stop = true;
			max = 0.0;
			for (j = 1; j <= n; j++)
				for (i = 1; i <= n; i++) {
					dtemp = -(vec[i][j]) * (val[j]);
					for (k = 1; k <= n; k++)
						dtemp += (a[i][k]) * (vec[k][j]);
					r[i][j] = dtemp;
					if (Math.abs(r[i][j]) > max)
						max = Math.abs(r[i][j]);
				}
			if (max > tol)
				stop = false;
			if ((!stop) && (iter < maxitp1)) {
				for (i = 1; i <= n; i++) {
					dtemp = (val[i]);
					for (k = 1; k <= n; k++)
						dtemp += (vec[k][i]) * (r[k][i]);
					rq[i] = dtemp;
				}
				for (j = 1; j <= n; j++) {
					for (i = 1; i <= n; i++)
						eta[i] = r[i][j] - (rq[j] - val[j]) * vec[i][j];
					z[j] = Math.sqrt(Basic.vecvec(1, n, 0, eta, eta));
				}
				mergesort(rq, perm, 1, n);
				vecperm(perm, 1, n, rq);
				for (i = 1; i <= n; i++) {
					eps[i] = z[perm[i]];
					val3[i] = val[perm[i]];
					rowperm(perm, 1, n, i, vec);
					rowperm(perm, 1, n, i, r);
				}
				for (i = 1; i <= n; i++)
					for (j = i; j <= n; j++)
						p[i][j] = p[j][i] = Basic.tammat(1, n, i, j, vec, r);
			}
			i0 = 1;
			do {
				j = i1 = i0;
				j++;
				while ((j > n) ? false : (rq[j] - rq[j - 1] <= Math.sqrt((eps[j] + eps[j - 1]) * norma))) {
					i1 = j;
					j++;
				}
				if (stop || (iter == maxitp1)) {
					i = i0;
					do {
						j = i01 = i;
						j++;
						while ((j > i1) ? false : rq[j] - rq[j - 1] <= eps[j] + eps[j - 1]) {
							i01 = j;
							j++;
						}
						if (i == i01) {
							if (i < n) {
								if (i == 1)
									dl = dr = rq[i + 1] - rq[i] - eps[i + 1];
								else {
									dl = rq[i] - rq[i - 1] - eps[i - 1];
									dr = rq[i + 1] - rq[i] - eps[i + 1];
								}
							}
							else dl = dr = rq[i] - rq[i - 1] - eps[i - 1];
							eps2 = eps[i] * eps[i];
							lbound[i] = eps2 / dr + mateps;
							ubound[i] = eps2 / dl + mateps;
						}
						else for (k = i; k <= i01; k++)
							lbound[k] = ubound[k] = eps[k] + mateps;
						i01++;
						i = i01;
					} while (i <= i1); /* bounds */
				}
				else {
					if (i0 == i1) {
						for (k = 1; k <= n; k++)
							if (k == i0)
								y[k][i0] = 1.0;
							else r[k][i0] = p[k][i0];
						val[i0] = rq[i0];
					}
					else {
						n1 = i1 - i0 + 1;
						em[0] = em[2] = Double.MIN_VALUE;
						em[4] = 10 * n1;
						double val4[] = new double[n1 + 1];
						double pp[][] = new double[n1 + 1][n1 + 1];
						m1 = 0.0;
						for (k = i0; k <= i1; k++)
							m1 += val3[k];
						m1 /= n1;
						for (i = 1; i <= n1; i++)
							for (j = 1; j <= n1; j++) {
								pp[i][j] = p[i + i0 - 1][j + i0 - 1];
								if (i == j)
									pp[i][j] += val3[j + i0 - 1] - m1;
							}
						for (i = i0; i <= i1; i++) {
							val3[i] = m1;
							val[i] = rq[i];
						}
						qrisym(pp, n1, val4, em);
						mergesort(val4, perm, 1, n1);
						for (i = 1; i <= n1; i++)
							for (j = 1; j <= n1; j++)
								p[i + i0 - 1][j + i0 - 1] = pp[i][perm[j]];
						i0m1 = i0 - 1;
						i1p1 = i1 + 1;
						for (j = i0; j <= i1; j++) {
							for (i = 1; i <= i0m1; i++) {
								s = 0.0;
								for (k = i0; k <= i1; k++)
									s += p[i][k] * p[k][j];
								r[i][j] = s;
							}
							for (i = i1p1; i <= n; i++) {
								s = 0.0;
								for (k = i0; k <= i1; k++)
									s += p[i][k] * p[k][j];
								r[i][j] = s;
							}
							for (i = i0; i <= i1; i++)
								y[i][j] = p[i][j];
						}
					} /* innerblock */
				} /* not stop */
				i0 = i1 + 1;
			} while (i0 <= n); /* while i0 loop */
			if ((!stop) && (iter < maxitp1)) {
				for (j = 1; j <= n; j++)
					for (i = 1; i <= n; i++)
						if (val3[i] != val3[j])
							y[i][j] = r[i][j] / (val3[j] - val3[i]);
				for (i = 1; i <= n; i++) {
					for (j = 1; j <= n; j++)
						z[j] = Basic.matmat(1, n, i, j, vec, y);
					for (j = 1; j <= n; j++)
						vec[i][j] = z[j];
				}
				orthog(n, 1, n, vec);
			}
			else {
				aux[5] = iter - 1;
				break;
			}
		} /* for iter loop */
		aux[1] = norma;
		aux[3] = max;
	}

	public static void mergesort(double a[], int p[], int low, int up) {

		int i, lo, step, stap, umlp1, umsp1, rest, restv;
		int hp[] = new int[up + 1];

		for (i = low; i <= up; i++)
			p[i] = i;
		restv = 0;
		umlp1 = up - low + 1;
		step = 1;
		do {
			stap = 2 * step;
			umsp1 = up - stap + 1;
			for (lo = low; lo <= umsp1; lo += stap)
				merge(lo, step, step, p, a, hp);
			rest = up - lo + 1;
			if (rest > restv && restv > 0)
				merge(lo, rest - restv, restv, p, a, hp);
			restv = rest;
			step *= 2;
		} while (step < umlp1);
	}

	public static void orthog(int n, int lc, int uc, double x[][]) {

		int i, j, k;
		double normx;

		for (j = lc; j <= uc; j++) {
			normx = Math.sqrt(Basic.tammat(1, n, j, j, x, x));
			for (i = 1; i <= n; i++)
				x[i][j] /= normx;
			for (k = j + 1; k <= uc; k++)
				Basic.elmcol(1, n, k, j, x, x, -Basic.tammat(1, n, k, j, x, x));
		}

	}

	public static void vecperm(int perm[], int low, int upp, double vector[]) {

		int t, j, k;
		double a;
		boolean todo[] = new boolean[upp + 1];

		for (t = low; t <= upp; t++)
			todo[t] = true;
		for (t = low; t <= upp; t++)
			if (todo[t]) {
				k = t;
				a = vector[k];
				j = perm[k];
				while (j != t) {
					vector[k] = vector[j];
					todo[k] = false;
					k = j;
					j = perm[k];
				}
				vector[k] = a;
				todo[k] = false;
			}
	}

	/* this procedure is used internally by MERGESORT */
	private static void merge(int lo, int ls, int rs, int p[], double a[], int hp[]) {

		int l, r, i, pl, pr;
		boolean lout, rout;

		l = lo;
		r = lo + ls;
		lout = rout = false;
		i = lo;
		do {
			pl = p[l];
			pr = p[r];
			if (a[pl] > a[pr]) {
				hp[i] = pr;
				r++;
				rout = (r == lo + ls + rs);
			}
			else {
				hp[i] = pl;
				l++;
				lout = (l == lo + ls);
			}
			i++;
		} while (!(lout || rout));
		if (rout) {
			for (i = lo + ls - 1; i >= l; i--)
				p[i + rs] = p[i];
			r = l + rs;
		}
		for (i = r - 1; i >= lo; i--)
			p[i] = hp[i];
	}

	public static void rowperm(int perm[], int low, int upp, int i, double mat[][]) {

		int t, j, k;
		double a;
		boolean todo[] = new boolean[upp + 1];

		for (t = low; t <= upp; t++)
			todo[t] = true;
		for (t = low; t <= upp; t++)
			if (todo[t]) {
				k = t;
				a = mat[i][k];
				j = perm[k];
				while (j != t) {
					mat[i][k] = mat[i][j];
					todo[k] = false;
					k = j;
					j = perm[k];
				}
				mat[i][k] = a;
				todo[k] = false;
			}

	}

	public static int qrisym(double a[][], int n, double val[], double em[]) {

		int i;
		double b[] = new double[n + 1];
		double bb[] = new double[n + 1];
		tfmsymtri2(a, n, val, b, bb, em);
		tfmprevec(a, n);
		i = qrisymtri(a, n, val, b, bb, em);
		return i;

	}

	public static void tfmsymtri2(double a[][], int n, double d[], double b[], double bb[], double em[]) {

		int i, j, r, r1;
		double w, x, a1, b0, bb0, machtol, norm;

		norm = 0.0;
		for (j = 1; j <= n; j++) {
			w = 0.0;
			for (i = 1; i <= j; i++)
				w += Math.abs(a[i][j]);
			for (i = j + 1; i <= n; i++)
				w += Math.abs(a[j][i]);
			if (w > norm)
				norm = w;
		}
		machtol = em[0] * norm;
		em[1] = norm;
		r = n;
		for (r1 = n - 1; r1 >= 1; r1--) {
			d[r] = a[r][r];
			x = Basic.tammat(1, r - 2, r, r, a, a);
			a1 = a[r1][r];
			if (Math.sqrt(x) <= machtol) {
				b0 = b[r1] = a1;
				bb[r1] = b0 * b0;
				a[r][r] = 1.0;
			}
			else {
				bb0 = bb[r1] = a1 * a1 + x;
				b0 = (a1 > 0.0) ? -Math.sqrt(bb0) : Math.sqrt(bb0);
				a1 = a[r1][r] = a1 - b0;
				w = a[r][r] = 1.0 / (a1 * b0);
				for (j = 1; j <= r1; j++)
					b[j] = (Basic.tammat(1, j, j, r, a, a) + Basic.matmat(j + 1, r1, j, r, a, a)) * w;
				Basic.elmveccol(1, r1, r, b, a, Basic.tamvec(1, r1, r, a, b) * w * 0.5);
				for (j = 1; j <= r1; j++) {
					Basic.elmcol(1, j, j, r, a, a, b[j]);
					Basic.elmcolvec(1, j, j, a, b, a[j][r]);
				}
				b[r1] = b0;
			}
			r = r1;
		}
		d[1] = a[1][1];
		a[1][1] = 1.0;
		b[n] = bb[n] = 0.0;
	}

	public static void tfmprevec(double a[][], int n) {

		int i, j, j1, k;
		double ab;

		j1 = 1;
		for (j = 2; j <= n; j++) {
			for (i = 1; i <= j1 - 1; i++)
				a[i][j1] = 0.0;
			for (i = j; i <= n; i++)
				a[i][j1] = 0.0;
			a[j1][j1] = 1.0;
			ab = a[j][j];
			if (ab < 0)
				for (k = 1; k <= j1; k++)
					Basic.elmcol(1, j1, k, j, a, a, Basic.tammat(1, j1, j, k, a, a) * ab);
			j1 = j;
		}
		for (i = n - 1; i >= 1; i--)
			a[i][n] = 0.0;
		a[n][n] = 1.0;
	}

	public static int qrisymtri(double a[][], int n, double d[], double b[], double bb[], double em[]) {

		int j, j1, k, m, m1, count, max;
		double bbmax, r, s, sin, t, cos, oldcos, g, p, w, tol, tol2, lambda, dk1;

		g = 0.0;
		tol = em[2] * em[1];
		tol2 = tol * tol;
		count = 0;
		bbmax = 0.0;
		max = (int) em[4];
		m = n;
		do {
			k = m;
			m1 = m - 1;
			while (true) {
				k--;
				if (k <= 0)
					break;
				if (bb[k] < tol2) {
					if (bb[k] > bbmax)
						bbmax = bb[k];
					break;
				}
			}
			if (k == m1)
				m = m1;
			else {
				t = d[m] - d[m1];
				r = bb[m1];
				if (Math.abs(t) < tol)
					s = Math.sqrt(r);
				else {
					w = 2.0 / t;
					s = w * r / (Math.sqrt(w * w * r + 1.0) + 1.0);
				}
				if (k == m - 2) {
					d[m] += s;
					d[m1] -= s;
					t = -s / b[m1];
					r = Math.sqrt(t * t + 1.0);
					cos = 1.0 / r;
					sin = t / r;
					Basic.rotcol(1, n, m1, m, a, cos, sin);
					m -= 2;
				}
				else {
					count++;
					if (count > max)
						break;
					lambda = d[m] + s;
					if (Math.abs(t) < tol) {
						w = d[m1] - s;
						if (Math.abs(w) < Math.abs(lambda))
							lambda = w;
					}
					k++;
					t = d[k] - lambda;
					cos = 1.0;
					w = b[k];
					p = Math.sqrt(t * t + w * w);
					j1 = k;
					for (j = k + 1; j <= m; j++) {
						oldcos = cos;
						cos = t / p;
						sin = w / p;
						dk1 = d[j] - lambda;
						t *= oldcos;
						d[j1] = (t + dk1) * sin * sin + lambda + t;
						t = cos * dk1 - sin * w * oldcos;
						w = b[j];
						p = Math.sqrt(t * t + w * w);
						g = b[j1] = sin * p;
						bb[j1] = g * g;
						Basic.rotcol(1, n, j1, j, a, cos, sin);
						j1 = j;
					}
					d[m] = cos * t + lambda;
					if (t < 0.0)
						b[m1] = -g;
				}
			}
		} while (m > 0);
		em[3] = Math.sqrt(bbmax);
		em[5] = count;
		return m;
	}

}

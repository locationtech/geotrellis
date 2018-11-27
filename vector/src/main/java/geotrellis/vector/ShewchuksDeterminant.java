/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package geotrellis.vector;

// This file is a modified port that is in JTS of SHewchuk's predicates.
// Below are the original comments by the JTS developer.

/**
 * This implementation is a port of Shewchuks original implementation in c
 * which is placed in the public domain:
 *
 * [...]
 *  Placed in the public domain by
 *  Jonathan Richard Shewchuk
 *  School of Computer Science
 *  Carnegie Mellon University
 *  5000 Forbes Avenue
 *  Pittsburgh, Pennsylvania  15213-3891
 *  jrs@cs.cmu.edu
 *  [...]
 *
 *  See http://www.cs.cmu.edu/~quake/robust.html for information about the original implementation
 *
 * The strategy used during porting has been to resemble the original as much as possible in order to
 * be able to proofread the result. This strategy has not been followed in the following cases:
 *
 * - function "exactinit" has been replaced by a static block, in order to ensure that the code is only
 *   executed once.
 *
 * - The main part of the so called "tail" functions has been removed in favor to "head" functions. This means
 *   that all calls to such a main functions has been replaced with on call to the head function and one call
 *   to the tail function. The rationale for this is that the corresponding source macros has multiple output parameters
 *   which is not supported in Java. Using objects to pass the results to the caller was not considered for performance
 *   reasons. It is assumed that the extra allocations of memory would affect performance negatively.
 *
 * - The porting of the two_two_diff methods and the other methods involved was tricky since the original macros
 *   had lots of output parameters. These methods has the highest probability of bugs as a result of the porting
 *   operation. Each original function(macro) has been replaced with one method for each output parameter. They are
 *   named as *__x0, *__x2 etc. where the postfix is named after the original output parameter in the SOURCE CODE.
 *   The rational for the naming has been to facilitate proofreading. Each of these methods has an annotation above
 *   it that shows the original macro.
 *
 * - One bug has been found in the original source. The use of the prefix incrementation operator in
 *   fast_expansion_sum_zeroelim caused the code to access memory outside of the array boundary. This is not
 *   allowed in Java which is the reason why this bug was discovered. It is unclear if the code worked correctly
 *   as a compiled c-program. It has been confirmed by valgrind that this actually happens in the original code.
 *   The author of the original code has been contacted, and we are waiting for an answer. All occurrences of the
 *   prefix incrementation operator in that function have been replaced with the postfix version. This change looks
 *   reasonable by a quick look at the code, but this needs to be more thoroughly analyzed.
 *
 * - Function orientationIndex is new and its contract is copied from
 *   org.locationtech.jts.algorithm.CGAlgorithms.orientationIndex so that the current implementation of that method
 *   can be easily replaced.
 *
 * Some relevant comments in the original code has been kept untouched in its entirety. For more in-depth information
 * refer to the original source.
 */

/*****************************************************************************/
/*                                                                           */
/*  Routines for Arbitrary Precision Floating-point Arithmetic               */
/*  and Fast Robust Geometric Predicates                                     */
/*  (predicates.c)                                                           */
/*                                                                           */
/*  May 18, 1996                                                             */
/*                                                                           */
/*  Placed in the public domain by                                           */
/*  Jonathan Richard Shewchuk                                                */
/*  School of Computer Science                                               */
/*  Carnegie Mellon University                                               */
/*  5000 Forbes Avenue                                                       */
/*  Pittsburgh, Pennsylvania  15213-3891                                     */
/*  jrs@cs.cmu.edu                                                           */
/*                                                                           */
/*  This file contains C implementation of algorithms for exact addition     */
/*    and multiplication of floating-point numbers, and predicates for       */
/*    robustly performing the orientation and incircle tests used in         */
/*    computational geometry.  The algorithms and underlying theory are      */
/*    described in Jonathan Richard Shewchuk.  "Adaptive Precision Floating- */
/*    Point Arithmetic and Fast Robust Geometric Predicates."  Technical     */
/*    Report CMU-CS-96-140, School of Computer Science, Carnegie Mellon      */
/*    University, Pittsburgh, Pennsylvania, May 1996.  (Submitted to         */
/*    Discrete & Computational Geometry.)                                    */
/*                                                                           */
/*  This file, the paper listed above, and other information are available   */
/*    from the Web page http://www.cs.cmu.edu/~quake/robust.html .           */
/*                                                                           */
/*****************************************************************************/

/*****************************************************************************/
/*                                                                           */
/*  Using this code:                                                         */
/*                                                                           */
/*  First, read the short or long version of the paper (from the Web page    */
/*    above).                                                                */
/*                                                                           */
/*  Be sure to call exactinit() once, before calling any of the arithmetic   */
/*    functions or geometric predicates.  Also be sure to turn on the        */
/*    optimizer when compiling this file.                                    */
/*                                                                           */
/*                                                                           */
/*  Several geometric predicates are defined.  Their parameters are all      */
/*    points.  Each point is an array of two or three floating-point         */
/*    numbers.  The geometric predicates, described in the papers, are       */
/*                                                                           */
/*    orient2d(pa, pb, pc)                                                   */
/*    orient2dfast(pa, pb, pc)                                               */
/*    orient3d(pa, pb, pc, pd)                                               */
/*    orient3dfast(pa, pb, pc, pd)                                           */
/*    incircle(pa, pb, pc, pd)                                               */
/*    incirclefast(pa, pb, pc, pd)                                           */
/*    insphere(pa, pb, pc, pd, pe)                                           */
/*    inspherefast(pa, pb, pc, pd, pe)                                       */
/*                                                                           */
/*  Those with suffix "fast" are approximate, non-robust versions.  Those    */
/*    without the suffix are adaptive precision, robust versions.  There     */
/*    are also versions with the suffices "exact" and "slow", which are      */
/*    non-adaptive, exact arithmetic versions, which I use only for timings  */
/*    in my arithmetic papers.                                               */
/*                                                                           */
/*                                                                           */
/*  An expansion is represented by an array of floating-point numbers,       */
/*    sorted from smallest to largest magnitude (possibly with interspersed  */
/*    zeros).  The length of each expansion is stored as a separate integer, */
/*    and each arithmetic function returns an integer which is the length    */
/*    of the expansion it created.                                           */
/*                                                                           */
/*  Several arithmetic functions are defined.  Their parameters are          */
/*                                                                           */
/*    e, f           Input expansions                                        */
/*    elen, flen     Lengths of input expansions (must be >= 1)              */
/*    h              Output expansion                                        */
/*    b              Input scalar                                            */
/*                                                                           */
/*  The arithmetic functions are                                             */
/*                                                                           */
/*    grow_expansion(elen, e, b, h)                                          */
/*    grow_expansion_zeroelim(elen, e, b, h)                                 */
/*    expansion_sum(elen, e, flen, f, h)                                     */
/*    expansion_sum_zeroelim1(elen, e, flen, f, h)                           */
/*    expansion_sum_zeroelim2(elen, e, flen, f, h)                           */
/*    fast_expansion_sum(elen, e, flen, f, h)                                */
/*    fast_expansion_sum_zeroelim(elen, e, flen, f, h)                       */
/*    linear_expansion_sum(elen, e, flen, f, h)                              */
/*    linear_expansion_sum_zeroelim(elen, e, flen, f, h)                     */
/*    scale_expansion(elen, e, b, h)                                         */
/*    scale_expansion_zeroelim(elen, e, b, h)                                */
/*    compress(elen, e, h)                                                   */
/*                                                                           */
/*  All of these are described in the long version of the paper; some are    */
/*    described in the short version.  All return an integer that is the     */
/*    length of h.  Those with suffix _zeroelim perform zero elimination,    */
/*    and are recommended over their counterparts.  The procedure            */
/*    fast_expansion_sum_zeroelim() (or linear_expansion_sum_zeroelim() on   */
/*    processors that do not use the round-to-even tiebreaking rule) is      */
/*    recommended over expansion_sum_zeroelim().  Each procedure has a       */
/*    little note next to it (in the code below) that tells you whether or   */
/*    not the output expansion may be the same array as one of the input     */
/*    expansions.                                                            */
/*                                                                           */
/*                                                                           */
/*  If you look around below, you'll also find macros for a bunch of         */
/*    simple unrolled arithmetic operations, and procedures for printing     */
/*    expansions (commented out because they don't work with all C           */
/*    compilers) and for generating random floating-point numbers whose      */
/*    significand bits are all random.  Most of the macros have undocumented */
/*    requirements that certain of their parameters should not be the same   */
/*    variable; for safety, better to make sure all the parameters are       */
/*    distinct variables.  Feel free to send email to jrs@cs.cmu.edu if you  */
/*    have questions.                                                        */
/*                                                                           */
/*****************************************************************************/

public class ShewchuksDeterminant
{

  /**
   * Implements a filter for computing the orientation index of three coordinates.
   * <p>
   * If the orientation can be computed safely using standard DP
   * arithmetic, this routine returns the orientation index.
   * Otherwise, a value i &gt; 1 is returned.
   * In this case the orientation index must
   * be computed using some other method.
   *
   * @param pa a coordinate
   * @param pb a coordinate
   * @param pc a coordinate
   * @return the orientation index if it can be computed safely, or
   * i &gt; 1 if the orientation index cannot be computed safely
   */
  public static int orientationIndexFilter(double pax,
                                           double pay,
                                           double pbx,
                                           double pby,
                                           double pcx,
                                           double pcy)
  {
    double detsum;

    double detleft = (pax - pcx) * (pby - pcy);
    double detright = (pay - pcy) * (pbx - pcx);
    double det = detleft - detright;

    if (detleft > 0.0) {
      if (detright <= 0.0) {
        return signum(det);
      }
      else {
        detsum = detleft + detright;
      }
    }
    else if (detleft < 0.0) {
      if (detright >= 0.0) {
        return signum(det);
      }
      else {
        detsum = -detleft - detright;
      }
    }
    else {
      return signum(det);
    }

    double ERR_BOUND = 1e-15;
    double errbound = ERR_BOUND * detsum;
    //double errbound = ccwerrboundA * detsum;
    if ((det >= errbound) || (-det >= errbound)) {
      return signum(det);
    }

    return 2;
  }

  private static int signum(double x)
  {
    if (x > 0) return 1;
    if (x < 0) return -1;
    return 0;
  }

  /**
   * Returns the index of the direction of the point <code>q</code> relative to
   * a vector specified by <code>p1-p2</code>.
   *
   * @param p1
   *          the origin point of the vector
   * @param p2
   *          the final point of the vector
   * @param q
   *          the point to compute the direction to
   *
   * @return 1 if q is counter-clockwise (left) from p1-p2;
   * -1 if q is clockwise (right) from p1-p2;
   * 0 if q is collinear with p1-p2
   */
  public static int orientationIndex(double p1x,
                                     double p1y,
                                     double p2x,
                                     double p2y,
                                     double qx,
                                     double qy)
  {
      double orientation = orient2d(p1x, p1y, p2x, p2y, qx, qy);
    if (orientation > 0.0) return 1;
    if (orientation < 0.0) return -1;
    return 0;
  }

  public static double orient2d(double pax,
                                 double pay,
                                 double pbx,
                                 double pby,
                                 double pcx,
                                 double pcy)
  {
    double detsum;

    double detleft = (pax - pcx) * (pby - pcy);
    double detright = (pay - pcy) * (pbx - pcx);
    double det = detleft - detright;

    if (detleft > 0.0) {
      if (detright <= 0.0) {
        return det;
      }
      else {
        detsum = detleft + detright;
      }
    }
    else if (detleft < 0.0) {
      if (detright >= 0.0) {
        return det;
      }
      else {
        detsum = -detleft - detright;
      }
    }
    else {
      return det;
    }

    double errbound = ccwerrboundA * detsum;
    if ((det >= errbound) || (-det >= errbound)) {
      return det;
    }

    return orient2dadapt(pax, pay, pbx, pby, pcx, pcy, detsum);
  }

  /*****************************************************************************/
  /*                                                                           */
  /* orient2d() Adaptive exact 2D orientation test. Robust. */
  /*                                                                           */
  /* Return a positive value if the points pa, pb, and pc occur */
  /* in counterclockwise order; a negative value if they occur */
  /* in clockwise order; and zero if they are collinear. The */
  /* result is also a rough approximation of twice the signed */
  /* area of the triangle defined by the three points. */
  /*                                                                           */
  /* The last three use exact arithmetic to ensure a correct answer. The */
  /* result returned is the determinant of a matrix. In orient2d() only, */
  /* this determinant is computed adaptively, in the sense that exact */
  /* arithmetic is used only to the degree it is needed to ensure that the */
  /* returned value has the correct sign. Hence, orient2d() is usually quite */
  /* fast, but will run more slowly when the input points are collinear or */
  /* nearly so. */
  /*                                                                           */
  /*****************************************************************************/

  private static double orient2dadapt(double pax,
                                      double pay,
                                      double pbx,
                                      double pby,
                                      double pcx,
                                      double pcy,
                                      double detsum)
  {

    double acx = pax - pcx;
    double bcx = pbx - pcx;
    double acy = pay - pcy;
    double bcy = pby - pcy;

    double detleft = Two_Product_Head(acx, bcy);
    double detlefttail = Two_Product_Tail(acx, bcy, detleft);

    double detright = Two_Product_Head(acy, bcx);
    double detrighttail = Two_Product_Tail(acy, bcx, detright);

    double B[] = new double[4];
    B[2] = Two_Two_Diff__x2(detleft, detlefttail, detright, detrighttail);
    B[1] = Two_Two_Diff__x1(detleft, detlefttail, detright, detrighttail);
    B[0] = Two_Two_Diff__x0(detleft, detlefttail, detright, detrighttail);
    B[3] = Two_Two_Diff__x3(detleft, detlefttail, detright, detrighttail);

    double det = B[0] + B[1] + B[2] + B[3];
    //det = estimate(4, B);
    double errbound = ccwerrboundB * detsum;
    if ((det >= errbound) || (-det >= errbound)) {
      return det;
    }

    double acxtail = Two_Diff_Tail(pax, pcx, acx);
    double bcxtail = Two_Diff_Tail(pbx, pcx, bcx);
    double acytail = Two_Diff_Tail(pay, pcy, acy);
    double bcytail = Two_Diff_Tail(pby, pcy, bcy);

    if ((acxtail == 0.0) && (acytail == 0.0) && (bcxtail == 0.0)
        && (bcytail == 0.0)) {
      return det;
    }

    errbound = ccwerrboundC * detsum + resulterrbound * Absolute(det);
    det += (acx * bcytail + bcy * acxtail) - (acy * bcxtail + bcx * acytail);
    if ((det >= errbound) || (-det >= errbound)) {
      return det;
    }

    double s1 = Two_Product_Head(acxtail, bcy);
    double s0 = Two_Product_Tail(acxtail, bcy, s1);

    double t1 = Two_Product_Head(acytail, bcx);
    double t0 = Two_Product_Tail(acytail, bcx, t1);

    double u3 = Two_Two_Diff__x3(s1, s0, t1, t0);
    double u[] = new double[4];
    u[2] = Two_Two_Diff__x2(s1, s0, t1, t0);
    u[1] = Two_Two_Diff__x1(s1, s0, t1, t0);
    u[0] = Two_Two_Diff__x0(s1, s0, t1, t0);

    u[3] = u3;
    double C1[] = new double[8];
    int C1length = fast_expansion_sum_zeroelim(4, B, 4, u, C1);

    s1 = Two_Product_Head(acx, bcytail);
    s0 = Two_Product_Tail(acx, bcytail, s1);

    t1 = Two_Product_Head(acy, bcxtail);
    t0 = Two_Product_Tail(acy, bcxtail, t1);

    u3 = Two_Two_Diff__x3(s1, s0, t1, t0);
    u[2] = Two_Two_Diff__x2(s1, s0, t1, t0);
    u[1] = Two_Two_Diff__x1(s1, s0, t1, t0);
    u[0] = Two_Two_Diff__x0(s1, s0, t1, t0);

    u[3] = u3;
    double C2[] = new double[12];
    int C2length = fast_expansion_sum_zeroelim(C1length, C1, 4, u, C2);

    s1 = Two_Product_Head(acxtail, bcytail);
    s0 = Two_Product_Tail(acxtail, bcytail, s1);

    t1 = Two_Product_Head(acytail, bcxtail);
    t0 = Two_Product_Tail(acytail, bcxtail, t1);

    u3 = Two_Two_Diff__x3(s1, s0, t1, t0);
    u[2] = Two_Two_Diff__x2(s1, s0, t1, t0);
    u[1] = Two_Two_Diff__x1(s1, s0, t1, t0);
    u[0] = Two_Two_Diff__x0(s1, s0, t1, t0);

    u[3] = u3;
    double D[] = new double[16];
    int Dlength = fast_expansion_sum_zeroelim(C2length, C2, 4, u, D);

    return (D[Dlength - 1]);
  }


  private static final double epsilon;

  private static final double splitter;

  private static final double resulterrbound;

  private static final double ccwerrboundA;

  private static final double ccwerrboundB;

  private static final double ccwerrboundC;

  private static final double o3derrboundA;

  private static final double o3derrboundB;

  private static final double o3derrboundC;

  private static final double iccerrboundA;

  private static final double iccerrboundB;

  private static final double iccerrboundC;

  private static final double isperrboundA;

  private static final double isperrboundB;

  private static final double isperrboundC;

  /*****************************************************************************/
  /*                                                                           */
  /* exactinit() Initialize the variables used for exact arithmetic. */
  /*                                                                           */
  /* `epsilon' is the largest power of two such that 1.0 + epsilon = 1.0 in */
  /* floating-point arithmetic. `epsilon' bounds the relative roundoff */
  /* error. It is used for floating-point error analysis. */
  /*                                                                           */
  /* `splitter' is used to split floating-point numbers into two half- */
  /* length significands for exact multiplication. */
  /*                                                                           */
  /* I imagine that a highly optimizing compiler might be too smart for its */
  /* own good, and somehow cause this routine to fail, if it pretends that */
  /* floating-point arithmetic is too much like real arithmetic. */
  /*                                                                           */
  /* Don't change this routine unless you fully understand it. */
  /*                                                                           */
  /*****************************************************************************/

  static {
    double epsilon_temp;
    double splitter_temp;
    double half;
    double check, lastcheck;
    int every_other;

    every_other = 1;
    half = 0.5;
    epsilon_temp = 1.0;
    splitter_temp = 1.0;
    check = 1.0;
    /* Repeatedly divide `epsilon' by two until it is too small to add to */
    /* one without causing roundoff. (Also check if the sum is equal to */
    /* the previous sum, for machines that round up instead of using exact */
    /* rounding. Not that this library will work on such machines anyway. */
    do {
      lastcheck = check;
      epsilon_temp *= half;
      if (every_other != 0) {
        splitter_temp *= 2.0;
      }
      every_other = every_other == 0 ? 1 : 0;
      check = 1.0 + epsilon_temp;
    } while ((check != 1.0) && (check != lastcheck));
    splitter_temp += 1.0;

    /* Error bounds for orientation and incircle tests. */
    resulterrbound = (3.0 + 8.0 * epsilon_temp) * epsilon_temp;
    ccwerrboundA = (3.0 + 16.0 * epsilon_temp) * epsilon_temp;
    ccwerrboundB = (2.0 + 12.0 * epsilon_temp) * epsilon_temp;
    ccwerrboundC = (9.0 + 64.0 * epsilon_temp) * epsilon_temp * epsilon_temp;
    o3derrboundA = (7.0 + 56.0 * epsilon_temp) * epsilon_temp;
    o3derrboundB = (3.0 + 28.0 * epsilon_temp) * epsilon_temp;
    o3derrboundC = (26.0 + 288.0 * epsilon_temp) * epsilon_temp * epsilon_temp;
    iccerrboundA = (10.0 + 96.0 * epsilon_temp) * epsilon_temp;
    iccerrboundB = (4.0 + 48.0 * epsilon_temp) * epsilon_temp;
    iccerrboundC = (44.0 + 576.0 * epsilon_temp) * epsilon_temp * epsilon_temp;
    isperrboundA = (16.0 + 224.0 * epsilon_temp) * epsilon_temp;
    isperrboundB = (5.0 + 72.0 * epsilon_temp) * epsilon_temp;
    isperrboundC = (71.0 + 1408.0 * epsilon_temp) * epsilon_temp * epsilon_temp;
    epsilon = epsilon_temp;
    splitter = splitter_temp;
  }

  private static double Absolute(double a)
  {
    return ((a) >= 0.0 ? (a) : -(a));
  }

  private static double Fast_Two_Sum_Tail(double a, double b, double x)
  {
    double bvirt = x - a;
    double y = b - bvirt;

    return y;
  }

  private static double Fast_Two_Sum_Head(double a, double b)
  {
    double x = (double) (a + b);

    return x;
  }

  private static double Two_Sum_Tail(double a, double b, double x)
  {
    double bvirt = (double) (x - a);
    double avirt = x - bvirt;
    double bround = b - bvirt;
    double around = a - avirt;

    double y = around + bround;

    return y;
  }

  private static double Two_Sum_Head(double a, double b)
  {
    double x = (double) (a + b);

    return x;
  }

  private static double Two_Diff_Tail(double a, double b, double x)
  {
    double bvirt = (double) (a - x); // porting issue: why this cast?
    double avirt = x + bvirt;
    double bround = bvirt - b;
    double around = a - avirt;
    double y = around + bround;

    return y;
  }

  private static double Two_Diff_Head(double a, double b)
  {
    double x = (double) (a - b);

    return x;
  }

  private static double SplitLo(double a)
  {
    double c = (double) (splitter * a); // porting issue: why this cast?
    double abig = (double) (c - a); // porting issue: why this cast?
    double ahi = c - abig;
    double alo = a - ahi;

    return alo;
  }

  private static double SplitHi(double a)
  {
    double c = (double) (splitter * a); // porting issue: why this cast?
    double abig = (double) (c - a); // porting issue: why this cast?
    double ahi = c - abig;

    return ahi;
  }

  private static double Two_Product_Tail(double a, double b, double x)
  {
    double ahi = SplitHi(a);
    double alo = SplitLo(a);
    double bhi = SplitHi(b);
    double blo = SplitLo(b);

    double err1 = x - (ahi * bhi);
    double err2 = err1 - (alo * bhi);
    double err3 = err2 - (ahi * blo);
    double y = (alo * blo) - err3;

    return y;
  }

  private static double Two_Product_Tail_Presplit(double a, double b, double bhi, double blo, double x)
  {
    double ahi = SplitHi(a);
    double alo = SplitLo(a);

    double err1 = x - (ahi * bhi);
    double err2 = err1 - (alo * bhi);
    double err3 = err2 - (ahi * blo);
    double y = (alo * blo) - err3;

    return y;
  }

  private static double Two_Product_Head(double a, double b)
  {
    double x = (double) (a * b);

    return x;
  }

  // #define Two_One_Diff(a1, a0, b, x2, x1, x0)
  private static double Two_One_Diff__x0(double a1, double a0, double b)
  {
    double _i = Two_Diff_Head(a0, b);
    double x0 = Two_Diff_Tail(a0, b, _i);

    return x0;
  }

  // #define Two_One_Diff(a1, a0, b, x2, x1, x0)
  private static double Two_One_Diff__x1(double a1, double a0, double b)
  {
    double _i = Two_Diff_Head(a0, b);
    double x2 = Two_Sum_Head(a1, _i);
    double x1 = Two_Sum_Tail(a1, _i, x2);

    return x1;
  }

  // #define Two_One_Diff(a1, a0, b, x2, x1, x0)
  private static double Two_One_Diff__x2(double a1, double a0, double b)
  {
    double _i = Two_Diff_Head(a0, b);
    double x2 = Two_Sum_Head(a1, _i);

    return x2;
  }

  // #define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Diff__x0(double a1, double a0, double b1,
      double b0)
  {
    double x0 = Two_One_Diff__x0(a1, a0, b0);

    return x0;
  }

  // #define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Diff__x1(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Diff__x2(a1, a0, b0);
    double _0 = Two_One_Diff__x1(a1, a0, b0);

    double x1 = Two_One_Diff__x0(_j, _0, b1);

    return x1;
  }

  // #define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Diff__x2(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Diff__x2(a1, a0, b0);
    double _0 = Two_One_Diff__x1(a1, a0, b0);

    double x2 = Two_One_Diff__x1(_j, _0, b1);

    return x2;
  }

  // #define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Diff__x3(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Diff__x2(a1, a0, b0);
    double _0 = Two_One_Diff__x1(a1, a0, b0);

    double x3 = Two_One_Diff__x2(_j, _0, b1);

    return x3;
  }

  // #define Two_One_Sum(a1, a0, b, x2, x1, x0)
  private static double Two_One_Sum__x0(double a1, double a0, double b)
  {
    double _i = Two_Sum_Head(a0, b);
    double x0 = Two_Sum_Tail(a0, b, _i);

    return x0;
  }

  // #define Two_One_Sum(a1, a0, b, x2, x1, x0)
  private static double Two_One_Sum__x1(double a1, double a0, double b)
  {
    double _i = Two_Sum_Head(a0, b);
    double x2 = Two_Sum_Head(a1, _i);
    double x1 = Two_Sum_Tail(a1, _i, x2);

    return x1;
  }

  // #define Two_One_Sum(a1, a0, b, x2, x1, x0)
  private static double Two_One_Sum__x2(double a1, double a0, double b)
  {
    double _i = Two_Sum_Head(a0, b);
    double x2 = Two_Sum_Head(a1, _i);

    return x2;
  }

  // #define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Sum__x0(double a1, double a0, double b1,
      double b0)
  {
    double x0 = Two_One_Sum__x0(a1, a0, b0);

    return x0;
  }

  // #define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Sum__x1(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Sum__x2(a1, a0, b0);
    double _0 = Two_One_Sum__x1(a1, a0, b0);

    double x1 = Two_One_Sum__x0(_j, _0, b1);

    return x1;
  }

  // #define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Sum__x2(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Sum__x2(a1, a0, b0);
    double _0 = Two_One_Sum__x1(a1, a0, b0);

    double x2 = Two_One_Sum__x1(_j, _0, b1);

    return x2;
  }

  // #define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0)
  private static double Two_Two_Sum__x3(double a1, double a0, double b1,
      double b0)
  {
    double _j = Two_One_Sum__x2(a1, a0, b0);
    double _0 = Two_One_Sum__x1(a1, a0, b0);

    double x3 = Two_One_Sum__x2(_j, _0, b1);

    return x3;
  }

  private static double Square_Tail(double a, double x) {
    double ahi = SplitHi(a);
    double alo = SplitLo(a);
    double err1 = x - (ahi * ahi);
    err1 = err1 - ((ahi + ahi) * alo);
    return (alo * alo) - err1;
  }

  private static double Square_Head(double a) {
    return a * a;
  }

  /*****************************************************************************/
  /*                                                                           */
  /* fast_expansion_sum_zeroelim() Sum two expansions, eliminating zero */
  /* components from the output expansion. */
  /*                                                                           */
  /* Sets h = e + f. See the long version of my paper for details. */
  /*                                                                           */
  /* If round-to-even is used (as with IEEE 754), maintains the strongly */
  /* nonoverlapping property. (That is, if e is strongly nonoverlapping, h */
  /* will be also.) Does NOT maintain the nonoverlapping or nonadjacent */
  /* properties. */
  /*                                                                           */
  /*****************************************************************************/

  private static int fast_expansion_sum_zeroelim(int elen, double[] e,
      int flen, double[] f, double[] h) /* h cannot be e or f. */
  {
    double Q;
    double Qnew;
    double hh;

    int eindex, findex, hindex;
    double enow, fnow;

    enow = e[0];
    fnow = f[0];
    eindex = findex = 0;
    if ((fnow > enow) == (fnow > -enow)) {
      Q = enow;
      enow = e[eindex++];
    }
    else {
      Q = fnow;
      fnow = f[findex++];
    }
    hindex = 0;
    if ((eindex < elen) && (findex < flen)) {
      if ((fnow > enow) == (fnow > -enow)) {
        Qnew = Fast_Two_Sum_Head(enow, Q);
        hh = Fast_Two_Sum_Tail(enow, Q, Qnew);
        enow = e[eindex++];
      }
      else {
        Qnew = Fast_Two_Sum_Head(fnow, Q);
        hh = Fast_Two_Sum_Tail(fnow, Q, Qnew);
        fnow = f[findex++];
      }
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
      while ((eindex < elen) && (findex < flen)) {
        if ((fnow > enow) == (fnow > -enow)) {
          Qnew = Two_Sum_Head(Q, enow);
          hh = Two_Sum_Tail(Q, enow, Qnew);
          enow = e[eindex++];
        }
        else {
          Qnew = Two_Sum_Head(Q, fnow);
          hh = Two_Sum_Tail(Q, fnow, Qnew);
          fnow = f[findex++];
        }
        Q = Qnew;
        if (hh != 0.0) {
          h[hindex++] = hh;
        }
      }
    }
    while (eindex < elen) {
      Qnew = Two_Sum_Head(Q, enow);
      hh = Two_Sum_Tail(Q, enow, Qnew);
      enow = e[eindex++];
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
    }
    while (findex < flen) {
      Qnew = Two_Sum_Head(Q, fnow);
      hh = Two_Sum_Tail(Q, fnow, Qnew);
      fnow = f[findex++];
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
    }
    if ((Q != 0.0) || (hindex == 0)) {
      h[hindex++] = Q;
    }
    return hindex;
  }

  /*****************************************************************************/
  /*                                                                           */
  /* estimate() Produce a one-word estimate of an expansion's value. */
  /*                                                                           */
  /* See either version of my paper for details. */
  /*                                                                           */
  /*****************************************************************************/

  private static double estimate(int elen, double[] e)
  {
    double Q;
    int eindex;

    Q = e[0];
    for (eindex = 1; eindex < elen; eindex++) {
      Q += e[eindex];
    }
    return Q;
  }

  /*****************************************************************************/
  /*  scale_expansion_zeroelim port added by Rob Emanuele (@lossyrob) in 2016  */
  /*****************************************************************************/
  /*****************************************************************************/
  /*                                                                           */
  /*  scale_expansion_zeroelim()   Multiply an expansion by a scalar,          */
  /*                               eliminating zero components from the        */
  /*                               output expansion.                           */
  /*                                                                           */
  /*  Sets h = be.  See either version of my paper for details.                */
  /*                                                                           */
  /*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
  /*  with IEEE 754), maintains the strongly nonoverlapping and nonadjacent    */
  /*  properties as well.  (That is, if e has one of these properties, so      */
  /*  will h.)                                                                 */
  /*                                                                           */
  /*****************************************************************************/

  /* e and h cannot be the same. */
  private static int scale_expansion_zeroelim(int elen, double[] e, double b, double[] h) {
    double Q, sum;
    double hh;
    double product1;
    double product0;
    int eindex, hindex;
    double enow;
    double bvirt;
    double avirt, bround, around;
    double c;
    double abig;
    double ahi, alo, bhi, blo;
    double err1, err2, err3;

    bhi = SplitHi(b);
    blo = SplitLo(b);

    Q = Two_Product_Head(e[0], b);
    hh = Two_Product_Tail_Presplit(e[0], b, bhi, blo, Q);
    hindex = 0;
    if (hh != 0) {
      h[hindex++] = hh;
    }
    for (eindex = 1; eindex < elen; eindex++) {
      enow = e[eindex];
      product1 = Two_Product_Head(enow, b);
      product0 = Two_Product_Tail_Presplit(enow, b, bhi, blo, product1);
      sum = Two_Sum_Head(Q, product0);
      hh = Two_Sum_Tail(Q, product0, sum);
      if (hh != 0) {
        h[hindex++] = hh;
      }
      Q = Fast_Two_Sum_Head(product1, sum);
      hh = Fast_Two_Sum_Tail(product1, sum, Q);
      if (hh != 0) {
        h[hindex++] = hh;
      }
    }
    if ((Q != 0.0) || (hindex == 0)) {
      h[hindex++] = Q;
    }
    return hindex;
  }

  /*****************************************************************************/
  /* incircle and incircleadapt port added by Rob Emanuele (@lossyrob) in 2016 */
  /*****************************************************************************/
  /*****************************************************************************/
  /*                                                                           */
  /*  incircle()   Adaptive exact 2D incircle test.  Robust.                   */
  /*                                                                           */
  /*               Return a positive value if the point pd lies inside the     */
  /*               circle passing through pa, pb, and pc; a negative value if  */
  /*               it lies outside; and zero if the four points are cocircular.*/
  /*               The points pa, pb, and pc must be in counterclockwise       */
  /*               order, or the sign of the result will be reversed.          */
  /*                                                                           */
  /*  Only the first and last routine should be used; the middle two are for   */
  /*  timings.                                                                 */
  /*                                                                           */
  /*  The last three use exact arithmetic to ensure a correct answer.  The     */
  /*  result returned is the determinant of a matrix.  In incircle() only,     */
  /*  this determinant is computed adaptively, in the sense that exact         */
  /*  arithmetic is used only to the degree it is needed to ensure that the    */
  /*  returned value has the correct sign.  Hence, incircle() is usually quite */
  /*  fast, but will run more slowly when the input points are cocircular or   */
  /*  nearly so.                                                               */
  /*                                                                           */
  /*****************************************************************************/

  public static double incircle(double pax,
                                double pay,
                                double pbx,
                                double pby,
                                double pcx,
                                double pcy,
                                double pdx,
                                double pdy) {
    double adx, bdx, cdx, ady, bdy, cdy;
    double bdxcdy, cdxbdy, cdxady, adxcdy, adxbdy, bdxady;
    double alift, blift, clift;
    double det;
    double permanent, errbound;

    adx = pax - pdx;
    bdx = pbx - pdx;
    cdx = pcx - pdx;
    ady = pay - pdy;
    bdy = pby - pdy;
    cdy = pcy - pdy;

    bdxcdy = bdx * cdy;
    cdxbdy = cdx * bdy;
    alift = adx * adx + ady * ady;

    cdxady = cdx * ady;
    adxcdy = adx * cdy;
    blift = bdx * bdx + bdy * bdy;

    adxbdy = adx * bdy;
    bdxady = bdx * ady;
    clift = cdx * cdx + cdy * cdy;

    det = alift * (bdxcdy - cdxbdy)
      + blift * (cdxady - adxcdy)
      + clift * (adxbdy - bdxady);

    permanent = (Absolute(bdxcdy) + Absolute(cdxbdy)) * alift
      + (Absolute(cdxady) + Absolute(adxcdy)) * blift
      + (Absolute(adxbdy) + Absolute(bdxady)) * clift;
    errbound = iccerrboundA * permanent;
    if ((det > errbound) || (-det > errbound)) {
      return det;
    }

    return incircleadapt(pax,
                         pay,
                         pbx,
                         pby,
                         pcx,
                         pcy,
                         pdx,
                         pdy,
                         permanent);
  }

  public static double incircleadapt(double pax,
                                     double pay,
                                     double pbx,
                                     double pby,
                                     double pcx,
                                     double pcy,
                                     double pdx,
                                     double pdy,
                                     double permanent) {
    double adx, bdx, cdx, ady, bdy, cdy;
    double det, errbound;

    double bdxcdy1, cdxbdy1, cdxady1, adxcdy1, adxbdy1, bdxady1;
    double bdxcdy0, cdxbdy0, cdxady0, adxcdy0, adxbdy0, bdxady0;
    double[] bc = new double[4];
    double[] ca = new double[4];
    double[] ab = new double[4];
    double bc3, ca3, ab3;
    double[] axbc = new double[8];
    double[] axxbc = new double[16];
    double[] aybc = new double[8];
    double[] ayybc = new double[16];
    double[] adet = new double[32];
    int axbclen, axxbclen, aybclen, ayybclen, alen;
    double[] bxca = new double[8];
    double[] bxxca = new double[16];
    double[] byca = new double[8];
    double[] byyca = new double[16];
    double[] bdet = new double[32];
    int bxcalen, bxxcalen, bycalen, byycalen, blen;
    double[] cxab = new double[8];
    double[] cxxab = new double[16];
    double[] cyab = new double[8];
    double[] cyyab = new double[16];
    double[] cdet = new double[32];
    int cxablen, cxxablen, cyablen, cyyablen, clen;
    double[] abdet = new double[64];
    int ablen;
    double[] fin1 = new double[1152];
    double[] fin2 = new double[1152];
    double[] finnow, finother, finswap;
    int finlength;

    double adxtail, bdxtail, cdxtail, adytail, bdytail, cdytail;
    double adxadx1, adyady1, bdxbdx1, bdybdy1, cdxcdx1, cdycdy1;
    double adxadx0, adyady0, bdxbdx0, bdybdy0, cdxcdx0, cdycdy0;
    double[] aa = new double[4];
    double[] bb = new double[4];
    double[] cc = new double[4];
    double aa3, bb3, cc3;
    double ti1, tj1;
    double ti0, tj0;
    double[] u = new double[4];
    double[] v = new double[4];
    double u3, v3;
    double[] temp8 = new double[8];
    double[] temp16a = new double[16];
    double[] temp16b = new double[16];
    double[] temp16c = new double[16];
    double[] temp32a = new double[32];
    double[] temp32b = new double[32];
    double[] temp48 = new double[48];
    double[] temp64 = new double[64];
    int temp8len, temp16alen, temp16blen, temp16clen;
    int temp32alen, temp32blen, temp48len, temp64len;
    double[] axtbb = new double[8];
    double[] axtcc = new double[8];
    double[] aytbb = new double[8];
    double[] aytcc = new double[8];
    int axtbblen, axtcclen, aytbblen, aytcclen;
    double[] bxtaa = new double[8];
    double[] bxtcc = new double[8];
    double[] bytaa = new double[8];
    double[] bytcc = new double[8];
    int bxtaalen, bxtcclen, bytaalen, bytcclen;
    double[] cxtaa = new double[8];
    double[] cxtbb = new double[8];
    double[] cytaa = new double[8];
    double[] cytbb = new double[8];
    int cxtaalen, cxtbblen, cytaalen, cytbblen;
    double[] axtbc = new double[8];
    double[] aytbc = new double[8];
    double[] bxtca = new double[8];
    double[] bytca = new double[8];
    double[] cxtab = new double[8];
    double[] cytab = new double[8];
    int axtbclen = 0, aytbclen = 0, bxtcalen = 0, bytcalen = 0, cxtablen = 0, cytablen = 0;
    double[] axtbct = new double[16];
    double[] aytbct = new double[16];
    double[] bxtcat = new double[16];
    double[] bytcat = new double[16];
    double[] cxtabt = new double[16];
    double[] cytabt = new double[16];
    int axtbctlen, aytbctlen, bxtcatlen, bytcatlen, cxtabtlen, cytabtlen;
    double[] axtbctt = new double[8];
    double[] aytbctt = new double[8];
    double[] bxtcatt = new double[8];
    double[] bytcatt = new double[8];
    double[] cxtabtt = new double[8];
    double[] cytabtt = new double[8];
    int axtbcttlen, aytbcttlen, bxtcattlen, bytcattlen, cxtabttlen, cytabttlen;
    double[] abt = new double[8];
    double[] bct = new double[8];
    double[] cat = new double[8];
    int abtlen, bctlen, catlen;
    double[] abtt = new double[4];
    double[] bctt = new double[4];
    double[] catt = new double[4];
    int abttlen, bcttlen, cattlen;
    double abtt3, bctt3, catt3;
    double negate;

    double bvirt;
    double avirt, bround, around;
    double c;
    double abig;
    double ahi, alo, bhi, blo;
    double err1, err2, err3;
    double _i, _j;
    double _0;

    adx = (double) (pax - pdx);
    bdx = (double) (pbx - pdx);
    cdx = (double) (pcx - pdx);
    ady = (double) (pay - pdy);
    bdy = (double) (pby - pdy);
    cdy = (double) (pcy - pdy);

    //port:    Two_Product(bdx, cdy, bdxcdy1, bdxcdy0)
    bdxcdy1 = Two_Product_Head(bdx, cdy);
    bdxcdy0 = Two_Product_Tail(bdx, cdy, bdxcdy1);
    //endport
    //port:    Two_Product(cdx, bdy, cdxbdy1, cdxbdy0);
    cdxbdy1 = Two_Product_Head(cdx, bdy);
    cdxbdy0 = Two_Product_Tail(cdx, bdy, cdxbdy1);
    //endport
    //port:    Two_Two_Diff(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0, bc3, bc[2], bc[1], bc[0]);
    bc3 = Two_Two_Diff__x3(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0);
    bc[2] = Two_Two_Diff__x2(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0);
    bc[1] = Two_Two_Diff__x1(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0);
    bc[0] = Two_Two_Diff__x0(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0);
    //endport
    bc[3] = bc3;
    axbclen = scale_expansion_zeroelim(4, bc, adx, axbc);
    axxbclen = scale_expansion_zeroelim(axbclen, axbc, adx, axxbc);
    aybclen = scale_expansion_zeroelim(4, bc, ady, aybc);
    ayybclen = scale_expansion_zeroelim(aybclen, aybc, ady, ayybc);
    alen = fast_expansion_sum_zeroelim(axxbclen, axxbc, ayybclen, ayybc, adet);

    //port:    Two_Product(cdx, ady, cdxady1, cdxady0);
    cdxady1 = Two_Product_Head(cdx, ady);
    cdxady0 = Two_Product_Tail(cdx, ady, cdxady1);
    //endport
    //port:    Two_Product(adx, cdy, adxcdy1, adxcdy0);
    adxcdy1 = Two_Product_Head(adx, cdy);
    adxcdy0 = Two_Product_Tail(adx, cdy, adxcdy1);
    //endport
    //port:    Two_Two_Diff(cdxady1, cdxady0, adxcdy1, adxcdy0, ca3, ca[2], ca[1], ca[0]);
    ca3 = Two_Two_Diff__x3(cdxady1, cdxady0, adxcdy1, adxcdy0);
    ca[2] = Two_Two_Diff__x2(cdxady1, cdxady0, adxcdy1, adxcdy0);
    ca[1] = Two_Two_Diff__x1(cdxady1, cdxady0, adxcdy1, adxcdy0);
    ca[0] = Two_Two_Diff__x0(cdxady1, cdxady0, adxcdy1, adxcdy0);
    //endport
    ca[3] = ca3;
    bxcalen = scale_expansion_zeroelim(4, ca, bdx, bxca);
    bxxcalen = scale_expansion_zeroelim(bxcalen, bxca, bdx, bxxca);
    bycalen = scale_expansion_zeroelim(4, ca, bdy, byca);
    byycalen = scale_expansion_zeroelim(bycalen, byca, bdy, byyca);
    blen = fast_expansion_sum_zeroelim(bxxcalen, bxxca, byycalen, byyca, bdet);

    //port:    Two_Product(adx, bdy, adxbdy1, adxbdy0);
    adxbdy1 = Two_Product_Head(adx, bdy);
    adxbdy0 = Two_Product_Tail(adx, bdy, adxbdy1);
    //endport
    //port:    Two_Product(bdx, ady, bdxady1, bdxady0);
    bdxady1 = Two_Product_Head(bdx, ady);
    bdxady0 = Two_Product_Tail(bdx, ady, bdxady1);
    //endport
    //port:    Two_Two_Diff(adxbdy1, adxbdy0, bdxady1, bdxady0, ab3, ab[2], ab[1], ab[0]);
    ab3 = Two_Two_Diff__x3(adxbdy1, adxbdy0, bdxady1, bdxady0);
    ab[2] = Two_Two_Diff__x2(adxbdy1, adxbdy0, bdxady1, bdxady0);
    ab[1] = Two_Two_Diff__x1(adxbdy1, adxbdy0, bdxady1, bdxady0);
    ab[0] = Two_Two_Diff__x0(adxbdy1, adxbdy0, bdxady1, bdxady0);
    //endport
    ab[3] = ab3;
    cxablen = scale_expansion_zeroelim(4, ab, cdx, cxab);
    cxxablen = scale_expansion_zeroelim(cxablen, cxab, cdx, cxxab);
    cyablen = scale_expansion_zeroelim(4, ab, cdy, cyab);
    cyyablen = scale_expansion_zeroelim(cyablen, cyab, cdy, cyyab);
    clen = fast_expansion_sum_zeroelim(cxxablen, cxxab, cyyablen, cyyab, cdet);

    ablen = fast_expansion_sum_zeroelim(alen, adet, blen, bdet, abdet);
    finlength = fast_expansion_sum_zeroelim(ablen, abdet, clen, cdet, fin1);

    det = estimate(finlength, fin1);
    errbound = iccerrboundB * permanent;
    if ((det >= errbound) || (-det >= errbound)) {
      return det;
    }

    //port:    Two_Diff_Tail(pax, pdx, adx, adxtail);
    adxtail = Two_Diff_Tail(pax, pdx, adx);
    //endport
    //port:    Two_Diff_Tail(pay, pdy, ady, adytail);
    adytail = Two_Diff_Tail(pay, pdy, ady);
    //endport
    //port:    Two_Diff_Tail(pbx, pdx, bdx, bdxtail);
    bdxtail = Two_Diff_Tail(pbx, pdx, bdx);
    //endport
    //port:    Two_Diff_Tail(pby, pdy, bdy, bdytail);
    bdytail = Two_Diff_Tail(pby, pdy, bdy);
    //endport
    //port:    Two_Diff_Tail(pcx, pdx, cdx, cdxtail);
    cdxtail = Two_Diff_Tail(pcx, pdx, cdx);
    //endport
    //port:    Two_Diff_Tail(pcy, pdy, cdy, cdytail);
    cdytail = Two_Diff_Tail(pcy, pdy, cdy);
    //endport
    if ((adxtail == 0.0) && (bdxtail == 0.0) && (cdxtail == 0.0)
        && (adytail == 0.0) && (bdytail == 0.0) && (cdytail == 0.0)) {
      return det;
    }

    errbound = iccerrboundC * permanent + resulterrbound * Absolute(det);
    det += ((adx * adx + ady * ady) * ((bdx * cdytail + cdy * bdxtail)
                                       - (bdy * cdxtail + cdx * bdytail))
            + 2.0 * (adx * adxtail + ady * adytail) * (bdx * cdy - bdy * cdx))
      + ((bdx * bdx + bdy * bdy) * ((cdx * adytail + ady * cdxtail)
                                    - (cdy * adxtail + adx * cdytail))
         + 2.0 * (bdx * bdxtail + bdy * bdytail) * (cdx * ady - cdy * adx))
      + ((cdx * cdx + cdy * cdy) * ((adx * bdytail + bdy * adxtail)
                                    - (ady * bdxtail + bdx * adytail))
         + 2.0 * (cdx * cdxtail + cdy * cdytail) * (adx * bdy - ady * bdx));
    if ((det >= errbound) || (-det >= errbound)) {
      return det;
    }

    finnow = fin1;
    finother = fin2;

    if ((bdxtail != 0.0) || (bdytail != 0.0)
        || (cdxtail != 0.0) || (cdytail != 0.0)) {
      //port:      Square(adx, adxadx1, adxadx0);
      adxadx1 = Square_Head(adx);
      adxadx0 = Square_Tail(adx, adxadx1);
      //endport
      //port:      Square(ady, adyady1, adyady0);
      adyady1 = Square_Head(ady);
      adyady0 = Square_Tail(ady, adyady1);
      //endport
      //port:      Two_Two_Sum(adxadx1, adxadx0, adyady1, adyady0, aa3, aa[2], aa[1], aa[0]);
      aa3 = Two_Two_Sum__x3(adxadx1, adxadx0, adyady1, adyady0);
      aa[2] = Two_Two_Sum__x2(adxadx1, adxadx0, adyady1, adyady0);
      aa[1] = Two_Two_Sum__x1(adxadx1, adxadx0, adyady1, adyady0);
      aa[0] = Two_Two_Sum__x0(adxadx1, adxadx0, adyady1, adyady0);
      //endport
      aa[3] = aa3;
    }
    if ((cdxtail != 0.0) || (cdytail != 0.0)
        || (adxtail != 0.0) || (adytail != 0.0)) {
      //port:      Square(bdx, bdxbdx1, bdxbdx0);
      bdxbdx1 = Square_Head(bdx);
      bdxbdx0 = Square_Tail(bdx, bdxbdx1);
      //endport
      //port:      Square(bdy, bdybdy1, bdybdy0);
      bdybdy1 = Square_Head(bdy);
      bdybdy0 = Square_Tail(bdy, bdybdy1);
      //endport
      //port:      Two_Two_Sum(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0, bb3, bb[2], bb[1], bb[0]);
      bb3 = Two_Two_Sum__x3(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0);
      bb[2] = Two_Two_Sum__x2(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0);
      bb[1] = Two_Two_Sum__x1(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0);
      bb[0] = Two_Two_Sum__x0(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0);
      //endport
      bb[3] = bb3;
    }
    if ((adxtail != 0.0) || (adytail != 0.0)
        || (bdxtail != 0.0) || (bdytail != 0.0)) {
      //port:      Square(cdx, cdxcdx1, cdxcdx0);
      cdxcdx1 = Square_Head(cdx);
      cdxcdx0 = Square_Tail(cdx, cdxcdx1);
      //endport
      //port:      Square(cdy, cdycdy1, cdycdy0);
      cdycdy1 = Square_Head(cdy);
      cdycdy0 = Square_Tail(cdy, cdycdy1);
      //endport
      //port:      Two_Two_Sum(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0, cc3, cc[2], cc[1], cc[0]);
      cc3 = Two_Two_Sum__x3(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0);
      cc[2] = Two_Two_Sum__x2(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0);
      cc[1] = Two_Two_Sum__x1(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0);
      cc[0] = Two_Two_Sum__x0(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0);
      //endport
      cc[3] = cc3;
    }

    if (adxtail != 0.0) {
      axtbclen = scale_expansion_zeroelim(4, bc, adxtail, axtbc);
      temp16alen = scale_expansion_zeroelim(axtbclen, axtbc, 2.0 * adx,
                                            temp16a);

      axtcclen = scale_expansion_zeroelim(4, cc, adxtail, axtcc);
      temp16blen = scale_expansion_zeroelim(axtcclen, axtcc, bdy, temp16b);

      axtbblen = scale_expansion_zeroelim(4, bb, adxtail, axtbb);
      temp16clen = scale_expansion_zeroelim(axtbblen, axtbb, -cdy, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (adytail != 0.0) {
      aytbclen = scale_expansion_zeroelim(4, bc, adytail, aytbc);
      temp16alen = scale_expansion_zeroelim(aytbclen, aytbc, 2.0 * ady,
                                            temp16a);

      aytbblen = scale_expansion_zeroelim(4, bb, adytail, aytbb);
      temp16blen = scale_expansion_zeroelim(aytbblen, aytbb, cdx, temp16b);

      aytcclen = scale_expansion_zeroelim(4, cc, adytail, aytcc);
      temp16clen = scale_expansion_zeroelim(aytcclen, aytcc, -bdx, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (bdxtail != 0.0) {
      bxtcalen = scale_expansion_zeroelim(4, ca, bdxtail, bxtca);
      temp16alen = scale_expansion_zeroelim(bxtcalen, bxtca, 2.0 * bdx,
                                            temp16a);

      bxtaalen = scale_expansion_zeroelim(4, aa, bdxtail, bxtaa);
      temp16blen = scale_expansion_zeroelim(bxtaalen, bxtaa, cdy, temp16b);

      bxtcclen = scale_expansion_zeroelim(4, cc, bdxtail, bxtcc);
      temp16clen = scale_expansion_zeroelim(bxtcclen, bxtcc, -ady, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (bdytail != 0.0) {
      bytcalen = scale_expansion_zeroelim(4, ca, bdytail, bytca);
      temp16alen = scale_expansion_zeroelim(bytcalen, bytca, 2.0 * bdy,
                                            temp16a);

      bytcclen = scale_expansion_zeroelim(4, cc, bdytail, bytcc);
      temp16blen = scale_expansion_zeroelim(bytcclen, bytcc, adx, temp16b);

      bytaalen = scale_expansion_zeroelim(4, aa, bdytail, bytaa);
      temp16clen = scale_expansion_zeroelim(bytaalen, bytaa, -cdx, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (cdxtail != 0.0) {
      cxtablen = scale_expansion_zeroelim(4, ab, cdxtail, cxtab);
      temp16alen = scale_expansion_zeroelim(cxtablen, cxtab, 2.0 * cdx,
                                            temp16a);

      cxtbblen = scale_expansion_zeroelim(4, bb, cdxtail, cxtbb);
      temp16blen = scale_expansion_zeroelim(cxtbblen, cxtbb, ady, temp16b);

      cxtaalen = scale_expansion_zeroelim(4, aa, cdxtail, cxtaa);
      temp16clen = scale_expansion_zeroelim(cxtaalen, cxtaa, -bdy, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (cdytail != 0.0) {
      cytablen = scale_expansion_zeroelim(4, ab, cdytail, cytab);
      temp16alen = scale_expansion_zeroelim(cytablen, cytab, 2.0 * cdy,
                                            temp16a);

      cytaalen = scale_expansion_zeroelim(4, aa, cdytail, cytaa);
      temp16blen = scale_expansion_zeroelim(cytaalen, cytaa, bdx, temp16b);

      cytbblen = scale_expansion_zeroelim(4, bb, cdytail, cytbb);
      temp16clen = scale_expansion_zeroelim(cytbblen, cytbb, -adx, temp16c);

      temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                               temp16blen, temp16b, temp32a);
      temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c,
                                              temp32alen, temp32a, temp48);
      finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                              temp48, finother);
      finswap = finnow; finnow = finother; finother = finswap;
    }

    if ((adxtail != 0.0) || (adytail != 0.0)) {
      if ((bdxtail != 0.0) || (bdytail != 0.0)
          || (cdxtail != 0.0) || (cdytail != 0.0)) {
        //port:        Two_Product(bdxtail, cdy, ti1, ti0);
        ti1 = Two_Product_Head(bdxtail, cdy);
        ti0 = Two_Product_Tail(bdxtail, cdy, ti1);
        //endport
        //port:        Two_Product(bdx, cdytail, tj1, tj0);
        tj1 = Two_Product_Head(bdx, cdytail);
        tj0 = Two_Product_Tail(bdx, cdytail, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, u3, u[2], u[1], u[0]);
        u3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        u[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        u[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        u[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        u[3] = u3;
        negate = -bdy;
        //port:        Two_Product(cdxtail, negate, ti1, ti0);
        ti1 = Two_Product_Head(cdxtail, negate);
        ti0 = Two_Product_Tail(cdxtail, negate, ti1);
        //endport
        negate = -bdytail;
        //port:        Two_Product(cdx, negate, tj1, tj0);
        tj1 = Two_Product_Head(cdx, negate);
        tj0 = Two_Product_Tail(cdx, negate, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, v3, v[2], v[1], v[0]);
        v3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        v[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        v[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        v[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        v[3] = v3;
        bctlen = fast_expansion_sum_zeroelim(4, u, 4, v, bct);

        //port:        Two_Product(bdxtail, cdytail, ti1, ti0);
        ti1 = Two_Product_Head(bdxtail, cdytail);
        ti0 = Two_Product_Tail(bdxtail, cdytail, ti1);
        //endport
        //port:        Two_Product(cdxtail, bdytail, tj1, tj0);
        tj1 = Two_Product_Head(cdxtail, bdytail);
        tj0 = Two_Product_Tail(cdxtail, bdytail, tj1);
        //endport
        //port:        Two_Two_Diff(ti1, ti0, tj1, tj0, bctt3, bctt[2], bctt[1], bctt[0]);
        bctt3 = Two_Two_Diff__x3(ti1, ti0, tj1, tj0);
        bctt[2] = Two_Two_Diff__x2(ti1, ti0, tj1, tj0);
        bctt[1] = Two_Two_Diff__x1(ti1, ti0, tj1, tj0);
        bctt[0] = Two_Two_Diff__x0(ti1, ti0, tj1, tj0);
        //endport
        bctt[3] = bctt3;
        bcttlen = 4;
      } else {
        bct[0] = 0.0;
        bctlen = 1;
        bctt[0] = 0.0;
        bcttlen = 1;
      }

      if (adxtail != 0.0) {
        temp16alen = scale_expansion_zeroelim(axtbclen, axtbc, adxtail, temp16a);
        axtbctlen = scale_expansion_zeroelim(bctlen, bct, adxtail, axtbct);
        temp32alen = scale_expansion_zeroelim(axtbctlen, axtbct, 2.0 * adx,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;
        if (bdytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, cc, adxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, bdytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (cdytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, bb, -adxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, cdytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32alen = scale_expansion_zeroelim(axtbctlen, axtbct, adxtail,
                                              temp32a);
        axtbcttlen = scale_expansion_zeroelim(bcttlen, bctt, adxtail, axtbctt);
        temp16alen = scale_expansion_zeroelim(axtbcttlen, axtbctt, 2.0 * adx,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(axtbcttlen, axtbctt, adxtail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (adytail != 0.0) {
        temp16alen = scale_expansion_zeroelim(aytbclen, aytbc, adytail, temp16a);
        aytbctlen = scale_expansion_zeroelim(bctlen, bct, adytail, aytbct);
        temp32alen = scale_expansion_zeroelim(aytbctlen, aytbct, 2.0 * ady,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32alen = scale_expansion_zeroelim(aytbctlen, aytbct, adytail,
                                              temp32a);
        aytbcttlen = scale_expansion_zeroelim(bcttlen, bctt, adytail, aytbctt);
        temp16alen = scale_expansion_zeroelim(aytbcttlen, aytbctt, 2.0 * ady,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(aytbcttlen, aytbctt, adytail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }
    if ((bdxtail != 0.0) || (bdytail != 0.0)) {
      if ((cdxtail != 0.0) || (cdytail != 0.0)
          || (adxtail != 0.0) || (adytail != 0.0)) {
        //port:        Two_Product(cdxtail, ady, ti1, ti0);
        ti1 = Two_Product_Head(cdxtail, ady);
        ti0 = Two_Product_Tail(cdxtail, ady, ti1);
        //endport
        //port:        Two_Product(cdx, adytail, tj1, tj0);
        tj1 = Two_Product_Head(cdx, adytail);
        tj0 = Two_Product_Tail(cdx, adytail, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, u3, u[2], u[1], u[0]);
        u3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        u[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        u[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        u[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        u[3] = u3;
        negate = -cdy;
        //port:        Two_Product(adxtail, negate, ti1, ti0);
        ti1 = Two_Product_Head(adxtail, negate);
        ti0 = Two_Product_Tail(adxtail, negate, ti1);
        //endport
        negate = -cdytail;
        //port:        Two_Product(adx, negate, tj1, tj0);
        tj1 = Two_Product_Head(adx, negate);
        tj0 = Two_Product_Tail(adx, negate, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, v3, v[2], v[1], v[0]);
        v3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        v[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        v[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        v[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        v[3] = v3;
        catlen = fast_expansion_sum_zeroelim(4, u, 4, v, cat);

        //port:        Two_Product(cdxtail, adytail, ti1, ti0);
        ti1 = Two_Product_Head(cdxtail, adytail);
        ti0 = Two_Product_Tail(cdxtail, adytail, ti1);
        //endport
        //port:        Two_Product(adxtail, cdytail, tj1, tj0);
        tj1 = Two_Product_Head(adxtail, cdytail);
        tj0 = Two_Product_Tail(adxtail, cdytail, tj1);
        //endport
        //port:        Two_Two_Diff(ti1, ti0, tj1, tj0, catt3, catt[2], catt[1], catt[0]);
        catt3 = Two_Two_Diff__x3(ti1, ti0, tj1, tj0);
        catt[2] = Two_Two_Diff__x2(ti1, ti0, tj1, tj0);
        catt[1] = Two_Two_Diff__x1(ti1, ti0, tj1, tj0);
        catt[0] = Two_Two_Diff__x0(ti1, ti0, tj1, tj0);
        //endport
        catt[3] = catt3;
        cattlen = 4;
      } else {
        cat[0] = 0.0;
        catlen = 1;
        catt[0] = 0.0;
        cattlen = 1;
      }

      if (bdxtail != 0.0) {
        temp16alen = scale_expansion_zeroelim(bxtcalen, bxtca, bdxtail, temp16a);
        bxtcatlen = scale_expansion_zeroelim(catlen, cat, bdxtail, bxtcat);
        temp32alen = scale_expansion_zeroelim(bxtcatlen, bxtcat, 2.0 * bdx,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;
        if (cdytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, aa, bdxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, cdytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (adytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, cc, -bdxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, adytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32alen = scale_expansion_zeroelim(bxtcatlen, bxtcat, bdxtail,
                                              temp32a);
        bxtcattlen = scale_expansion_zeroelim(cattlen, catt, bdxtail, bxtcatt);
        temp16alen = scale_expansion_zeroelim(bxtcattlen, bxtcatt, 2.0 * bdx,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(bxtcattlen, bxtcatt, bdxtail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (bdytail != 0.0) {
        temp16alen = scale_expansion_zeroelim(bytcalen, bytca, bdytail, temp16a);
        bytcatlen = scale_expansion_zeroelim(catlen, cat, bdytail, bytcat);
        temp32alen = scale_expansion_zeroelim(bytcatlen, bytcat, 2.0 * bdy,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32alen = scale_expansion_zeroelim(bytcatlen, bytcat, bdytail,
                                              temp32a);
        bytcattlen = scale_expansion_zeroelim(cattlen, catt, bdytail, bytcatt);
        temp16alen = scale_expansion_zeroelim(bytcattlen, bytcatt, 2.0 * bdy,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(bytcattlen, bytcatt, bdytail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }
    if ((cdxtail != 0.0) || (cdytail != 0.0)) {
      if ((adxtail != 0.0) || (adytail != 0.0)
          || (bdxtail != 0.0) || (bdytail != 0.0)) {
        //port:        Two_Product(adxtail, bdy, ti1, ti0);
        ti1 = Two_Product_Head(adxtail, bdy);
        ti0 = Two_Product_Tail(adxtail, bdy, ti1);
        //endport
        //port:        Two_Product(adx, bdytail, tj1, tj0);
        tj1 = Two_Product_Head(adx, bdytail);
        tj0 = Two_Product_Tail(adx, bdytail, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, u3, u[2], u[1], u[0]);
        u3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        u[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        u[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        u[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        u[3] = u3;
        negate = -ady;
        //port:        Two_Product(bdxtail, negate, ti1, ti0);
        ti1 = Two_Product_Head(bdxtail, negate);
        ti0 = Two_Product_Tail(bdxtail, negate, ti1);
        //endport
        negate = -adytail;
        //port:        Two_Product(bdx, negate, tj1, tj0);
        tj1 = Two_Product_Head(bdx, negate);
        tj0 = Two_Product_Tail(bdx, negate, tj1);
        //endport
        //port:        Two_Two_Sum(ti1, ti0, tj1, tj0, v3, v[2], v[1], v[0]);
        v3 = Two_Two_Sum__x3(ti1, ti0, tj1, tj0);
        v[2] = Two_Two_Sum__x2(ti1, ti0, tj1, tj0);
        v[1] = Two_Two_Sum__x1(ti1, ti0, tj1, tj0);
        v[0] = Two_Two_Sum__x0(ti1, ti0, tj1, tj0);
        //endport
        v[3] = v3;
        abtlen = fast_expansion_sum_zeroelim(4, u, 4, v, abt);

        //port:        Two_Product(adxtail, bdytail, ti1, ti0);
        ti1 = Two_Product_Head(adxtail, bdytail);
        ti0 = Two_Product_Tail(adxtail, bdytail, ti1);
        //endport
        //port:        Two_Product(bdxtail, adytail, tj1, tj0);
        tj1 = Two_Product_Head(bdxtail, adytail);
        tj0 = Two_Product_Tail(bdxtail, adytail, tj1);
        //endport
        //port:        Two_Two_Diff(ti1, ti0, tj1, tj0, abtt3, abtt[2], abtt[1], abtt[0]);
        abtt3 = Two_Two_Diff__x3(ti1, ti0, tj1, tj0);
        abtt[2] = Two_Two_Diff__x2(ti1, ti0, tj1, tj0);
        abtt[1] = Two_Two_Diff__x1(ti1, ti0, tj1, tj0);
        abtt[0] = Two_Two_Diff__x0(ti1, ti0, tj1, tj0);
        //endport
        abtt[3] = abtt3;
        abttlen = 4;
      } else {
        abt[0] = 0.0;
        abtlen = 1;
        abtt[0] = 0.0;
        abttlen = 1;
      }

      if (cdxtail != 0.0) {
        temp16alen = scale_expansion_zeroelim(cxtablen, cxtab, cdxtail, temp16a);
        cxtabtlen = scale_expansion_zeroelim(abtlen, abt, cdxtail, cxtabt);
        temp32alen = scale_expansion_zeroelim(cxtabtlen, cxtabt, 2.0 * cdx,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;
        if (adytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, bb, cdxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, adytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (bdytail != 0.0) {
          temp8len = scale_expansion_zeroelim(4, aa, -cdxtail, temp8);
          temp16alen = scale_expansion_zeroelim(temp8len, temp8, bdytail,
                                                temp16a);
          finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen,
                                                  temp16a, finother);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32alen = scale_expansion_zeroelim(cxtabtlen, cxtabt, cdxtail,
                                              temp32a);
        cxtabttlen = scale_expansion_zeroelim(abttlen, abtt, cdxtail, cxtabtt);
        temp16alen = scale_expansion_zeroelim(cxtabttlen, cxtabtt, 2.0 * cdx,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(cxtabttlen, cxtabtt, cdxtail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (cdytail != 0.0) {
        temp16alen = scale_expansion_zeroelim(cytablen, cytab, cdytail, temp16a);
        cytabtlen = scale_expansion_zeroelim(abtlen, abt, cdytail, cytabt);
        temp32alen = scale_expansion_zeroelim(cytabtlen, cytabt, 2.0 * cdy,
                                              temp32a);
        temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                temp32alen, temp32a, temp48);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len,
                                                temp48, finother);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32alen = scale_expansion_zeroelim(cytabtlen, cytabt, cdytail,
                                              temp32a);
        cytabttlen = scale_expansion_zeroelim(abttlen, abtt, cdytail, cytabtt);
        temp16alen = scale_expansion_zeroelim(cytabttlen, cytabtt, 2.0 * cdy,
                                              temp16a);
        temp16blen = scale_expansion_zeroelim(cytabttlen, cytabtt, cdytail,
                                              temp16b);
        temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a,
                                                 temp16blen, temp16b, temp32b);
        temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a,
                                                temp32blen, temp32b, temp64);
        finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len,
                                                temp64, finother);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }

    return finnow[finlength - 1];
  }
}

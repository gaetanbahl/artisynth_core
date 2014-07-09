/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.spatialmotion.*;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.render.*;

import javax.media.opengl.*;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SegmentedPlanarConnector extends RigidBodyConnector 
   implements CopyableComponent {
   // protected Point3d myPCA;

   // protected ArrayList<Plane> myPlanes;
   // protected ArrayList<Point3d> myPoints;

   // protected RigidTransform3d myXPB;
   // private RigidTransform3d myXBA;
   // private Twist myVelA;
   // private Twist myVelB;
   // private Vector3d myNrm; // scratch copy of the normal
   private double myPlaneSize;
   private static final double defaultPlaneSize = 1;
   private boolean myRenderNormalReversedP = false;
   // private ArrayList<RigidBodyConstraint> myArray =
   // new ArrayList<RigidBodyConstraint>();

   private SegmentedPlanarCoupling mySegPlaneCoupling;
   private Point3d[] myRenderVtxs;
   private float[] myRenderCoords = new float[3];

   public static PropertyList myProps =
      new PropertyList (
         SegmentedPlanarConnector.class, RigidBodyConnector.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createPointFaceProps (null);
      props.setFaceStyle (RenderProps.Faces.FRONT_AND_BACK);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(1);

   static {
      myProps.addReadOnly (
         "activation getPlanarActivation", "activation of planar constraint");
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add ("planeSize * *", "renderable size of the plane", null);
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = defaultPlaneSize;
      setRenderProps (defaultRenderProps (null));
   }

   public double getPlaneSize() {
      return myPlaneSize;
   }

   public void setPlaneSize (double len) {
      myPlaneSize = len;
   }

   public double getPlanarActivation() {
      // segmented planar connector is defined by only one constraint
      return super.getActivation (0);
   }

   private void initializeCoupling() {
      mySegPlaneCoupling = new SegmentedPlanarCoupling ();
      myCoupling = mySegPlaneCoupling;
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public SegmentedPlanarConnector() {
      setDefaultValues();
      myRenderVtxs = new Point3d[4];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      initializeCoupling();
   }

   public SegmentedPlanarConnector (RigidBody bodyA, Vector3d pCA,
   RigidBody bodyB, RigidTransform3d XPB, double[] segs) {
      this();
      set (bodyA, pCA, bodyB, XPB, segs);
   }

   public SegmentedPlanarConnector (RigidBody bodyA, Vector3d pCA,
   RigidTransform3d XPW, double[] segs) {
      this();
      set (bodyA, pCA, XPW, segs);
   }

   /**
    * Sets this SegmentedPlanarConnectorX to connect two rigid bodies. The first
    * body (A) is the one in which the contact point is fixed, while the second
    * body (B) is the one in which the planes are fixed.
    * 
    * <p>
    * The plane segments are defined by a sequence of x-z coordinate pairs in
    * the D coordinate frame. The number of segments equals the number of pairs
    * minus one, so there must be at least two pairs (which would define a
    * single plane). Segments are assumed to be contiguous, and the normal for
    * each is defined by u X y, where u is a vector in the direction of the
    * segment (from first to last coordinate pair) and y is the direction of the
    * y axis. The first and last plane segments are assumed to extend to
    * infinity.
    * 
    * @param bodyA
    * first rigid body
    * @param pCA
    * location of contact point relative to body A
    * @param bodyB
    * second rigid body
    * @param XDB
    * transformation from frame D to body B
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane.
    */
   public void set (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d XDB,
      double[] segs) {
      doset (bodyA, pCA, bodyB, XDB, segs);
   }

   /**
    * Sets this SegmentedPlanarConnectorX to connect a rigid body with the world
    * frame. The contact point is fixed in the body frame, while the planes are
    * fixed in the world frame. Plane segments are defined as in the other
    * {@link #set(RigidBody,Vector3d,RigidBody,RigidTransform3d,double[]) set}
    * routine.
    * 
    * @param bodyA
    * rigid body
    * @param pCA
    * location of contact point relative to body
    * @param XDW
    * transformation from frame D to world coordinates
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane.
    */
   public void set (
      RigidBody bodyA, Vector3d pCA, RigidTransform3d XDW, double[] segs) {
      doset (bodyA, pCA, null, XDW, segs);
   }

   // private void makePlanesFromSegments (
   // RigidTransform3d XPB, ArrayList<Point3d> segPoints)
   // {
   // myPlanes = new ArrayList<Plane>();
   // // now create the planes
   // Vector3d u = new Vector3d();
   // Vector3d nrm = new Vector3d();
   // Vector3d y = new Vector3d (XPB.R.m01, XPB.R.m11, XPB.R.m21);
   // for (int i=1; i<segPoints.size(); i++)
   // { u.sub (segPoints.get(i), segPoints.get(i-1));
   // nrm.cross (u, y);
   // nrm.normalize();
   // myPlanes.add (new Plane (nrm, nrm.dot(segPoints.get(i))));
   // }
   // }

   // private void makeSegmentPoints (
   // RigidTransform3d XPB, double[] segs)
   // {
   // myPoints = new ArrayList<Point3d>();
   // // make private copy of the points
   // for (int i=0; i<segs.length; i+=2)
   // { Point3d pnt = new Point3d(segs[i], 0.0, segs[i+1]);
   // pnt.transform (XPB);
   // myPoints.add (pnt);
   // }
   // }

   private void doset (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d XDB,
      double[] segs) {
      RigidTransform3d XFA = new RigidTransform3d();
      XFA.p.set (pCA);
      setBodies (bodyA, XFA, bodyB, XDB);
      mySegPlaneCoupling.setSegments (segs);
   }

   // public Plane closestPlane (Point3d p)
   // {
   // int nearestIdx = -1;
   // double dsqrMin = 0;

   // // u will be a vector in the direction of the segment
   // Vector3d u = new Vector3d();
   // // v will be a vector from p to the first segment point
   // Vector3d v = new Vector3d();

   // for (int i=0; i<myPlanes.size(); i++)
   // { double dplane = myPlanes.get(i).distance (p);
   // double dsqr = dplane*dplane;

   // u.sub (myPoints.get(i+1), myPoints.get(i));
   // double segLength = u.norm();
   // u.scale (1/segLength);

   // v.sub (myPoints.get(i), p);
   // double vdotu = v.dot(u);
   // if (vdotu > 0 && i > 0)
   // { dsqr += (vdotu*vdotu);
   // }
   // else if (vdotu < -segLength && i < myPlanes.size()-1)
   // { dsqr += (vdotu+segLength)*(vdotu+segLength);
   // }
   // if (nearestIdx == -1 || dsqr < dsqrMin)
   // { nearestIdx = i;
   // dsqrMin = dsqr;
   // }
   // }
   // return myPlanes.get(nearestIdx);
   // }

   // public void updateValues (double t)
   // {
   // super.updateValues (t);

   // // closest plane and distances

   // Point3d pCB = new Point3d();
   // pCB.inverseTransform (mmXBA, myPCA);
   // Plane plane = closestPlane (pCB);
   // myDistances[0] = plane.distance (pCB);

   // // wrenches
   // myNrm.transform (mmXBA, plane.getNormal());
   // myWrenches[0].f.set (myNrm);
   // myWrenches[0].m.cross (myPCA, myNrm);

   // // derivatives

   // // get normal in A coordinates
   // myNrm.transform (mmXBA, plane.getNormal());
   // // can use either velBA or velAB because one is the negative
   // // of the other and the signs will cancel
   // Wrench Gdot = new Wrench();
   // Gdot.f.cross (mmVelBA.w, myNrm);
   // Gdot.m.cross (myPCA, Gdot.f);
   // myDerivatives[0] = Gdot.dot (mmVelBA);
   // }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      RigidTransform3d XDW = getCurrentXDW();
      for (int i = 0; i < mySegPlaneCoupling.numPlanes(); i++) {
         computeRenderVtxs (i, XDW);
         for (int k = 0; k < myRenderVtxs.length; k++) {
            myRenderVtxs[k].updateBounds (pmin, pmax);
         }
      }
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      RigidTransform3d XFW = getCurrentXFW();
      myRenderCoords[0] = (float)XFW.p.x;
      myRenderCoords[1] = (float)XFW.p.y;
      myRenderCoords[2] = (float)XFW.p.z;
   }

   public void render (GLRenderer renderer, int flags) {
      Vector3d nrm = new Vector3d (0, 0, 1);
      RigidTransform3d XDW = getCurrentXDW();

      GL2 gl = renderer.getGL2().getGL2();
      RenderProps props = myRenderProps;

      renderer.setMaterialAndShading (props, props.getFaceMaterial(), isSelected());
      renderer.setFaceMode (props.getFaceStyle());
      ArrayList<Plane> planes = mySegPlaneCoupling.getPlanes();

      for (int i = 0; i < planes.size(); i++) {
         Plane plane = planes.get (i);
         nrm.set (plane.getNormal());
         computeRenderVtxs (i, XDW);
         gl.glBegin (GL2.GL_POLYGON);
         if (myRenderNormalReversedP) {
            gl.glNormal3d (-nrm.x, -nrm.y, -nrm.z);
         }
         else {
            gl.glNormal3d (nrm.x, nrm.y, nrm.z);
         }
         for (int k = 0; k < myRenderVtxs.length; k++) {
            Point3d p = myRenderVtxs[k];
            gl.glVertex3d (p.x, p.y, p.z);
         }
         gl.glEnd();
      }
      renderer.restoreShading (props);
      renderer.setDefaultFaceMode();
      renderer.drawPoint (myRenderProps, myRenderCoords, isSelected());
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "segs")) {
         double[] segs = Scan.scanDoubles (rtok);
         mySegPlaneCoupling.setSegments (segs);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      pw.println ("segs=[");
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();
      for (int i = 0; i < segPnts.size(); i++) {
         Point3d p = segPnts.get (i);
         pw.println ("  " + fmt.format (p.x) + " " + fmt.format (p.z));
      }
      pw.println ("]");
      super.writeItems (pw, fmt, ancestor);
   }

   protected void computeRenderVtxs (int planeIdx, RigidTransform3d XDW) {
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();

      if (planeIdx >= segPnts.size() - 1) {
         throw new InternalErrorException ("index " + planeIdx
         + " exceeds number of planes");
      }
      Point3d p0 = segPnts.get (planeIdx);
      Point3d p1 = segPnts.get (planeIdx + 1);

      Vector3d yaxis = Vector3d.Y_UNIT;

      myRenderVtxs[0].set (p0);
      myRenderVtxs[0].scaledAdd (-myPlaneSize / 2, yaxis, myRenderVtxs[0]);

      myRenderVtxs[3].set (p0);
      myRenderVtxs[3].scaledAdd (myPlaneSize / 2, yaxis, myRenderVtxs[3]);

      myRenderVtxs[1].set (p1);
      myRenderVtxs[1].scaledAdd (-myPlaneSize / 2, yaxis, myRenderVtxs[1]);

      myRenderVtxs[2].set (p1);
      myRenderVtxs[2].scaledAdd (myPlaneSize / 2, yaxis, myRenderVtxs[2]);

      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].transform (XDW);
      }
   }

   // private void updateCouplingTransforms()
   // {
   // RigidTransform3d XCA = new RigidTransform3d();
   // XCA.p.set (myPCA);
   // myCoupling.setConstraintToBodyA (XCA);
   // }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myPlaneSize *= s;
      myRenderProps.scaleDistance (s);
   }

   // public void transformGeometry (
   // AffineTransform3dBase Xa, TransformableGeometry topObject)
   // {
   // // myPCA.mulAdd (Xa.getMatrix(), myPCA, Xa.getOffset());
   // if (myBodies.size() == 1)
   // { myXPB.mulAffineLeft (Xa, null);
   // for (int i=0; i<myPoints.size(); i++)
   // { myPoints.get(i).transform (Xa);
   // }
   // makePlanesFromSegments (myXPB, myPoints);
   // }
   // }

   public boolean isRenderNormalReversed() {
      return myRenderNormalReversedP;
   }

   public void setRenderNormalReversed (boolean reversed) {
      myRenderNormalReversedP = reversed;
   }

   // public RigidTransform3d getTransformPB()
   // {
   // return myXPB;
   // }

   public boolean isUnilateral() {
      return mySegPlaneCoupling.isUnilateral();
   }

   public void setUnilateral (boolean unilateral) {
      mySegPlaneCoupling.setUnilateral (unilateral);
      notifyParentOfChange (StructureChangeEvent.defaultEvent);
   }

   // public int numBilateralConstraints()
   // {
   // return getUnilateral() ? 0 : 1;
   // }

   // private void updateConstraint()
   // {
   // myArray.clear();
   // updateBodyVelocities();
   // RigidTransform3d XAW = myBodies.get(0).getPose();
   // RigidTransform3d XBW;
   // if (numBodies() == 2)
   // { XBW = myBodies.get(1).getPose();
   // }
   // else
   // { XBW = RigidTransform3d.IDENTITY;
   // }
   // // need to dynamically set the coupling plane
   // mmXBA.mulInverseLeft (XAW, XBW);
   // Point3d pCB = new Point3d();
   // pCB.inverseTransform (mmXBA, myPCA);
   // myCoupling.setXCBFromPlane (closestPlane (pCB));
   // myCoupling.getConstraints (
   // myArray, 0, XAW, XBW, mmVelA, mmVelB);
   // setBodyIndices (myArray, 0, 1);
   // }

   // public int getBilateralConstraints (
   // ArrayList<RigidBodyConstraint> bilaterals)
   // {
   // if (getUnilateral())
   // { return 0;
   // }
   // else
   // { updateConstraint();
   // bilaterals.add (myArray.get(0));
   // return 1;
   // }
   // }

   // @Override
   // public boolean hasUnilateralConstraints()
   // {
   // return true;
   // }

   // @Override
   // public int getUnilateralConstraints (
   // ArrayList<RigidBodyConstraint> unilaterals, double dlimit)
   // {
   // if (getUnilateral())
   // { updateConstraint();
   // RigidBodyConstraint c = myArray.get(0);
   // if (c.getDistance() < dlimit)
   // { unilaterals.add (c);
   // return 1;
   // }
   // }
   // return 0;
   // }

   // @Override
   // public int updateUnilateralConstraints (
   // ArrayList<RigidBodyConstraint> unilaterals, int offset, int num)
   // {
   // if (getUnilateral() && num == 1)
   // { updateConstraint();
   // return offset + 1;
   // }
   // else
   // { return offset;
   // }
   // }

   // /**
   // * {@inheritDoc}
   // */
   // @Override
   // public void updateForBodyPositionChange (
   // RigidBody body, RigidTransform3d XBodyToWorld)
   // {
   // if (body == myBodies.get(0))
   // { // bodyA
   // }
   // else if (body == myBodies.get(1))
   // { // bodyB
   // }
   // else
   // { throw new IllegalArgumentException (
   // "body is not referenced by this connector");
   // }
   // }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SegmentedPlanarConnector copy =
         (SegmentedPlanarConnector)super.copy (flags, copyMap);
      copy.initializeCoupling();
      copy.setPlaneSize (myPlaneSize);
      copy.setUnilateral (isUnilateral());
      copy.setRenderProps (getRenderProps());
      copy.setBodies (copy.myBodyA, getXFA(), copy.myBodyB, getXDB());
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();
      double[] segs = new double[segPnts.size() * 2];
      for (int i = 0; i < segPnts.size(); i++) {
         Point3d pnt = segPnts.get (i);
         segs[i * 2] = pnt.x;
         segs[i * 2 + 1] = pnt.z;
      }
      copy.mySegPlaneCoupling.setSegments (segs);
      return copy;
   }

}

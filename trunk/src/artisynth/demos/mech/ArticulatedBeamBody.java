package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

public class ArticulatedBeamBody extends RootModel {
   public static boolean debug = false;

   boolean readDemoFromFile = false;

   boolean writeDemoToFile = true;

   boolean usePlanarJoint = false;
   boolean useLink2 = true;

   boolean useSphericalJoint = false;

   boolean usePlanarContacts = false;

   Color myLinkColor = new Color (228/255f, 115/255f, 33/255f);
   Color myJointColor = new Color (93/255f, 93/255f, 168/255f);
   Color myEdgeColor = new Color (144/255f, 52/255f, 0);

   double lenx0 = 15;
   double leny0 = 15;
   double lenz0 = 1.5;
   double stiffness = 10;
   double density = 1;

   RigidBody myBase;

   public ArticulatedBeamBody() {
      super (null);
   }

   // private void addBase (MechModel mechMod) {
   //    myBase = new RigidBody ("base");
   //    myBase.setInertia (SpatialInertia.createBoxInertia (
   //       10, lenx0, leny0, lenz0));
   //    PolygonalMesh mesh = MeshFactory.createTriangularBox (lenx0, leny0, lenz0);
   //    // XMB.setIdentity();
   //    // XMB.R.setAxisAngle (1, 1, 1, 2*Math.PI/3);
   //    // mesh.transform (XMB);
   //    // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
   //    myBase.setMesh (mesh, /* fileName= */null);
   //    RigidTransform3d XLW = new RigidTransform3d();
   //    XLW.setIdentity();
   //    XLW.p.set (0, 0, 22);
   //    myBase.setPose (XLW);
   //    myBase.setDynamic (false);
   //    mechMod.addRigidBody (myBase);
   // }

   public ArticulatedBeamBody (String name) {
      this();
      setName (name);

      MechModel mechMod = new MechModel ("mechMod");
      // mechMod.setProfiling (true);
      mechMod.setGravity (0, 0, -9.8);
      // mechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      mechMod.setFrameDamping (0.1);
      mechMod.setRotaryDamping (0.1);
      mechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      RigidTransform3d XMB = new RigidTransform3d();
      RigidTransform3d XLW = new RigidTransform3d();
      RigidTransform3d XCA = new RigidTransform3d();
      RigidTransform3d XCB = new RigidTransform3d();
      RigidTransform3d XAB = new RigidTransform3d();
      PolygonalMesh mesh;
      int nslices = 16; // number of slices for approximating a circle
      int nsegs = 16; // number of cylinder segments

      // // set view so tha points upwards
      // X.R.setAxisAngle (1, 0, 0, -Math.PI/2);
      // viewer.setTransform (X);

      RenderProps props;

      FrameMarker mk0 = new FrameMarker();
      props = mk0.createRenderProps();
      // props.setColor (Color.GREEN);
      props.setPointRadius (0.1);
      props.setPointStyle (RenderProps.PointStyle.SPHERE);
      mk0.setRenderProps (props);
      //mechMod.addFrameMarker (mk0, myBase, new Point3d (lenx0 / 2, leny0 / 2, 0));

      // FrameMarker mk1 = new FrameMarker();
      // mk1.setRenderProps (props);
      // mechMod.addFrameMarker (
      //    mk1, myBase, new Point3d (-lenx0 / 2, -leny0 / 2, 0));

      // FrameMarker mk2 = new FrameMarker();
      // mk2.setRenderProps (props);

      // FrameMarker mk3 = new FrameMarker();
      // mk3.setRenderProps (props);

      double ks = 10;
      double ds = 10;

      // AxialSpring spr0 = new AxialSpring (50, 10, 0);
      // AxialSpring spr1 = new AxialSpring (50, 10, 0);

      // props = spr0.createRenderProps();
      // props.setLineStyle (RenderProps.LineStyle.CYLINDER);
      // props.setLineRadius (0.2);
      // props.setLineColor (Color.RED);
      // spr0.setRenderProps (props);
      // spr1.setRenderProps (props);

      // mechMod.addRigidBody (base);

      // first link
      double lenx1 = 2;
      double leny1 = 0.4;
      double lenz1 = 0.6;

      XMB.setIdentity();
      XMB.R.setAxisAngle (0, 1, 0, Math.PI/2);

      mesh =
         MeshFactory.createRoundedBox (
            lenx1, leny1, lenz1, 20, 4, 4, nslices / 2);
      mesh.transform (XMB);
      // mesh =
      //    MeshFactory.createBox (
      //       lenx1+leny1, leny1, lenz1, new Point3d(), 24, 4, 4);
      //mesh.triangulate();
      RigidBody link1 = null;
      if (false && useLink2) {
         link1 = RigidBody.createFromMesh ("link1", mesh, density, 1.0);         
      }
      else {
         link1 = new BeamBody (mesh, density, lenx1, stiffness); 
      }
      link1.setName ("link1");
      if (useLink2) {
         //link1.setDynamic (false);
      }

      if (link1 instanceof DeformableBody) {
         DeformableBody defBody = (DeformableBody)link1;
         defBody.setMassDamping (1);
         defBody.setMaterial (new LinearMaterial (5*stiffness, 0.3));
      }

      // mesh.setRenderMaterial (Material.createSpecial (Material.GRAY));
      //link1.setMesh (mesh, /* fileName= */null);
      //XLW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
      // // XLW.R.mulAxisAngle (0, 1, 0, Math.PI/4);
      XLW.p.set (0, 0, 1.5 * lenx1);
      link1.setPose (XLW);
      mechMod.addRigidBody (link1);

      // mechMod.addFrameMarker (mk2, link1, new Point3d (
      //    -lenx1 / 2, 0, -lenz1 / 2));
      // mechMod.addFrameMarker (
      //    mk3, link1, new Point3d (-lenx1 / 2, 0, lenz1 / 2));

      RigidBodyConnector joint1 = null;
      RigidBodyConnector joint2 = null;

      // joint 1
      if (usePlanarJoint) {
         XCA.setIdentity();
         XCA.p.set (-lenx1/2, 0, 0);
         XCA.R.setAxisAngle (1, 0, 0, -Math.PI/2);
         XCB.p.set (0, 0, lenx1);
         // XCB.R.setAxisAngle (1, 0, 0, -Math.PI/2);
         PlanarConnector planar =
            new PlanarConnector (link1, XCA.p, XCB);
         planar.setName ("plane1");
         planar.setPlaneSize (20);
         RenderProps.setPointColor (planar, Color.BLUE);
         joint1 = planar;
      }
      else {
         XCA.setIdentity();
         XCA.p.set (-lenx1/2, 0, 0);
         XCA.R.set (1, 0, 0, 0, 0, -1, 0, 1, 0);
         //XCA.R.mulAxisAngle (1, 0, 0, Math.PI/2);
         XCB.set (link1.getPose());
         XCB.mul (XCA);
         RevoluteJoint rjoint = new RevoluteJoint (link1, XCA, XCB);
         rjoint.setName ("joint1");
         rjoint.setAxisLength (0.8);
         RenderProps.setLineRadius(rjoint, 0.04);
         RenderProps.setLineColor (rjoint, myJointColor);
         joint1 = rjoint;
         // SphericalJoint sjoint = new SphericalJoint (
         // link1, XCA, XCB);
         // sjoint.setName ("joint1");
         // sjoint.setAxisLength (5);
         // joint1 = sjoint;
         //joint1 = new SolidJoint (link1, XCA, XCB);
      }

      // second link
      double lenx2 = 2;
      double leny2 = 0.4;
      double lenz2 = 0.4;

      // mesh =
      //    MeshFactory.createTriangularRoundedCylinder (
      //       leny2/2, lenx2, nslices, nsegs);
      mesh =
         MeshFactory.createRoundedBox (
            lenx2, leny2, lenz2, 20, 4, 4, nslices / 2);
      mesh.transform (XMB);
      // mesh =
      //    MeshFactory.createBox (
      //       lenx2+leny2, leny2, leny2, new Point3d(), 24, 4, 4);


      DeformableBody link2 = new BeamBody (mesh, density, lenx2, stiffness);   
      link2.setName ("link2");

      //RenderProps.setDrawEdges (link1, true);
      //RenderProps.setDrawEdges (link2, true);
      RenderProps.setFaceColor (link1, myLinkColor);
      RenderProps.setFaceColor (link2, myLinkColor);
      RenderProps.setEdgeColor (link1, myEdgeColor);
      RenderProps.setEdgeColor (link2, myEdgeColor);

      //RenderProps.setFaceStyle (link1, RenderProps.Faces.NONE);
      //RenderProps.setFaceStyle (link2, RenderProps.Faces.NONE);

      //XLW.R.setAxisAngle (1, 0, 0, Math.PI / 2);
      XLW.p.set (lenx1 / 2 + lenx2 / 2, 0, 1.5 * lenx1);
      if (useSphericalJoint) {
         double ang = 0; // Math.PI/4;
         XLW.R.mulAxisAngle (0, 1, 0, ang);
         XLW.p.y += Math.sin (ang) * lenx2 / 2;
         XLW.p.x -= (1 - Math.cos (ang)) * lenx2 / 2;
      }
      link2.setPose (XLW);
      //link2.setMesh (mesh, /* fileName= */null);

      if (link2 instanceof DeformableBody) {
         DeformableBody defBody = (DeformableBody)link2;
         defBody.setMassDamping (1);
         defBody.setMaterial (new LinearMaterial (5*stiffness, 0.3));
      }

      if (useLink2) {
         mechMod.addRigidBody (link2);
      }

      // joint 2
      if (useSphericalJoint) {
         XCA.setIdentity();
         XCA.p.set (-lenx2 / 2, 0, 0);
         XAB.mulInverseLeft (link1.getPose(), link2.getPose());
         XCB.mul (XAB, XCA);
         SphericalJoint sjoint = new SphericalJoint (link2, XCA, link1, XCB);
         // RevoluteJoint joint2 = new RevoluteJoint (link2, XCA, XCB);
         sjoint.setName ("joint2");
         // RenderProps.setLineRadius(sjoint, 0.2);
         sjoint.setAxisLength (1);
         joint2 = sjoint;
      }
      else {
         XCA.setIdentity();
         XCA.p.set (-lenx2 / 2, 0, 0);
         XCA.R.set (1, 0, 0, 0, 0, -1, 0, 1, 0);
         // XCA.R.mulAxisAngle (1, 0, 0, -Math.toRadians(90));
         XAB.mulInverseLeft (link1.getPose(), link2.getPose());
         XCB.mul (XAB, XCA);
         RevoluteJoint rjoint = new RevoluteJoint (link2, XCA, link1, XCB);
         RenderProps.setLineColor (rjoint, myJointColor);
         rjoint.setName ("joint2");
         rjoint.setAxisLength (0.8);
         RenderProps.setLineRadius (rjoint, 0.04);
         joint2 = rjoint;
      }


      RenderProps.setPointColor (mechMod, Color.BLUE);
      RenderProps.setPointStyle (mechMod, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, 0.05);

      if (!useLink2) {
         FrameMarker mkr = new FrameMarker ("mkr1");
         mechMod.addFrameMarker (mkr, link1, new Point3d ((lenx1+leny1)/2, 0, 0));
         mkr.setExternalForce (new Vector3d (0, 3, 0));
      }

      // if (useLink2) {
      //    FrameMarker mkr = new FrameMarker ("mkr2");
      //    mechMod.addFrameMarker (mkr, link2, new Point3d ((lenx2+leny2)/2, 0, 0));
      //    mkr.setExternalForce (new Vector3d(0, 0, 0));
      // }
      
      if (joint1 != null) {
         mechMod.addRigidBodyConnector (joint1);
      }
      if (useLink2 && joint2 != null) {
         mechMod.addRigidBodyConnector (joint2);
      }
      // mechMod.attachAxialSpring (mk0, mk2, spr0);
      // mechMod.attachAxialSpring (mk1, mk3, spr1);

      if (usePlanarContacts) {
         XCA.setIdentity();
         XCA.p.set (lenx2 / 2 + leny2 / 2, 0, 0);
         XCB.setIdentity();
         // XCB.p.set (0, 0, -lenx2/2);
         // XCB.p.set (0, 0, lenx2/2);

         XCB.R.setIdentity();
         XCB.R.setAxisAngle (0, 0, 1, Math.PI / 2);
         XCB.R.mulAxisAngle (1, 0, 0, Math.toRadians (20));

         PlanarConnector contact1 = new PlanarConnector (link2, XCA.p, XCB);
         contact1.setUnilateral (true);
         contact1.setName ("contact1");
         contact1.setPlaneSize (20);
         RenderProps.setFaceColor (contact1, new Color (0.5f, 0.5f, 1f));
         RenderProps.setAlpha (contact1, 0.5);
         mechMod.addRigidBodyConnector (contact1);

         XCB.R.setIdentity();
         XCB.R.setAxisAngle (0, 0, 1, Math.PI / 2);
         XCB.R.mulAxisAngle (1, 0, 0, -Math.toRadians (20));

         PlanarConnector contact2 = new PlanarConnector (link2, XCA.p, XCB);
         contact2.setUnilateral (true);
         contact2.setName ("contact2");
         contact2.setPlaneSize (20);
         RenderProps.setFaceColor (contact2, new Color (0.5f, 0.5f, 1f));
         RenderProps.setAlpha (contact2, 0.5);

         mechMod.addRigidBodyConnector (contact2);
      }

      //mechMod.setBounds (new Point3d (0, 0, -10), new Point3d (0, 0, 10));

      addModel (mechMod);


      // RigidTransform3d X = new RigidTransform3d (link1.getPose());
      // X.R.mulRpy (Math.toRadians(-10), 0, 0);
      // link1.setPose (X);
      // mechMod.projectRigidBodyPositionConstraints();

      //mechMod.setProfiling (true);
      //mechMod.setIntegrator (Integrator.ForwardEuler);
      //addBreakPoint (0.57);
   }

   ControlPanel myControlPanel;

   public void attach (DriverInterface driver) {
      super.attach (driver);

      if (getControlPanels().size() == 0) {
         myControlPanel = new ControlPanel ("options", "");
         myControlPanel.addWidget (this, "models/mechMod:integrator");
         myControlPanel.addWidget (this, "models/mechMod:maxStepSize");
         myControlPanel.pack();
         //myControlPanel.setVisible (true);
         java.awt.Point loc = driver.getFrame().getLocation();
         myControlPanel.setLocation (
            loc.x + driver.getFrame().getWidth(), loc.y);
         addControlPanel (myControlPanel);
      }
      WayPoint way = new WayPoint (1.3);
      way.setBreakPoint (true);
      // Main.getWorkspace().addWayPoint (way);
   }

   public void detach (DriverInterface driver) {
      super.detach (driver);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return null;
   }

}

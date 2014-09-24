/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import maspack.render.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding RigidBodyConnectors to a MechModel.
 */
public class RigidBodyConnectorAgent extends
AddComponentAgent<RigidBodyConnector> {
   private MechModel myModel;
   private RigidBody myBodyA;
   private RigidBody myBodyB;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   private JButton myFixedButton;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == RevoluteJoint.class) {
         RevoluteJoint joint = (RevoluteJoint)comp;
         joint.setAxisLength (getDefaultAxisLength());
         RenderProps.setLineRadius (joint, getDefaultLineRadius());
      }
      else if (type == SphericalJoint.class) {
         SphericalJoint joint = (SphericalJoint)comp;
         joint.setAxisLength (getDefaultAxisLength());
      }
   }

   protected void setInitialState() {
      setState (State.SelectingBodyA);
   }

   protected void resetState() {
      setState (State.Complete);
   }

   private class BodyFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return (c instanceof RigidBody
         && ComponentUtils.withinHierarchy (c, (ModelComponent)myModel) && (myState != State.SelectingBodyB || c != myBodyA));
      }
   }

   private enum State {
      SelectingBodyA, SelectingBodyB, SelectingLocation, Complete
   };

   private State myState = State.Complete;

   private void setState (State state) {
      switch (state) {
         case SelectingBodyA: {
            myInstructionBox.setText ("Select first rigidBody");
            myFixedButton.setEnabled (false);
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText ("");
            uninstallLocationListener();
            installSelectionFilter (new BodyFilter());
            break;
         }
         case SelectingBodyB: {
            myInstructionBox.setText ("Select second rigidBody or click Fixed");
            myFixedButton.setEnabled (true);
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText (getBodyName (myBodyA));
            uninstallLocationListener();
            installSelectionFilter (new BodyFilter());
            break;
         }
         case SelectingLocation: {
            myInstructionBox.setText ("Pick location in viewer");
            myFixedButton.setEnabled (false);
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText (getBodyName (myBodyA) + " - "
            + (myBodyB == null ? "fixed" : getBodyName (myBodyB)));
            installLocationListener();
            uninstallSelectionFilter();
            break;
         }
         case Complete: {
            myInstructionBox.setText (" ");
            myFixedButton.setEnabled (false);
            myAddButton.setText ("Add");
            myAddButton.setActionCommand ("Add");
            myProgressBox.setText ("");
            uninstallLocationListener();
            uninstallSelectionFilter();
            myBodyA = null;
            myBodyB = null;
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public void setBodies (RigidBody bodyA, RigidBody bodyB) {
      myBodyA = bodyA;
      myBodyB = bodyB;
      setState (State.SelectingLocation);
   }

   public RigidBodyConnectorAgent (Main main, MechModel model) {

      super (main,
             (ComponentList<RigidBodyConnector>)model.rigidBodyConnectors(),
             model);
      myModel = model;
   }

   // public void show()
   // {
   // myDisplay = createDisplay();
   // setConnectorType ("RevoluteJoint");
   // GuiUtils.locateRight (myDisplay, myMain.getFrame());
   // myComponentList.setSelectionManager (mySelectionManager);
   // mySelectionManager.addSelectionListener(this);
   // setState (State.SelectingBodyA);
   // myRootModel = myMain.getRootModel();
   // myRootModel.addComponentChangeListener (this);
   // if (myRootModel != myLastRootModel)
   // { resetTypeMap();
   // myLastRootModel = myMain.getRootModel();
   // }
   // myMain.getWorkspace().registerDisposable(this);
   // myDisplay.setVisible (true);
   // // installMouseRayListener();
   // }

   protected void createDisplay() {
      createDisplayFrame ("Add RigidBodyConnectors");

      addComponentType (RevoluteJoint.class, new String[] {"name"});
      addBasicProps (RevoluteJoint.class,
                     new String[] {"renderProps", "axisLength"});
      addComponentType (SphericalJoint.class, new String[] {"name"});
      addBasicProps (SphericalJoint.class,
                     new String[] {"renderProps", "axisLength"});

      createComponentList (
         "Existing rigid body connectors:",
         new RigidBodyConnectorList (myModel.rigidBodyConnectors(), myModel));
      createSeparator();
      createNameField();
      createTypeSelector ("Connector type");
      createPropertyFrame("Default TYPE properties:");
      // createSeparator();
      createProgressBox();
      createInstructionBox();
      JButton[] buttons = createOptionPanel ("Add Fixed Done");
      myFixedButton = buttons[1];

   }

   private String getBodyName (RigidBody body) {
      if (body == null) {
         return "fixed";
      }
      else {
         return ComponentUtils.getPathName (myModel, body);
      }
   }

   @Override
   public void selectionChanged (SelectionEvent e) {
      ModelComponent c = e.getLastAddedComponent();
      if (myState == State.SelectingBodyA) {
         if (c instanceof RigidBody) {
            myBodyA = (RigidBody)c;
            setState (State.SelectingBodyB);
         }
      }
      else if (myState == State.SelectingBodyB) {
         if (c instanceof RigidBody) {
            myBodyB = (RigidBody)c;
            setState (State.SelectingLocation);
         }
      }
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   @Override
   public void actionPerformed (ActionEvent e) {
      super.actionPerformed (e);
      String cmd = e.getActionCommand();
      if (cmd.equals ("Fixed")) {
         if (myState != State.SelectingBodyB) {
            throw new InternalErrorException ("Illegal state " + myState
            + ", expecting SelectBodyB");
         }
         myBodyB = null;
         setState (State.SelectingLocation);
      }
   }

   private void createAndAddConnector (Point3d origin) {
      RigidBodyConnector connector;

      RigidTransform3d TCW = new RigidTransform3d();
      TCW.R.set (myBodyA.getPose().R);
      TCW.p.set (origin);

      RigidTransform3d TCA = new RigidTransform3d();
      TCA.mulInverseLeft (myBodyA.getPose(), TCW);

      if (myComponentType == RevoluteJoint.class) {
         RevoluteJoint joint;
         if (myBodyB == null) {
            joint = new RevoluteJoint (myBodyA, TCA, TCW);
         }
         else {
            RigidTransform3d TCB = new RigidTransform3d();
            TCB.mulInverseLeft (myBodyB.getPose(), TCW);
            joint = new RevoluteJoint (myBodyA, TCA, myBodyB, TCB);
         }
         connector = joint;
      }
      else if (myComponentType == SphericalJoint.class) {
         SphericalJoint joint;
         if (myBodyB == null) {
            joint = new SphericalJoint (myBodyA, TCA, TCW);
         }
         else {
            RigidTransform3d TCB = new RigidTransform3d();
            TCB.mulInverseLeft (myBodyB.getPose(), TCW);
            joint = new SphericalJoint (myBodyA, TCA, myBodyB, TCB);
         }
         connector = joint;
      }
      else {
         throw new InternalErrorException ("Unimplemented connector type "
         + myComponentType);
      }
      connector.setName (getNameFieldValue());
      setProperties (connector, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
           "add RigidBodyConnector",
           connector,
           (MutableCompositeComponent<?>)myModel.rigidBodyConnectors()));

      setState (State.Complete);
      myMain.setSelectionMode (Main.SelectionMode.Translate);
   }

   @Override
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      Point3d origin = new Point3d();
      Line ray = rayEvent.getRay();
      ray.nearestPoint (origin, myBodyA.getPose().p);
      System.out.println ("origin=" + origin);

      createAndAddConnector (origin);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myModel, myMain.getRootModel()));
   }

}

class RigidBodyConnectorList extends ComponentListWidget<RigidBodyConnector> {
   RigidBodyConnectorList (ComponentListView<RigidBodyConnector> list,
   CompositeComponent ancestor) {
      super (list, ancestor);
   }

   @Override
   protected String getName (
      RigidBodyConnector comp, CompositeComponent ancestor) {
      RigidBody bodyA = comp.getBodyA();
      RigidBody bodyB = comp.getBodyB();
      if (bodyB != null) {
         return ComponentUtils.getPathName (ancestor, bodyA) + " - "
         + ComponentUtils.getPathName (ancestor, bodyB);
      }
      else {
         return ComponentUtils.getPathName (ancestor, bodyA) + " - " + "fixed";
      }
   }
}

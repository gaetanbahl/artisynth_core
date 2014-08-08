/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JMenuItem;

import maspack.matrix.AxisAngle;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.GLViewer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.util.Disposable;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Round;
import maspack.util.Write;
import maspack.util.DataBuffer;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeListener;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListView;
import artisynth.core.modelbase.ComponentState;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeState;
import artisynth.core.modelbase.Controller;
import artisynth.core.modelbase.HasState;
import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.ModelAgent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.Monitor;
import artisynth.core.modelbase.NumericState;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.RenderableModelBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.Tracable;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.probes.Probe;
import artisynth.core.probes.TracingProbe;
import artisynth.core.probes.WayPoint;
import artisynth.core.probes.WayPointProbe;
import artisynth.core.renderables.GLRenderableHolder;
import artisynth.core.util.*;

/**
 * RootModel is the top-most model of an ArtiSynth model hierarchy. It contains
 * a list of models, plus a number of other workspace components such as
 * probes, controller, monitors, and control panels.
 */
public class RootModel extends RenderableModelBase
   implements Disposable, ActionListener {
   // DependencyClosure[] myClosures = null;

   // Set this to true to test save-and-restore state before each model advance
   // step. (This will take some time, so you normally want it to be false).
   public static boolean testSaveAndRestoreState = false;

   LinkedList<ComponentChangeListener> myComponentListeners;
   protected ComponentList<Model> myModels;
   protected LinkedHashMap<Model,ModelInfo> myModelInfo;
   protected ComponentList<ControlPanel> myControlPanels;
   protected RenderableComponentList<Probe> myInputProbes;
   protected RenderableComponentList<Probe> myOutputProbes;
   protected RenderableComponentList<RenderableComponent> myRenderables;
   protected LinkedHashSet<Tracable> myTraceSet;
   protected WayPointProbe myWayPoints = null;
   protected ComponentList<Controller> myControllers;
   protected ComponentList<Monitor> myMonitors;

   // flag to stop advancing - which we need if we are in the midst of 
   // lots of small adaptive steps
   protected boolean myStopRequested = false;

   protected boolean myModelInfoValid = false;
   private ModelInfo myRootInfo;
   protected static boolean use125Stepping = true;

   protected boolean myAdaptiveStepping = DEFAULT_ADAPTIVE_STEPPING;
   protected double myMinStepSize = DEFAULT_MIN_STEP_SIZE;

   private static final Point3d DEFAULT_VIEWER_CENTER = new Point3d();
   private static final Point3d DEFAULT_VIEWER_EYE = new Point3d (0, -1, 0);
   private static final AxisAngle DEFAULT_VIEW_ORIENTATION = AxisAngle.ROT_X_90;
   private static final double DEFAULT_MIN_STEP_SIZE = 1e-7;
   private static final double DEFAULT_MAX_STEP_SIZE = 0.01;
   private static final boolean DEFAULT_ADAPTIVE_STEPPING = true;

   AxisAngle myDefaultViewOrientation = new AxisAngle (DEFAULT_VIEW_ORIENTATION);
   
   GLViewer myMainViewer;
   
   private JFrame controlPanelsFrame;
   private JTabbedPane controlPanelTabs;

   protected class ModelInfo {
      Model model;
      CompositeState state;
      LinkedList<Controller> controllers;
      LinkedList<Monitor> monitors;
      LinkedList<Probe> inputProbes;
      LinkedList<Probe> outputProbes;
      // Last state created by each component. Supplied to each component when
      // asked to create a new state, so as to provide sizing hints
      HashMap<HasState,ComponentState> lastStateMap;
      double h; // current step size
      double lasts; // last return value from advance
      int successCnt;
      int failedIncreaseCnt;
      boolean attemptingIncrease;

      ModelInfo (Model m) {
         controllers = new LinkedList<Controller>();
         monitors = new LinkedList<Monitor>();
         inputProbes = new LinkedList<Probe>();
         outputProbes = new LinkedList<Probe>();
         lastStateMap = new HashMap<HasState,ComponentState>();
         model = m;
         clear();
      }

      void clear() {
         inputProbes.clear();
         controllers.clear();
         monitors.clear();
         outputProbes.clear();
         lastStateMap.clear();
         h = getEffectiveMaxStepSize();
         lasts = 1;
         successCnt = 0;
         failedIncreaseCnt = 0;
         attemptingIncrease = false;
      }
      
      void createState() {
         state = createModelAndControllersState();
      }

      CompositeState createModelAndControllersState() {
         return new CompositeState();
      }

      CompositeState createFullState() {
         return new CompositeState();
      }
      
      private boolean maybeGetSubstate (
         CompositeState state, ModelComponent comp) {
         if (comp.hasState() && comp instanceof HasState) {
            HasState c = (HasState)comp;
            ComponentState prevState = lastStateMap.get(c);
            ComponentState substate = c.createState(prevState);
            lastStateMap.put(c, substate);
            c.getState (substate);
            state.addState (substate);
            return true;
         }
         else {
            return false;
         }
      }

      private boolean maybeGetInitialSubState (
         CompositeState state, ModelComponent comp,
         HashMap<Object,ComponentState> stateMap) {

         if (comp.hasState() && comp instanceof HasState) {
            HasState c = (HasState)comp;
            ComponentState prevState = lastStateMap.get(c);
            ComponentState substate = c.createState(prevState);
            lastStateMap.put(c, substate);
            c.getInitialState (substate, stateMap.get(comp));
            state.addState (substate);
            state.addComponent (comp);
            return true;
         }
         else {
            return false;
         }
      }

      private void doGetModelAndControllersState (CompositeState state) {
         for (Controller ctl : controllers) {
            maybeGetSubstate (state, ctl);
         }
         if (model == RootModel.this) {
            ComponentState substate = RootModel.this.createRootState();
            RootModel.this.getRootState (substate);
            state.addState (substate);
         }
         else {
            maybeGetSubstate (state, model);
         }       
      }

      void getModelAndControllersState (CompositeState state) {
         state.clear();
         doGetModelAndControllersState (state);
      }

      void getFullState (CompositeState state) {
         state.clear();
         doGetModelAndControllersState (state);
         for (Monitor mon : monitors) {
            maybeGetSubstate (state, mon);
         }  
         for (Probe prb : inputProbes) {
            maybeGetSubstate (state, prb);
         }         
         for (Probe prb : outputProbes) {
            maybeGetSubstate (state, prb);
         }         
      }

      void getInitialState (CompositeState newstate, CompositeState oldstate) {
         HashMap<Object,ComponentState> stateMap =
            new HashMap<Object,ComponentState>();

         if (oldstate != null) {
            if (oldstate.numComponents() != oldstate.numSubStates()) {
               throw new InternalErrorException (
                  "Oldstate has "+oldstate.numComponents()+" components and "+
                  oldstate.numSubStates()+" substates");
            }
            ArrayList<Object> comps = oldstate.getComponents();
            if (comps != null) {
               for (int i=0; i<oldstate.numSubStates(); i++) {
                  stateMap.put (comps.get(i), oldstate.getState(i));
               }
            }
         }
         
         for (Controller ctl : controllers) {
            maybeGetInitialSubState (newstate, ctl, stateMap);
         }
         if (model == RootModel.this) {
            ComponentState substate = RootModel.this.createRootState();
            RootModel.this.getRootState (substate);
            newstate.addState (substate);
            newstate.addComponent (RootModel.this);
         }
         else {
            maybeGetInitialSubState (newstate, model, stateMap);
         }       
         
         for (Monitor mon : monitors) {
            maybeGetInitialSubState (newstate, mon, stateMap);
         }  
         for (Probe prb : inputProbes) {
            maybeGetInitialSubState (newstate, prb, stateMap);
         }         
         for (Probe prb : outputProbes) {
            maybeGetInitialSubState (newstate, prb, stateMap);
         } 
      }

      int setModelAndControllersState (CompositeState state) {
         if (state.numSubStates() < this.state.numSubStates()) {
            throw new InternalErrorException (
               "state has only "+state.numSubStates()+" substates, "+
               this.state.numSubStates()+" required");
         }
         int idx = 0;
         for (Controller ctl : controllers) {
            if (ctl.hasState()) {
               ctl.setState (state.getState(idx++));
            }
         }
         if (model == RootModel.this) {
            RootModel.this.setRootState (state.getState(idx++));            
         }
         else if (model.hasState()) {
            model.setState (state.getState(idx++));
         }       
         return idx;
      }

      void setFullState (CompositeState state) {
         int idx = setModelAndControllersState (state);
         for (Monitor mon : monitors) {
            if (mon.hasState()) {
               mon.setState (state.getState(idx++));
            }
         } 
         for (Probe prb : inputProbes) {
            if (prb.hasState()) {
               prb.setState (state.getState(idx++));
            }
         }
         for (Probe prb : outputProbes) {
            if (prb.hasState()) {
               prb.setState (state.getState(idx++));
            }
         }
      }

      double getEffectiveMaxStepSize() {
         double modelMax = model.getMaxStepSize();
         double rootMax = getMaxStepSize();
         if (modelMax == -1 || modelMax >= rootMax) {
            return rootMax;
         }
         else {
            return modelMax;
         }
      }

      double getNextAdvanceTime (double t0, double t1) {
         double hmax = getEffectiveMaxStepSize();
         if (h > hmax) {
            h = hmax;
         }
         double te = nextProbeEvent (outputProbes, t0);
         if (TimeBase.compare (te, t1) < 0) {
            t1 = te;
         }
         attemptingIncrease = false; // should be false, just being paranoid
         if (myAdaptiveStepping) {
            if (h < hmax && TimeBase.compare (t1-t0, 2*h) >= 0) {
               // see if we can increase the step size
               // don't look at lasts for now.
               if (successCnt > failedIncreaseCnt) {
                  h = TimeBase.round (increaseStepSize (h, hmax));
                  System.out.println ("t0=" + t0 + ", > step " + h);
                  attemptingIncrease = true;
               }
            }
         }
         if (TimeBase.compare (t1-t0, h) > 0) {
            t1 = t0 + h;
         }
         return TimeBase.round (t1);
      }

      double reduceAdvanceTime (
         double s, double t0, double t1, String diagnostic) {

         successCnt = 0;
         if (attemptingIncrease) {
            failedIncreaseCnt++;
            attemptingIncrease = false;
         }
         // if s is not unspecified, limit it to 0.1
         if (s != 0) {
            s = Math.max (s, 0.1);
         }
         // need to reduce step size
         if (TimeBase.compare (t1-t0, h) < 0) {
            // if tb - ta is less than h, reduce s even more:
            s *= (t1-t0)/h;
         }
         h = reduceStepSize (h, s, getEffectiveMaxStepSize());
         if (h < getMinStepSize()) {
            String msg =
               "adaptive step size fell below minimum of " + getMinStepSize();
            if (diagnostic != null) {
               msg += "; caused by " + diagnostic;
            }
            throw new NumericalException (msg);
         }
         h = TimeBase.round (h);
         System.out.println ("t0=" + t0 + ", < step " + h);
         return TimeBase.round (t0 + h);
      }

      protected void updateStepInfo (double s) {
         successCnt++;
         lasts = s;
         if (attemptingIncrease) {  
            attemptingIncrease = false;
            failedIncreaseCnt = 0;
         }
      }

      protected double reduceStepSize (double h, double s, double hmax) {
         double hr = h/hmax;
         double a = 0.9;
         if (s != 0 && s < a) {
            a = s;
         }
         if (use125Stepping) {
            hr = Round.down125 (a*hr);
         }
         else {
            hr = Round.downPow2 (a*hr);
         }
         return TimeBase.round (hr*hmax);
      }

      protected double increaseStepSize (double h, double hmax) {
         double hr = h/hmax;
         double a = 1.1;
         // ignore s for now ...
         if (use125Stepping) {
            hr = Round.up125 (a*hr);
         }
         else {
            hr = Round.upPow2 (a*hr);
         }
         return TimeBase.round (hr*hmax);
      }

      protected int getDStateSize() {
         return 2;
      }

      protected int getZStateSize() {
         return 2;
      }

      protected void getState (DataBuffer data) {
         data.zput (successCnt);
         data.zput (failedIncreaseCnt);
         data.dput (h);
         data.dput (lasts);
      }
      
      protected void setState (DataBuffer data) {
         successCnt = data.zget();
         failedIncreaseCnt = data.zget();
         h = data.dget();
         lasts = data.dget();
      }

   }

   private static boolean myFocusableP = true;

   public static void setFocusable (boolean focusable) {
      myFocusableP = focusable;
   }

   public static boolean isFocusable() {
      return myFocusableP;
   }

   /**
    * Returns a text string giving a short description of this model.
    * 
    * @return text description of this model

    */

   public String getAbout() {
      return null;
   }

   public static PropertyList myProps =
      new PropertyList (RootModel.class, RenderableModelBase.class);

   static {
      myProps.add (
         "viewerCenter", "viewer center of attention",
         DEFAULT_VIEWER_CENTER, "NW");
      myProps.add (
         "viewerEye", "viewer eye location",
         DEFAULT_VIEWER_EYE, "NW");
      myProps.add (
         "defaultViewOrientation", "default eye-to-world rotation transform",
         DEFAULT_VIEW_ORIENTATION);
      myProps.add (
         "minStepSize",
         "minimum allowed step size of adaptive stepping", DEFAULT_MIN_STEP_SIZE);
      myProps.add (
         "adaptiveStepping",
         "enables/disables adaptive step sizing", DEFAULT_ADAPTIVE_STEPPING);
      // remove and replace maxStepSize to redefine default value and range
      myProps.remove ("maxStepSize");
      myProps.add (
         "maxStepSize", "maximum step size for this component (seconds)",
         DEFAULT_MAX_STEP_SIZE, "(0,inf]");
   }

   @Override
   public void setDefaultValues() {
      super.setDefaultValues();
      myDefaultViewOrientation =
         new AxisAngle (DEFAULT_VIEW_ORIENTATION);
   }

   public boolean getAdaptiveStepping() {
      return myAdaptiveStepping;
   }
   
   public void setAdaptiveStepping (boolean enable) {
      myAdaptiveStepping = enable;
   }
   
   public double getMinStepSize() {
      return myMinStepSize;
   }
   
   public void setMinStepSize (double step) {
      myMinStepSize = step;
   }
   
   public void setMaxStepSize (double step) {
      if (step <= 0) {
         throw new IllegalArgumentException (
            "step size must be positive");
      }
      super.setMaxStepSize (step);
      componentChanged (new PropertyChangeEvent (this, "maxStepSize"));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Empty constructor, for creating a basic class instance
    */
   public RootModel() {
      this (null);
   }

   /**
    * Constructor used to build the model
    * @param name the name of the new model
    */
   public RootModel (String name) {
      super (name);
      myComponentListeners = new LinkedList<ComponentChangeListener>();
      myModels = new ComponentList<Model> (Model.class, "models", "m");
      myMonitors = new ComponentList<Monitor> (Monitor.class, "monitors", "mo");
      myControllers =
         new ComponentList<Controller> (Controller.class, "controllers", "c");
      myControlPanels =
         new ComponentList<ControlPanel> (
            ControlPanel.class, "controlPanels", "c");
      myInputProbes =
         new RenderableComponentList<Probe> (Probe.class, "inputProbes", "i");
      myOutputProbes =
         new RenderableComponentList<Probe> (Probe.class, "outputProbes", "o");
      myRenderables = new RenderableComponentList<RenderableComponent>(
         RenderableComponent.class, "renderables", "r");
      
      addFixed (myModels);
      addFixed (myControllers);
      addFixed (myMonitors);
      addFixed (myControlPanels);
      addFixed (myInputProbes);
      addFixed (myOutputProbes);
      addFixed (myRenderables);
      
      myWayPoints = new WayPointProbe (this);
      myWayPoints.setName ("WayPoints");
      myTraceSet = new LinkedHashSet<Tracable>();

      myModelInfo = new LinkedHashMap<Model,ModelInfo>();
      
      controlPanelsFrame = new JFrame (myName + ": Control panels");
      controlPanelTabs = new JTabbedPane();
      controlPanelsFrame.setContentPane (controlPanelTabs);
      myMaxStepSize = DEFAULT_MAX_STEP_SIZE;
   }

   public ComponentListView<Model> models() {
      return myModels;
   }

   public void addModel (Model model) {
      if (model == null) {
         throw new IllegalArgumentException ("model is null");
      }
      myModels.add (model);
   }

   public boolean removeModel (Model model) {
      return myModels.remove (model);
   }
   
   public void removeAllModels () {
      myModels.removeAll();
   }
 
   public void addMonitor (Monitor monitor) {
      if (monitor.getModel() != null) {
         if (!myModels.contains (monitor.getModel())) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      myMonitors.add (monitor);
   }

   public void addController (Controller controller) {
      if (controller.getModel() != null) {
         if (!myModels.contains (controller.getModel())) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      myControllers.add (controller);
   }

   public GLRenderableHolder addRenderable(GLRenderable renderable) {
      GLRenderableHolder holder = new GLRenderableHolder(renderable);
      addRenderable(holder);
      return holder;
   }
   
   public boolean removeRenderable(GLRenderable renderable) {
      for (RenderableComponent rc : myRenderables) {
         if (rc instanceof GLRenderableHolder) {
            GLRenderableHolder holder = (GLRenderableHolder)rc;
            if (renderable == holder.getRenderable()) {
               removeRenderable(holder);
               return true;
            }
         }
      }
      return false;
   }
   
   public void addRenderable (RenderableComponent comp) {
      if (comp == null) {
         return;
      }
      myRenderables.add(comp);
   }
   
   public boolean removeRenderable(RenderableComponent comp) {
      if (comp == null) {
         return false;
      }
      return myRenderables.remove(comp);
   }
   
   public void clearRenderables() {
      myRenderables.clear();
   }
   
   public RenderableComponentList<RenderableComponent> renderables() {
      return myRenderables;
   }
   
   public GLViewer getMainViewer() {
      return myMainViewer;
   }

   public void setMainViewer (GLViewer v) {
      myMainViewer = v;
   }

   public void setViewerCenter (Point3d c) {
      if (myMainViewer != null) {
         myMainViewer.setCenter (c);
      }
   }

   public Point3d getViewerCenter() {
      if (myMainViewer != null) {
         return myMainViewer.getCenter();
      }
      else {
         return new Point3d (DEFAULT_VIEWER_CENTER);
      }
   }

   public void setViewerEye (Point3d e) {
      if (myMainViewer != null) {
         myMainViewer.setEye (e);
      }
   }

   public Point3d getViewerEye() {
      if (myMainViewer != null) {
         return myMainViewer.getEye();
      }
      else {
         return new Point3d (DEFAULT_VIEWER_EYE);
      }
   }

   /**
    * Obtains the default orientation that is used for viewing this
    * model.
    * 
    * @return default rotational transform from eye to world coordinates
    */
   public AxisAngle getDefaultViewOrientation() {
      return myDefaultViewOrientation;
   }

   /**
    * Sets the default orientation that should be used for viewing
    * this model.
    * 
    * @param R rotational transform from eye to world coordinates
    */
   public void setDefaultViewOrientation (AxisAngle R) {
      if (!myDefaultViewOrientation.equals (R)) {
         myDefaultViewOrientation.set (R);
         componentChanged (
            new PropertyChangeEvent (this, "defaultViewOrientation"));
      }
   }

   public void addMonitor (Monitor monitor, Model model) {
      if (model != null) {
         if (!myModels.contains (model)) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      monitor.setModel (model);
      myMonitors.add (monitor);
   }

   public boolean removeMonitor (Monitor monitor) {
      return myMonitors.remove (monitor);
   }
   
   public void removeAllMonitors () {
      myMonitors.removeAll();
   }
   
   public ComponentListView<Monitor> getMonitors() {
      return myMonitors;
   }

   public void addController (Controller controller, Model model) {
      if (model != null) {
         if (!myModels.contains (model)) {
            throw new IllegalArgumentException (
               "Model not contained within RootModel");
         }
      }
      controller.setModel (model);
      myControllers.add (controller);
   }

   public boolean removeController (Controller controller) {
      return myControllers.remove (controller);
   }
   
   public void removeAllControllers () {
      myControllers.removeAll();
   }
   
   public ComponentListView<Controller> getControllers() {
      return myControllers;
   }

   public void addControlPanel (ControlPanel panel) {
      myControlPanels.add (panel);
      // panel.setSynchronizeObject (Main.getScheduler());
   }

   public void addControlPanel (ControlPanel panel, int idx) {
      myControlPanels.add (panel, idx);
      // panel.setSynchronizeObject (Main.getScheduler());
   }

   public boolean removeControlPanel (ControlPanel panel) {
      return myControlPanels.remove (panel);
   }

   public void removeAllControlPanels() {
      myControlPanels.removeAll();
   }

   public ComponentListView<ControlPanel> getControlPanels() {
      return myControlPanels;
   }

   public ControlPanel loadControlPanel (String filename) {
      ControlPanel panel = null;
      try {
         panel =
            (ControlPanel)ComponentUtils.loadComponent (
               new File (filename), this, ControlPanel.class);
      }
      catch (Exception e) {
         System.out.println (
            "Error reading control panel file "+filename+
            ", error="+ e.getMessage());
      }

      if (panel != null && panel.numWidgets() > 0) {
         //panel.pack();
         //panel.setVisible (true);
         addControlPanel (panel);
      }

      return panel;
   }

   public void addInputProbe (Probe probe) {
      if (!probe.isInput()) {
         throw new IllegalArgumentException ("probe is not an input probe");
      }
      myInputProbes.add (probe);
   }

   public void addInputProbe (Probe probe, int idx) {
      if (!probe.isInput()) {
         throw new IllegalArgumentException ("probe is not an input probe");
      }
      myInputProbes.add (probe, idx);
   }

   public boolean removeInputProbe (Probe probe) {
      return myInputProbes.remove (probe);
   }

   public void removeAllInputProbes() {
      myInputProbes.removeAll();
   }

   public ComponentList<Probe> getInputProbes() {
      return myInputProbes;
   }

   /**
    * Convenience routine to add a tracing probe to this RootModel. The probe is
    * created for a specified trace of a Tracable component. Start and stop
    * times are given in seconds. The probe's update interval is set to the
    * maximum step size of this RootModel, and the render interval is set to 50
    * msec.
    * 
    * @param tracable
    * component to be traced
    * @param traceName
    * name of the trace
    * @param startTime
    * start time (seconds)
    * @param stopTime
    * stop time (seconds)
    * @return created tracing probe
    */
   public TracingProbe addTracingProbe (
      Tracable tracable, String traceName, double startTime, double stopTime) {
      TracingProbe probe = tracable.getTracingProbe (traceName);
      probe.setStartTime (startTime);
      probe.setStopTime (stopTime);
      probe.setUpdateInterval (getMaxStepSize());
      probe.setRenderInterval (0.05);
      addOutputProbe (probe);
      return probe;
   }

   public void addOutputProbe (Probe probe) {
      if (probe.isInput()) {
         throw new IllegalArgumentException ("probe is an input probe");
      }
      myOutputProbes.add (probe);
   }

   public void addOutputProbe (Probe probe, int idx) {
      if (probe.isInput()) {
         throw new IllegalArgumentException ("probe is an input probe");
      }
      myOutputProbes.add (probe, idx);
   }

   public boolean removeOutputProbe (Probe probe) {
      return myOutputProbes.remove (probe);
   }

   public void removeAllOutputProbes() {
      myOutputProbes.removeAll();
   }

   public RenderableComponentList<Probe> getOutputProbes() {
      return myOutputProbes;
   }

   public boolean hasTracingProbes() {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            return true;
         }
      }
      return false;
   }

   public void setTracingProbesVisible (boolean visible) {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            RenderProps.setVisible ((TracingProbe)p, visible);
         }
      }
   }

   // WS
   public WayPointProbe getWayPoints() {
      return myWayPoints;
   }

   // WS
   public void addWayPoint (WayPoint way) {
      if (way.getTime() != 0) {
         myWayPoints.add (way);
         // set the state if we can. Don't bother if myRootInfo not set up yet
         if (way.getTime() == Main.getTime() && myRootInfo != null) {
            way.setState (this);
         }
         componentChanged (new StructureChangeEvent(myWayPoints));
      }
   }

   public WayPoint addWayPoint (double t) {
      if (t != 0) {
         WayPoint way = new WayPoint (t);
         addWayPoint (way);
         return way;
      }
      else {
         return null;
      }
   }

   public WayPoint addBreakPoint (double t) {
      if (t != 0) {
         WayPoint way = new WayPoint (t);
         way.setBreakPoint(true);
         addWayPoint (way);
         return way;
      }
      else {
         return null;
      }
   }

   // WS
   public boolean removeWayPoint (WayPoint way) {
      if (myWayPoints.remove (way)) {
         componentChanged (new StructureChangeEvent(myWayPoints));
         return true;
      }
      else {
         return false;
      }
   }

   // WS
   public WayPoint getWayPoint (double t) {
      return myWayPoints.get (t);
   }

   public void removeAllWayPoints() {
      myWayPoints.clear();
      componentChanged (new StructureChangeEvent(myWayPoints));
   }

   public TracingProbe getTracingProbe (Tracable tr, String propName) {
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            TracingProbe tp = (TracingProbe)p;
            if (tp.isTracing (tr, propName)) {
               return tp;
            }
         }
      }
      return null;
   }

   public void enableTracing (Tracable tr) {
      if (getTracingProbe (tr, "position") == null) {
         addTracingProbe (tr, "position", 0, 10);
         rerender();
      }
   }

   public boolean isTracing (Tracable tr) {
      return getTracingProbe (tr, "position") != null;
   }

   public boolean disableTracing (Tracable tr) {
      TracingProbe tp = getTracingProbe (tr, "position");
      if (tp != null) {
         removeOutputProbe (tp);
         rerender();
         return true;
      }
      else {
         return false;
      }
   }

   public void clearTracing (Tracable tr) {
      TracingProbe tp = getTracingProbe (tr, "position");
      if (tp != null) {
         tp.getNumericList().clear();
         tp.setData (tp.getStartTime());
         tp.setData (tp.getStopTime());
         rerender();
      }
   }

   public LinkedList<TracingProbe> getTracingProbes() {
      LinkedList<TracingProbe> tprobes = new LinkedList<TracingProbe>();
      for (Probe p : myOutputProbes) {
         if (p instanceof TracingProbe) {
            TracingProbe tp = (TracingProbe)p;
            if (tp.isTracing (null, "position") &&
                tp.getHost (0) instanceof Tracable) {
               tprobes.add (tp);
            }
         }
      }
      return tprobes;
   }

   public void disableAllTracing() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      for (TracingProbe tp : tprobes) {
         removeOutputProbe (tp);
      }
      rerender();
   }

   public void clearTraces() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      for (TracingProbe tp : tprobes) {
         tp.getNumericList().clear();
         tp.setData (tp.getStartTime());
         tp.setData (tp.getStopTime());
      }
      rerender();
   }

   public Collection<Tracable> getTraceSet() {
      Collection<TracingProbe> tprobes = getTracingProbes();
      ArrayList<Tracable> traceSet = new ArrayList<Tracable>();
      for (TracingProbe tp : tprobes) {
         traceSet.add ((Tracable)tp.getHost (0));
      }
      return traceSet;
   }

   public int getNumTracables() {
      return myTraceSet.size();
   }

   public void clear() {
      myControllers.removeAll();
      myModels.removeAll();
      myMonitors.removeAll();
      myControlPanels.removeAll();
      myInputProbes.removeAll();
      myOutputProbes.removeAll();
      myRenderables.removeAll();
   }

   // implementations for Renderable

   public void prerender (RenderList list) {
      for (Controller c : myControllers) {
         if (c instanceof Renderable) {
            list.addIfVisible ((Renderable)c);
         }
      }
      for (Model m : myModels) {
         if (m instanceof Renderable) {
            list.addIfVisible ((Renderable)m);
         }
      }
      for (Monitor m : myMonitors) {
         if (m instanceof Renderable) {
            list.addIfVisible ((Renderable)m);
         }
      }
      list.addIfVisible (myOutputProbes);
      list.addIfVisible (myInputProbes);
      list.addIfVisible (myRenderables);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      for (Model m : myModels) {
         if (m instanceof Renderable) {
            ((Renderable)m).updateBounds (pmin, pmax);
         }
      }
      myOutputProbes.updateBounds (pmin, pmax);
      myRenderables.updateBounds(pmin, pmax);
   }

   public void render (GLRenderer renderer, int flags) {
      // no actual rendering; all-subcomponents render themselves
      // after being assembled in the RenderList
   }

   public void rerender() {
      Main.getWorkspace().rerender();
   }

   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
      if (!myModelInfoValid) {
         updateModelInfo();
         myModelInfoValid = true;
      }
      for (Probe p : myInputProbes) {
         p.initialize(t);
      }
      for (Controller c : myControllers) {
         c.initialize(t);
      }
      for (Iterator<Model> it = myModels.iterator(); it.hasNext();) {
         it.next().initialize (t);
      }
      for (Monitor m : myMonitors) {
         m.initialize(t);
      }
      for (Probe p : myOutputProbes) {
         p.initialize(t);
      }
   }

   /**
    * Attach this root model to a driver program
    * 
    * @param driver
    * Interface giving access to the frame and viewer
    */
   public void attach (DriverInterface driver) {
   }

   /**
    * Detach this root model from a driver program.
    */
   public void detach (DriverInterface driver) {
   }

   public void addComponentChangeListener (ComponentChangeListener l) {
      myComponentListeners.add (l);
   }

   public boolean removeComponentChangeListener (ComponentChangeListener l) {
      return myComponentListeners.remove (l);
   }

   private void fireComponentChangeListeners (ComponentChangeEvent e) {
      // clone the listener list in case some of the listeners
      // want to remove themselves from the list
      @SuppressWarnings("unchecked")
      LinkedList<ComponentChangeListener> listeners =
         (LinkedList<ComponentChangeListener>)myComponentListeners.clone();

      for (ComponentChangeListener l : listeners) {
         l.componentChanged (e);
      }
   }

   public void componentChanged (ComponentChangeEvent e) {
      // no need to notify parent since there is none
      if (e.getCode() == ComponentChangeEvent.Code.STRUCTURE_CHANGED) {
         synchronized (this) {
            myModelInfoValid = false;
         }
      }
      // if called in constructor, myControlPanels might still be null ...
      if (myControlPanels != null) {
         for (ControlPanel panel : myControlPanels) {
            panel.removeStalePropertyWidgets();
         }
         fireComponentChangeListeners (e);
      }
   }

   public void notifyStructureChanged (Object comp) {
      synchronized (this) {
         myModelInfoValid = false;
      }
      super.notifyStructureChanged (comp);
   }

   private ModelInfo getModelInfo (ModelAgent agent)  {

      ModelInfo info = null;
      Model model = agent.getModel();
      if (model != null) {
         info = myModelInfo.get (model);
      }
      if (info == null) {
         info = myRootInfo;
      }
      return info;
   }

   private void updateModelInfo() {

      myRootInfo = new ModelInfo (this);
      // rebuild modelinfo, removing info for deleted models
      // and adding info for new models.
      LinkedHashMap<Model,ModelInfo> newinfo =
         new LinkedHashMap<Model,ModelInfo>();
      for (int i = 0; i < myModels.size(); i++) {
         // add model info for any new models
         Model model = myModels.get(i);
         ModelInfo info = myModelInfo.get (model);
         if (info != null) {
            info.clear();
         }
         else {
            info = new ModelInfo(model);
         }
         newinfo.put (model, info);
      }
      myModelInfo = newinfo;

      myRootInfo.clear();
      for (int i = 0; i < myMonitors.size(); i++) {
         Monitor mon = myMonitors.get(i);
         ModelInfo info = getModelInfo (mon);
         info.monitors.add (mon);
      }
      for (int i = 0; i < myControllers.size(); i++) {
         Controller ctl = myControllers.get(i);
         ModelInfo info = getModelInfo (ctl);
         info.controllers.add (ctl);
      }
      for (int i = 0; i < myInputProbes.size(); i++) {
         Probe p = myInputProbes.get(i);         
         ModelInfo info = getModelInfo (p);
         info.inputProbes.add (p);
      }
      for (int i = 0; i < myOutputProbes.size(); i++) {
         Probe p = myOutputProbes.get(i);
         ModelInfo info = getModelInfo (p);
         info.outputProbes.add (p);
      }
      for (ModelInfo info : myModelInfo.values()) {
         info.createState ();
      }
      myRootInfo.createState();
      myRootInfo.outputProbes.add (myWayPoints);
   }
   
   public boolean hasState() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public ComponentState createState(ComponentState prevState) {
      // state is a composite state for every model plus a numeric state
      // for the root model itself
      int numMods = myModelInfo.size();
      CompositeState state = new CompositeState(numMods+1);
      for (ModelInfo info : myModelInfo.values()) {
         state.addState (info.createFullState());
      }
      state.addState (myRootInfo.createFullState());
      return state;
   }

   protected ComponentState createRootState() {
      // Create local state to store adaptive integration info for all
      // the models
      int numMods = myModelInfo.size();
      int dsize = numMods*myRootInfo.getDStateSize();
      int zsize = numMods*myRootInfo.getZStateSize();
      return new NumericState(dsize, zsize);
   }

   /**
    * {@inheritDoc}
    */
   public void setState (ComponentState state) {
      if (!(state instanceof CompositeState)) {
         throw new IllegalArgumentException ("state is not a CompositeState");
      }
      CompositeState newState = (CompositeState)state;
      if (newState.numSubStates() != myModels.size()+1) {
         throw new IllegalArgumentException (
            "new state has "+newState.numSubStates()+
            " sub-states vs. "+myModels.size()+1);
      }
      int k = 0;
      for (ModelInfo info : myModelInfo.values()) {
         info.setFullState ((CompositeState)newState.getState(k++));
      }
      myRootInfo.setFullState ((CompositeState)newState.getState(k++));      
   }
   
   protected void setRootState (ComponentState state) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException ("state is not a NumericState");
      }
      // Set local state, including adaptive integration info for all models
      NumericState rootState = (NumericState)state;
      rootState.resetOffsets();
      for (ModelInfo info : myModelInfo.values()) {
         info.setState (rootState);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getState (ComponentState state) {
      if (!(state instanceof CompositeState)) {
         throw new IllegalArgumentException ("state is not a CompositeState");
      }
      CompositeState substate;
      CompositeState saveState = (CompositeState)state;
      saveState.clear();
      for (ModelInfo info : myModelInfo.values()) {
         substate = new CompositeState();
         info.getFullState (substate);
         saveState.addState (substate);
      }
      substate = new CompositeState();
      myRootInfo.getFullState (substate);
      saveState.addState (substate);
   }
   
   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {

      if (!(newstate instanceof CompositeState)) {
         throw new IllegalArgumentException (
            "newstate is not a CompositeState");
      }
      CompositeState saveState = (CompositeState)newstate;
      CompositeState substate;

      HashMap<ModelInfo,CompositeState> stateMap =
         new HashMap<ModelInfo,CompositeState>();

      if (oldstate != null) {
         if (!(oldstate instanceof CompositeState)) {
            throw new IllegalArgumentException (
               "oldstate is not a CompositeState");
         }
         CompositeState ostate = (CompositeState)oldstate;
         if (ostate.numComponents() != ostate.numSubStates()) {
            throw new IllegalArgumentException (
               "oldstate has "+ostate.numComponents()+" vs. "+
               ostate.numSubStates()+" substates");
         }
         ArrayList<Object> comps = ostate.getComponents();
         if (comps != null) {
            for (int k=0; k<comps.size(); k++) {
               stateMap.put ((ModelInfo)comps.get(k),
                             (CompositeState)ostate.getState(k));
            }
         }
      }
      saveState.clear();
       for (ModelInfo info : myModelInfo.values()) {
         substate = new CompositeState();
         info.getInitialState (substate, stateMap.get(info));
         saveState.addState (substate);
         saveState.addComponent (info);
      }
      substate = new CompositeState();
      myRootInfo.getInitialState (substate, stateMap.get(myRootInfo));
      saveState.addState (substate);
      saveState.addComponent (myRootInfo);
   }

   protected void getRootState (ComponentState state) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException ("state is not a NumericState");
      }
      // Get local state, including adaptive integration info for all models
      NumericState rootState = (NumericState)state;
      rootState.resetOffsets();      
      for (ModelInfo info : myModelInfo.values()) {
         info.getState (rootState);
      }
   }

   public StepAdjustment advance (
      double t0, double t1, int flags) {

      synchronized (this) {
         if (!myModelInfoValid) {
            updateModelInfo();
            myModelInfoValid = true;
         }
      }
      doadvance (t0, t1, flags);
      return null;
   }

   public synchronized void applyInputProbes (List<Probe> list, double t) {
      for (Probe p : list) {
         if (p.isActive() && 
             TimeBase.compare (p.getStartTime(), t) <= 0 && 
             TimeBase.compare (p.getStopTime(), t) >= 0) {
            p.apply (t);
         }
      }
   }

   public synchronized void applyControllers (
      List<Controller> list, double t0, double t1) {

      for (Controller c : list) {
         c.apply (t0, t1);
      }
   }

   public synchronized void applyMonitors (
      List<Monitor> list, double t0, double t1) {

      for (Monitor m : list) {
         m.apply (t0, t1);
      }
   }

   public synchronized void applyOutputProbes (
      List<Probe> list, double t1, ModelInfo info) {

      // see if t1 coincides with the model's max step size
      double maxStep = info.model.getMaxStepSize();
      boolean coincidesWithStep =
         (maxStep != -1 && TimeBase.modulo (t1, maxStep) == 0);  

      for (Probe p : list) {
         if (p.isActive() && (p.isEventTime(t1) || 
              (coincidesWithStep && p.getUpdateInterval() < 0))) {
            p.apply (t1);
         }
      }
   }

   private double nextProbeEvent (List<Probe> probes, double t0) {
      double te = Double.MAX_VALUE;
      for (Probe p : probes) {
         
         double ta = p.nextEventTime (t0);
         if (ta != -1 && ta < te) {
            te = ta;
         }
      }
      return te;
   }

   public double getNextAdvanceTime (
      List<Probe> probes, double stepSize, double t0, double t1) {
      
      // nextStepTime is the next time after t0 lying on a step boundary
      double nextStepTime = t0 + (stepSize-TimeBase.modulo(t0,stepSize));
      if (TimeBase.compare (nextStepTime, t1) < 0) {
         t1 = nextStepTime;
      }
      double te = nextProbeEvent (probes, t0);
      if (TimeBase.compare (te, t1) < 0) {
         t1 = te;
      }
      return TimeBase.round (t1);
   }

   private double getRecommendedScaling (StepAdjustment adj) {
      return adj != null ? adj.getScaling() : 1;
   }
   
   /** 
    * This is used by the scheduler to interrupts the current call to advance
    * and cause state to be restored to that of the start time for the advance.
    */
   public synchronized void requestStop() {
      myStopRequested = true;
   }

   protected void advanceModel (
      ModelInfo info, double t0, double t1, int flags) {

      double ta = t0;
      if (t0 == 0) {
         applyOutputProbes (info.outputProbes, t0, info);
      }
      while (ta < t1) {
         double s;
         synchronized (this) {
            info.getModelAndControllersState (info.state);
         }
         if (testSaveAndRestoreState) {  
            // test save-and-restore of model state 
            CompositeState fullState = info.createFullState();
            CompositeState testState = info.createFullState();
            info.getFullState (fullState);
            info.setFullState (fullState);
            info.getFullState (testState);
            if (!testState.equals (fullState)) {
               throw new InternalErrorException (
                  "Error: save/restore state test failed");
            }  
         }
         double tb = info.getNextAdvanceTime (ta, t1);
         do {
            synchronized (this) {
               StepAdjustment adj;
               //info.model.setDefaultInputs (ta, tb);
               adj = info.model.preadvance (ta, tb, flags);
               s = getRecommendedScaling (adj);
               if (s >= 1) {
                  applyInputProbes (info.inputProbes, tb);
                  applyControllers (info.controllers, ta, tb);
                  adj = info.model.advance (ta, tb, flags);
                  s = getRecommendedScaling (adj);
               }
               if (myAdaptiveStepping && s < 1) {
                  tb = info.reduceAdvanceTime (
                     s, ta, tb, adj.getMessage());
                  info.setModelAndControllersState (info.state);
                  info.model.initialize (ta);
               }
            }
         }
         while (myAdaptiveStepping && s < 1 && !myStopRequested);
         if (!(myAdaptiveStepping && s < 1)) {
            // then we have advanced to tb:
            info.updateStepInfo (s);
            applyMonitors (info.monitors, ta, tb);
            applyOutputProbes (info.outputProbes, tb, info);
            ta = tb;
         }
      }
   }

   protected void doadvance (double t0, double t1, int flags) {
      
      double ta = t0;
      if (t0 == 0) {
         applyOutputProbes (myRootInfo.outputProbes, t0, myRootInfo);
      }
      while (ta < t1) {
         double tb = getNextAdvanceTime (
            myRootInfo.outputProbes, getMaxStepSize(), ta, t1);
         //setDefaultInputs (ta, tb);
         applyInputProbes (myRootInfo.inputProbes, tb);
         applyControllers (myRootInfo.controllers, ta, tb);
         for (Model m : myModels) {
            advanceModel (myModelInfo.get(m), ta, tb, flags);
         }
         applyMonitors (myRootInfo.monitors, ta, tb);
         applyOutputProbes (myRootInfo.outputProbes, tb, myRootInfo);
         ta = tb;
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      // XXX - may not want to write the working directory to the model file
      // should be changed to "probe directory"
      pw.println ("workingDir="
                  + Write.getQuotedString (ArtisynthPath.getWorkingDirPath()));
      super.writeItems (pw, fmt, ancestor);
      // XXX - write way points, may want to have special purpose routine to
      // write
      // probes and waypoints. Probe file should have same format as model file
      // see Workspace.writeProbes()
      pw.println ("waypoints=");
      myWayPoints.write (pw, fmt, this);
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "workingDir")) {
         String dirName = rtok.scanQuotedString('"');
         File workingDir = new File (dirName);
         if (workingDir.exists() && workingDir.isDirectory()) {
            ArtisynthPath.setWorkingDir (workingDir);
         }
         return true;
      }
      else if (scanAttributeName (rtok, "waypoints")) {
         tokens.add (new StringToken ("waypoints", rtok.lineno()));
         myWayPoints.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (ScanWriteUtils.postscanAttributeName (tokens, "waypoints")) {
         myWayPoints.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /**
    * {@inheritDoc}
    */
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      for (ControlPanel cp : myControlPanels) {
         cp.pack();
         cp.setVisible (true);
      }
   }

   public void dispose() {
      for (Model m : myModels) {
         m.dispose();
      }
      for (Controller c : myControllers) {
         c.dispose();
      }
      for (Monitor m : myMonitors) {
         m.dispose();
      }
      // This dispose code not needed since dispose will now be called
      // be the control panel remove handler
      // for (ControlPanel cp : myControlPanels) {
      //    cp.dispose();
      // }
      myControlPanels.removeAll();
      
      controlPanelsFrame.setVisible (false);
      controlPanelTabs.removeAll ();
   }

   public void setWaypointChecking (boolean enable) {
      myWayPoints.setCheckState (enable);
   }
   
   public boolean getWaypointChecking () {
      return myWayPoints.getCheckState();
   }
   
   // This check stuff is for checking repeatability by comparing
   // data with that computed on a previous run

   private BufferedReader myCheckReader = null;
   private PrintWriter myCheckWriter = null;
   private boolean myCheckEnable = false;

   public boolean isCheckEnabled() {
      return myCheckEnable;
   }

   public void setCheckEnabled (boolean enable) {
      myCheckEnable = enable;
   }

   private boolean openCheckFiles() {
      try {
         String rootName = getName();
         File file = new File (rootName + "_state.txt");
         if (file.exists()) {
            myCheckReader = new BufferedReader (new FileReader (file));
            System.out.println ("CHECK STATE BEGIN, checking");
         }
         else {
            myCheckWriter =
               new PrintWriter (new BufferedWriter (new FileWriter (file)));
            System.out.println ("CHECK STATE BEGIN, writing");
         }
      }
      catch (Exception e) {
         System.out.println ("Check error: " + e.getMessage());
         return false;
      }
      return true;
   }

   public void checkWrite (String str) {
      if (!myCheckEnable) {
         return;
      }
      if (myCheckReader == null && myCheckWriter == null) {
         if (!openCheckFiles()) {
            myCheckEnable = false;
            return;
         }
      }
      try {
         if (myCheckWriter != null) {
            myCheckWriter.println (str);
            myCheckWriter.flush();
         }
         else {
            String check = myCheckReader.readLine();
            if (check == null) {
               System.out.println ("CHECK FINISHED, time "+Main.getTime());
               myCheckEnable = false;
               return;
            }
            else if (!check.equals (str)) {
               System.out.println ("CHECK FAILED, time "+Main.getTime());
               System.out.println ("original:");
               System.out.println (check);
               System.out.println ("current:");
               System.out.println (str);
               myCheckEnable = false;
            }
         }
      }
      catch (Exception e) {
         System.out.println ("Check error: " + e.getMessage());
         myCheckEnable = false;
      }
   }

   public void mergeAllControlPanels (boolean combine) {
      if (!myControlPanels.isEmpty ()) {         
         for (ControlPanel panel : myControlPanels) {
            Container contentPane = panel.getFrame ().getContentPane ();
            
            if (combine) {
               controlPanelTabs.addTab (panel.getFrame().getTitle(), contentPane);
            }
            else {
               panel.getFrame ().setContentPane (contentPane);
            }
            
            panel.setVisible (!combine);
         }
         
         controlPanelsFrame.pack ();
         
         if (controlPanelTabs.getTabCount () == 0) {
            controlPanelsFrame.setVisible (false);
         }
         else if (!controlPanelsFrame.isVisible ()) {
            Point loc = Main.getMainFrame ().getLocation();
            controlPanelsFrame.setLocation (
               loc.x + Main.getMainFrame ().getWidth(), loc.y);
            controlPanelsFrame.setVisible (true);
         }
      }
   }
   
   public JTabbedPane getControlPanelTabs() {
      return controlPanelTabs;
   }

   public void mergeControlPanel (boolean combine, ControlPanel panel) {
      Container contentPane = panel.getFrame ().getContentPane ();
      
      if (combine) {
         controlPanelTabs.addTab (panel.getFrame().getTitle(), contentPane);
      }
      else {
         panel.getFrame ().setContentPane (contentPane);
      }
      
      panel.setVisible (!combine);
      
      controlPanelsFrame.pack ();
      
      if (controlPanelTabs.getTabCount () == 0) {
         controlPanelsFrame.setVisible (false);
      }
      else if (!controlPanelsFrame.isVisible ()) {
         Point loc = Main.getMainFrame ().getLocation();
         controlPanelsFrame.setLocation (
            loc.x + Main.getMainFrame ().getWidth(), loc.y);
         controlPanelsFrame.setVisible (true);
      }
   }

   /**
    * Convenience method for creating menu items to be placed under the "Model"
    * menu. Creates and returns a JMenuItem with name and command text as
    * specified by <code>cmd</code>, this RootModel as an ActionListener, and
    * optional tool-tip text as specified by <code>toolTip</code>.
    */
   public JMenuItem makeMenuItem (String cmd, String toolTip) {
      JMenuItem item = new JMenuItem(cmd);
      item.addActionListener(this);
      item.setActionCommand(cmd);
      if (toolTip != null && !toolTip.equals ("")) {
         item.setToolTipText (toolTip);
      }
      return item;
   }

   /**
    * By default, this method does nothing. Subclasses should override
    * this to handle events, such as those generated from menu items
    * such those generated by {@link #getModelMenuItems}.
    */
   public void actionPerformed(ActionEvent event) {
   }   

   /**
    * Can be overriden by a subclass to generate a list of objects to be
    * displayed under a "Model" menu in the main ArtiSynth frame. If this
    * method returns <code>null</code> (the default behavior), that is taken to
    * indicate that this RootModel has no application menu items and no "Model"
    * menu should be created.  Otherwise, the method should return an array of
    * Objects, which should be items capable of being added to a JMenu,
    * including Component, JMenuItem, and String.
    *
    * @see #makeMenuItem
    * @return a list of components to be added to the ArtiSynth "Model" menu,
    * or <code>null</code> if no such menu is to be created.
    */
   public Object[] getModelMenuItems() {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void notifyParentOfChange (ComponentChangeEvent e) {
      // no parent, so just call component change:
      componentChanged (e);
   }

   /**
    * Find the most immediate RootModel, if any, that is an ancestor of a 
    * specified component.
    *
    * @param comp component to seek RootModel for
    * @return Most immediate RootModel ancestor, or <code>null</code>
    */
   public static RootModel getRoot (ModelComponent comp) {
      while (comp != null) {
         if (comp instanceof RootModel) {
            return (RootModel)comp;
         }
         comp = comp.getParent();
      }
      return null;      
   }

   /**
    * Returns true if a specified component has a RootModel as an ancestor.
    *
    * @return true if <code>comp</code> has a RootModel as an ancestor.
    */
   public static boolean hasRoot (ModelComponent comp) {
      return getRoot (comp) != null;
   }

   
   
}
package artisynth.demos.fem;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.JFrame;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.matrix.*;

public class SingleShellTri extends RootModel {
   ShellFemModel3d m_femShellModel;
   MechModel m_mechModel;

   FemNode3d m_node0;
   FemNode3d m_node1;
   FemNode3d m_node2;
   FemNode3d m_node3;

   public void build (String[] args) {
      m_femShellModel = new ShellFemModel3d();
      
//      m_node0 = new FemNode3d (-1, -1, 1);
//      m_node1 = new FemNode3d (-1, 0, -1);
//      m_node2 = new FemNode3d (-1, 1, 1);
//      m_node3 = new FemNode3d (1, 0, 0);

//      QuadTetShellElement tetShell = new QuadTetShellElement (m_node0, m_node1, m_node2, m_node3);
//
//      m_femShellModel.addNode (m_node0);
//      m_femShellModel.addNode (m_node1);
//      m_femShellModel.addNode (m_node2);
//      m_femShellModel.addNode (m_node3);
      
//      m_femShellModel.addElement (tetShell);
//      
//      m_node0.setDynamic (false);
//      m_node1.setDynamic (false);
//      m_node2.setDynamic (false);
      
      m_node0 = new FemNode3d (0, 0, 0);
      m_node1 = new FemNode3d (1, 0, 0);
      m_node2 = new FemNode3d (1, 1, 0);
      ShellTriElement triShell = new ShellTriElement(m_node0, m_node1, m_node2);
      m_femShellModel.addNode (m_node0);
      m_femShellModel.addNode (m_node1);
      m_femShellModel.addNode (m_node2);
      m_femShellModel.addElement (triShell);

      m_femShellModel.setSurfaceRendering (SurfaceRender.Shaded);

      RenderProps.setFaceColor (m_femShellModel, Color.PINK);
      RenderProps.setShininess (m_femShellModel, m_femShellModel.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (m_femShellModel, true);
      RenderProps.setFaceStyle (m_femShellModel, Renderer.FaceStyle.FRONT);

      MooneyRivlinMaterial monMat = new MooneyRivlinMaterial();
      monMat.setBulkModulus (15000000);
      monMat.setC10 (150000);
      monMat.setJLimit (0.2);
      QLVBehavior qlv = new QLVBehavior();
      qlv.setTau (0.1, 0.0, 0, 0, 0, 0);
      qlv.setGamma (4.0, 0, 0, 0, 0, 0);
      monMat.setViscoBehavior (qlv);

      m_femShellModel.setMaterial (monMat);

      m_mechModel = new MechModel ("mech");
      m_mechModel.addModel (m_femShellModel);
      addModel (m_mechModel);

      RenderProps.setPointStyle (m_femShellModel.getNodes(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (m_femShellModel.getNodes(), 0.05);

      m_femShellModel.setGravity (0, 0, 0);
      m_femShellModel.setDensity (10000);
      m_femShellModel.setParticleDamping (0);

      m_mechModel.setProfiling (true);
      
//      int i = 0;
//      for (FemNode3d node : femMod.getNodes()) {
//         node.setIndex(i);
//         i++;
//      }
//      
//      for (FemNode3d node : femMod.getNodes()) {
//         System.out.println ("Node #" + node.getIndex ());
//         System.out.println (node.myDirector0);
//         System.out.println ("Neighs: ");
//         for (FemNodeNeighbor neigh : node.getNodeNeighbors()) {
//            System.out.print (" " + neigh.getNode ().getIndex ());
//         }
//      }
   }   
}
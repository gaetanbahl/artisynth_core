package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.render.*;
import maspack.matrix.*;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class FemBeamWithBlock extends FemBeam {

   public void build (String[] args) throws IOException {

      super.build (args);

      // Create a rigid block and move to the side of FEM
      RigidBody block = RigidBody.createBox (
         "block", width/2, 1.2*width, 1.2*width, 2*density);
      mech.addRigidBody (block);
      block.setPose (new RigidTransform3d (length/2+width/4, 0, 0));
      
      // Attach right-side nodes to rigid block
      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().x >= length/2-EPS) {
            mech.addAttachment (new PointFrameAttachment (block, node));
         }
      }
   }

}

package artisynth.core.femmodels;

import artisynth.core.femmodels.ShellIntegrationPoint3d.NODE_POS;
import maspack.matrix.Matrix3d;
import maspack.util.InternalErrorException;

/**
 * Base class for a shell element. Compared to traditional elements, 
 * shell elements are thin elements that can better model surfaces.
 * Examples include water surfaces, clothing, and aluminium sheet.
 * 
 * The shell logic is mainly found in ShellIntegrationPoint3d,
 * ShellFemModel3d, ShellNodeNeighbor, and FemUtilties.
 */
public abstract class ShellFemElement3d extends FemElement3d {
   
   /* Default thickness. Stability problems can occur if raised too high. */
   protected double myShellThickness = 0.01; 
   
   @Override
   public abstract ShellIntegrationPoint3d[] getIntegrationPoints();
   
   @Override
   public abstract ShellIntegrationPoint3d getWarpingPoint();
   
   @Override
   public abstract ShellIntegrationData3d[] getIntegrationData();
   
   @Override
   public abstract ShellIntegrationData3d getWarpingData();
   
   @Override
   public double computeVolumes () {
      return _computeVolume (/* isRest= */false);
   }

   @Override
   public double computeRestVolumes () {
      return _computeVolume (/* isRest= */true);
   }
   
   public double _computeVolume (boolean isRest) {
      double vol = 0;

      // For each integration point...
      ShellIntegrationPoint3d[] iPts = getIntegrationPoints ();
      for (int i = 0; i < iPts.length; i++) {
         ShellIntegrationPoint3d iPt = iPts[i];
         if (isRest) {
            iPt.computeJacobian(NODE_POS.REST);
         }
         else {
            iPt.computeJacobian(NODE_POS.CURRENT);
         }
         Matrix3d J = iPt.getJ();
         vol += J.determinant () * iPt.myWeight;
      }

      return vol;
   }
   
   public double getShellThickness() {
      return myShellThickness;
   }
   
   public void setShellThickness(double newThickness) {
      myShellThickness = newThickness;
   }
   
   @Override
   protected ShellIntegrationPoint3d[] createIntegrationPoints (
      double[] integCoords) {
      int numi = integCoords.length/4;
      ShellIntegrationPoint3d[] pnts = new ShellIntegrationPoint3d[numi];
      if (integCoords.length != 4*numi) {
         throw new InternalErrorException (
            "Coordinate data length is "+integCoords.length+","
            + " expecting "+4*numi);
      }
      for (int k=0; k<numi; k++) {
         pnts[k] = ShellIntegrationPoint3d.create (
            this, integCoords[k*4], integCoords[k*4+1], integCoords[k*4+2],
            integCoords[k*4+3]);
         pnts[k].setNumber (k);
      }
      return pnts;
   }
}

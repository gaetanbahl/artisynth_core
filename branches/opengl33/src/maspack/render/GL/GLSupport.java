/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import jogamp.opengl.glu.error.Error;
import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix2dBase;
import maspack.matrix.Vector2d;
import maspack.util.BufferUtilities;

public class GLSupport {
   
   // sizes of elements in bytes (to circumvent Java 7/8 problems)
   public static final int BYTE_SIZE = 1;
   public static final int SHORT_SIZE = 2;
   public static final int INTEGER_SIZE = 4;
   public static final int FLOAT_SIZE = 4;
   
   /**
    * Java 8 version of float hashcode
    * @param f
    * @return the computed hashcode
    */
   public static int hashCode(float f) {
      return Float.floatToIntBits (f);
   }
   
   /**
    * Java 8 version of double hashcode
    * @param d
    * @return the computed hashcode
    */
   // NOT USED
   public static int hashCode(double d) {
      long bits = Double.doubleToLongBits(d);
      return (int)(bits ^ (bits >>> 32));
   }
   
   /**
    * Converts a 2D affine transform to a 4D matrix expected by opengl
    * @param mat
    * @param T
    */
   public static void transformToGLMatrix (double[] mat, AffineTransform2dBase T) {
      Matrix2dBase M = T.getMatrix ();
      Vector2d p = T.getOffset ();
      
      mat[0] = M.m00;
      mat[1] = M.m10;
      mat[2] = 0;
      mat[3] = 0;

      mat[4] = M.m01;
      mat[5] = M.m11;
      mat[6] = 01;
      mat[7] = 0;

      mat[8] = 0;
      mat[9] = 0;
      mat[10] = 0;
      mat[11] = 0;

      mat[12] = p.x;
      mat[13] = p.y;
      mat[14] = 0;
      mat[15] = 1;
   }
   
   public static void transformToGLMatrix (double[] mat, Matrix T) {
      int nr = T.rowSize ();
      int nc = T.colSize ();
      int idx = 0;
      for (int c=0; c<nc; ++c) {
         for (int r=0; r<nr; ++r) {
            mat[idx++] = T.get (r, c);
         }
      }
   }

//   public static void GLMatrixToTransform (DenseMatrix T, double[] mat) {
//      T.set (0, 0, mat[0]);
//      T.set (1, 0, mat[1]);
//      T.set (2, 0, mat[2]);
//      T.set (3, 0, mat[3]);
//
//      T.set (0, 1, mat[4]);
//      T.set (1, 1, mat[5]);
//      T.set (2, 1, mat[6]);
//      T.set (3, 1, mat[7]);
//
//      T.set (0, 2, mat[8]);
//      T.set (1, 2, mat[9]);
//      T.set (2, 2, mat[10]);
//      T.set (3, 2, mat[11]);
//
//      T.set (0, 3, mat[12]);
//      T.set (1, 3, mat[13]);
//      T.set (2, 3, mat[14]);
//      T.set (3, 3, mat[15]);
//   }
//
//   public static void transformToGLMatrix (double[] mat, AffineTransform3d T) {
//      mat[0] = T.A.m00;
//      mat[1] = T.A.m10;
//      mat[2] = T.A.m20;
//      mat[3] = 0;
//
//      mat[4] = T.A.m01;
//      mat[5] = T.A.m11;
//      mat[6] = T.A.m21;
//      mat[7] = 0;
//
//      mat[8] = T.A.m02;
//      mat[9] = T.A.m12;
//      mat[10] = T.A.m22;
//      mat[11] = 0;
//
//      mat[12] = T.p.x;
//      mat[13] = T.p.y;
//      mat[14] = T.p.z;
//      mat[15] = 1;
//   }
//   
//   public static void transformToGLMatrix (double[] mat, int offset, AffineTransform3dBase T) {
//      Matrix3dBase M = T.getMatrix();
//      Vector3d b = T.getOffset();
//      int idx = offset;
//      mat[idx++] = M.m00;
//      mat[idx++] = M.m10;
//      mat[idx++] = M.m20;
//      mat[idx++] = 0;
//
//      mat[idx++] = M.m01;
//      mat[idx++] = M.m11;
//      mat[idx++] = M.m21;
//      mat[idx++] = 0;
//
//      mat[idx++] = M.m02;
//      mat[idx++] = M.m12;
//      mat[idx++] = M.m22;
//      mat[idx++] = 0;
//
//      mat[idx++] = b.x;
//      mat[idx++] = b.y;
//      mat[idx++] = b.z;
//      mat[idx++] = 1;
//   }
   
//   public static void transformToGLMatrix (float[] mat, int offset, AffineTransform3dBase T) {
//      Matrix3dBase M = T.getMatrix();
//      Vector3d b = T.getOffset();
//      int idx = offset;
//      mat[idx++] = (float)M.m00;
//      mat[idx++] = (float)M.m10;
//      mat[idx++] = (float)M.m20;
//      mat[idx++] = 0f;
//
//      mat[idx++] = (float)M.m01;
//      mat[idx++] = (float)M.m11;
//      mat[idx++] = (float)M.m21;
//      mat[idx++] = 0f;
//
//      mat[idx++] = (float)M.m02;
//      mat[idx++] = (float)M.m12;
//      mat[idx++] = (float)M.m22;
//      mat[idx++] = 0f;
//
//      mat[idx++] = (float)b.x;
//      mat[idx++] = (float)b.y;
//      mat[idx++] = (float)b.z;
//      mat[idx++] = 1f;
//   }
//   
//   public static void transformToGLMatrixTranspose (float[] mat, int offset, AffineTransform3dBase T) {
//      Matrix3dBase M = T.getMatrix();
//      Vector3d b = T.getOffset();
//      int idx = offset;
//      mat[idx++] = (float)M.m00;
//      mat[idx++] = (float)M.m01;
//      mat[idx++] = (float)M.m02;
//      mat[idx++] = (float)b.x;
//
//      mat[idx++] = (float)M.m10;
//      mat[idx++] = (float)M.m11;
//      mat[idx++] = (float)M.m12;
//      mat[idx++] = (float)b.y;
//
//      mat[idx++] = (float)M.m20;
//      mat[idx++] = (float)M.m21;
//      mat[idx++] = (float)M.m22;
//      mat[idx++] = (float)b.z;
//
//      mat[idx++] = 0f;
//      mat[idx++] = 0f;
//      mat[idx++] = 0f;
//      mat[idx++] = 1f;
//   }
   
//   public static void transformToGLMatrixTranspose (double[] mat, int offset, AffineTransform3dBase T) {
//      Matrix3dBase M = T.getMatrix();
//      Vector3d b = T.getOffset();
//      int idx = offset;
//      mat[idx++] = M.m00;
//      mat[idx++] = M.m01;
//      mat[idx++] = M.m02;
//      mat[idx++] = b.x;
//
//      mat[idx++] = M.m10;
//      mat[idx++] = M.m11;
//      mat[idx++] = M.m12;
//      mat[idx++] = b.y;
//
//      mat[idx++] = M.m20;
//      mat[idx++] = M.m21;
//      mat[idx++] = M.m22;
//      mat[idx++] = b.z;
//
//      mat[idx++] = 0.0;
//      mat[idx++] = 0.0;
//      mat[idx++] = 0.0;
//      mat[idx++] = 1.0;
//   }

//   public static void GLMatrixToTransform (AffineTransform3d T, double[] mat) {
//      T.A.m00 = mat[0];
//      T.A.m10 = mat[1];
//      T.A.m20 = mat[2];
//
//      T.A.m01 = mat[4];
//      T.A.m11 = mat[5];
//      T.A.m21 = mat[6];
//
//      T.A.m02 = mat[8];
//      T.A.m12 = mat[9];
//      T.A.m22 = mat[10];
//
//      T.p.x = mat[12];
//      T.p.y = mat[13];
//      T.p.z = mat[14];
//   }
   
   public static BufferedImage downloadTexture(GL2GL3 gl, int target) {
      int[] v = new int[2];
      gl.glGetTexLevelParameteriv (target, 0, GL2GL3.GL_TEXTURE_WIDTH, v, 0);
      gl.glGetTexLevelParameteriv (target, 0, GL2GL3.GL_TEXTURE_HEIGHT, v, 1);
      
      if (v[0]*v[1] == 0) {
         return null;
      }
      
      ByteBuffer buff = BufferUtilities.newNativeByteBuffer (v[0]*v[1]*4);
      gl.glGetTexImage (target, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buff);
      
      final ComponentColorModel RGBA_COLOR =
         new ComponentColorModel (
            ColorSpace.getInstance (ColorSpace.CS_sRGB),
            new int[] { 8, 8, 8, 8 }, true, false,
            ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      // sRGBA color model
      WritableRaster raster = Raster.createInterleavedRaster (
         DataBuffer.TYPE_BYTE, v[0], v[1], 4, null);
      BufferedImage image = new BufferedImage (RGBA_COLOR, raster, false, null);
      
      // flip vertically
      int scanline =4*v[0];
      int pos = v[0]*v[1]*4-scanline;
      for (int i=0; i<v[1]; ++i) {
         for (int j=0; j<scanline; ++j) {
            raster.getDataBuffer ().setElem (pos+j, buff.get ());   
         }
         pos -= scanline;
      }
      
      return image;
      
   }

   private static void printErr(String msg) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      String fullClassName = trace[3].getClassName();            
      String className = 
      fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
      String methodName = trace[3].getMethodName();
      int lineNumber = trace[3].getLineNumber();
      System.err.println (
         className + "." + methodName + "():" + lineNumber + ": " + msg);
   }
   
   public static boolean checkAndPrintGLError (GL gl) {
      int err = gl.glGetError();
      if (err != GL.GL_NO_ERROR) {
         String msg = Error.gluErrorString(err);
         printErr(msg + " (" +err + ")");
         return false;
      }
      return true;
   }
}

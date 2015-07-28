package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import maspack.render.GL.GL3.BufferStorage.StorageType;

public abstract class NormalBufferPutter {

   public abstract void putNormal(ByteBuffer buff, float nx, float ny, float nz);
   public abstract int bytesPerNormal();
   public abstract BufferStorage storage();
   
   public void putNormal(ByteBuffer buff, float[] nrm) {
      putNormal(buff, nrm[0], nrm[1], nrm[2]);
   }
   
   public void putNormal(ByteBuffer buff, float[] nrm, int offset) {
      putNormal(buff, nrm[offset], nrm[offset+1], nrm[offset+2]);
   }
   
   public void putNormals(ByteBuffer buff, float[] nrms) {
      for (int i=0; i<nrms.length-2; i+=3) {
         putNormal(buff, nrms, i);
      }
   }
   
   public void putNormals(ByteBuffer buff, float[] nrms, int offset, int stride, int count) {
      if (stride <= 0) {
         stride = 3;
      }
      int idx = offset;
      for (int i=0; i<count; ++i) {
         putNormal(buff, nrms, idx);
         idx += stride;
      }  
   }
   
   public void putNormals(ByteBuffer buff, Iterable<float[]> normals) {
      for (float[] nrm : normals) {
         putNormal(buff, nrm);
      }
   }
   
   private void setLocation(ByteBuffer buff, int location) {
      if (location >= 0) {
         if (buff.position() != location) {
            buff.position(location);
         }
      }
   }
   
   public void putNormal(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putNormal(buff, pos);
   }
   
   public void putNormal(ByteBuffer buff, int location, float[] pos, int offset) {
      setLocation(buff, location);
      putNormal(buff, pos, offset);
   }
   
   public void putNormals(ByteBuffer buff, int location, float[] pos) {
      setLocation(buff, location);
      putNormals(buff, pos);
   }
   
   public void putNormals(ByteBuffer buff, int location, float[] pos, int offset, int stride, int count) {
      setLocation(buff, location);
      putNormals(buff, pos, offset, stride, count);
   }
   
   public void putNormals(ByteBuffer buff, int location, int bstride, float[] pos, int offset, int pstride, int count) {
      
      if (pstride <= 0) {
         pstride = 3;
      }
      if (bstride <= 0) {
         bstride = bytesPerNormal();
      }
      for (int i=0; i<count; ++i) {
         setLocation(buff, location+i*bstride);   
         putNormal(buff, pos, offset+i*pstride);   
      }
      
   }
   
   public void putNormals(ByteBuffer buff, int location, Iterable<float[]> positions) {
      setLocation(buff, location);
      putNormals(buff, positions);
   }
   
   public static class FloatNormalBufferPutter extends NormalBufferPutter {
      
      @Override
      public void putNormal(ByteBuffer buff, float nx, float ny, float nz) {
         buff.putFloat(nx);
         buff.putFloat(ny);
         buff.putFloat(nz);
      }
      
      @Override
      public int bytesPerNormal() {
         return 3*Float.BYTES;
      }
      
      @Override
      public BufferStorage storage() {
         return BufferStorage.FLOAT_3;
      }
      
   }
   
   public static class ShortNormalBufferPutter extends NormalBufferPutter {

      @Override
      public void putNormal(ByteBuffer buff, float nx, float ny, float nz) {
         // scale
         float nmax = Math.max(Math.max(Math.abs(nx), Math.abs(ny)), Math.abs(nz));
         if (nmax <= 0) {
            nmax = 1;
         }
         buff.putShort((short)(nx/nmax*(Short.MAX_VALUE+0.5)-0.5));
         buff.putShort((short)(ny/nmax*(Short.MAX_VALUE+0.5)-0.5));
         buff.putShort((short)(nz/nmax*(Short.MAX_VALUE+0.5)-0.5));
         buff.putShort((short)0); // alignment bytes
      }

      @Override
      public int bytesPerNormal() {
         return 4*Short.BYTES;
      }
      
      @Override
      public BufferStorage storage() {
         return new BufferStorage(StorageType.SIGNED_SHORT, 3, 4*Short.BYTES, true);
      }
   }
   
   public static NormalBufferPutter createDefault() {
      return new ShortNormalBufferPutter();
   }
   
}

package maspack.render;

import java.util.List;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.VertexDrawMode;

public class RenderObjectExamples {

   /**
    * Example showing basic construction, but advanced usage in the form
    * of multiple color buffers.
    */
   public static void example1(Renderer renderer, int selectedAxis) {
      
      // get shared object (so we only have one instance per viewer)
      RenderObject axes = renderer.getSharedObject("axes");
      
      // create if doesn't exist
      if (axes == null) {
         axes = new RenderObject();  // constructor
      
         byte[] red    = new byte[]{(byte)255,(byte)0,  (byte)0,  (byte)255};
         byte[] green  = new byte[]{(byte)0,  (byte)255,(byte)0,  (byte)255};
         byte[] blue   = new byte[]{(byte)0,  (byte)0,  (byte)255,(byte)255};
         byte[] yellow = new byte[]{(byte)255,(byte)255,(byte)0,  (byte)255};
         float[] origin = new float[]{0,0,0};
         float[] xunit = new float[]{1,0,0};
         float[] yunit = new float[]{0,1,0};
         float[] zunit = new float[]{0,0,1};
         
         int cx = axes.addColor(red);
         int cy = axes.addColor(green);
         int cz = axes.addColor(blue);
         
         // regular axes
         axes.color(cx);
         axes.addLine(origin, xunit);
         axes.color(cy);
         axes.addLine(origin, yunit);
         axes.color(cz);
         axes.addLine(origin, zunit);
         
         // selected axes
         axes.createColorSetFrom(0);    // copy original colors
         axes.setColor(cx, yellow);     // replace x-axis color
         
         axes.createColorSetFrom(0);
         axes.setColor(cy, yellow);
         
         axes.createColorSetFrom(0);
         axes.setColor(cz, yellow);
         
         renderer.addSharedObject("axes", axes); // share with others
      }
      
      // draw axes
      // select appropriate color set
      switch (selectedAxis) {
         case 0:  // x-axis 
            axes.colorSet(1);
            break;
         case 1:  // y-axis
            axes.colorSet(2);
            break;
         case 2:  // z-axis
            axes.colorSet(3);
            break;
         default: // no axis
            axes.colorSet(0); // default
      }
      
      // <apply transform>
      // draw axes
      renderer.draw(axes);
      
   }
   
   /**
    * Example showing a simplified mesh renderer 
    */
   public static void example2(Renderer renderer, PolygonalMesh mesh, boolean drawEdges, boolean drawVertices) {
      
      RenderObject r = new RenderObject();
      
      // add all appropriate info
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
      }
      if (mesh.hasNormals()) {
         for (Vector3d nrm : mesh.getNormalList()) {
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
      }
      if (mesh.hasColors()) {
         for (byte[] color : mesh.getColorList()) {
            r.addColor(color);
         }
      }
      if (mesh.hasTextureCoords()) {
         for (Vector3d texCoord : mesh.getTextureCoordList()) {
            // according to existing MeshRenderer, we need to flip y
            r.addTextureCoord((float)texCoord.x, (float)(1-texCoord.y));
         }
      }
      
      // build faces
      List<Face> faces = mesh.getFaces();
      final int[] invalid = new int[] {-1}; 
      for (int i=0; i<faces.size(); i++) {
         Face f = faces.get(i);
         
         int[] pidxs = f.getVertexIndices();
         int[] nidxs = invalid;
         int[] cidxs = invalid;
         int[] tidxs = invalid;
         
         if (mesh.hasNormals()) {
            nidxs = mesh.getNormalIndices().get(i);
         }
         if (mesh.hasColors()) {
            cidxs = mesh.getColorIndices().get(i);
         }
         if (mesh.hasTextureCoords()) {
            tidxs = mesh.getTextureIndices().get(i);
         }
         
         int[] vidxs = new int[pidxs.length]; // vertex indices
         for (int j=0; j<pidxs.length; j++) {
            vidxs[j] = r.addVertex(
               pidxs[j], 
               nidxs[j%nidxs.length], // account for invalid
               cidxs[j%cidxs.length], 
               tidxs[j%tidxs.length]);
         }
         
         // triangle fan for faces, line loop for edges
         r.addTriangleFan(vidxs);
         if (drawEdges) {
            r.addLineLoop(vidxs);
         }
         
      }
      
      // draw mesh
      // <set face color and maybe polygon offset>
      // draw faces
      renderer.drawTriangles(r);
      if (drawEdges) {
         // <set edge color>
         // draw edges
         renderer.drawLines(r);
      }
      if (drawVertices) {
         // <set vertex color>
         // Rather than set up a set of point primitives, which would result
         // in an extra index array in corresponding VBOs, draw the vertex
         // array directly with an appropriate mode.
         renderer.drawVertices(r, VertexDrawMode.POINTS);  // draw all vertices as points
         
         // less-efficient alternative:
         // for (int i=0; i<r.numVertices(); i++) {
         //    r.addPoint(i);
         // }
         // renderer.drawPoints(r);
      }
      
   }
   
}
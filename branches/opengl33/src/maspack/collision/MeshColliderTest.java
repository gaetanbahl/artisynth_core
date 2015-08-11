package maspack.collision;

import javax.media.opengl.GL2;

import javax.swing.JFrame;

import com.jogamp.opengl.util.gl2.GLUT;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.GL.GLRenderable;
import maspack.render.GL.GLViewerFrame;
import maspack.render.GL.GL2.GL2Viewer;

public class MeshColliderTest {
   static double epsilon = 1e-8;

   /**
    * @param args
    */
   public static void main (String[] args) {
      MeshColliderTest test = new MeshColliderTest();

      System.out.println ("Vertex Vertex: " + test.testVertexVertex());
      System.out.println ("Vertex Edge: " + test.testVertexEdge());
      System.out.println ("Vertex Face: " + test.testVertexFace());
      System.out.println ("Edge Edge: " + test.testEdgeEdge());
      System.out.println ("Edge Face: " + test.testEdgeFace());
      System.out.println ("Regions: " + test.testRegions());
   }

   boolean testVertexVertex() {
      PolygonalMesh mesh0 = MeshFactory.createBox (1, 1, 1);
      PolygonalMesh mesh1 = MeshFactory.createBox (1, 1, 1);

      RigidTransform3d trans0 =
         new RigidTransform3d (
            new Vector3d (Math.sqrt (3), 0, 0), new AxisAngle());
      trans0.mulAxisAngle (new AxisAngle (0, 1, 0, Math.atan (Math.sqrt (2))));
      trans0.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh0.setMeshToWorld (trans0);

      RigidTransform3d trans1 = new RigidTransform3d();
      trans1.mulAxisAngle (new AxisAngle (0, 1, 0, Math.atan (Math.sqrt (2))));
      trans1.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh1.setMeshToWorld (trans1);

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      ContactRegion region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0) / 2.0, 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         0.9994849337479609, 0, -0.03209154422638611), epsilon))
         return false;

      // second way
      info = collider.getContacts (mesh1, mesh0, true);
      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0) / 2.0, 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         -0.9994849337479609, 0, 0.03209154422638611), epsilon))
         return false;

      return true;
   }

   boolean testVertexEdge() {
      PolygonalMesh mesh0 = MeshFactory.createBox (1, 1, 1);
      PolygonalMesh mesh1 = MeshFactory.createBox (1, 1, 1);

      RigidTransform3d trans0 =
         new RigidTransform3d (new Vector3d (
            Math.sqrt (3.0) / 2.0 + 0.5, 0, 0.5), new AxisAngle());
      mesh0.setMeshToWorld (trans0);

      RigidTransform3d trans1 = new RigidTransform3d();
      trans1.mulAxisAngle (new AxisAngle (0, 1, 0, Math.atan (Math.sqrt (2))));
      trans1.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh1.setMeshToWorld (trans1);

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      ContactRegion region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0 / 4.0), 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         1.0 / Math.sqrt (2), 0, 1.0 / Math.sqrt (2)), epsilon))
         return false;

      // second way
      info = collider.getContacts (mesh1, mesh0, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0 / 4.0), 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         -1.0 / Math.sqrt (2), 0, -1.0 / Math.sqrt (2)), epsilon))
         return false;

      return true;
   }

   boolean testVertexFace() {
      PolygonalMesh mesh0 = MeshFactory.createBox (1, 1, 1);
      PolygonalMesh mesh1 = MeshFactory.createBox (1, 1, 1);

      RigidTransform3d trans0 =
         new RigidTransform3d (new Vector3d (
            Math.sqrt (3.0) / 2.0 + 0.5, 0, 0.25), new AxisAngle());
      mesh0.setMeshToWorld (trans0);

      RigidTransform3d trans1 = new RigidTransform3d();
      trans1.mulAxisAngle (new AxisAngle (0, 1, 0, Math.atan (Math.sqrt (2))));
      trans1.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh1.setMeshToWorld (trans1);

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      ContactRegion region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0 / 4.0), 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (1, 0, 0), epsilon))
         return false;

      // first way
      info = collider.getContacts (mesh1, mesh0, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (Math.sqrt (3.0 / 4.0), 0, 0), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (-1, 0, 0), epsilon))
         return false;

      return true;
   }

   boolean testEdgeEdge() {
      PolygonalMesh mesh0 = MeshFactory.createBox (1, 1, 1);
      PolygonalMesh mesh1 = MeshFactory.createBox (1, 1, 1);

      RigidTransform3d trans0 =
         new RigidTransform3d (new Vector3d (1, 0, 1), new AxisAngle());
      mesh0.setMeshToWorld (trans0);

      RigidTransform3d trans1 = new RigidTransform3d();
      trans1.mulAxisAngle (new AxisAngle (0, 1, 0, Math.PI / 4));
      trans1.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh1.setMeshToWorld (trans1);

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      ContactRegion region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (0.5, 0, 0.5), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         1.0 / Math.sqrt (2.0), 0, 1.0 / Math.sqrt (2.0)), epsilon))
         return false;

      // second way
      info = collider.getContacts (mesh1, mesh0, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      region = info.regions.get (0);
      if (region.points.size() != 1)
         return false;
      if (!region.points.get (0).epsilonEquals (
         new Vector3d (0.5, 0, 0.5), epsilon))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         -1.0 / Math.sqrt (2.0), 0, -1.0 / Math.sqrt (2.0)), epsilon))
         return false;

      return true;
   }

   boolean testEdgeFace() {
      PolygonalMesh mesh0 = new PolygonalMesh();
      PolygonalMesh mesh1 = new PolygonalMesh();

      mesh0.addVertex (new Point3d (-1, -0.1, 0));
      mesh0.addVertex (new Point3d (1, -0.1, 0));
      mesh0.addVertex (new Point3d (0, 1, 0));
      mesh0.addFace (new int[] { 0, 1, 2 });

      mesh1.addVertex (new Point3d (0, 0.1, -1));
      mesh1.addVertex (new Point3d (0, 0.1, 1));
      mesh1.addVertex (new Point3d (0, -1, 0));
      mesh1.addFace (new int[] { 0, 1, 2 });

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      ContactRegion region = info.regions.get (0);
      if (region.points.size() != 2)
         return false;
      if (!(region.points.get (0).epsilonEquals (
         new Vector3d (0, 0.1, 0), epsilon) ||
            region.points.get (1).epsilonEquals (
               new Vector3d (0, 0.1, 0), epsilon)) ||
          !(region.points.get (0).epsilonEquals (
         new Vector3d (0, -0.1, 0), epsilon) ||
            region.points.get (1).epsilonEquals (
               new Vector3d (0, -0.1, 0), epsilon)))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         1.0 / Math.sqrt (2), 0, -1.0 / Math.sqrt (2)), epsilon))
         return false;

      // second way
      info = collider.getContacts (mesh1, mesh0, true);

      if (info == null)
         return false;
      if (info.regions.size() != 1)
         return false;
      region = info.regions.get (0);
      if (region.points.size() != 2)
         return false;
      if (!(region.points.get (0).epsilonEquals (
         new Vector3d (0, 0.1, 0), epsilon) ||
            region.points.get (1).epsilonEquals (
               new Vector3d (0, 0.1, 0), epsilon)) ||
          !(region.points.get (0).epsilonEquals (
         new Vector3d (0, -0.1, 0), epsilon) ||
            region.points.get (1).epsilonEquals (
               new Vector3d (0, -0.1, 0), epsilon)))
         return false;
      if (!region.normal.epsilonEquals (new Vector3d (
         -1.0 / Math.sqrt (2), 0, 1.0 / Math.sqrt (2)), epsilon))
         return false;

      return true;
   }

   boolean testRegions() {
      PolygonalMesh mesh0 = MeshFactory.createBox (1, 1, 1);
      PolygonalMesh mesh1 = MeshFactory.createBox (1, 1, 1);

      mesh0.scale (0.9);

      RigidTransform3d trans1 = new RigidTransform3d();
      trans1.mulAxisAngle (new AxisAngle (0, 1, 0, Math.PI / 4));
      trans1.mulAxisAngle (new AxisAngle (1, 0, 0, Math.PI / 4));
      mesh1.setMeshToWorld (trans1);

      MeshCollider collider = new MeshCollider();

      // first way
      ContactInfo info = collider.getContacts (mesh0, mesh1, true);

      if (info == null)
         return false;
      if (info.regions.size() != 6)
         return false;

      // second way
      info = collider.getContacts (mesh1, mesh0, true);

      if (info == null)
         return false;
      if (info.regions.size() != 6)
         return false;

      return true;
   }

   void displayContacts (PolygonalMesh m0, PolygonalMesh m1) {
      final PolygonalMesh mesh0 = m0;
      final PolygonalMesh mesh1 = m1;

      MeshCollider collider = new MeshCollider();
      final ContactInfo info = collider.getContacts (mesh0, mesh1, true);
      // final ContactInfo info = new ContactInfo(mesh0, mesh1);
      // System.out.println("intersections " + info.intersections.size());
      // System.out.println("regions " + info.regions.size());

      GLViewerFrame frame = new GLViewerFrame ("", 512, 512);
      frame.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);

      mesh0.getRenderProps().setDrawEdges (true);
      // mesh0.getRenderProps().setAlpha(0.3);

      mesh1.getRenderProps().setDrawEdges (true);
      // mesh1.getRenderProps().setAlpha(0.3);

      frame.getViewer().addRenderable (mesh0);
      frame.getViewer().addRenderable (mesh1);

      frame.getViewer().addRenderable (new GLRenderable() {
         public int getRenderHints() {
            // TODO Auto-generated method stub
            return 0;
         }

         public void prerender (RenderList list) {
         }

         public void render (Renderer renderer, int flags) {
            if (!(renderer instanceof GL2Viewer)) {
               return;
            }
            GL2Viewer viewer = (GL2Viewer)renderer;
            GL2 gl = viewer.getGL2();
            
            renderer.setLightingEnabled (false);

            if (info != null) {
               renderer.setColor (0, 0, 1);
               gl.glPointSize (6);
               gl.glBegin (GL2.GL_POINTS);
               for (TriTriIntersection isect : info.intersections)
                  for (Point3d p : isect.points)
                     gl.glVertex3d (p.x, p.y, p.z);
               gl.glEnd();

               renderer.setColor (1, 0, 0);
               gl.glBegin (GL2.GL_LINES);
               for (ContactRegion region : info.regions) {
                  Point3d avg = new Point3d();
                  int np = 0;
                  for (Point3d rp : region.points) {
                     avg.add (rp);
                     np++;
                  }
                  avg.scale (1.0 / np);
                  gl.glVertex3d (avg.x, avg.y, avg.z);
                  avg.add (region.normal);
                  gl.glVertex3d (avg.x, avg.y, avg.z);
               }
               gl.glEnd();
            }
            ;

            // mesh0.getObbtree().render(renderer);
            // mesh1.getObbtree().render(renderer);

            // ////////////////////////////
            // draw mesh numbers
            Vector3d avg0 = new Vector3d();
            Vector3d avg1 = new Vector3d();

            for (Vertex3d v : mesh0.getVertices())
               avg0.add (v.pnt);
            avg0.scale (1.0 / mesh0.getVertices().size());
            avg0.add (mesh0.getMeshToWorld().p);

            for (Vertex3d v : mesh1.getVertices())
               avg1.add (v.pnt);
            avg1.scale (1.0 / mesh1.getVertices().size());
            avg1.add (mesh1.getMeshToWorld().p);

            GLUT glut = new GLUT();
            renderer.setColor (1, 1, 1);

            gl.glRasterPos3d (avg0.x, avg0.y, avg0.z);
            glut.glutBitmapString (GLUT.BITMAP_HELVETICA_18, "0");

            gl.glRasterPos3d (avg1.x, avg1.y, avg1.z);
            glut.glutBitmapString (GLUT.BITMAP_HELVETICA_18, "1");

            // draw mesh normals
            // //////////////////////////////

            renderer.setLightingEnabled (true);
         }

         public void updateBounds (Point3d pmin, Point3d pmax) {
            // TODO Auto-generated method stub

         }
      });

      frame.getViewer().rerender();
      frame.getViewer().autoFit();
      frame.setVisible (true);
   }
}
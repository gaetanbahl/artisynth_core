/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.util.Iterator;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.GL.GLSelectable;
import maspack.render.GL.GLSelectionFilter;

public interface Renderer {

   /** 
    * Flag requesting that an object be rendered as though it is selected.
    */   
   public static int SELECTED = 0x1;

   /** 
    * Flag requesting that vertex coloring should be used for mesh rendering.
    */
   public static int VERTEX_COLORING = 0x2;

   /** 
    * Flag requesting color interpolation in HSV space, if possible.
    */
   public static int HSV_COLOR_INTERPOLATION = 0x4;
   
   public enum VertexDrawMode {
      POINTS,
      LINES,
      LINE_STRIP,
      LINE_LOOP,
      TRIANGLES,
      TRIANGLE_STRIP,
      TRIANGLE_FAN
   }
   public enum SelectionHighlighting {
      None, Color
   };

   public int getHeight();

   public int getWidth();

   public double getViewPlaneHeight();

   public double getViewPlaneWidth();

   // public double getFieldOfViewY();

   public double centerDistancePerPixel ();
   
   public double distancePerPixel (Vector3d p);

   public boolean isOrthogonal();

   public double getNearClipPlaneZ();

   public double getFarClipPlaneZ();
   
   public void setPointSize(float size);
   
   public float getPointSize();
   
   public void setLineWidth(float w);
   
   public float getLineWidth();

   public void rerender();

   public RigidTransform3d getEyeToWorld();

   /**
    * Returns whether or not lighting is enabled.
    */
   public boolean isLightingEnabled();

   /**
    * Enables or disables GL_LIGHTING. The reason for doing this through the
    * renderer, rather than calling GL directly, is so that the renderer can
    * discard the request when performing a selection render.
    */
   public void setLightingEnabled (boolean enable);

   public void setColor (float[] rgb, boolean selected);
   
   public void setColor (float[] frontRgba, float[] backRgba, boolean selected);

   public void setColor (float[] rgb);
   
   public void setColor (float[] frontRgba, float[] backRgba);

   public void setColor (Color color);
   
   public void setColor (Color frontColor, Color backColor);

   public void updateColor (float[] rgba, boolean selected);
   
   public void updateColor (float[] frontRgba, float[] backRgba, boolean selected);

   public void setColor (float r, float g, float b);

   public void setColor (float r, float g, float b, float a);

   public void setMaterial (Material material, boolean selected);
   
   public void setMaterial (Material material, float[] diffuseColor,
      boolean selected);
   
   public void setMaterial (Material frontMaterial, float[] frontDiffuse,
      Material backMaterial, float[] backDiffuse, 
      boolean selected);

   public void setMaterialAndShading (
      RenderProps props, Material mat, boolean selected);
   
   public void setMaterialAndShading (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected);
   
   public void setMaterialAndShading (
      RenderProps props, Material frontMaterial, float[] frontDiffuse,
      Material backMaterial, float[] backDiffuse, 
      boolean selected);
   
   public void restoreShading (RenderProps props);
   
   public Shading getShadeModel();
   
   public void setShadeModel(Shading shading);

   public void updateMaterial (
      RenderProps props, Material material, boolean selected);
   
   public void updateMaterial (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected);
   
   public void updateMaterial (
      RenderProps props, Material frontMaterial, float[] frontDiffuse, 
      Material backMaterial, float[] backDiffuse, boolean selected);

   public void drawSphere (RenderProps props, float[] coords);
   
   public void drawSphere (RenderProps props, float[] coords, double r);

   public void drawTet (RenderProps props, double scale,
                        float[] v0, float[] v1, float[] v2, float[] v3);

   public void drawHex (RenderProps props, double scale,
                        float[] v0, float[] v1, float[] v2, float[] v3,
                        float[] v4, float[] v5, float[] v6, float[] v7);

   public void drawWedge (RenderProps props, double scale,
                          float[] v0, float[] v1, float[] v2, 
                          float[] v3, float[] v4, float[] v5);

   public void drawPyramid (RenderProps props, double scale,
                          float[] v0, float[] v1, float[] v2, 
                          float[] v3, float[] v4);

   public void drawTaperedEllipsoid (
      RenderProps props, float[] coords0, float[] coords1);

   public void drawCylinder (RenderProps props, float[] coords0, float[] coords1);

   public void drawCylinder (
      RenderProps props, float[] coords0, float[] coords1, boolean capped);
   
   public void drawCone (RenderProps props, float[] coords0, float[] coords1);

   public void drawCone (
      RenderProps props, float[] coords0, float[] coords1, boolean capped);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean selected);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      float[] color, boolean selected);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected);

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped);

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1);

   public void drawArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected);

   public void drawLines (
      RenderProps props, Iterator<? extends RenderableLine> iterator);

   public void drawPoint (RenderProps props, float[] coords, boolean selected);
   
   public void drawPoint(float[] coords);
   
   public void drawLine(float[] coords0, float[] coords1);

   public void drawPoints (
      RenderProps props, Iterator<? extends RenderablePoint> iterator);

   public void drawAxes (
      RenderProps props, RigidTransform3d X, double len, boolean selected);

   public void drawAxes (
      RenderProps props, RigidTransform3d X, double[] len, boolean selected);

   public boolean isTransparencyEnabled();

   public void setTransparencyEnabled (boolean enable);

   // public void drawXYGrid (double size, int numcells);

   public void setFaceMode (RenderProps.Faces mode);

   public void setDefaultFaceMode();

   public void mulTransform (AffineTransform3d X);

   public void mulTransform (RigidTransform3d X);
   
   /**
    * Direction pointing out of monitor
    */
   public Vector3d getZDirection();
   
   /**
    * Start displaying 2D objects, dimensions given by pixels
    */
   public void begin2DRendering();
   
   /**
    * Start displaying 2D objects, dimensions governed by 
    * supplied width/height
    */
   public void begin2DRendering(double w, double h);
   
   /**
    * Finalize 2D rendering, returning to default 3D mode
    */
   public void end2DRendering();
   
   /**
    * Check whether renderer is currently in 2D mode
    */
   public boolean is2DRendering();

   public void drawTriangles(RenderObject robj);
   
   public void drawLines(RenderObject robj);
   
   public void drawLines(RenderObject robj, LineStyle style, double rad);
   
   public void drawPoints(RenderObject robj);
   
   public void drawPoints(RenderObject robj, PointStyle style, double rad);
   
   public void drawVertices(RenderObject r, VertexDrawMode mode);
   
   public void draw(RenderObject r);
   
   public RenderObject getSharedObject(Object key);
   
   public void addSharedObject(Object key, RenderObject r);

   public void removeSharedObject(Object key);
   
   public void pushModelMatrix();
   
   public boolean popModelMatrix();
   
   public void pushViewMatrix();
   
   public boolean popViewMatrix();
   
   public void pushProjectionMatrix();
   
   public boolean popProjectionMatrix();
   
   //===============================================================
   // THINGS TO MOVE OUT OF RENDERER
   //===============================================================
   
   /**
    * Flag requesting that all display lists be cleared
    */
   public static int CLEAR_RENDER_CACHE = 0x100;

   /**
    * Flag requesting components refresh any custom rendering info
    */
   public static int UPDATE_RENDER_CACHE = 0x200;
   
   /**
    * Returns true if the renderer is currently performing a selection
    * render. When this is the case, the application should avoid calls
    * affecting color and lighting.
    *
    * @return true if the renderer is in selection mode.
    */
   public boolean isSelecting();
   
   public SelectionHighlighting getSelectionHighlighting();

   public Color getSelectionColor();

   public Material getSelectionMaterial();
   
   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.GL.GLSelectable}, then <code>qid</code> must lie in the range
    * 0 to <code>numq</code>-1, where <code>numq</code> is the value returned
    * by {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable.numSelectionQueriesNeeded()}.  Selection queries cannot be
    * nested, and a given query identifier should be used only once.
    *
    * @param qid identifier for the selection query
    * @see #endSelectionQuery
    */
   public void beginSelectionQuery (int qid);

   /**
    * Ends a selection query that was initiated with a call to
    * {@link #beginSelectionQuery}.
    *
    * @see #beginSelectionQuery
    */
   public void endSelectionQuery ();

   /**
    * Begins selection for a {@link maspack.render.GL.GLSelectable}
    * <code>s</code>that
    * manages its own selection; this call should
    * be used in place of {@link #beginSelectionQuery} for such objects.
    * Selectables that manage their own selection are identified by
    * having a value <code>numq</code> >= 0, where <code>numq</code>
    * is the value returned by
    * {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable#numSelectionQueriesNeeded{}}.
    * The argument <code>qid</code> is the current selection query identifier,
    * which after the call should be increased by <code>numq</code>.
    *
    * @param s Selectable that is managing its own sub-selection
    * @param qid current selection query identifier
    */
   public void beginSubSelection (GLSelectable s, int qid);

   /**
    * Ends object sub-selection that was initiated with a call
    * to {@link #beginSubSelection}.
    *
    * @see #endSubSelection
    */
   public void endSubSelection ();

   /**
    * Sets a selection filter for the renderer. This restricts which objects
    * are actually rendered when a selection render is performed, and therefore
    * restricts which objects can actually be selected. This allows
    * selection of objects that might otherwise be occluded within a scene.
    *
    * @param filter Selection filter to be applied
    */
   public void setSelectionFilter (GLSelectionFilter filter);

   /**
    * Returns the current selection filter for the renderer, if any.
    *
    * @return current selection filter, or <code>null</code> if there is none.
    */
   public GLSelectionFilter getSelectionFilter ();

   /**
    * Returns true if <code>s</code> is selectable in the current selection
    * context. This will be the case if <code>s.isSelectable()</code> returns
    * <code>true</code>, and <code>s</code> also passes whatever selection
    * filter might currently be set in the renderer.
    */
   public boolean isSelectable (GLSelectable s);
   
}
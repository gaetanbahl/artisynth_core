/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.render.*;

public abstract class MonitorBase extends ControllerMonitorBase
   implements Monitor {
   
   public void render (GLRenderer gl, int flags) {
   }

}
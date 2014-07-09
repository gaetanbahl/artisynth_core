/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Font;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class MenuEntry implements Comparable<MenuEntry> {

   private String title;
   private String icon;
   private Font menuFont;

   public MenuEntry() {
      menuFont = null;
   }
   public MenuEntry(String title) {
      this.title = title;
   }
   public String getIcon() {
      return icon;
   }
   public void setIcon(String icon) {
      this.icon = icon;
   }
   public String getTitle() {
      return title;
   }
   public void setTitle(String title) {
      this.title = title;
   }
   public MenuType getType() {
      return MenuType.MENU;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MenuEntry)) { return false; }

      MenuEntry mObj = (MenuEntry) obj;
      boolean res = (title.equals(mObj.title));

      if (icon != null) {
	 res = res && (icon.equals(mObj.icon));
      } else {
	 res = res && (mObj.icon == null);
      }
      return res;
   }

   public int compareTo(MenuEntry o) {
      return title.compareTo(o.title); // sort based on titles
   }
   
   public void setFont(Font font) {
      menuFont = font;
   }
   public Font getFont() {
      return menuFont;
   }

}
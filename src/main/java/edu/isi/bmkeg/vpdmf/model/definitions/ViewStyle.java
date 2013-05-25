package edu.isi.bmkeg.vpdmf.model.definitions;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.awt.Image;



public class ViewStyle  {
  private int textSize;
  private int borderWidth;
  private Image altBgImage;
  private Image bgImage;
  private String textFont;
  private Image bannerImage;
  private String textColor;
  private String contact;
  private Image linkLogo;
  private String link;
  private String borderColor;


  public void destroy () 
  {
    
    
  }

  public Image get_altBgImage () 
  {
    return this.altBgImage;
  }

  public Image get_bannerImage () 
  {
    return this.bannerImage;
  }

  public Image get_bgImage () 
  {
    return this.bgImage;
  }

  public String get_borderColor () 
  {
    return this.borderColor;
  }

  public int get_borderWidth () 
  {
    return this.borderWidth;
  }

  public String get_contact () 
  {
    return this.contact;
  }

  public String get_link () 
  {
    return this.link;
  }

  public Image get_linkLogo () 
  {
    return this.linkLogo;
  }

  public String get_textColor () 
  {
    return this.textColor;
  }

  public String get_textFont () 
  {
    return this.textFont;
  }

  public int get_textSize () 
  {
    return this.textSize;
  }

  public void set_altBgImage (Image altBgImage) 
  {
    this.altBgImage = altBgImage;
  }

  public void set_bannerImage (Image bannerImage) 
  {
    this.bannerImage = bannerImage;
  }

  public void set_bgImage (Image bgImage) 
  {
    this.bgImage = bgImage;
  }

  public void set_borderColor (String borderColor) 
  {
    this.borderColor = borderColor;
  }

  public void set_borderWidth (int borderWidth) 
  {
    this.borderWidth = borderWidth;
  }

  public void set_contact (String contact) 
  {
    this.contact = contact;
  }

  public void set_link (String link) 
  {
    this.link = link;
  }

  public void set_linkLogo (Image linkLogo) 
  {
    this.linkLogo = linkLogo;
  }

  public void set_textColor (String textColor) 
  {
    this.textColor = textColor;
  }

  public void set_textFont (String textFont) 
  {
    this.textFont = textFont;
  }

  public void set_textSize (int textSize) 
  {
    this.textSize = textSize;
  }


};

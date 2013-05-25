package edu.isi.bmkeg.vpdmf.model.instances;

import java.util.Vector;

public abstract class DataHolder {

  protected Vector attrVec;

  public Vector attrVec() {
    return this.attrVec;
  }

  public int getColumnNumber(String addr) {
    for (int i = 0; i < this.attrVec.size(); i++) {
      String chk = (String)this.attrVec.get(i);
      if (chk.endsWith(addr))
        return i;
    }
    return -1;

  }

}

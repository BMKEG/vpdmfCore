package edu.isi.bmkeg.vpdmf.model.instances;

import java.util.Vector;

import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;

public class ObjectDataHolder extends DataHolder {

  private ObjectMatrix2D data;

  public ObjectDataHolder( ObjectMatrix2D data, Vector attrVec ) {
    this.data = data;
    this.attrVec = attrVec;
  }

  public ObjectDataHolder add( ObjectDataHolder odh ) {

    return this.add( odh, this.attrVec);

  }


  public ObjectDataHolder add( ObjectDataHolder dh, Vector resultantAttrVec ) {

    ObjectFactory2D F = ObjectFactory2D.dense;

    ObjectDataHolder newDh = null;
    if( this.data != null && dh.data != null ) {
      ObjectMatrix2D  newMatrix = F.appendRows(this.data, dh.data);
      newDh = new ObjectDataHolder( newMatrix, resultantAttrVec);
    }
    else if( this.data != null && dh.data == null ) {
      newDh = this;
    }
    else if( this.data == null && dh.data != null ) {
      newDh = dh;
    }

    return newDh;

  }

  public ObjectMatrix2D getData() {
    return this.data;
  }

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


  public ObjectMatrix1D getData( String addr ) {

    int c = this.getColumnNumber( addr );
    return this.data.viewColumn( c );

  }

}

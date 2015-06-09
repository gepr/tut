/*
 * Copyright 2015 - Regents of the University of California, San
 * Francisco.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */
package tut.view;

import tut.model.LooseDyn;

public class ObsAcc extends Obs {
  public ObsAcc(String en, tut.ctrl.Parameters p) {
    super(en,p);
  }

  @Override
  public void init(java.io.File dir, tut.model.Model m) {
    super.init(dir,m);
    if (!(m instanceof LooseDyn)) throw new RuntimeException("Acc can only be measured from LooseDyn");
  }
  @Override
  public void writeHeader() {
    outFile.println("Time, Acc");
  }
  
  @Override
  public java.util.ArrayList<Double> measure() {
    java.util.ArrayList<Double> retVal = new java.util.ArrayList<>();
    retVal.add((double)((LooseDyn)subject).acc);
    return retVal;
  }
}

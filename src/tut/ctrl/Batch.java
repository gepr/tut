/*
 * Copyright 2015 - Regents of the University of California, San
 * Francisco.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */
package tut.ctrl;

public class Batch {
  public static int MODEL_ORDER = 10;
  long seed = -Integer.MAX_VALUE;
  Parameters params = null;
  public sim.engine.SimState state = null;
  public static String expName="notset";
  static java.io.File dir = null;
  tut.model.Tight modelT = null;
  tut.model.Loose modelL = null;
  tut.model.LooseDyn modelLD = null;
  
  public Batch(String en, tut.ctrl.Parameters p) {
    if (p != null) params = p;
    Number seed_n = params.batch.get("seed");
    if (seed_n != null) {
      seed = seed_n.longValue();
    } else {
      seed = System.currentTimeMillis();
      params.batch.put("seed",seed);
    }
    if (en != null && !en.equals("")) expName = en;
    else throw new RuntimeException("Experiment name cannot be null or empty.");
    state = new sim.engine.SimState(seed);
  }
  public final void load() {
    // setup output target
    setupOutput(params);

    // launch the tightly coupled model
    modelT = new tut.model.Tight(params);
    modelT.init(state, 
            params.tight.get("timeLimit").doubleValue(),
            params.tight.get("cyclePerTime").doubleValue());
    state.schedule.scheduleOnce(modelT, MODEL_ORDER);

    // attach the observer for the the tightly coupled model
    tut.view.ObsDrug mTObs = new tut.view.ObsDrug(expName, params);
    mTObs.init(dir, modelT);
    state.schedule.scheduleOnce(mTObs, tut.view.ObsDrug.VIEW_ORDER);
    
    // launch the loosely coupled model
    modelL = new tut.model.Loose(params);
    modelL.init(state, 
            params.loose.get("timeLimit").doubleValue(),
            params.loose.get("cyclePerTime").doubleValue());
    state.schedule.scheduleOnce(modelL, MODEL_ORDER);
    
    // attach the observer for the loosely coupled model
    tut.view.ObsDrug mLObs = new tut.view.ObsDrug(expName, params);
    mLObs.init(dir,modelL);
    state.schedule.scheduleOnce(mLObs, tut.view.Obs.VIEW_ORDER);
    
    // launch the dynamic loosely coupled model
    modelLD = new tut.model.LooseDyn(params);
    modelLD.init(state, 
            params.loose.get("timeLimit").doubleValue(),
            params.loose.get("cyclePerTime").doubleValue());
    state.schedule.scheduleOnce(modelLD, MODEL_ORDER);
    
    // attach the compound observer for the dynamic loosely coupled model
    tut.view.ObsDrug mLDObsC = new tut.view.ObsDrug(expName, params);
    mLDObsC.init(dir,modelLD);
    state.schedule.scheduleOnce(mLDObsC, tut.view.Obs.VIEW_ORDER);
    // attach the acc observer
    tut.view.ObsPain mLDObsA = new tut.view.ObsPain(expName, params);
    mLDObsA.init(dir,modelLD);
    state.schedule.scheduleOnce(mLDObsA, tut.view.Obs.VIEW_ORDER);
  }
  
  public void go() {
    while (!modelT.finished || !modelL.finished)
      state.schedule.step(state);
    log("Batch.go() - Submodels are finished!");
  }
  
  public void finish() {
    log.close();
  }

  private static java.io.PrintWriter log = null;
  public static void log(String entry) { log.println(entry); log.flush(); }
  static void setupOutput(Parameters p) {
    String out_dir = null;
    final String DATE_FORMAT = "yyyy-MM-dd-HHmmss";
    final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
    StringBuffer date_s = new StringBuffer("");
    sdf.format(new java.util.Date(System.currentTimeMillis()), date_s,
            new java.text.FieldPosition(0));
    out_dir = date_s.toString();

    // create a directory using the current date and time
    dir = new java.io.File(out_dir);
    if (!dir.exists()) dir.mkdir();
    
    // write parameters
    try {
      java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(out_dir 
              + java.io.File.separator 
              + "parameters-" + tut.Main.MAJOR_VERSION + "-" + System.currentTimeMillis()+".json"));
      p.version = tut.Main.MAJOR_VERSION+" Subversion"+tut.Main.MINOR_VERSION;
      fw.write(p.describe());
      fw.close();
    } catch (java.io.IOException ioe) {
      System.exit(-1);
    }

    // initialize the output for run-time measurements
    try {
      log = new java.io.PrintWriter(new java.io.File(out_dir
              + java.io.File.separator + expName + "-output.txt"));
    } catch (java.io.FileNotFoundException fnfe) {
      throw new RuntimeException("Couldn't open " + out_dir
              + java.io.File.separator + expName + ".txt", fnfe);
    }

  }
  public double getMaxCycle() {
    return Math.max(modelT.timeLimit*modelT.cyclePerTime, modelL.timeLimit*modelL.cyclePerTime);
  }
}

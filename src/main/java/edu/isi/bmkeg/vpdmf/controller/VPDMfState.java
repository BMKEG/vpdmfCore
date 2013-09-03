package edu.isi.bmkeg.vpdmf.controller;

import edu.isi.bmkeg.utils.events.BmkegState;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.ViewHolder;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class VPDMfState extends BmkegState {
	
	public static int HTML = 0;
	public static int DOCUMENTATION = 1;
	public static int QUERY = 2;
	public static int INSERT = 3;
	public static int LIST = 4;
	public static int DISPLAY = 5;
	public static int EDIT = 6;
	public static int TABULATE = 7;
	public static int RANK = 10;
	
	private int state;
	
	// All the schema
	private VPDMf top;
		
	// The name of the current ViewDefinitions
	private String currentViewType;	
	
	// The currently loaded view
	private ViewInstance currentViewInstance;
	
	// the currently loaded view holder... basically a list of many views. 
	private ViewHolder vh;
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public VPDMf getTop() {
		return top;
	}

	public void setTop(VPDMf top) {
		this.top = top;
	}

	public String getCurrentViewType() {
		return currentViewType;
	}

	public void setCurrentViewType(String currentViewType) {
		this.currentViewType = currentViewType;
	}
	
	public ViewHolder getVh() {
		return vh;
	}

	public void setVh(ViewHolder vh) {
		this.vh = vh;
	}

	public ViewInstance getCurrentViewInstance() {
		return currentViewInstance;
	}

	public void setCurrentViewInstance(ViewInstance currentViewInstance) {
		this.currentViewInstance = currentViewInstance;
	}
	
}

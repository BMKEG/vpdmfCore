package edu.isi.bmkeg.vpdmf.model.instances;

import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class RelationInstance extends SuperGraphEdge {
	static final long serialVersionUID = 2593554832580697147L;

	private ViewInstance rlnView;

	public RelationInstance() {
		super();
	}

	public RelationInstance(ViewDefinition definition) throws Exception {
		super();
		this.rlnView = new ViewInstance(definition);
	}

	public void destroy() {
		super.destroy();
	}

	public ViewInstance getRlnView() {
		return this.rlnView;
	}

	public void setRlnView(ViewInstance rlnView) {
		this.rlnView = rlnView;
	}

};

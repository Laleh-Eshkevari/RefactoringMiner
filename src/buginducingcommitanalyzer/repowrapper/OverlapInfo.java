package buginducingcommitanalyzer.repowrapper;

import org.refactoringminer.api.Refactoring;

public class OverlapInfo {
	
	private Overlap overlap;
	private Refactoring ref;
	
	public OverlapInfo(Overlap overlap, Refactoring ref) {
		super();
		this.overlap = overlap;
		this.ref = ref;
	}
	public Overlap getOverlap() {
		return overlap;
	}
	public Refactoring getRef() {
		return ref;
	}

}

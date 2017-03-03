package buginducingcommitanalyzer.repowrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;

public class Commit {
	
	private String commitHash;
	private List<Refactoring> refs;
	private boolean isBugInducingCommit;
	private boolean hasFix; // if it is fixed in the following commits
	private boolean isFixingCommit; // when some other commit has this hash in its fixedIn
	private List<Commit> fixedIn; // when hasFix == true then we have the list of fixes here
	private Set<String> changedFiles;
	// key: a hash that is already in fixedIn 
	// value: the overlap of a refactoring in refs with the changed files of the key
	private HashMap<Commit, Set<OverlapInfo>> refactoingsAndFixOverlap; 
	private String date;
	
	public Commit(String commitHash){
		this.commitHash = commitHash;
		this.refs =  new ArrayList<Refactoring>();
		this.isBugInducingCommit = false;
		this.hasFix = false;
		this.fixedIn =  new ArrayList<Commit>();
		this.changedFiles =  new HashSet<String>();
		this.isFixingCommit = false;
		this.refactoingsAndFixOverlap = new  HashMap<Commit, Set<OverlapInfo>>();
	}

	public String getCommitHash() {
		return commitHash;
	}

	public List<Refactoring> getRefactorings() {
		return this.refs;
	}

	public boolean isBugInducingCommit() {
		return isBugInducingCommit;
	}

	public boolean hasFix() {
		return hasFix;
	}

	public boolean isFixingCommit() {
		return isFixingCommit;
	}
	
	public void setIsFixingCommit(boolean isFixingCommit) {
		this.isFixingCommit = isFixingCommit;
	}

	public List<Commit> getFixedIn() {
		return fixedIn;
	}

	public Set<String> getChangedFiles() {
		return changedFiles;
	}

	public void setBugInducingCommit(boolean isBugInducingCommit) {
		this.isBugInducingCommit=isBugInducingCommit;
		
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setHasFix(boolean hasFix) {
		this.hasFix=hasFix;
	}
	
	public HashMap<Commit, Set<OverlapInfo>> getRefactoingsAndFixOverlap() {
		return refactoingsAndFixOverlap;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.commitHash +  "  date: " +this.date +"\n");
		if(isBugInducingCommit)
			sb.append("\t is bug inducing commit\n");
		if(hasFix){	
			sb.append("\t it is fixed in the following commit(s):\n");
			for(Commit fix:this.fixedIn){
				sb.append("\t\t"+ fix.getCommitHash() + "  date: " + fix.getDate() +"\n");
			}
		}
		if(this.isFixingCommit){
			sb.append("\t is bug fixing commit\n");
		}
		
		if(this.refs.size()>0){
			sb.append("\t it contains the following refactoring(s):\n");
			for (Refactoring ref : this.refs) {
				sb.append("\t\t"+ ref.toString()+"\n");
			}
		}
		if(refactoingsAndFixOverlap.size()>0){
			sb.append("\t refactoring(s) with bug fix overlap:\n");
			for(Commit c: refactoingsAndFixOverlap.keySet()){
				if(c.getRefactorings().size()>0)
					sb.append("\t\t fixesd in: " + c.getCommitHash()+ "  with "+ c.getRefactorings().size()+" refactoring(s) \n");
				else
					sb.append("\t\t fixesd in: " + c.getCommitHash()+ "\n");
				for(OverlapInfo info: refactoingsAndFixOverlap.get(c)){
					sb.append("\t\t\t" +  info.getOverlap() +",  "+info.getRef().toString()+ "\n");
					sb.append("\n");
				}
			}
		}
		if( changedFiles.size()>0)
			sb.append("\t Java files changed: "+  changedFiles.size());
		return sb.toString();
	}
	
	public boolean equals(Object o){
		if(! (o instanceof Commit)){
			return false;
		}else{
			if(((Commit)o).getCommitHash().contentEquals(this.commitHash)){
				return true;
			}else{
				return false;
			}
		}
	}
}

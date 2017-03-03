package buginducingcommitanalyzer;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import buginducingcommitanalyzer.repowrapper.Commit;
import buginducingcommitanalyzer.repowrapper.RepositoryAnalyzer;


public class DetectedRefactoring extends RefactoringHandler{
       
	private RepositoryAnalyzer analyzer;
	
	public DetectedRefactoring(String repoName){
		this.analyzer= new RepositoryAnalyzer(repoName);
	}	

	public RepositoryAnalyzer getAnalyzer() {
		return analyzer;
	}


	@Override
    public void handle(RevCommit commitData, List<Refactoring> refactorings) {
		
		System.out.println("Refactorings at " + commitData.getId().getName());
        Commit commit = new Commit(commitData.getId().getName());
        commit.getRefactorings().addAll(refactorings);
        this.analyzer.setTotalRefactorings(this.analyzer.getTotalRefactorings()+refactorings.size() );
        this.analyzer.getAllCommits().put(commitData.getId().getName(), commit);
		for (Refactoring ref : refactorings) {
        	System.out.println(ref.toString());
         }
    }

}

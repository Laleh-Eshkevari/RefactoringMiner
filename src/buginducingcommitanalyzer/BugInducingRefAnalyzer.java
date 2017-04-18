package buginducingcommitanalyzer;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class BugInducingRefAnalyzer {

	public static void main(String[] args) {

		////////////////////////// from  https://github.com/tsantalis/RefactoringMiner ///////////////////////////////
        // 1- detect refactorings
		GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        Repository repo;
		try {
			repo = gitService.cloneIfNotExists(
			        "tmp/desproject5-with-metadata",
			        "https://LalehEshkevari@bitbucket.org/desmaintenance/desproject5-with-metadata.git");	
			DetectedRefactoring	refHandler= new DetectedRefactoring("/Users/Laleh/Documents/work/Refactoring/Aiko/commitGuruResults/bugInducingCommitAndRefactorings/desproject5");
			miner.detectAll(repo, "master",refHandler);
			
			// 2- load the results of Commit Guru 
			BugInducingCommitLoader bugInducingCommits= new BugInducingCommitLoader("/Users/Laleh/Documents/work/Refactoring/Aiko/commitGuruResults/desproject-5-modified.csv");
			bugInducingCommits.load(refHandler.getAnalyzer().getAllCommits());
			
			// 3- find the overlap between the fixes and refactorings in bug inducing commits 
			//refHandler.getAnalyzer().findLineOverlapsBetweenRefsAndFixes(repo);
			refHandler.getAnalyzer().kim(repo);
			
			
			
			System.out.println("================================================");
			System.out.println("================================================");
			refHandler.getAnalyzer().summarize();
			
			refHandler.getAnalyzer().analyzeEntityHistoryBeforeRefactoring(repo);
			
			//RefactoringGranularityAnalysis.analyze();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

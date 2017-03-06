package org.refactoringminer.test;

import static org.junit.Assert.*;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;
import org.junit.Test;

public class TestAllRefactorings {

	
	@Test
	public void testAllRefactorings() throws Exception {
		
		int refactoring= Refactorings.RenameLocalVariable.getValue();

		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "tmp2", refactoring);
		
		RefactoringPopulator.feedRefactoringsInstances(refactoring, Systems.FSE.getValue(), test);

		test.assertExpectations();

	}
}

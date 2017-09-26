package org.refactoringminer.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.test.RefactoringPopulator.Refactoring;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Root;
import org.refactoringminer.test.RefactoringPopulator.Systems;
import org.refactoringminer.test.TestBuilder.ProjectMatcher;
import org.refactoringminer.util.GitServiceImpl;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.junit.Test;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {

		int refactoring = Refactorings.RenamePackage.getValue();

		List<Root> roots = RefactoringPopulator.getFSERefactorings(refactoring);

		// for (Root root : roots) {
		// List<String> renamePackageRefactorings = getRenamePackage(root);
		// if (root.sha1.equals("abbf32571232db81a5343db17a933a9ce6923b44"))
		// if (renamePackageRefactorings.size() > 0) {
		// String tempDir = "/Volumes/ADATA HV610/tmp1";
		// GitServiceImpl gitService = new GitServiceImpl();
		// String folder = tempDir + "/" +
		// root.repository.substring(root.repository.lastIndexOf('/') + 1,
		// root.repository.lastIndexOf('.'));
		// try (Repository rep = gitService.cloneIfNotExists(folder,
		// root.repository/* , m.branch */)) {
		// RevWalk walk = new RevWalk(rep);
		// try {
		// RevCommit commit = walk.parseCommit(rep.resolve(root.sha1));
		// walk.parseCommit(commit.getParent(0));
		// List<String> files = gitService.getFilesInCurrentRevision(rep,
		// commit.getParent(0));
		//
		// System.out.println(root.sha1);
		// for (String file : files) {
		// System.out.println(file);
		// }
		//
		// List<String> oldFiles = gitService.getFilesInCurrentRevision(rep,
		// commit.getParent(1));
		// System.out.println(commit.getParent(1).name());
		// for (String file : oldFiles) {
		// System.out.println(oldFiles);
		// }
		//
		// } catch (Exception e) {
		//
		// } finally {
		// walk.dispose();
		// }
		//
		// }
		// }
		// }

		packageExists(roots);
		System.out.println("end");

		// TestBuilder test = new TestBuilder(new
		// GitHistoryRefactoringMinerImpl(), "tmp8", refactoring);
		//
		// RefactoringPopulator.feedRefactoringsInstances(refactoring,
		// Systems.FSE.getValue(), test);
		//
		// test.assertExpectations();

	}

	private void packageExists(List<Root> roots) throws Exception {
		Dictionary<String, String> exists = new Hashtable<>();
		Hashtable<String, Ref> detected = new Hashtable<>();
		for (Root root : roots) {
			List<String> renamePackageRefactorings = getRenamePackage(root);
			if (root.sha1.equals("f77804dad35c13d9ff96456e85737883cf7ddd99"))
				if (renamePackageRefactorings.size() > 0) {
					String tempDir = "/Volumes/ADATA HV610/tmp1";
					GitServiceImpl gitService = new GitServiceImpl();
					String folder = tempDir + "/" + root.repository.substring(root.repository.lastIndexOf('/') + 1,
							root.repository.lastIndexOf('.'));
					try (Repository rep = gitService.cloneIfNotExists(folder,
							root.repository/* , m.branch */)) {
						RevWalk walk = new RevWalk(rep);
						try {
							RevCommit commit = walk.parseCommit(rep.resolve(root.sha1));
							//walk.parseCommit(commit);
							List<String> files = gitService.getFilesInCurrentRevision(rep, commit);
							walk.parseCommit(commit.getParent(0));
							List<String> baseFiles = gitService.getFilesInCurrentRevision(rep, commit.getParent(0));
							System.out.println(root.sha1);
							for (String ref : renamePackageRefactorings) {
								List<String> existedInRefactored = existRenamePackage(files, ref);
								List<String> existedInBase = existRenamePackage(baseFiles, ref);
								List<String> refactored= getRename(files,ref);
								if (existedInBase.size() > 0) {
									detected.put(ref, new Ref(ref, existedInBase, existedInRefactored,refactored));
								}

								// if (existed) {
								// System.out.println("EXIST:\t" + ref);
								// exists.put(root.sha1, ref);
								// } else {
								// System.out.println("NOT exist:\t" + ref);
								// }
							}

						} catch (Exception e) {
							System.out.println(e.getMessage());
						} finally {
							walk.dispose();
						}
						System.out.println("end");
						for (String key : detected.keySet()) {
							System.out.println(key);
							Ref element= detected.get(key);
							System.out.println("base:\t");
							for (String base : element.baseFiles) {
								
								System.out.println("base:\t"+base);
							}
							System.out.println("refactored:\t");
							for (String base : element.basePackageInRefactored) {
								
								System.out.println("refactored:\t"+base);
							}
							System.out.println("**:\t");
							for (String base : element.refactoredFiles) {
								
								System.out.println("**:\t"+base);
							}
							System.out.println(element.refactoredFiles.size()>0);
							System.out.println("-------------");
						}
					}
				}
		}
	}

	private List<String> getRenamePackage(Root root) {
		List<String> renamePackageRefactorings = new ArrayList<>();

		for (Refactoring ref : root.refactorings) {
			if (ref.type.startsWith("Rename Package"))
				renamePackageRefactorings.add(ref.description);
		}
		return renamePackageRefactorings;
	}

	private List<String> existRenamePackage(List<String> files, String ref) {
		List<String> detectedFiles = new ArrayList<>();
		String start = "Rename Package ";
		String end = " to ";
		for (String file : files) {

			String basePackage = ref.substring(start.length(), ref.indexOf(end)).replace(".", "/");
			if (file.contains(basePackage))
				// return true;
				detectedFiles.add(file);
		}
		return detectedFiles;
	}
	
	private List<String> getRename(List<String> files, String ref){
		List<String> detectedFiles = new ArrayList<>();
//		String end = "Rename Package ";
		String start = " to ";
		for (String file : files) {

			String basePackage = ref.substring(ref.indexOf(start)+start.length()).replace(".", "/");
			if (file.contains(basePackage))
				// return true;
				detectedFiles.add(file);
		}
		return detectedFiles;
	}
	

	private static class Ref {
		public String refactoring;
		public List<String> baseFiles;
		public List<String> refactoredFiles;
		public List<String> basePackageInRefactored;

		public Ref(String ref, List<String> base, List<String> refactored, List<String> basePackageInRefactored) {
			refactoring = ref;
			baseFiles = base;
			refactoredFiles = refactored;
			this.basePackageInRefactored= basePackageInRefactored;
		}
	}

}

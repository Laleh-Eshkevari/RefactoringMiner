package org.refactoringminer.test;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactoring;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Root;
import org.refactoringminer.test.RefactoringPopulator.Systems;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {
		List<Root> roots = new ArrayList<>();
		int refactoring = Refactorings.RenameLocalVariable.getValue();

		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "tmp1", refactoring);

		RefactoringPopulator.feedRefactoringsInstances(refactoring, Systems.FSE.getValue(), test);

		// mergeWithOriginalJson(RefactoringPopulator.prepareFSERefactorings(test,
		// refactoring));
//		mergeRepantResults();
		// setIgnored(RefactoringPopulator.prepareFSERefactorings(test,
		// refactoring));
		// RefactoringPopulator.printRefDiffResults(refactoring);
		 RefactoringPopulator.printRefDiffResults(refactoring);
		 test.assertExpectations();
	}

	private void mergeWithOriginalJson(List<Root> roots) {
		String pattern = "Rename Local Variable\\s(.+?)\\s:\\s(.+?)\\srenamed\\sto\\s(.+?)\\s:\\s(.+?)\\sin\\smethod\\s.+?\\s([A-Za-z_0-9]+).+in\\sclass(.+)";

		String REPANT = "/Users/matin/Documents/mergeRepant.json";
		ObjectMapper mapper = new ObjectMapper();

		try {
			List<Root> repantRoots = mapper.readValue(new File(REPANT),
					mapper.getTypeFactory().constructCollectionType(List.class, Root.class));
			int i = 0;
			for (Root repRoot : repantRoots) {
				// if(repRoot.sha1.equals("cf495c5560198de4e6f3556d6c40e3ce0dbf3868"))
				for (Root root : roots) {
					if (repRoot.sha1.equals(root.sha1)) {

						for (Refactoring repRef : repRoot.refactorings) {
							Matcher m;

							Pattern.compile(pattern).matcher(repRef.description).find();
							m = Pattern.compile(pattern).matcher(repRef.description);
							m.matches();

							String repOldVar = m.group(1);
							String repOldType = m.group(2);
							String repNewVar = m.group(3);
							String repNewType = m.group(4);
							String repMethod = m.group(5);
							String repClass = m.group(6);
							boolean exists = false;
							for (Refactoring rootReg : root.refactorings) {
								Pattern.compile(pattern).matcher(rootReg.description).find();
								m = Pattern.compile(pattern).matcher(rootReg.description);
								m.matches();

								String rOldVar = m.group(1);
								String rOldType = m.group(2);
								String rNewVar = m.group(3);
								String rNewType = m.group(4);
								String rMethod = m.group(5);
								String rClass = m.group(6);

								if (repOldVar.equals(rOldVar) && repOldType.equals(rOldType)
										&& repNewVar.equals(rNewVar) && repNewType.equals(rNewType)
										&& repMethod.equals(rMethod) && repClass.trim().equals(rClass.trim())) {
									if (rootReg.detectionTools == null)
										rootReg.detectionTools = "REPANT";
									else
										rootReg.detectionTools += ", REPANT";
									rootReg.ignored = false;
									rootReg.fileName = repRef.fileName;
									exists = true;

								}

							}
							if (exists == false)
								System.out.println(repRef.description);

						}

					}
				}
				System.out.println(i++);
			}
			mapper.writeValue(new File("/Users/matin/Documents/fMergeRepant.json"), roots);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private void setIgnored(List<Root> roots) {

		FileReader fileReader;
		int i = 0;
		try {
			fileReader = new FileReader("/Users/matin/Documents/renameVar/svnCommitFileMapping.txt");
			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				i++;
				String[] input = line.split("_DiffLine_");
				String before = input[1].split("-")[0];
				String after = input[1].split("-")[1];
				for (Root root : roots) {
					String svnRev = root.svnVersionMappings;
					try {
						if (Integer.parseInt(svnRev.split(" -> ")[0]) == Integer.parseInt(before)
								&& Integer.parseInt(svnRev.split(" -> ")[1]) == Integer.parseInt(after)) {
							// if (svnRev.split("-")[0].trim().equals(before) &&
							// svnRev.split("-")[1].trim().equals(after)) {
							for (Refactoring ref : root.refactorings) {
								if (ref.fileName != null) {
									if (ref.fileName.contains(input[0].replace("./", "")))
										ref.ignored = false;
								}
							}
						}
					} catch (Exception e) {
						System.out.println("ghalat ast digar");
					}

				}
			}
			// Always close files.
			bufferedReader.close();
		} catch (

		Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(new File("/Users/matin/Documents/newJson.json"), roots);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// private void addFile(List<Root> roots){
	// String REPANT = "/Users/matin/Documents/mergeRepant.json";
	// ObjectMapper mapper = new ObjectMapper();
	//
	// try {
	// List<Root> repantRoots = mapper.readValue(new File(REPANT),
	// mapper.getTypeFactory().constructCollectionType(List.class, Root.class));
	// int i = 0;
	// for (Root repRoot : repantRoots) {
	//// if(repRoot.sha1.equals("cf495c5560198de4e6f3556d6c40e3ce0dbf3868"))
	// for (Root root : roots) {
	// for (Root reproot : repantRoots) {
	// if(root.sha1.equals(repRoot.sha1)){
	// for (Refactoring repRef : repRoot.refactorings) {
	// for (Refactoring rootReg : root.refactorings) {
	// if(root)
	// }
	// }
	// }
	// }
	// }
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// System.err.println(e.getMessage());
	// e.printStackTrace();
	// }
	//
	// }
	private void mergeRepantResults() {
		ObjectMapper mapper = new ObjectMapper();

		String jsonTPFile = "/Users/matin/Documents/renameVar/tomcat new/oracle-LVD-tomcat-TP_20171101.json";
		String jsonAllFile = "/Users/matin/Documents/renameVar/tomcat new/tomcat-LVD-all-minus-TP_20171101.json";
		int i = 0;

		List<Root> tobeAdded = new ArrayList<>();

		try {
			List<Root> allFileroots = mapper.readValue(new File(jsonAllFile),
					mapper.getTypeFactory().constructCollectionType(List.class, Root.class));

			List<Root> tpFileroots = mapper.readValue(new File(jsonTPFile),
					mapper.getTypeFactory().constructCollectionType(List.class, Root.class));

			List<Root> allFilerootsFixed = fixRepeatition(allFileroots);
			List<Root> tpFilerootsFixed = fixRepeatition(tpFileroots);

			for (Root tpRoot : tpFilerootsFixed) {
				boolean exists = false;
				for (Root allRoot : allFilerootsFixed) {
					if (tpRoot.sha1.equals(allRoot.sha1)) {
						for (Refactoring tpRef : tpRoot.refactorings) {
							// for (Refactoring tpRef : tpRoot.refactorings) {
							// if(allRef.description.equals(tpRef.description)){
							// allRef.validation = "TP";
							// }
							// }

							allRoot.refactorings.add(tpRef);
							exists = true;
						}

					}

				}
				if (!exists) {
					i++;
					tobeAdded.add(tpRoot);
				}
			}
			allFilerootsFixed.addAll(tobeAdded);

			System.out.println(i);
			mapper.writeValue(new File("/Users/matin/Documents/mergeRepantT.json"), allFilerootsFixed);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<Root> fixRepeatition(List<Root> roots) {
		List<Root> newRoot = new ArrayList<>();
		int i = 1;
		for (Root innerRoot : roots) {
			innerRoot.id = i++;
		}
		Root tmp = new Root();
		for (Root root : roots) {
			if (root.id < 0)
				continue;
			tmp = root;
			root.id = -1;
			for (Root innerRoot : roots) {
				if (innerRoot.id > 0 && innerRoot.sha1.equals(root.sha1)) {
					for (Refactoring ref : innerRoot.refactorings) {
						tmp.refactorings.add(ref);
					}

					innerRoot.id = -1;
				}
			}
			newRoot.add(tmp);
		}

		return newRoot;
	}

}

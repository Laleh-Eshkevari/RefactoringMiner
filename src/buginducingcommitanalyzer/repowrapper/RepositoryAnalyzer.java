package buginducingcommitanalyzer.repowrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.*;

public class RepositoryAnalyzer {
	
	private final String name;
	private HashMap<String, Commit > allCommits;
	private static LinkedHashMap<String, Commit> sortedCommits = new LinkedHashMap<String, Commit>();
	private int totalRefactorings;
	// key: the commit in which the refactoring occurred
	// value: the UMLModel that correspond to the parent commit of the key
	private static HashMap<String, UMLModel> commitUmlModels = new HashMap<String, UMLModel>();
	
	
	public RepositoryAnalyzer(String name) {
		this.name = name;
		this.allCommits = new HashMap<String, Commit >();
		this.totalRefactorings = 0;
	}

	public String getName() {
		return this.name;
	}

	public  HashMap<String, Commit> getAllCommits() {
		return this.allCommits;
	}
	
	public int getTotalRefactorings() {
		return this.totalRefactorings;
	}
	
	public static HashMap<String, UMLModel> getCommitUmlModels() {
		return commitUmlModels;
	}

	public void sortHashMapByValues() {
	    List<String> mapKeys = new ArrayList<>(this.allCommits.keySet());
	    List<Commit> mapValues = new ArrayList<>(this.allCommits.values());
	    
	    Collections.sort(mapValues);
	    Collections.sort(mapKeys);

	    Iterator<Commit> valueIt = mapValues.iterator();
	    while (valueIt.hasNext()) {
	        Commit val = valueIt.next();
	        Iterator<String> keyIt = mapKeys.iterator();

	        while (keyIt.hasNext()) {
	            String key = keyIt.next();
	            Commit comp1 = this.allCommits.get(key);
	            Commit comp2 = val;

	            if (comp1.equals(comp2)) {
	                keyIt.remove();
	                sortedCommits.put(key, val);
	                break;
	            }
	        }
	    }
	    

	    List<Commit> sortedValues = new ArrayList<>(sortedCommits.values());
	    sortedValues.get(sortedValues.size()-1).setParent(null);
	    for(int i=sortedValues.size()-1; i > 0; i-- ){
	    	Commit parent = sortedValues.get(i);
	    	Commit child = sortedValues.get(i-1);
	    	sortedValues.get(i-1).setParent(sortedValues.get(i));
	    }

	}
	
	private String summarizeBugInducingCommits(){
		
		StringBuilder sb = new StringBuilder();
		int bug_inducing_commits = 0;
		int bug_inducing_commits_with_fix = 0;
		int bug_inducing_commits_with_refactorings = 0;
		int refactorings_in_bug_inducing_commits = 0;
		
		for(String hashId:allCommits.keySet() ){
			Commit c =  allCommits.get(hashId);
			if(c.isBugInducingCommit()){
				bug_inducing_commits++;
				if(c.hasFix()){
					bug_inducing_commits_with_fix++;
				}
				if(c.getRefactorings().size()>0){
					bug_inducing_commits_with_refactorings++;
					refactorings_in_bug_inducing_commits = refactorings_in_bug_inducing_commits + 
															c.getRefactorings().size();
				}
			}
			
		}
		
		sb.append("Total refactorings: " + this.totalRefactorings +"\n");
		System.out.println("Total refactorings: " + this.totalRefactorings);
		
		System.out.println("Total number of bug inducing commits: " + bug_inducing_commits );
		sb.append( "Total number of bug inducing commits: " + bug_inducing_commits + "\n");
		
		System.out.println("Total number of bug inducing commits with fix: " + bug_inducing_commits_with_fix );
		sb.append("Total number of bug inducing commits with fix: " + bug_inducing_commits_with_fix  + "\n");
		
		System.out.println("Total number of bug inducing commits with refactoring: " + bug_inducing_commits_with_refactorings );
		sb.append("Total number of bug inducing commits with refactoring: " + bug_inducing_commits_with_refactorings +"\n");
		
		System.out.println("Total number of refactorings in bug inducing commits: " + refactorings_in_bug_inducing_commits );
		sb.append("Total number of refactorings in bug inducing commits: " + refactorings_in_bug_inducing_commits +"\n");
		return sb.toString();
	}
	
	private String summarizeBugFixingCommits(){
		StringBuilder sb = new StringBuilder();
		Set<Commit> fixingCommits = new HashSet<Commit >();
		int bug_fixinig_commits_with_refactoring = 0;
		int refactorings_in_bug_fixinig_commits = 0;
		
		for(String hashId:allCommits.keySet() ){
			Commit c= allCommits.get(hashId);
			if(c.isBugInducingCommit() && c.hasFix()){
				for(Commit fix:c.getFixedIn()){
					fixingCommits.add(fix);
					if(fix.getRefactorings().size()>0){
						bug_fixinig_commits_with_refactoring++;
						refactorings_in_bug_fixinig_commits = refactorings_in_bug_fixinig_commits + fix.getRefactorings().size();
					}
				}
			}
		}
		
		int bug_fixinig_commits=fixingCommits.size();

		System.out.println("Total number of bug fixing commits: " + bug_fixinig_commits );
		sb.append("Total number of bug fixing commits: " + bug_fixinig_commits + "\n");
		
		System.out.println("Total number of bug fixing commits with refactoring: " + bug_fixinig_commits_with_refactoring );
		sb.append("Total number of bug fixing commits with refactoring: " + bug_fixinig_commits_with_refactoring + "\n");
		
		System.out.println("Total number of refactorings in bug fixing commits: " + refactorings_in_bug_fixinig_commits );
		sb.append("Total number of refactorings in bug fixing commits: " + refactorings_in_bug_fixinig_commits + "\n");
		return sb.toString();
		
	}

	public void setTotalRefactorings(int totalRefactorings) {
		this.totalRefactorings = totalRefactorings;
	}
	
	public void analyzeEntityHistoryBeforeRefactoring(Repository repo){
		PrintWriter pw1;
		try {
			pw1 = new PrintWriter(new File(this.name+"_fixsBeforeRefactorings.csv"));
			pw1.write("commit_id,commit_date,is_fixing_commit,is_bug_inducing,refactorings,previous_fixes_on_refactored_entity,distances"+"\n");
			for(String hashId:this.sortedCommits.keySet() ){
				Commit c= this.sortedCommits.get(hashId);
				if(c.getRefactorings().size()>0){
					for(Refactoring ref: c.getRefactorings()){
						ArrayList<String> files = new ArrayList<String>();
						ArrayList<String> namesBeforeRef = new ArrayList<String>();
						if(ref instanceof ExtractOperationRefactoring){
							files.add(((ExtractOperationRefactoring)ref).getExtractedFromOperation().getSourceFile());
							namesBeforeRef.add( ((ExtractOperationRefactoring)ref).getExtractedFromOperation().getName());
						}else if(ref instanceof PullUpOperationRefactoring){
							files.add(((PullUpOperationRefactoring)ref).getOriginalOperation().getSourceFile());
							namesBeforeRef.add( ((PullUpOperationRefactoring)ref).getOriginalOperation().getName());
						}else if(ref instanceof PushDownOperationRefactoring){
							files.add(((PushDownOperationRefactoring)ref).getOriginalOperation().getSourceFile());
							namesBeforeRef.add( ((PushDownOperationRefactoring)ref).getOriginalOperation().getName());
						}else if(ref instanceof MoveOperationRefactoring && !(ref instanceof PushDownOperationRefactoring) && !(ref instanceof PullUpOperationRefactoring)){
							files.add(((MoveOperationRefactoring)ref).getOriginalOperation().getSourceFile());
							namesBeforeRef.add( ((MoveOperationRefactoring)ref).getOriginalOperation().getName());
						}else if(ref instanceof InlineOperationRefactoring){ // we need the inlineTo as well   
							files.add(((InlineOperationRefactoring)ref).getInlinedOperation().getSourceFile());
							files.add(((InlineOperationRefactoring)ref).getInlinedToOperation().getSourceFile());
							namesBeforeRef.add( ((InlineOperationRefactoring)ref).getInlinedOperation().getName());
							namesBeforeRef.add(((InlineOperationRefactoring)ref).getInlinedToOperation().getName());
						}else if(ref instanceof RenameOperationRefactoring){ 
							files.add(((RenameOperationRefactoring)ref).getOriginalOperation().getSourceFile());
							namesBeforeRef.add( ((RenameOperationRefactoring)ref).getOriginalOperation().getName());
						}else if(ref instanceof MoveClassRefactoring){ 
							files.add(((MoveClassRefactoring)ref).getOriginalClass().getSourceFile());                                          
							namesBeforeRef.add( ((MoveClassRefactoring)ref).getOriginalClassName());
						}else if(ref instanceof RenameClassRefactoring){ 
							files.add(((RenameClassRefactoring)ref).getOriginalClass().getSourceFile());                                        
							namesBeforeRef.add( ((RenameClassRefactoring)ref).getOriginalClassName());
						}else if(ref instanceof PushDownAttributeRefactoring){ 
							files.add(((PushDownAttributeRefactoring)ref).getOriginalAttribute().getSourceFile());
							namesBeforeRef.add( ((PushDownAttributeRefactoring)ref).getName());
						}else if(ref instanceof PullUpAttributeRefactoring){ 
							files.add(((PullUpAttributeRefactoring)ref).getOriginalAttribute().getSourceFile());
							namesBeforeRef.add( ((PullUpAttributeRefactoring)ref).getName());
						}else if(ref instanceof MoveAttributeRefactoring && !(ref instanceof PullUpAttributeRefactoring) && !(ref instanceof PushDownAttributeRefactoring)){ 
							files.add(((MoveAttributeRefactoring)ref).getOriginalAttribute().getSourceFile());
							namesBeforeRef.add( ((MoveAttributeRefactoring)ref).getName());
						}else if(ref instanceof  ExtractSuperclassRefactoring){  // also support extract interface
							for(UMLClass aSubClass: ((ExtractSuperclassRefactoring)ref).getSubclasses()){
								files.add(aSubClass.getSourceFile());                                  
								namesBeforeRef.add(aSubClass.getName()); 
							}                      
						}
						for(int i=0 ; i<= files.size()-1 ; i++){
							pw1.write(hashId+","+c.getDateInString()+","+c.isFixingCommit()+","+ c.isBugInducingCommit()+","+ref.getName()+",");
							String toPrint=findPreviousFixexOnRefacoredEntity(repo, c, ref, files.get(i), namesBeforeRef.get(i));
							if(toPrint == null){
								pw1.write("[],[]"+"\n");
							}else{
								pw1.write(toPrint);
							}
						}
						
					}
				}
			}
			pw1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String findPreviousFixexOnRefacoredEntity(Repository repo,  Commit c, Refactoring ref, String file,
			String nameBeforeRef) {
		Set<Commit> passedFixCommits =this.findAllPassedFixedCommitOnFile(c, file);
		if(passedFixCommits.size()>0){
			
			Set<Commit> previousFixesOnRefactoredEntity= new HashSet<Commit>();
			for(Commit passedFix: passedFixCommits){
				Commit parrent= passedFix.getParent();
				String diffAsString = GitUtil.getFileDiff(repo, parrent.getCommitHash(), passedFix.getCommitHash(), file);
				List<String> diffLineRanges = this.findChangedLines(diffAsString);
				List<String> linesInNewFiles = this.extractFileRangeInNewVersion(diffLineRanges);
				if(ref instanceof ExtractOperationRefactoring || 
					ref instanceof InlineOperationRefactoring ||
					ref instanceof MoveOperationRefactoring ||
					ref instanceof RenameOperationRefactoring){
					Set<UMLOperation> umlOperations = this.findUMLOperation(nameBeforeRef, file, passedFix.getCommitHash());
					if(umlOperations.size() > 1){
						System.err.println(" =================> WE HAVE OVERLOADING METHOD, methodname: "+ nameBeforeRef +  "  in file: "+ file + " in commit: " + passedFix.getCommitHash());
					}
					boolean hasOverlapWithChange=false;
					for(UMLOperation aUMLOperation : umlOperations){
						Set<Overlap> overlaps= findChangedLinesdAndEntityOverlaps(linesInNewFiles, aUMLOperation.getStartLine() , aUMLOperation.getEndLine());
						if(overlaps.contains(Overlap.CHANGE_INSIDE_ENTITY) || 
								overlaps.contains(Overlap.ENTITY_INSIDE_CHANGE) ||
								overlaps.contains(Overlap.ENTITY_AND_CHANGE_HAVE_PARTIAL_OVERLAP)){
							hasOverlapWithChange=true;
						}
					}
					if(hasOverlapWithChange){
						previousFixesOnRefactoredEntity.add(passedFix);
					}	
				}else if(ref instanceof MoveClassRefactoring || 
						ref instanceof RenameClassRefactoring ||
						ref instanceof ExtractSuperclassRefactoring){
					
					Set<UMLClass> umlClasses = this.findUMLClasses(nameBeforeRef, file, passedFix.getCommitHash());
					boolean hasOverlapWithChange=false;
					for(UMLClass aUMLClass : umlClasses){
						Set<Overlap> overlaps= findChangedLinesdAndEntityOverlaps(linesInNewFiles, aUMLClass.getStartLine() , aUMLClass.getEndLine());
						if(overlaps.contains(Overlap.CHANGE_INSIDE_ENTITY) || 
								overlaps.contains(Overlap.ENTITY_INSIDE_CHANGE) ||
								overlaps.contains(Overlap.ENTITY_AND_CHANGE_HAVE_PARTIAL_OVERLAP)){
							hasOverlapWithChange=true;
						}
					}
					if(hasOverlapWithChange){
						previousFixesOnRefactoredEntity.add(passedFix);
					}	
				}else if (ref instanceof MoveAttributeRefactoring){
					if(diffAsString.contains("this."+ nameBeforeRef)){
						previousFixesOnRefactoredEntity.add(passedFix);
					}else if(diffAsString.contains( nameBeforeRef+ ".")){
						previousFixesOnRefactoredEntity.add(passedFix);
					}
				} 
				

			}
			String temp = "[";
			String distance = "[";
			for(Commit aCommit: previousFixesOnRefactoredEntity){
				temp = temp + aCommit.getCommitHash()+";";
				distance = distance + "-"+getCommitDistance(c, aCommit) +";";
			}
			temp = temp + "],";
			temp = temp.replace(";],", "],");
			
			distance = distance+ "],";
			distance = distance.replace(";],", "],");
			
			
			return temp+ distance+ "\n";
			
		}else{
			return null;
		}
	}
	
	private Set<UMLClass> findUMLClasses(String nameBeforeRef, String file, String commitHash) {
		Set<UMLClass> umlClasses= new HashSet<UMLClass>();
		if(commitUmlModels.containsKey(commitHash)){
			UMLModel umlModel = commitUmlModels.get(commitHash);
			for (UMLClass aClass: umlModel.getClassList()){
				if(aClass.getSourceFile().contentEquals(file) && aClass.getName().contentEquals(nameBeforeRef)){
					umlClasses.add(aClass);
				}
			}
			return umlClasses;
		}else 
			return null;
	}

	private Set<Overlap> findChangedLinesdAndEntityOverlaps(List<String> lineRanges, int startLine, int endLine) {
		Set<Overlap> overlaps = new HashSet<Overlap>();
		for(String ranges: lineRanges){
			int start=Integer.valueOf(ranges.split(",")[0]);
			int end=Integer.valueOf(ranges.split(",")[1]);
			//System.out.println("\n\t\t lined changed: [" + start + "," + end +"]");
			Overlap overlap=this.findChangedLinedAndEntityOverlap(start, end, startLine, endLine );
			overlaps.add(overlap);
		}
		return overlaps;
	}

	private Overlap findChangedLinedAndEntityOverlap(int changeStart, int changeEnd, int entityStart, int entityEnd) {
		Overlap status ;
		if(entityStart >= changeStart &&  entityEnd <= changeEnd){
			// changes inside entity
			status=Overlap.ENTITY_INSIDE_CHANGE;
		}else if(entityStart <= changeStart && entityEnd >= changeEnd){
			// entity inside changes
			status=Overlap.CHANGE_INSIDE_ENTITY;
		} else if(entityStart < changeStart && entityEnd < changeStart){
			// entity before changes
			status=Overlap.ENTITY_BEFORE_OUTSIDE_CHANGE;
		}else if(changeEnd < entityStart && changeStart < entityStart){
			// change before entity
			status=Overlap.ENTITY_AFTER_OUTSIDE_CHANGE;
		}else {
			status=Overlap.ENTITY_AND_CHANGE_HAVE_PARTIAL_OVERLAP;
		}
		
		return status;
	}

	private Set<UMLOperation> findUMLOperation(String nameBeforeRef, String file, String commitHash) {
		Set<UMLOperation> umlOperations= new HashSet<UMLOperation>();
		if(commitUmlModels.containsKey(commitHash)){
			UMLModel umlModel = commitUmlModels.get(commitHash);
			for (UMLClass aClass: umlModel.getClassList()){
				if(aClass.getSourceFile().contentEquals(file)){
					for(UMLOperation aUMLOperation : aClass.getOperations()){
						if(aUMLOperation.getName().contentEquals(nameBeforeRef)){
							umlOperations.add(aUMLOperation);
						}
					}
					break;
				}
			}
			
			return umlOperations;
		}else 
			return null;
	}

	private Set<Commit> findAllPassedFixedCommitOnFile(Commit current, String targetFile) {
		Set<Commit> keep = new HashSet<Commit>();
		for(String hashId: sortedCommits.keySet() ){
			Commit aCommit = sortedCommits.get(hashId);
			if(current.compareTo(aCommit) < 0){
				if(aCommit.getChangedFiles().contains(targetFile) && aCommit.isFixingCommit()) {
					keep.add(aCommit);
				}
			}
		}
		return keep;
	}

	public void summarize(){
		try {
		PrintWriter pw2 = new PrintWriter(new File(this.name+".txt"));
		pw2.write(this.summarizeBugInducingCommits());
		pw2.write(this.summarizeBugFixingCommits());
		pw2.write(this.summerizeRefactroingTypes());
		pw2.write(this.summerizeRefactroingTypesInBugInducingCommits());
		System.out.println();
		PrintWriter pw1 = new PrintWriter(new File(this.name+".csv"));
		pw1.write("commit_id,commit_date,is_fixing_commit,is_bug_inducing,fixed_in,fixed_distance,refactorings,line_overlap_withBugFix_in_commits,num_files_changed"+"\n");
		for(String hashId:allCommits.keySet() ){
			Commit c= allCommits.get(hashId);
			String print = c.toString(); 
			System.out.println(c.toString());
			pw2.write(print);
			printToFile(pw1, c);
		}
		pw1.close();
		pw2.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	private void printToFile(PrintWriter pw, Commit c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getCommitHash() +  "," +c.getDateInString()+ ",");
		sb.append(c.isFixingCommit() + ",");
		sb.append(c.isBugInducingCommit() +",");
		
		if(c.hasFix()){
			String fixes="[";
			String distance="[";
			for(Commit fix:c.getFixedIn()){
				fixes= fixes + fix.getCommitHash()+ ";";
				distance = distance + RepositoryAnalyzer.getCommitDistance(c, fix)+";";
			}
			fixes = fixes +"],";
			distance = distance +"],";
			sb.append(fixes.replace(";],", "],"));
			sb.append(distance.replace(";],", "],"));
		}else{
			sb.append("[],[],");
		}
		
		
		if(c.getRefactorings().size()>0){
			String refs= "[";
			for (Refactoring ref : c.getRefactorings()) {
				refs= refs+ ref.getName()+";";
			}
			refs = refs + "],";
			sb.append(refs.replace(";],", "],"));
		}else{
			sb.append("[],");
		}
		
		if(c.getRefactoingsAndFixOverlap().size()>0){
			String overlaps = "[";
			for(Commit overlap: c.getRefactoingsAndFixOverlap().keySet()){
				overlaps= overlaps+ overlap.getCommitHash()+";";
			}
			overlaps = overlaps + "],";
			sb.append(overlaps.replace(";],", "],"));
		}else{
			sb.append("[],");
		}
		if( c.getChangedFiles().size()>0)
			sb.append(c.getChangedFiles().size()+"\n");
		else{
			sb.append(",\n");
		}
		pw.write(sb.toString());
	}

	private String summerizeRefactroingTypes() {
		StringBuilder sb = new StringBuilder();
		int rename_method_counter = 0;
		int inline_method_counter = 0;
		int move_method_counter = 0;
		int pull_up_method_counter = 0;
		int move_class_counter = 0;
		int rename_class_counter = 0;
		int extract_superclass_counter = 0;
		int push_down_attribute_counter = 0;
		int pull_up_attribute_counter = 0;
		int move_attribute_counter = 0;
		int extract_interface_counter = 0;
		int extract_method_counter = 0;
		
		for(String hashId:allCommits.keySet() ){
			Commit c= allCommits.get(hashId);
			if(c.getRefactorings().size()>0){
				
				for(Refactoring ref: c.getRefactorings()){
					if (ref.getName().equals(RefactoringType.RENAME_METHOD.getDisplayName())){
						rename_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.INLINE_OPERATION.getDisplayName())){
						inline_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_OPERATION.getDisplayName())){
						move_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.PULL_UP_OPERATION.getDisplayName())){
						pull_up_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_CLASS.getDisplayName())){
						move_class_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_SUPERCLASS.getDisplayName())){
						extract_superclass_counter ++;
					}else if (ref.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName())){
						push_down_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
						move_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_INTERFACE.getDisplayName())){
						extract_interface_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_OPERATION.getDisplayName())){
						extract_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName())){
						pull_up_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.RENAME_CLASS.getDisplayName())){
						rename_class_counter ++;
					}
				}
			}
		}
		System.out.println("\n Refactorings details: ");
		sb.append("\n Refactorings details: "+ "\n");
		System.out.println("\t Total Rename_Method: " + rename_method_counter);
		sb.append("\t Total Rename_Method: " + rename_method_counter+ "\n");
		System.out.println("\t Total Inline_Method: " + inline_method_counter);
		sb.append("\t Total Inline_Method: " + inline_method_counter+ "\n");
		System.out.println("\t Total Move_Method: " + move_method_counter);
		sb.append("\t Total Move_Method: " + move_method_counter+ "\n");
		System.out.println("\t Total Pull_Up_Method: " + pull_up_method_counter);
		sb.append("\t Total Pull_Up_Method: " + pull_up_method_counter+ "\n");
		System.out.println("\t Total Move_Class: " + move_class_counter);
		sb.append("\t Total Move_Class: " + move_class_counter+ "\n");
		System.out.println("\t Total Extract_Superclass: " + extract_superclass_counter);
		sb.append("\t Total Extract_Superclass: " + extract_superclass_counter+ "\n");
		System.out.println("\t Total Push_Down_Attribute: " + push_down_attribute_counter);
		sb.append("\t Total Push_Down_Attribute: " + push_down_attribute_counter+ "\n");
		System.out.println("\t Total Pull_Up_Attribute: " + pull_up_attribute_counter); 
		sb.append("\t Total Pull_Up_Attribute: " + pull_up_attribute_counter+ "\n");
		System.out.println("\t Total Move_Attribute: " + move_attribute_counter);
		sb.append("\t Total Move_Attribute: " + move_attribute_counter+ "\n");
		System.out.println("\t Total Extract_Interface: " + extract_interface_counter);
		sb.append("\t Total Extract_Interface: " + extract_interface_counter+ "\n");
		System.out.println("\t Total Extract_Method: " + extract_method_counter);
		sb.append("\t Total Extract_Method: " + extract_method_counter+ "\n");
		System.out.println("\t Total Rename_Class: " + rename_class_counter);
		sb.append("\t Total Rename_Class: " + rename_class_counter+ "\n");
		
		return sb.toString();
	}

	private String summerizeRefactroingTypesInBugInducingCommits() {
		StringBuilder sb = new StringBuilder();
		int rename_method_counter = 0;
		int inline_method_counter = 0;
		int move_method_counter = 0;
		int pull_up_method_counter = 0;
		int move_class_counter = 0;
		int rename_class_counter = 0;
		int extract_superclass_counter = 0;
		int push_down_attribute_counter = 0;
		int pull_up_attribute_counter = 0;
		int move_attribute_counter = 0;
		int extract_interface_counter = 0;
		int extract_method_counter = 0;
		
		for(String hashId:allCommits.keySet() ){
			Commit c= allCommits.get(hashId);
			if(c.isBugInducingCommit() && c.getRefactorings().size()>0){
				for(Refactoring ref: c.getRefactorings()){
					if (ref.getName().equals(RefactoringType.RENAME_METHOD.getDisplayName())){
						rename_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.INLINE_OPERATION.getDisplayName())){
						inline_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_OPERATION.getDisplayName())){
						move_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.PULL_UP_OPERATION.getDisplayName())){
						pull_up_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_CLASS.getDisplayName())){
						move_class_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_SUPERCLASS.getDisplayName())){
						extract_superclass_counter ++;
					}else if (ref.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName())){
						push_down_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
						move_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_INTERFACE.getDisplayName())){
						extract_interface_counter ++;
					}else if (ref.getName().equals(RefactoringType.EXTRACT_OPERATION.getDisplayName())){
						extract_method_counter ++;
					}else if (ref.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName())){
						pull_up_attribute_counter ++;
					}else if (ref.getName().equals(RefactoringType.RENAME_CLASS.getDisplayName())){
						rename_class_counter ++;
					}
				}
			}
		}
		System.out.println("\n Refactorings in bug inducing commits details: ");
		sb.append("\n Refactorings in bug inducing commits details: "+ "\n");
		System.out.println("\t Total Rename_Method: " + rename_method_counter);
		sb.append("\t Total Rename_Method: " + rename_method_counter+ "\n");
		System.out.println("\t Total Inline_Method: " + inline_method_counter);
		sb.append("\t Total Inline_Method: " + inline_method_counter+ "\n");
		System.out.println("\t Total Move_Method: " + move_method_counter);
		sb.append("\t Total Move_Method: " + move_method_counter+ "\n");
		System.out.println("\t Total Pull_Up_Method: " + pull_up_method_counter);
		sb.append("\t Total Pull_Up_Method: " + pull_up_method_counter+ "\n");
		System.out.println("\t Total Move_Class: " + move_class_counter);
		sb.append("\t Total Move_Class: " + move_class_counter+ "\n");
		System.out.println("\t Total Extract_Superclass: " + extract_superclass_counter);
		sb.append("\t Total Extract_Superclass: " + extract_superclass_counter+ "\n");
		System.out.println("\t Total Push_Down_Attribute: " + push_down_attribute_counter);
		sb.append("\t Total Push_Down_Attribute: " + push_down_attribute_counter+ "\n");
		System.out.println("\t Total Pull_Up_Attribute: " + pull_up_attribute_counter); 
		sb.append("\t Total Pull_Up_Attribute: " + pull_up_attribute_counter+ "\n");
		System.out.println("\t Total Move_Attribute: " + move_attribute_counter);
		sb.append("\t Total Move_Attribute: " + move_attribute_counter+ "\n");
		System.out.println("\t Total Extract_Interface: " + extract_interface_counter);
		sb.append("\t Total Extract_Interface: " + extract_interface_counter+ "\n");
		System.out.println("\t Total Extract_Method: " + extract_method_counter);
		sb.append("\t Total Extract_Method: " + extract_method_counter+ "\n");
		System.out.println("\t Total Rename_Class: " + rename_class_counter);
		sb.append("\t Total Rename_Class: " + rename_class_counter+ "\n");
		
		return sb.toString();
	}
	
	public void findLineOverlapsBetweenRefsAndFixes(Repository repo){
		System.out.println("===============================================");
		for(String hashId:allCommits.keySet() ){
			Commit c= allCommits.get(hashId);
			if(c.isBugInducingCommit() && c.getRefactorings().size()>0 && c.hasFix()){
				System.out.println("Processing commit: "+ c.getCommitHash());
				HashMap<String, List<Refactoring>> afterRefFilePathsToRefactoringMap =this.ExtractAfterRefactroingFiles(c.getRefactorings());
				for(Commit fixingCommit:c.getFixedIn()){
					System.out.println("\t  it is fixed it: " +c.getCommitHash());
					HashMap<String, List<Refactoring>> fileIntersectionWithRefs= this.findIntersectionBetweenFiles(afterRefFilePathsToRefactoringMap, fixingCommit.getChangedFiles());
					if(fileIntersectionWithRefs.size() ==0 ){
						System.out.println(" \t no intersection between changed files and files constain refactoring");
					}
					for(String file: fileIntersectionWithRefs.keySet()){
						System.out.println("\t Processing fixed file: "+ file + "   from fixing commit: "+ fixingCommit.getCommitHash());	
						String diffAsString =GitUtil.getFileDiff(repo, hashId, fixingCommit.getCommitHash(), file);
						List<String> diffLineRanges = this.findChangedLines(diffAsString);
						//System.out.println(" \t diff lines size: "+ diffLineRanges.size());
						if(diffLineRanges.size()>0){
							List<String> linesInNewFiles = this.extractFileRangeInNewVersion(diffLineRanges);
							for(Refactoring r: fileIntersectionWithRefs.get(file)){
								System.out.println("\t\t "+ r.toString());
								Overlap overlap=this.findLineOverlaps(linesInNewFiles, r);
								if(overlap != Overlap.NO_OVERLAPS){
									OverlapInfo info= new OverlapInfo(overlap, r);
									if(c.getRefactoingsAndFixOverlap().containsKey(fixingCommit)){
										c.getRefactoingsAndFixOverlap().get(fixingCommit).add(info);
									}else{
										Set<OverlapInfo> aSet= new HashSet<OverlapInfo>();
										aSet.add(info);
										c.getRefactoingsAndFixOverlap().put(fixingCommit, aSet);
									}
								}else if(r.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName()) || 
										 r.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName()) ||
										 r.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
									if(diffAsString.contains("this."+((MoveAttributeRefactoring)r).getMovedAttribute().getName())){ // since the other two are subtypes of MoveAttributeRefactoring
										System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% POTENTIAL OVERLAP");
									}
								}
							}	
						}
					}
				}
			}
		}
	}
	
	private List<String> extractFileRangeInNewVersion(List<String> diffLineRanges) {
		List<String> newLines=new ArrayList<String>();
		for(String lineMpping: diffLineRanges ){ // lineMpping = -26,10 +31,18 
			String[] newFileLines=lineMpping.split(" ")[1].split(","); // +31,18
			int startLine = Math.abs(Integer.valueOf(newFileLines[0]));
			int length = Integer.valueOf(newFileLines[1]);
			int endLine = startLine+ length;
			//System.out.println(lineMpping + "  %%%%%%%%%%%%%%%%%%%%%%%  "+ startLine+ ","+ endLine);
			newLines.add(startLine+ ","+ endLine ); // [31, 49]
		}
		return newLines;
	}

	private Overlap findLineOverlaps(List<String> lineRanges, Refactoring ref) {

		int startLineAfterRef=-1;
		int endLineAfterRef=-1;
		int startLineAfterRef2=-1;
		int endLineAfterRef2=-1;
		if (ref.getName().equals(RefactoringType.RENAME_METHOD.getDisplayName())){				
			startLineAfterRef = ((RenameOperationRefactoring) ref).getRenamedOperation().getStartLine();
			endLineAfterRef= ((RenameOperationRefactoring) ref).getRenamedOperation().getEndLine();
		}else if (ref.getName().equals(RefactoringType.INLINE_OPERATION.getDisplayName())){ 
			startLineAfterRef=((InlineOperationRefactoring) ref).getInlinedToOperation().getStartLine();
			endLineAfterRef=((InlineOperationRefactoring) ref).getInlinedToOperation().getEndLine();
		}else if (ref.getName().equals(RefactoringType.MOVE_OPERATION.getDisplayName())){
			startLineAfterRef = ((MoveOperationRefactoring) ref).getMovedOperation().getStartLine();
			endLineAfterRef = ((MoveOperationRefactoring) ref).getMovedOperation().getEndLine();
		}else if (ref.getName().equals(RefactoringType.PULL_UP_OPERATION.getDisplayName())){
			startLineAfterRef = ((PullUpOperationRefactoring) ref).getMovedOperation().getStartLine();
			endLineAfterRef = ((PullUpOperationRefactoring) ref).getMovedOperation().getEndLine();
		}else if (ref.getName().equals(RefactoringType.MOVE_CLASS.getDisplayName())){  // NEED interpretation
			startLineAfterRef = ((MoveClassRefactoring) ref).getMovedClass().getStartLine();
			endLineAfterRef = ((MoveClassRefactoring) ref).getMovedClass().getEndLine();
		}else if (ref.getName().equals(RefactoringType.EXTRACT_SUPERCLASS.getDisplayName())){  // how about the subclasses?
			startLineAfterRef = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getStartLine();
			endLineAfterRef = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getEndLine();
		}else if (ref.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName())){
			startLineAfterRef = ((PushDownAttributeRefactoring) ref).getMovedAttribute().getStartLine();
			endLineAfterRef = ((PushDownAttributeRefactoring) ref).getMovedAttribute().getEndLine();
		}else if (ref.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
			startLineAfterRef = ((MoveAttributeRefactoring) ref).getMovedAttribute().getStartLine();
			startLineAfterRef = ((MoveAttributeRefactoring) ref).getMovedAttribute().getEndLine();
		}else if (ref.getName().equals(RefactoringType.EXTRACT_INTERFACE.getDisplayName())){ // how about the subclasses?
			startLineAfterRef = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getStartLine();
			endLineAfterRef = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getEndLine();
		}else if (ref.getName().equals(RefactoringType.EXTRACT_OPERATION.getDisplayName())){  
			if(((ExtractOperationRefactoring) ref).getExtractedFromOperationInNewVersion() !=null){
				startLineAfterRef2 = ((ExtractOperationRefactoring) ref).getExtractedFromOperationInNewVersion().getStartLine(); 
				endLineAfterRef2 = ((ExtractOperationRefactoring) ref).getExtractedFromOperationInNewVersion().getEndLine();
				
			}
			startLineAfterRef = ((ExtractOperationRefactoring) ref).getExtractedOperation().getStartLine(); 
			endLineAfterRef = ((ExtractOperationRefactoring) ref).getExtractedOperation().getEndLine();
		}else if (ref.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName())){
			startLineAfterRef = ((PullUpAttributeRefactoring) ref).getMovedAttribute().getStartLine();
			endLineAfterRef = ((PullUpAttributeRefactoring) ref).getMovedAttribute().getEndLine();
		}else if (ref.getName().equals(RefactoringType.RENAME_CLASS.getDisplayName())){ // NEED interpretation
			startLineAfterRef = ((RenameClassRefactoring) ref).getRenamedUMlClass().getStartLine();
			endLineAfterRef = ((RenameClassRefactoring) ref).getRenamedUMlClass().getEndLine();
		}else{
			System.err.println("NO SUCH REFACTORING: " + ref.toString());
		}
		System.out.println("\t\t Lines in refactored segments: ["+ startLineAfterRef + ","+ endLineAfterRef + "]");
		if(startLineAfterRef2 !=-1){
			System.out.println("\t\t Lines in refactored segments: ["+ startLineAfterRef2 + ","+ endLineAfterRef2 + "]");	
		}
		Overlap overlap = null;
		Set<Overlap> overlaps = new HashSet<Overlap>();
		for(String ranges: lineRanges){
			int start=Integer.valueOf(ranges.split(",")[0]);
			int end=Integer.valueOf(ranges.split(",")[1]);
			System.out.println("\n\t\t lined changed: [" + start + "," + end +"]");
			overlap=this.changedLinesAreInsideRef(start, end, startLineAfterRef, endLineAfterRef, startLineAfterRef2, endLineAfterRef2);
			if(overlap == null){
				overlap=this.changedLinesAreOutsideRef(start, end, startLineAfterRef, endLineAfterRef, startLineAfterRef2, endLineAfterRef2);
			}else{
				overlaps.add(overlap);
				//break;
			}
			if(overlap == null){
				overlap = Overlap.REF_AND_CHANGE_HAVE_PARTIAL_OVERLAP;
				overlaps.add(overlap);
				//break;
			}else{
				overlaps.add(overlap);
			}
		}
		// return if there is any overlap in overlaps
		if(overlaps.contains(Overlap.CHANGE_INSIDE_REF)){
			System.out.println("===============>"+ Overlap.CHANGE_INSIDE_REF);
			return Overlap.CHANGE_INSIDE_REF;
		}else if(overlaps.contains(Overlap.REF_INSIDE_CHANGE)){
			System.out.println("===============>"+ Overlap.REF_INSIDE_CHANGE);
			return Overlap.REF_INSIDE_CHANGE;
		}else if(overlaps.contains(Overlap.REF_AND_CHANGE_HAVE_PARTIAL_OVERLAP)){
			System.out.println("===============>"+ Overlap.REF_AND_CHANGE_HAVE_PARTIAL_OVERLAP);
			return Overlap.REF_AND_CHANGE_HAVE_PARTIAL_OVERLAP;
		}else{
			System.out.println("===============>"+ Overlap.NO_OVERLAPS);
			return Overlap.NO_OVERLAPS;
		}
	}

	private Overlap changedLinesAreOutsideRef(int start, int end, int startLineAfterRef, int endLineAfterRef, int startLineAfterRef2, int endLineAfterRef2) {
		
		Overlap status=null;
		if(startLineAfterRef2 == -1 &&  endLineAfterRef2 == -1){
			// first check if one is before the other one
			// find the smallest start line
			if(startLineAfterRef < start && endLineAfterRef< start){ // ref: 8,13  change 14, 20
				status = Overlap.REF_BEFORE_OUTSIDE_CHANGE;
			}else if(startLineAfterRef > start && startLineAfterRef > end) { // ref: 14, 20  change 8,13
				status = Overlap.REF_AFTER_OUTSIDE_CHANGE;
			}
		}else{
			
			if(startLineAfterRef < start && endLineAfterRef< start){ // ref: 8,13  change 14, 20
				status = Overlap.REF_BEFORE_OUTSIDE_CHANGE;
			}else if(startLineAfterRef > start && startLineAfterRef > end) { // ref: 14, 20  change 8,13
				status = Overlap.REF_AFTER_OUTSIDE_CHANGE;
			}
			
			if(status == null){
				if(startLineAfterRef2 < start && endLineAfterRef2< start){ // ref: 8,13  change 14, 20
					status = Overlap.REF_BEFORE_OUTSIDE_CHANGE;
				}else if(startLineAfterRef2 > start && startLineAfterRef2 > end) { // ref: 14, 20  change 8,13
					status = Overlap.REF_AFTER_OUTSIDE_CHANGE;
				}
				
			}
			
		}
		return status;
	}

	private Overlap changedLinesAreInsideRef(int start, int end, int startLineAfterRef, int endLineAfterRef, int startLineAfterRef2, int endLineAfterRef2) {
		Overlap status=null;
		
		if(startLineAfterRef2 == -1 &&  endLineAfterRef2 == -1){
			if(startLineAfterRef >= start &&  endLineAfterRef <= end){
				// changes inside ref
				status=Overlap.REF_INSIDE_CHANGE;
			}else if(startLineAfterRef <= start && endLineAfterRef >= end){
				// ref inside changes
				status=Overlap.CHANGE_INSIDE_REF;
			}
		}else{
			if(startLineAfterRef >= start &&  endLineAfterRef <= end){
				// changes inside ref
				status=Overlap.CHANGE_INSIDE_REF;
			}else if(startLineAfterRef <= start && endLineAfterRef >= end){
				// ref inside changes
				status=Overlap.REF_INSIDE_CHANGE;
			}
			if(status == null){
				if(startLineAfterRef2 >= start &&  endLineAfterRef2 <= end){
					// changes inside ref
					status=Overlap.CHANGE_INSIDE_REF;
				}else if(startLineAfterRef2 <= start && endLineAfterRef2 >= end){
					// ref inside changes
					status=Overlap.REF_INSIDE_CHANGE;
				}
			}
		}
		return status;
	}

	private HashMap<String, List<Refactoring>> findIntersectionBetweenFiles(HashMap<String, List<Refactoring>> afterRefFilePathsToRefactoringMap , Set<String> changedFiles){
		//System.out.println("IN findIntersectionBetweenFiles with changedFiles.size(): "+ changedFiles.size()+ "  afterRefFilePathsToRefactoringMap.size(): "+ afterRefFilePathsToRefactoringMap.size());
		HashMap<String, List<Refactoring>> fileIntersectionWithRefs= new HashMap<String, List<Refactoring>>();
		if(afterRefFilePathsToRefactoringMap.size() == 0 || changedFiles.size() == 0){
			return fileIntersectionWithRefs;
		}else{
			for(String refactoredFile: afterRefFilePathsToRefactoringMap.keySet()){
				//System.out.println("refactoredFile: "+ refactoredFile);
				int foundInstances = 0;
				String match=null;
				for(String changedFile: changedFiles){
					//System.out.println("matched: "+ changedFile);
					if(changedFile.endsWith(refactoredFile)){
						foundInstances++;
						match = changedFile;
						
					}
				}
				if(foundInstances == 1){
					if(fileIntersectionWithRefs.containsKey(match)){
						fileIntersectionWithRefs.get(match).addAll(afterRefFilePathsToRefactoringMap.get(refactoredFile));
					}else{
						fileIntersectionWithRefs.put(match, afterRefFilePathsToRefactoringMap.get(refactoredFile));
					}
				}else if(foundInstances > 1){
					System.err.println("found two matches for: "+ match);
				}
			}
			
			return fileIntersectionWithRefs;	
		}
	}

	private HashMap<String, List<Refactoring>> ExtractAfterRefactroingFiles(List<Refactoring> refactorings) {
		HashMap<String, List<Refactoring>> afterRefFilePathsToRefactoringMap= new HashMap<String, List<Refactoring>>();
		for(Refactoring ref: refactorings){
			String beforeRefFilePath;
			Set<String> beforeRefFilePaths= new HashSet<String>();
			String afterRefFilePath = null;
			if (ref.getName().equals(RefactoringType.RENAME_METHOD.getDisplayName())){
				beforeRefFilePath = ((RenameOperationRefactoring) ref).getOriginalOperation().getSourceFile();
				afterRefFilePath =  ((RenameOperationRefactoring) ref).getRenamedOperation().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.INLINE_OPERATION.getDisplayName())){
				beforeRefFilePath = ((InlineOperationRefactoring) ref).getInlinedOperation().getSourceFile();
				afterRefFilePath = ((InlineOperationRefactoring) ref).getInlinedToOperation().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.MOVE_OPERATION.getDisplayName())){
				beforeRefFilePath = ((MoveOperationRefactoring) ref).getOriginalOperation().getSourceFile();
				afterRefFilePath = ((MoveOperationRefactoring) ref).getMovedOperation().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.PULL_UP_OPERATION.getDisplayName())){
				beforeRefFilePath = ((PullUpOperationRefactoring) ref).getOriginalOperation().getSourceFile();
				afterRefFilePath = ((PullUpOperationRefactoring) ref).getMovedOperation().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.MOVE_CLASS.getDisplayName())){
				beforeRefFilePath = ((MoveClassRefactoring) ref).getOriginalClassName().replace(".", "/")+".java";
				afterRefFilePath = ((MoveClassRefactoring) ref).getMovedClassName().replace(".", "/")+".java";
			}else if (ref.getName().equals(RefactoringType.EXTRACT_SUPERCLASS.getDisplayName())){ 
				for(String subclass: ((ExtractSuperclassRefactoring) ref).getSubclassSet()){
					beforeRefFilePaths.add(subclass.replace(".", "/") + ".java" );
				}
				afterRefFilePath = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName())){
				beforeRefFilePath = ((PushDownAttributeRefactoring) ref).getSourceClassName().replace(".", "/")+".java";
				afterRefFilePath = ((PushDownAttributeRefactoring) ref).getTargetClassName().replace(".", "/")+".java";
			}else if (ref.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
				beforeRefFilePath = ((MoveAttributeRefactoring) ref).getSourceClassName().replace(".", "/")+".java";
				afterRefFilePath = ((MoveAttributeRefactoring) ref).getTargetClassName().replace(".", "/")+".java";
			}else if (ref.getName().equals(RefactoringType.EXTRACT_INTERFACE.getDisplayName())){
				for(String subclass: ((ExtractSuperclassRefactoring) ref).getSubclassSet()){
					beforeRefFilePaths.add(subclass.replace(".", "/") + ".java" );
				}
				afterRefFilePath = ((ExtractSuperclassRefactoring) ref).getExtractedClass().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.EXTRACT_OPERATION.getDisplayName())){
				beforeRefFilePath = ((ExtractOperationRefactoring) ref).getExtractedFromOperation().getSourceFile();
				afterRefFilePath = ((ExtractOperationRefactoring) ref).getExtractedOperation().getSourceFile();
			}else if (ref.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName())){
				beforeRefFilePath = ((PullUpAttributeRefactoring) ref).getSourceClassName().replace(".", "/")+".java";
				afterRefFilePath = ((PullUpAttributeRefactoring) ref).getTargetClassName().replace(".", "/")+".java";
			}else if (ref.getName().equals(RefactoringType.RENAME_CLASS.getDisplayName())){
				beforeRefFilePath = ((RenameClassRefactoring) ref).getOriginalClassName().replace(".", "/")+".java";
				afterRefFilePath = ((RenameClassRefactoring) ref).getRenamedClassName().replace(".", "/")+".java";
			}else{
				System.err.println("NO SUCH REFACTORING: " + ref.toString());
			}
			//System.out.println("==========> "+ afterRefFilePath);
			if(afterRefFilePathsToRefactoringMap.containsKey(afterRefFilePath)){
				afterRefFilePathsToRefactoringMap.get(afterRefFilePath).add(ref);
			}else {
				List<Refactoring> aList= new ArrayList<Refactoring>();
				aList.add(ref);
				afterRefFilePathsToRefactoringMap.put(afterRefFilePath, aList);
			}
			
		}
		return afterRefFilePathsToRefactoringMap;
		
	}

	private List<String> findChangedLines(String diffAsString){
		
		List<String> lineRanges= new ArrayList<String>();
		String toMatch ="(@@\\s[-+]\\d+\\,\\d+\\s[-+]\\d+\\,\\d+\\s@@)+"; // matches: @@ -111,7 +111,7 @@
		Pattern pattern = Pattern.compile(toMatch);
		Matcher matcher = pattern.matcher(diffAsString);
		while (matcher.find()) {
          //  System.out.print("Start index: " + matcher.start());
          //  System.out.print(" End index: " + matcher.end() + " ");
          //System.out.println("\t"+ matcher.group().replace("@", "").replaceFirst(" ", "")); 
          lineRanges.add(matcher.group().replace("@", "").replaceFirst(" ", "")); // -111,7 +111,7 
		}
		return lineRanges;
		
	}

	public void kim(Repository repo) {
		
		sortHashMapByValues();
		System.out.println("===============================================");
		for(String hashId:this.sortedCommits.keySet() ){
			Commit c= this.sortedCommits.get(hashId);
			if(c.isBugInducingCommit() && c.getRefactorings().size()>0 && c.hasFix()){
				System.out.println("Processing commit: "+ c.getCommitHash());
				HashMap<String, List<Refactoring>> afterRefFilePathsToRefactoringMap =this.ExtractAfterRefactroingFiles(c.getRefactorings());
				for(Commit fixingCommit:c.getFixedIn()){
					HashMap<String, List<Refactoring>> fileIntersectionWithRefs= this.findIntersectionBetweenFiles(afterRefFilePathsToRefactoringMap, fixingCommit.getChangedFiles());
					if(fileIntersectionWithRefs.size() ==0 ){
						System.out.println(" \t no intersection between changed files and files constain refactoring");
					}
					for(String file: fileIntersectionWithRefs.keySet()){
						System.out.println("\t Processing fixed file: "+ file + "   from fixing commit: "+ fixingCommit.getCommitHash());	
						String diffAsString =GitUtil.getFileDiff(repo, hashId, fixingCommit.getCommitHash(), file);
						List<String> diffLineRanges = this.findChangedLines(diffAsString);
						//System.out.println(" \t diff lines size: "+ diffLineRanges.size());
						if(diffLineRanges.size()>0){
							List<String> linesInNewFiles = this.extractFileRangeInNewVersion(diffLineRanges);
							for(Refactoring r: fileIntersectionWithRefs.get(file)){
								System.out.println("\t\t "+ r.toString());
								Overlap overlap=this.findLineOverlaps(linesInNewFiles, r);
								if(overlap != Overlap.NO_OVERLAPS){
									OverlapInfo info= new OverlapInfo(overlap, r);
									if(c.getRefactoingsAndFixOverlap().containsKey(fixingCommit)){
										c.getRefactoingsAndFixOverlap().get(fixingCommit).add(info);
									}else{
										Set<OverlapInfo> aSet= new HashSet<OverlapInfo>();
										aSet.add(info);
										c.getRefactoingsAndFixOverlap().put(fixingCommit, aSet);
									}
								}else if(r.getName().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE.getDisplayName()) || 
										 r.getName().equals(RefactoringType.PULL_UP_ATTRIBUTE.getDisplayName()) ||
										 r.getName().equals(RefactoringType.MOVE_ATTRIBUTE.getDisplayName())){
									if(diffAsString.contains("this."+((MoveAttributeRefactoring)r).getMovedAttribute().getName())){ // since the other two are subtypes of MoveAttributeRefactoring
										System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% POTENTIAL OVERLAP");
									}
								}
							}	
						}
					}
				}
			}
		}
	
	}
	
	public static int getCommitDistance(Commit c1, Commit c2){
		int distance;
		if(c1.equals(c2)){
			distance = 0;
		}else{
			Commit zero;
			Commit target;
			if(c1.compareTo(c2) > 0){ // c1 is after c2
				zero= c2;
				target = c1;
			}else{
				zero= c1;
				target = c2;
			}
			int count = 0;
			for(Commit c: sortedCommits.values()){
				if(c.equals(zero)){
					count = 0;
				}else if(! c.equals(target)){
					count ++;
				}else if(c.equals(target)){
					count ++;
					break;
				}
			}
			distance = count;
		}
		return distance;
	}

}

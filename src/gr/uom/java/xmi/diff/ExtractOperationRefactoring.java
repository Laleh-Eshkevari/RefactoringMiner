package gr.uom.java.xmi.diff;

import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.StringLiteralReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableRename;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	private String sourceClassName;
	private UMLOperation extractedFromOperationInNewVersion;
	private boolean isPureRefactoring = false;

	public ExtractOperationRefactoring(UMLOperation extractedOperation, UMLOperation extractedFromOperation, UMLOperation extractedFromOperationInNewVersion,String sourceClassName) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
		this.extractedFromOperationInNewVersion=extractedFromOperationInNewVersion;
		this.sourceClassName = sourceClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String isPureRefactoring = "";
		if(this.isPureRefactoring){
			isPureRefactoring = "(p)";
		}
		sb.append(getName()).append(" " + isPureRefactoring).append("\t");
		sb.append(extractedOperation);
		//sb.append(" line range: ["+ extractedOperation.getStartLine() + ", "+ extractedOperation.getEndLine()+ "]");
		sb.append(" extracted from ");
		sb.append(extractedFromOperation);
		//sb.append(" line range: ["+ extractedFromOperation.getStartLine() + ", "+ extractedFromOperation.getEndLine()+ "]");
		sb.append(" in class ");
		sb.append(sourceClassName);
		sb.append("in file ");
		sb.append(this.extractedFromOperation.getSourceFile());
		return sb.toString();
	}

	public UMLOperation getExtractedFromOperationInNewVersion() {
		return extractedFromOperationInNewVersion;
	}

	public UMLOperation getExtractedOperation() {
		return extractedOperation;
	}

	public UMLOperation getExtractedFromOperation() {
		return extractedFromOperation;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_OPERATION;
	}

	public boolean isPureRefactoring() {		
		return this.isPureRefactoring;
	}
	
	public void analyzeRefGranularity(UMLOperationBodyMapper operationBodyMapperExtractedFromBeforeAndAfter, 
										UMLOperationBodyMapper operationBodyMapperExtractedFromBeforeAndExtractedOperation){
		System.out.println("=======> " + this.toString());
		// first we work on operationBodyMapperExtractedFromBeforeAndAfter 
		// check that all nonMapped1 in ExtractedFromBeforeRef are inside ExtractedOperation
		// then the nonMapped2.size() should be zero
		
		List<AbstractCodeMapping> mappedExtractedFromBeforeAndAfter = operationBodyMapperExtractedFromBeforeAndAfter.getMappings();
		List<CompositeStatementObject> nonMappedInner1InExtractedFromBefore = operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedInnerNodesT1();
		List<StatementObject> nonMappedLeaves1InExtractedFromBefore = operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedLeavesT1();
		
		int notInExtracted1 = 0;
		int notInExtracted2 = 0;
		
		OperationBody extractededOperationBody = extractedOperation.getBody();
		CompositeStatementObject compositeInExtracted=null;
		if(extractededOperationBody != null) {
			compositeInExtracted = extractededOperationBody.getCompositeStatement();
		}
		
		// check for InnerNodes
		if(nonMappedInner1InExtractedFromBefore.size() > 0){
			if(compositeInExtracted != null) {
				for(CompositeStatementObject nonMappedInner : nonMappedInner1InExtractedFromBefore){
					if (!(nonMappedInner.toString().contentEquals("{") || nonMappedInner.toString().contentEquals("}"))){
						boolean found=false;
						for(AbstractStatement statement : compositeInExtracted.getStatements()){
							if(statement instanceof CompositeStatementObject){
								if(nonMappedInner.equalFragment(statement)){
									found=true;
									break;
								}
							}
						}
						if(!found){
							System.out.println("Did not find a match for "+ nonMappedInner.toString());
							notInExtracted1 ++;
						}
					}
				}
			}
		}
		
		// check for InnerLeaves
		if(nonMappedLeaves1InExtractedFromBefore.size() > 0){
			if(compositeInExtracted != null) {
				for(StatementObject nonMappedLeave : nonMappedLeaves1InExtractedFromBefore){
					if(!(nonMappedLeave.toString().contentEquals("{") || nonMappedLeave.toString().contentEquals("}"))){
						boolean found=false;
						for(AbstractStatement statement : compositeInExtracted.getStatements()){
							if(statement instanceof StatementObject){
								if(nonMappedLeave.equalFragment(statement)){
									found=true;
									break;
								}
							}
						}
						if(!found){
							System.out.println("Did not find a match for "+ nonMappedLeave.toString());
							notInExtracted2 ++;
						}	
					}
				}
			}
		}
		boolean cond1 = false;
		if (notInExtracted1 == 0 && notInExtracted2 == 0){
			System.out.println("All nonMapped are inside Extracted");
			cond1=true;
		}else{
			System.out.println("Some stataments in extractedFromBefore are nither in extracted nor in extractedFromAfter");
		}
		boolean cond2 = false;
		if(operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedInnerNodesT2().size() == 0 &&
				   operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedInnerNodesT2().size()==0){
			System.out.println("extractedFromAfter does not have stataments that are not in extractedFromBefore");
			cond2=true;
		}else{
			System.out.println("extractedFromAfter has stataments that are not in extractedFromBefore");
		}
		
		
		// Second we work on operationBodyMapperExtractedFromAndExtractedOperation 
		// check that all nonMapped2 in ExtractedOperation is empty
		boolean cond3 = false;
		if(operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedInnerNodesT2().size()==0 &&
				operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedLeavesT2().size()==0){
			System.out.println("extracted does not have extra stataments that were not in ExtractedFromBefore");
			cond3 = true;
		}else if(operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedInnerNodesT2().size()==0 &&
				operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedLeavesT2().size()==1){
			if(operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedLeavesT2().get(0) instanceof  StatementObject &&
					operationBodyMapperExtractedFromBeforeAndExtractedOperation.getNonMappedLeavesT2().get(0).toString().startsWith("return")){
				System.out.println("extracted has only a return-stataments difference  with ExtractedFromBefore");
				cond3 = true;	
			}else{
				System.out.println("extracted  has extra stataments that were not before in ExtractedFromBefore");
			}
		}
		else{
			System.out.println("extracted  has extra stataments that were not before in ExtractedFromBefore");
		}
		
		// now we check the mapped statements between extractedFromBeofor and extractedFromAfter
		int complexChange = 0;
		int complexChangeAcceptable = 0;
		if(mappedExtractedFromBeforeAndAfter.size()>0){
			for(AbstractCodeMapping codeMapping : mappedExtractedFromBeforeAndAfter){
				if(!codeMapping.getReplacements().isEmpty() || !codeMapping.getFragment1().equalFragment(codeMapping.getFragment2())) {
					for(Replacement r : codeMapping.getReplacements()){
						if(!(r instanceof StringLiteralReplacement) ){
							if(r.getAfter().contains(operationBodyMapperExtractedFromBeforeAndExtractedOperation.getOperation2().getName())){
								complexChangeAcceptable ++;
							}
							complexChange ++ ;
						}
					}
				}
			}
		}
		boolean cond4 = false;
		if(complexChange == 0){
			System.out.println("The mapping between extractedFromBefore and extractedFromAfter are Simple");
			cond4= true;
		}else{
			System.out.println("The mapping between extractedFromBefore and extractedFromAfter are complex, complexChange: " 
					+ complexChange + "  complexChangeAcceptable: "+ complexChangeAcceptable);
		}
		boolean cond5 = false;
		if(complexChange <= 1 && complexChangeAcceptable == 1){
			cond5=true;
		}
		
		if(cond1 && cond2 &&cond3 && (cond4 || cond5)){
			this.isPureRefactoring= true;
		}
		
		System.out.println();
	}
	
	
}

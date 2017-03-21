package gr.uom.java.xmi.diff;

import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	private String sourceClassName;
	private UMLOperation extractedFromOperationInNewVersion;

	public ExtractOperationRefactoring(UMLOperation extractedOperation, UMLOperation extractedFromOperation, UMLOperation extractedFromOperationInNewVersion,String sourceClassName) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
		this.extractedFromOperationInNewVersion=extractedFromOperationInNewVersion;
		this.sourceClassName = sourceClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
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

	@Override
	public boolean isPureRefactoring() {
		// to be implemented
		return false;
	}
	
	public void analyzeRefgranularity(UMLOperationBodyMapper operationBodyMapperExtractedFromBeforeAndAfter, 
										UMLOperationBodyMapper operationBodyMapperExtractedFromAndExtractedOperation){
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
		
		// check for InnerLeaves
		if(nonMappedLeaves1InExtractedFromBefore.size() > 0){
			if(compositeInExtracted != null) {
				for(StatementObject nonMappedLeave : nonMappedLeaves1InExtractedFromBefore){
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
		
		if (notInExtracted1 == 0 && notInExtracted2 == 0){
			System.out.println("All nonMapped are inside Extracted");
		}else{
			System.out.println("Some stataments are not in extracted and not in extractedFromAfter");
		}
		
		if(operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedInnerNodesT2().size() == 0 &&
				   operationBodyMapperExtractedFromBeforeAndAfter.getNonMappedInnerNodesT2().size()==0){
			System.out.println("extractedFromAfter does not have stataments that are not in extractedFromBefore");
		}else{
			System.out.println("extractedFromAfter has stataments that are not in extractedFromBefore");
		}
		
		
		// Second we work on operationBodyMapperExtractedFromAndExtractedOperation 
		// check that all nonMapped2 in ExtractedOperation is empty
		if(operationBodyMapperExtractedFromAndExtractedOperation.getNonMappedInnerNodesT2().size()==0 &&
				operationBodyMapperExtractedFromAndExtractedOperation.getNonMappedLeavesT2().size()==0){
			System.out.println("extracted does not have extra stataments that were not before in ExtractedFromBefore");
		}else{
			System.out.println("extracted  has extra stataments that were not before in ExtractedFromBefore");
		}
		
		// now we check the mapped statements between extractedFromBeofor and extractedFromAfter
		if(mappedExtractedFromBeforeAndAfter.size()>0){
			
		}
		
		
	}
	
	
}

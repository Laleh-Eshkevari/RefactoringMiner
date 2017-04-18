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
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.StringLiteralReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableRename;

public class InlineOperationRefactoring implements Refactoring {
	private UMLOperation inlinedOperation;
	private UMLOperation inlinedToOperation;
	private String sourceClassName;
	private boolean isPureRefactoring = false;
	
	
	public InlineOperationRefactoring(UMLOperation inlinedOperation, UMLOperation inlinedToOperation, String sourceClassName) {
		this.inlinedOperation = inlinedOperation;
		this.inlinedToOperation = inlinedToOperation;
		this.sourceClassName = sourceClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String isPure="";
		if(this.isPureRefactoring){
			isPure="(p)";
		}
		sb.append(getName()).append(" " + isPure).append("\t");
		sb.append(inlinedOperation);
		sb.append(" in file ");
		sb.append(inlinedOperation.getSourceFile());
		sb.append(" inlined to ");
		sb.append(inlinedToOperation);
		sb.append(" in class ");
		sb.append(sourceClassName);
		sb.append(" in file ");
		sb.append(inlinedToOperation.getSourceFile());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.INLINE_OPERATION;
	}

  public UMLOperation getInlinedOperation() {
    return inlinedOperation;
  }

  public UMLOperation getInlinedToOperation() {
    return inlinedToOperation;
  }

	public boolean isPureRefactoring() {
		return this.isPureRefactoring;
	}
	
	public void analyzeRefGranularity(UMLOperationBodyMapper operationBodyMapperInlinedToOperationBeforeAndAfter, 
			UMLOperationBodyMapper operationBodyMapperInlinedToOperationAfterAndRemovedOperation){
		System.out.println("=======> " + this.toString());
		// first we work on operationBodyMapperInlinedToOperationBeforeAndAfter 
		// check that all nonMapped2 in InlinedToOperationAfter are inside removedOperation
		// then the nonMapped2.size() should be zero

		List<AbstractCodeMapping> mappedInlinedToBeforeAndAfter = operationBodyMapperInlinedToOperationBeforeAndAfter.getMappings();
		List<CompositeStatementObject> nonMappedInner1InInlinedToAfter = operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedInnerNodesT2();
		List<StatementObject> nonMappedLeaves1InInlinedToAfter = operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedLeavesT2();

		int notInExtracted1 = 0;
		int notInExtracted2 = 0;

		CompositeStatementObject compositeInRemoved=null;
		if(inlinedOperation.getBody() != null) {
			compositeInRemoved = inlinedOperation.getBody().getCompositeStatement();
		}

		// check for InnerNodes
		if(nonMappedInner1InInlinedToAfter.size() > 0){
			if(compositeInRemoved != null) {
				for(CompositeStatementObject nonMappedInner : nonMappedInner1InInlinedToAfter){
					if (!(nonMappedInner.toString().contentEquals("{") || nonMappedInner.toString().contentEquals("}"))){
						boolean found=false;
						for(AbstractStatement statement : compositeInRemoved.getStatements()){
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
		if(nonMappedLeaves1InInlinedToAfter.size() > 0){
			if(compositeInRemoved != null) {
				for(StatementObject nonMappedLeave : nonMappedLeaves1InInlinedToAfter){
					if(!(nonMappedLeave.toString().contentEquals("{") || nonMappedLeave.toString().contentEquals("}"))){
						boolean found=false;
						for(AbstractStatement statement : compositeInRemoved.getStatements()){
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
			System.out.println("All nonMapped are inside removed");
			cond1=true;
		}else{
			System.out.println("Some stataments in inlinedToAfter are nither in removed nor in inlinedToAfterBefore");
		}
		boolean cond2 = false;
		if(operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedInnerNodesT1().size() == 0 &&
				operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedInnerNodesT1().size()==0){
			System.out.println("inlinedToBfore does not have stataments that are not in inlinedToAfter");
			cond2=true;
		} else if(operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedInnerNodesT1().size() == 1 ||
				operationBodyMapperInlinedToOperationBeforeAndAfter.getNonMappedInnerNodesT1().size()==1){
			System.out.println("inlinedToBfore does not have stataments that are not in inlinedToAfter");
			// we have only one nonmapped in inlinedToBfore which is the call to removedOpearation
			// I have to check for the return too
			cond2=true;
		}
		else{
			System.out.println("inlinedToBfore has stataments that are not in inlinedToAfter");
		}


		// Second we work on operationBodyMapperInlinedToOperationAfterAndRemovedOperation 
		// check that all nonMapped1 in removedOperation is empty
		boolean cond3 = false;
		if(operationBodyMapperInlinedToOperationAfterAndRemovedOperation.getNonMappedInnerNodesT1().size()==0 &&
				operationBodyMapperInlinedToOperationAfterAndRemovedOperation.getNonMappedLeavesT1().size()==0){
			System.out.println("removed does not have extra stataments that are not in inLinedToAfter");
			cond3 = true;
		}else{
			System.out.println("removed  has extra stataments that are not in inLinedToAfter");
		}

		// now we check the mapped statements between inLinedToBefore and inLinedToAfter
		int complexChange = 0;
		if(mappedInlinedToBeforeAndAfter.size()>0){
			for(AbstractCodeMapping codeMapping : mappedInlinedToBeforeAndAfter){
				if(!codeMapping.getReplacements().isEmpty() || !codeMapping.getFragment1().equalFragment(codeMapping.getFragment2())) {
					for(Replacement r : codeMapping.getReplacements()){
						if(!(r instanceof StringLiteralReplacement)){
							complexChange ++ ;
						}
					}
				}
			}
		}
		boolean cond4 = false;
		if(complexChange == 0){
			System.out.println("The mapping between inLinedToBefore and inLinedToAfter are Simple");
			cond4= true;
		}else{
			System.out.println("The mapping between inLinedToBefore and inLinedToAfter are complex, complexChange: " 
					+ complexChange );
		}

		if(cond1 && cond2 &&cond3 && cond4 ){
			this.isPureRefactoring= true;
		}

		System.out.println();
	}
	
}

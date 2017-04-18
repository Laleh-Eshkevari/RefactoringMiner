package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.StringLiteralReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableRename;

public class MoveOperationRefactoring implements Refactoring {
	protected UMLOperation originalOperation;
	protected UMLOperation movedOperation;
	private boolean isPureRefactoring=false;

	public MoveOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		this.originalOperation = originalOperation;
		this.movedOperation = movedOperation;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String isPure="";
		if(this.isPureRefactoring){
			isPure="(p)";
		}
		sb.append(getName()).append(" " + isPure).append("\t");
		sb.append(originalOperation);
		sb.append(" from class ");
		sb.append(originalOperation.getClassName());
		sb.append(" in file ");
		sb.append(originalOperation.getSourceFile());
		sb.append(" to ");
		sb.append(movedOperation);
		sb.append(" from class ");
		sb.append(movedOperation.getClassName());
		sb.append(" in file ");
		sb.append(movedOperation.getSourceFile());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_OPERATION;
	}

	public UMLOperation getOriginalOperation() {
		return originalOperation;
	}

	public UMLOperation getMovedOperation() {
		return movedOperation;
	}

	public boolean isPureRefactoring() {
		return this.isPureRefactoring;
	}

	public void analyzeRefGranularity(UMLOperationBodyMapper umlOperationBodyMapper) {
		
		System.out.println("=======> " + this.toString());
		boolean cond1=false;
		if(umlOperationBodyMapper.getNonMappedInnerNodesT1().size()==0 &&
			    umlOperationBodyMapper.getNonMappedInnerNodesT2().size()==0	&&
			    umlOperationBodyMapper.getNonMappedLeavesT1().size()==0  &&
			    umlOperationBodyMapper.getNonMappedLeavesT2().size()==0 ){
				System.out.println("All nonMapped are empty");
				 cond1 = true;
			}else{
				System.out.println("There are some nonMapped statements");
			}
			
			// now we check the mapped statements between originalOperation and movedOperation
			int complexChange = 0;
			if(umlOperationBodyMapper.getMappings().size()>0){
				for(AbstractCodeMapping codeMapping : umlOperationBodyMapper.getMappings()){
					if(!codeMapping.getReplacements().isEmpty() || !codeMapping.getFragment1().equalFragment(codeMapping.getFragment2())) {
						for(Replacement r : codeMapping.getReplacements()){
							if(!(r instanceof StringLiteralReplacement)){
								complexChange ++ ;
							}
						}
					}
				}
			}
			boolean cond2=false;
			if(complexChange == 0){
				System.out.println("The mapping between removed and added are Simple");
				cond2= true;
			}else{
				System.out.println("The mapping between removed and added are complex, complexChange: " 
						+ complexChange );
			}
			
			if(cond1 && cond2){
				this.isPureRefactoring=true;
			}
	}

}

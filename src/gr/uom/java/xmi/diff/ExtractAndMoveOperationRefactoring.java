package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;

public class ExtractAndMoveOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	
	public ExtractAndMoveOperationRefactoring(UMLOperation extractedOperation,
			UMLOperation extractedFromOperation) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(extractedFromOperation);
		sb.append(" in class ");
		sb.append(extractedFromOperation.getClassName());
		sb.append(" in file ");
		sb.append( extractedFromOperation.getSourceFile());
		sb.append(" & moved to class ");
		sb.append(extractedOperation.getClassName());
		sb.append(" in file ");
		sb.append( extractedOperation.getSourceFile());
		return sb.toString();
	}
	
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_AND_MOVE_OPERATION;
	}
	
	public UMLOperation getExtractedOperation() {
		return extractedOperation;
	}

	public UMLOperation getExtractedFromOperation() {
		return extractedFromOperation;
	}

	@Override
	public boolean isPureRefactoring() {
		// to be implemented
		return false;
	}
	
}

package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;

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
	
}

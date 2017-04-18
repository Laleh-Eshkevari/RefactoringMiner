package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;

public class MoveAttributeRefactoring implements Refactoring {
	protected UMLAttribute movedAttribute;
	protected UMLAttribute originalAttribute;
	protected String sourceClassName;
	protected String targetClassName;
	
	public MoveAttributeRefactoring(UMLAttribute movedAttribute, UMLAttribute originalAttribute,
			String sourceClassName, String targetClassName) {
		this.movedAttribute = movedAttribute;
		this.originalAttribute= originalAttribute;
		this.sourceClassName = sourceClassName;
		this.targetClassName = targetClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(movedAttribute);
		sb.append(" from class ");
		sb.append(sourceClassName);
		sb.append(" to class ");
		sb.append(targetClassName);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_ATTRIBUTE;
	}

  public UMLAttribute getMovedAttribute() {
    return movedAttribute;
  }

  public String getSourceClassName() {
    return sourceClassName;
  }

  public String getTargetClassName() {
    return targetClassName;
  }

	public UMLAttribute getOriginalAttribute() {
		return originalAttribute;
	}

}

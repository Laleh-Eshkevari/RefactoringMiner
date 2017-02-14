package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

/**
 * Created by matin on 2017-02-10.
 */
public class RenameLocalVariable implements Refactoring {

    private VariableDeclaration originalVariable;
    private VariableDeclaration renamedVariable;

    private UMLOperation originalVariableOperation;
    //private UMLOperation renamedVariableOperation;

    private String sourceClassName;

    public RenameLocalVariable(VariableDeclaration originalVarable, VariableDeclaration renamedVariable, UMLOperation originalVariableOperation) {
        this.originalVariable = originalVarable;
        this.renamedVariable = renamedVariable;
        this.originalVariableOperation = originalVariableOperation;
        //this.renamedVariableOperation = renamedVariableOperaiton;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_LOCAL_VARIABLE;
    }

    @Override
    public String getName() {

        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(originalVariable);
        sb.append(" renamed to ");
        sb.append(renamedVariable);
        sb.append(" in method ").append(originalVariableOperation);
        sb.append(" in class ").append(originalVariableOperation.getClassName());
        return sb.toString();
    }
}

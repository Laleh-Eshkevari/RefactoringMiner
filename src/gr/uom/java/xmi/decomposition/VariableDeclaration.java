package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclaration {
	private String variableName;
	private String initializer;
	private String variableType;
	private AbstractCodeFragment container;
	public VariableDeclaration(VariableDeclarationFragment fragment) {

		try {
			 
			if(fragment.getParent() instanceof VariableDeclarationExpression)
				variableType=((VariableDeclarationExpression)fragment.getParent()).getType().toString();
			else if(fragment.getParent() instanceof VariableDeclarationStatement)
				variableType = ((VariableDeclarationStatement)fragment.getParent()).getType().toString();

				this.variableName = fragment.getName().getIdentifier();
			this.initializer = fragment.getInitializer() != null ? fragment.getInitializer().toString() : null;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println();
		}
	

	}
	
	public void setContainer(AbstractCodeFragment container){
		this.container = container;
	}
	
	public AbstractCodeFragment getContainer(){
		return this.container;
	}
	
	public String getVariableName() {
		return variableName;
	}

	public String getVariableType(){
		return  variableType;
	}
	public String getInitializer() {
		return initializer;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableDeclaration other = (VariableDeclaration) obj;
		if (initializer == null) {
			if (other.initializer != null)
				return false;
		} else if (!initializer.equals(other.initializer))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(variableName);
        if(initializer != null) {
        	sb.append("=").append(initializer);
        }
        return sb.toString();
	}

	public String display(){
		StringBuilder sb = new StringBuilder();
		sb.append(variableName);
		sb.append(" : ");
		sb.append(variableType);

		return sb.toString();
	}
}

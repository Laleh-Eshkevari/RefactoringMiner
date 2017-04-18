package org.refactoringminer.api;

import java.io.Serializable;
import java.util.List;

import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public interface Refactoring extends Serializable {

	public RefactoringType getRefactoringType();
	
	public String getName();

	public String toString();	
	
}
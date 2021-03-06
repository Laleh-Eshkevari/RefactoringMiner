package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.TypeReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableRename;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import sun.print.PeekGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.internal.expressions.OrExpression;
import org.refactoringminer.api.Refactoring;

public class UMLClassDiff implements Comparable<UMLClassDiff> {
	private UMLClass originalClass;
	private UMLClass nextClass;
	private String className;
	private List<UMLOperation> addedOperations;
	private List<UMLOperation> removedOperations;
	private List<UMLAttribute> addedAttributes;
	private List<UMLAttribute> removedAttributes;
	private List<UMLAttributeDiff> attributeDiffList;
	private List<UMLOperationDiff> operationDiffList;
	private List<UMLOperationBodyMapper> operationBodyMapperList;
	private Map<UMLOperation, OperationInvocation> extractedDelegateOperations;
	private List<Refactoring> refactorings;
	private boolean visibilityChanged;
	private String oldVisibility;
	private String newVisibility;
	private boolean abstractionChanged;
	private boolean oldAbstraction;
	private boolean newAbstraction;
	private boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	private List<UMLAnonymousClass> addedAnonymousClasses;
	private List<UMLAnonymousClass> removedAnonymousClasses;
	private List<UMLType> addedImplementedInterfaces;
	private List<UMLType> removedImplementedInterfaces;
	private static final int MAX_OPERATION_POSITION_DIFFERENCE = 5;
	public static final double MAX_OPERATION_NAME_DISTANCE = 0.34;

	public UMLClassDiff(UMLClass originalClass, UMLClass nextClass) {
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.className = originalClass.getName();
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedAttributes = new ArrayList<UMLAttribute>();
		this.removedAttributes = new ArrayList<UMLAttribute>();
		this.attributeDiffList = new ArrayList<UMLAttributeDiff>();
		this.operationDiffList = new ArrayList<UMLOperationDiff>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		this.extractedDelegateOperations = new LinkedHashMap<UMLOperation, OperationInvocation>();
		this.refactorings = new ArrayList<Refactoring>();
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.removedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
	}

	public String getClassName() {
		return className;
	}

	public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
		this.addedAnonymousClasses.add(umlClass);
	}

	public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
		this.removedAnonymousClasses.add(umlClass);
	}

	public void reportAddedOperation(UMLOperation umlOperation) {
		this.addedOperations.add(umlOperation);
	}

	public void reportRemovedOperation(UMLOperation umlOperation) {
		this.removedOperations.add(umlOperation);
	}

	public void reportAddedAttribute(UMLAttribute umlAttribute) {
		this.addedAttributes.add(umlAttribute);
	}

	public void reportRemovedAttribute(UMLAttribute umlAttribute) {
		this.removedAttributes.add(umlAttribute);
	}

	public void reportAddedImplementedInterface(UMLType implementedInterface) {
		this.addedImplementedInterfaces.add(implementedInterface);
	}

	public void reportRemovedImplementedInterface(UMLType implementedInterface) {
		this.removedImplementedInterfaces.add(implementedInterface);
	}

	public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) {
		this.operationBodyMapperList.add(operationBodyMapper);
	}

	public void setVisibilityChanged(boolean visibilityChanged) {
		this.visibilityChanged = visibilityChanged;
	}

	public void setOldVisibility(String oldVisibility) {
		this.oldVisibility = oldVisibility;
	}

	public void setNewVisibility(String newVisibility) {
		this.newVisibility = newVisibility;
	}

	public void setAbstractionChanged(boolean abstractionChanged) {
		this.abstractionChanged = abstractionChanged;
	}

	public void setOldAbstraction(boolean oldAbstraction) {
		this.oldAbstraction = oldAbstraction;
	}

	public void setNewAbstraction(boolean newAbstraction) {
		this.newAbstraction = newAbstraction;
	}

	public void setSuperclassChanged(boolean superclassChanged) {
		this.superclassChanged = superclassChanged;
	}

	public void setOldSuperclass(UMLType oldSuperclass) {
		this.oldSuperclass = oldSuperclass;
	}

	public void setNewSuperclass(UMLType newSuperclass) {
		this.newSuperclass = newSuperclass;
	}

	public UMLType getSuperclass() {
		if (!superclassChanged && oldSuperclass != null && newSuperclass != null)
			return oldSuperclass;
		return null;
	}

	public UMLType getOldSuperclass() {
		return oldSuperclass;
	}

	public UMLType getNewSuperclass() {
		return newSuperclass;
	}

	public boolean containsOperationWithTheSameSignature(UMLOperation operation) {
		for (UMLOperation originalOperation : originalClass.getOperations()) {
			if (originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for (UMLOperation removedOperation : removedOperations) {
			if (removedOperation.equalSignature(operation))
				return removedOperation;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for (UMLAttribute removedAttribute : removedAttributes) {
			if (removedAttribute.equalsIgnoringChangedVisibility(attribute))
				return removedAttribute;
		}
		return null;
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() && addedAttributes.isEmpty()
				&& removedAttributes.isEmpty() && operationDiffList.isEmpty() && attributeDiffList.isEmpty()
				&& operationBodyMapperList.isEmpty() && !visibilityChanged && !abstractionChanged;
	}

	public List<UMLOperation> getAddedOperations() {
		return addedOperations;
	}

	public List<UMLOperation> getRemovedOperations() {
		return removedOperations;
	}

	public List<UMLAttribute> getAddedAttributes() {
		return addedAttributes;
	}

	public List<UMLAttribute> getRemovedAttributes() {
		return removedAttributes;
	}

	public List<UMLAnonymousClass> getAddedAnonymousClasses() {
		return addedAnonymousClasses;
	}

	public List<UMLAnonymousClass> getRemovedAnonymousClasses() {
		return removedAnonymousClasses;
	}

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}

	public Map<UMLOperation, OperationInvocation> getExtractedDelegateOperations() {
		return extractedDelegateOperations;
	}

	public List<UMLType> getAddedImplementedInterfaces() {
		return addedImplementedInterfaces;
	}

	public List<UMLType> getRemovedImplementedInterfaces() {
		return removedImplementedInterfaces;
	}

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	public void checkForAttributeChanges() {
		for (Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator
				.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for (Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator
					.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if (removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute);
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}

	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation,
			UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		return Math.abs(index1 - index2);
	}

	public void checkForOperationSignatureChanges() {
		int absoluteDifference = Math.abs(removedOperations.size() - addedOperations.size());
		int maxDifferenceInPosition = absoluteDifference > MAX_OPERATION_POSITION_DIFFERENCE ? absoluteDifference
				: MAX_OPERATION_POSITION_DIFFERENCE;
		if (removedOperations.size() <= addedOperations.size()) {
			for (Iterator<UMLOperation> removedOperationIterator = removedOperations
					.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for (Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator
						.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation
							.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for (UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation,
								maxDifferenceInPosition);
					}
				}

				if (!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);

					UMLOperation tmpAddedOperation = bestMapper.getOperation2();
					for (Iterator<UMLOperation> tmpRemovedOperationIterator = removedOperations
							.iterator(); tmpRemovedOperationIterator.hasNext();) {
						UMLOperation tmpRemovedOperation = tmpRemovedOperationIterator.next();
						updateMapperSet(mapperSet, tmpRemovedOperation, tmpAddedOperation, maxDifferenceInPosition);
						List<UMLOperation> operationsInsideAnonymousClass = tmpAddedOperation
								.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
						for (UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
							updateMapperSet(mapperSet, tmpRemovedOperation, operationInsideAnonymousClass,
									tmpAddedOperation, maxDifferenceInPosition);
						}
					}
					bestMapper = findBestMapper(mapperSet);
					removedOperation = bestMapper.getOperation1();
					UMLOperation addedOperation = bestMapper.getOperation2();
					addedOperations.remove(addedOperation);
					removedOperationIterator.remove();

					UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
					operationDiffList.add(operationDiff);
					if (!removedOperation.getName().equals(addedOperation.getName())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(bestMapper);
				}
			}
		} else {
			for (Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator
					.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for (Iterator<UMLOperation> removedOperationIterator = removedOperations
						.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation
							.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for (UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation,
								maxDifferenceInPosition);
					}
				}
				if (!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					UMLOperation removedOperation = bestMapper.getOperation1();
					addedOperation = bestMapper.getOperation2();
					removedOperations.remove(removedOperation);
					addedOperationIterator.remove();

					UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
					operationDiffList.add(operationDiff);
					if (!removedOperation.getName().equals(addedOperation.getName())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper);
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(bestMapper);
				}
			}
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation,
			UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if (mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation,
					addedOperation);
			if (operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0
					&& allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& addedOperation.getName().equals(removedOperation.getName()) && addedOperation
							.getParametersWithoutReturnType().equals(removedOperation.getParametersWithoutReturnType()))
				mapperSet.add(operationBodyMapper);
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation,
			UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation,
				operationInsideAnonymousClass);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if (mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation,
					addedOperation);
			if (operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0
					&& allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			} else if (mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper)
					&& absoluteDifferenceInPosition <= differenceInPosition
					&& isPartOfMethodInlined(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) || (mappings == 1
				&& nonMappedElementsT1 + nonMappedElementsT2 == 1
				&& operationBodyMapper.getOperation1().getName().equals(operationBodyMapper.getOperation2().getName()));
	}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper
				.nonMappedElementsT2CallingAddedOperation(addedOperations);
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2
				- nonMappedElementsT2CallingAddedOperation;
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation
				&& nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation);
	}

	private boolean mappedElementsMoreThanNonMappedT1(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT1CallingRemovedOperation = operationBodyMapper
				.nonMappedElementsT1CallingRemovedOperation(removedOperations);
		int nonMappedElementsT1WithoutThoseCallingRemovedOperation = nonMappedElementsT1
				- nonMappedElementsT1CallingRemovedOperation;
		return mappings > nonMappedElementsT1 || (mappings >= nonMappedElementsT1WithoutThoseCallingRemovedOperation
				&& nonMappedElementsT1CallingRemovedOperation >= nonMappedElementsT1WithoutThoseCallingRemovedOperation);
	}

	private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet) {
		List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>(mapperSet);
		UMLOperationBodyMapper bestMapper = mapperSet.first();
		for (int i = 1; i < mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			UMLOperation operation2 = mapper.getOperation2();
			Set<OperationInvocation> operationInvocations2 = operation2.getAllOperationInvocations();
			boolean anotherMapperCallsOperation2OfTheBestMapper = false;
			for (OperationInvocation invocation : operationInvocations2) {
				if (invocation.matchesOperation(bestMapper.getOperation2())
						&& !invocation.matchesOperation(bestMapper.getOperation1())
						&& !operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation,
								removedOperations)) {
					anotherMapperCallsOperation2OfTheBestMapper = true;
					break;
				}
			}
			UMLOperation operation1 = mapper.getOperation1();
			Set<OperationInvocation> operationInvocations1 = operation1.getAllOperationInvocations();
			boolean anotherMapperCallsOperation1OfTheBestMapper = false;
			for (OperationInvocation invocation : operationInvocations1) {
				if (invocation.matchesOperation(bestMapper.getOperation1())
						&& !invocation.matchesOperation(bestMapper.getOperation2())
						&& !operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation,
								addedOperations)) {
					anotherMapperCallsOperation1OfTheBestMapper = true;
					break;
				}
			}

			if (anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
				bestMapper = mapper;
				break;
			}
		}
		return bestMapper;
	}

	private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(OperationInvocation invocation,
			List<UMLOperation> operations) {
		for (UMLOperation operation : operations) {
			Set<OperationInvocation> operationInvocations = operation.getAllOperationInvocations();
			for (OperationInvocation operationInvocation : operationInvocations) {
				Set<String> argumentIntersection = new LinkedHashSet<String>(operationInvocation.getArguments());
				argumentIntersection.retainAll(invocation.getArguments());
				if (operationInvocation.getMethodName().equals(invocation.getMethodName())
						&& !argumentIntersection.isEmpty()) {
					return true;
				} else if (argumentIntersection.size() > 0
						&& argumentIntersection.size() == invocation.getArguments().size()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = removedOperationInvocations.size() - intersection.size();

		Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<OperationInvocation>();
		for (OperationInvocation addedOperationInvocation : addedOperationInvocations) {
			if (!intersection.contains(addedOperationInvocation)) {
				for (UMLOperation operation : addedOperations) {
					if (!operation.equals(addedOperation) && operation.getBody() != null) {
						if (addedOperationInvocation.matchesOperation(operation)) {
							// addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation
									.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);

		Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<OperationInvocation>(
				removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for (Iterator<OperationInvocation> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted
				.iterator(); operationInvocationIterator.hasNext();) {
			OperationInvocation invocation = operationInvocationIterator.next();
			if (invocation.getMethodName().startsWith("get")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation
				- numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations
				|| numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted
						.size();
	}

	private boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = addedOperationInvocations.size() - intersection.size();

		Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<OperationInvocation>();
		for (OperationInvocation removedOperationInvocation : removedOperationInvocations) {
			if (!intersection.contains(removedOperationInvocation)) {
				for (UMLOperation operation : removedOperations) {
					if (!operation.equals(removedOperation) && operation.getBody() != null) {
						if (removedOperationInvocation.matchesOperation(operation)) {
							// removedOperation calls another removed method
							operationInvocationsInMethodsCalledByRemovedOperation
									.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(addedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);

		int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation
				- numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
		return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations;
	}

	private boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper, int mappings) {
		if (mappings == operationBodyMapper.exactMatches()) {
			return true;
		}
		int mappingsWithTypeReplacement = 0;
		for (AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if (mapping.containsTypeReplacement()) {
				mappingsWithTypeReplacement++;
			}
		}
		if (mappings == operationBodyMapper.exactMatches() + mappingsWithTypeReplacement
				&& mappings > mappingsWithTypeReplacement) {
			return true;
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation,
			int absoluteDifferenceInPosition) {
		return addedOperation.equalParameterTypes(removedOperation)
				|| addedOperation.overloadedParameterTypes(removedOperation)
				|| addedOperation.replacedParameterTypes(removedOperation)
				|| ((absoluteDifferenceInPosition == 0
						|| operationsBeforeAndAfterMatch(removedOperation, addedOperation))
						&& (addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList())
								|| addedOperation
										.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE));
	}

	private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for (int i = 0; i < originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if (current.equals(removedOperation)) {
				if (i > 0) {
					operationBefore1 = originalClassOperations.get(i - 1);
				}
				if (i < originalClassOperations.size() - 1) {
					operationAfter1 = originalClassOperations.get(i + 1);
				}
			}
		}

		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for (int i = 0; i < nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if (current.equals(addedOperation)) {
				if (i > 0) {
					operationBefore2 = nextClassOperations.get(i - 1);
				}
				if (i < nextClassOperations.size() - 1) {
					operationAfter2 = nextClassOperations.get(i + 1);
				}
			}
		}

		boolean operationsBeforeMatch = false;
		if (operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = operationBefore1.equalParameterTypes(operationBefore2)
					&& operationBefore1.getName().equals(operationBefore2.getName());
		}

		boolean operationsAfterMatch = false;
		if (operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = operationAfter1.equalParameterTypes(operationAfter2)
					&& operationAfter1.getName().equals(operationAfter2.getName());
		}

		return operationsBeforeMatch || operationsAfterMatch;
	}

	public void checkForInlinedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for (Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator
				.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for (UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if (!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty()
						|| !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation1().getAllOperationInvocations();
					OperationInvocation removedOperationInvocation = null;
					for (OperationInvocation invocation : operationInvocations) {
						if (invocation.matchesOperation(removedOperation)) {
							removedOperationInvocation = invocation;
							break;
						}
					}
					if (removedOperationInvocation != null
							&& !invocationMatchesWithAddedOperation(removedOperationInvocation,
									mapper.getOperation2().getAllOperationInvocations())) {
						List<String> arguments = removedOperationInvocation.getArguments();
						List<String> parameters = removedOperation.getParameterNameList();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						// special handling for methods with varargs parameter
						// for which no argument is passed in the matching
						// invocation
						int size = Math.min(arguments.size(), parameters.size());
						for (int i = 0; i < size; i++) {
							parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation,
								mapper, parameterToArgumentMap);
						operationBodyMapper.getMappings();
						int mappings = operationBodyMapper.mappingsWithoutBlocks();
						if (mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT1()
								|| operationBodyMapper.exactMatches() > 0)) {
							InlineOperationRefactoring inlineOperationRefactoring = new InlineOperationRefactoring(
									operationBodyMapper);
							refactorings.add(inlineOperationRefactoring);
							mapper.addAdditionalMapper(operationBodyMapper);
							operationsToBeRemoved.add(removedOperation);
						}
					}
				}
			}
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private boolean invocationMatchesWithAddedOperation(OperationInvocation removedOperationInvocation,
			Set<OperationInvocation> operationInvocationsInNewMethod) {
		if (operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for (UMLOperation addedOperation : addedOperations) {
				if (removedOperationInvocation.matchesOperation(addedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public void checkForExtractedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for (Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator
				.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			for (UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if (!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty()
						|| !mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation2().getAllOperationInvocations();
					OperationInvocation addedOperationInvocation = null;
					for (OperationInvocation invocation : operationInvocations) {
						if (invocation.matchesOperation(addedOperation)) {
							addedOperationInvocation = invocation;
							break;
						}
					}
					if (addedOperationInvocation != null) {
						List<UMLParameter> originalMethodParameters = mapper.getOperation1()
								.getParametersWithoutReturnType();
						Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters = new LinkedHashMap<UMLParameter, UMLParameter>();
						List<String> arguments = addedOperationInvocation.getArguments();
						List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						// special handling for methods with varargs parameter
						// for which no argument is passed in the matching
						// invocation
						int size = Math.min(arguments.size(), parameters.size());
						for (int i = 0; i < size; i++) {
							String argumentName = arguments.get(i);
							String parameterName = parameters.get(i).getName();
							parameterToArgumentMap.put(parameterName, argumentName);
							for (UMLParameter originalMethodParameter : originalMethodParameters) {
								if (originalMethodParameter.getName().equals(argumentName)) {
									originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters
											.put(originalMethodParameter, parameters.get(i));
								}
							}
						}
						if (parameterTypesMatch(
								originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters)) {
							UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(mapper,
									addedOperation, new LinkedHashMap<String, String>(), parameterToArgumentMap);
							operationBodyMapper.getMappings();
							int mappings = operationBodyMapper.mappingsWithoutBlocks();
							if (mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT2()
									|| operationBodyMapper.exactMatches() > 0
									|| (mappings == 1 && mappings > operationBodyMapper.nonMappedLeafElementsT2()))) {
								ExtractOperationRefactoring extractOperationRefactoring = new ExtractOperationRefactoring(
										operationBodyMapper);
								refactorings.add(extractOperationRefactoring);
								mapper.addAdditionalMapper(operationBodyMapper);
								operationsToBeRemoved.add(addedOperation);
							} else if (addedOperation.isDelegate() != null && !mapper.getOperation1()
									.getAllOperationInvocations().contains(addedOperationInvocation)) {
								extractedDelegateOperations.put(addedOperation, addedOperation.isDelegate());
								operationsToBeRemoved.add(addedOperation);
							}
						}
					}
				}
			}
		}
		addedOperations.removeAll(operationsToBeRemoved);
	}

	private boolean parameterTypesMatch(
			Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for (UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if (!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType())) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!isEmpty())
			sb.append(className).append(":").append("\n");
		if (visibilityChanged) {
			sb.append("\t").append("visibility changed from " + oldVisibility + " to " + newVisibility).append("\n");
		}
		if (abstractionChanged) {
			sb.append("\t").append("abstraction changed from " + (oldAbstraction ? "abstract" : "concrete") + " to "
					+ (newAbstraction ? "abstract" : "concrete")).append("\n");
		}
		Collections.sort(removedOperations);
		for (UMLOperation umlOperation : removedOperations) {
			sb.append("operation " + umlOperation + " removed").append("\n");
		}
		Collections.sort(addedOperations);
		for (UMLOperation umlOperation : addedOperations) {
			sb.append("operation " + umlOperation + " added").append("\n");
		}
		Collections.sort(removedAttributes);
		for (UMLAttribute umlAttribute : removedAttributes) {
			sb.append("attribute " + umlAttribute + " removed").append("\n");
		}
		Collections.sort(addedAttributes);
		for (UMLAttribute umlAttribute : addedAttributes) {
			sb.append("attribute " + umlAttribute + " added").append("\n");
		}
		for (UMLOperationDiff operationDiff : operationDiffList) {
			sb.append(operationDiff);
		}
		for (UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for (UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper);
		}
		return sb.toString();
	}

	public int compareTo(UMLClassDiff classDiff) {
		return this.className.compareTo(classDiff.className);
	}

	public boolean nextClassImportsType(String targetClass) {
		return nextClass.importsType(targetClass);
	}

	public boolean originalClassImportsType(String targetClass) {
		return originalClass.importsType(targetClass);
	}

	public List<UMLAttribute> originalClassAttributesOfType(String targetClass) {
		return originalClass.attributesOfType(targetClass);
	}

	public void checkForRLV() {
		ArrayList<UMLOperationBodyMapper> bodyMapperList = new ArrayList<>(getOperationBodyMapperList());

		for (int index = 0; index < bodyMapperList.size(); index++) {

			UMLOperationBodyMapper umlOperationBodyMapper = bodyMapperList.get(index);
			HashMap<String, String> potentialLVDrenamings = new HashMap<>();

			if (umlOperationBodyMapper.getOperation1().getBody() != null
					&& umlOperationBodyMapper.getOperation2().getBody() != null) {
				List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();
				Set<VariableDeclaration> variableDeclarationInOperation1 = new HashSet<>(umlOperationBodyMapper
						.getOperation1().getBody().getCompositeStatement().getVariableDeclarations());
				Set<VariableDeclaration> variableDeclarationInOperation2 = new HashSet<>(umlOperationBodyMapper
						.getOperation2().getBody().getCompositeStatement().getVariableDeclarations());

				ArrayList<Replacement> replacements = new ArrayList<>(umlOperationBodyMapper.getReplacements());

				if (replacements.size() == 0 || variableDeclarationInOperation1.size() == 0)
					continue;

				for (VariableDeclaration variableDeclaration : variableDeclarationInOperation1) {
					potentialLVDrenamings.put(variableDeclaration.getVariableName(), "");
				}

				for (int i = 0; i < mappings.size(); i++) {
					List<String> vars = mappings.get(i).getFragment1().getVariables();
					if (vars.size() > 0) {
						for (int j = 0; j < vars.size(); j++) {
							// check its not in the
						}
					}
				}
			}
		}

	}

	public void checkForRenameLocalVariable() {
		long startTime = System.currentTimeMillis();
		// System.out.println("startTime:" + startTime);
		ArrayList<UMLOperationBodyMapper> bodyMapperList = new ArrayList<>(getOperationBodyMapperList());

		for (int index = 0; index < bodyMapperList.size(); index++) {

			UMLOperationBodyMapper umlOperationBodyMapper = bodyMapperList.get(index);
			HashMap<String, Replacement> potentialLVDrenamings = new HashMap<>();
			HashMap<String, String> occupiedTarget = new HashMap<>();
			HashSet<String> operationTwoVars = getVariables(umlOperationBodyMapper);

			UMLOperationBodyMapper b = new UMLOperationBodyMapper(umlOperationBodyMapper.getOperation1(),
					umlOperationBodyMapper.getOperation2());

			// System.out.println("class name: "+
			// umlOperationBodyMapper.getOperation1().getClassName()+"\tmethod1:
			// "+ umlOperationBodyMapper.getOperation1().getName()+ "\tmethod2:
			// "+umlOperationBodyMapper .getOperation2().getName());
			if (umlOperationBodyMapper.getOperation1().getBody() != null
					&& umlOperationBodyMapper.getOperation2().getBody() != null) {
				Set<VariableDeclaration> variableDeclarationInOperation1 = new HashSet<>(umlOperationBodyMapper
						.getOperation1().getBody().getCompositeStatement().getVariableDeclarations());
				Set<VariableDeclaration> variableDeclarationInOperation2 = new HashSet<>(umlOperationBodyMapper
						.getOperation2().getBody().getCompositeStatement().getVariableDeclarations());

				detectRename(umlOperationBodyMapper, potentialLVDrenamings, null);
				for (UMLOperationBodyMapper additionalMapper : umlOperationBodyMapper.getAdditionalMappers()) {
					variableDeclarationInOperation1.addAll(new HashSet<>(additionalMapper.getOperation1().getBody()
							.getCompositeStatement().getVariableDeclarations()));
					variableDeclarationInOperation2.addAll(new HashSet<>(additionalMapper.getOperation2().getBody()
							.getCompositeStatement().getVariableDeclarations()));
					detectRename(additionalMapper, potentialLVDrenamings, umlOperationBodyMapper);
				}

				for (String key : potentialLVDrenamings.keySet()) {
					// this might be changed to the results of the rename method
					// since the local variable can only happend in the same
					// method and the only way that the method name can be
					// different is method renaming!
					if (potentialLVDrenamings.get(key) != null) {
						Replacement replacement = potentialLVDrenamings.get(key);
						String refVar = null;
						if ((replacement.getAfter().contains("."))) {
							refVar = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));
						} else
							refVar = replacement.getAfter();
						if (refVar.contains("["))
							refVar = refVar.substring(0, refVar.indexOf("["));
						RenameLocalVariable lvr = new RenameLocalVariable(
								getVariable(key.replace("#", ""), variableDeclarationInOperation1),
								getVariable(refVar, variableDeclarationInOperation2),
								umlOperationBodyMapper.getOperation1());
						refactorings.add(lvr);
						System.out.println("=============== " + lvr);
					}

				}
			}
		}

		// System.out.println("took: " + (System.currentTimeMillis() -
		// startTime));
	}

	private void detectRename(UMLOperationBodyMapper umlOperationBodyMapper,
			HashMap<String, Replacement> potentialLVDrenamings, UMLOperationBodyMapper originalBodyMapper) {
		if (umlOperationBodyMapper.getOperation1().getBody() != null
				&& umlOperationBodyMapper.getOperation2().getBody() != null) {
			List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();

			Set<String> variables = new HashSet<>(); // new
														// HashSet<>(umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariables());

			Set<VariableDeclaration> variableDeclarationInOperation1 = new HashSet<>(
					umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations());
			Set<VariableDeclaration> variableDeclarationInOperation2 = new HashSet<>(
					umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations());
			// List<VariableDeclaration> variableDeclarationInOperation1 =
			// umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations();
			// List<VariableDeclaration> variableDeclarationInOperation2 =
			// umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations();
			ArrayList<Replacement> replacements = new ArrayList<>(umlOperationBodyMapper.getReplacements());

			for (VariableDeclaration variableDeclaration : variableDeclarationInOperation1) {
				// I should avoid adding revoved cases to this list
				if (!potentialLVDrenamings.containsKey(variableDeclaration.getVariableName()))
					potentialLVDrenamings.put(variableDeclaration.getVariableName(), null);
			}

			ArrayList<Replacement> removedCase = new ArrayList<>();
			if (replacements.size() == 0 || variableDeclarationInOperation1.size() == 0)
				return;

			for (int i = 0; i < replacements.size(); i++) {
				Replacement replacement = replacements.get(i);
				boolean existInAdditionalMapper = false;
				// check if everything is replaced
				if (originalBodyMapper != null
						// && findVariableByName(replacement.getAfter(),
						// variableDeclarationInOperation1)
						&& (findVariableByName(replacement.getAfter(),
								new HashSet<>(originalBodyMapper.getOperation1().getBody().getCompositeStatement()
										.getVariableDeclarations()))
								|| isVarInTargetParams(replacement.getBefore(), umlOperationBodyMapper)))
					continue;
				if (replacement instanceof VariableRename || replacement instanceof TypeReplacement) {

					if (existInAdditionalMapper)
						continue;

					if (potentialLVDrenamings.containsKey(replacement.getBefore())) {
						Replacement candidate = potentialLVDrenamings.get(replacement.getBefore());

						int varInMethod = isVariableExistInBoth(replacement.getBefore(), replacement.getAfter(),
								variableDeclarationInOperation1, variableDeclarationInOperation2, variables);
						boolean variableExistInBoth = varInMethod != 0;
						if (!variableExistInBoth)
							variableExistInBoth |= isVariableExistsInDepenantMapper(replacement.getBefore(),
									umlOperationBodyMapper.getAdditionalMappers())
									| isVarInTargetParams(replacement.getBefore(), umlOperationBodyMapper);

						// this might be destruptive
						// if(variableExistInBoth)
						// continue;

						if (candidate == null && !variableExistInBoth
								&& getVariable(replacement.getAfter(), variableDeclarationInOperation2) != null
								&& !isInRemovedCandidates(removedCase, replacement)) {
							mappedToTarget(potentialLVDrenamings, replacement);
							// potentialLVDrenamings.put(replacement.getBefore(),
							// replacement);
						} else if (variableExistInBoth) {
							// Create a method that check if there is the
							// vriable in the scope and traverse the scopes
							// upward instead of vrable exists in both and
							// in same scope

							//// if (varInMethod == 1 &&
							//// !inSameScope(replacement.getBefore())) {
							////
							// }
							if (candidate != null) {
								if (replacement.getCodeMapping() instanceof LeafMapping
										&& candidate.getCodeMapping() instanceof LeafMapping) {
									if (((LeafMapping) replacement.getCodeMapping())
											.compareTo(((LeafMapping) candidate.getCodeMapping())) < 0)
										potentialLVDrenamings.put(replacement.getBefore(), replacement);

									if (((LeafMapping) replacement.getCodeMapping())
											.compareTo(((LeafMapping) candidate.getCodeMapping())) == 0) {
										potentialLVDrenamings.remove(replacement.getBefore());
										removedCase.add(replacement);
									}
								} else {
									potentialLVDrenamings.remove(replacement.getBefore());
									removedCase.add(replacement);
								}
							}

						} else if (findBetterCandidatae(candidate, replacement) != null) {
							potentialLVDrenamings.put(replacement.getBefore(),
									findBetterCandidatae(candidate, replacement));
						} else if (isInRemovedCandidates(removedCase, replacement)) {
							potentialLVDrenamings.remove(replacement.getBefore());
							removedCase.add(replacement);
						} else if (getVariable(replacement.getAfter(), variableDeclarationInOperation2) != null
								&& !compareReplacements(replacement, candidate)) {
							if (originalBodyMapper != null) {
								if (candidate != null) {
									if (replacement.getCodeMapping() instanceof LeafMapping
											&& candidate.getCodeMapping() instanceof LeafMapping){
										if (((LeafMapping) replacement.getCodeMapping())
												.compareTo(((LeafMapping) candidate.getCodeMapping())) < 0)
											potentialLVDrenamings.put(replacement.getBefore(), replacement);

									if (((LeafMapping) replacement.getCodeMapping())
											.compareTo(((LeafMapping) candidate.getCodeMapping())) == 0) {
										potentialLVDrenamings.remove(replacement.getBefore());
										removedCase.add(replacement);
									}
								}}
							} else if (inSameScope(candidate, replacement)) {
								potentialLVDrenamings.remove(replacement.getBefore());
								removedCase.add(replacement);
							} else if (getVariable(replacement.getAfter(), variableDeclarationInOperation2) != null) {
								potentialLVDrenamings.put(replacement.getBefore() + "#", replacement);
							}
						}

					}
				} else if (replacement instanceof VariableReplacementWithMethodInvocation) {
					String baseVar;
					String refVar;
					if (replacement.getBefore().contains("."))
						baseVar = replacement.getBefore().substring(0, replacement.getBefore().indexOf("."));
					else
						baseVar = replacement.getBefore();

					if (replacement.getAfter().contains("."))
						refVar = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));
					else
						refVar = replacement.getAfter();

					if (!findVariableByName(baseVar, variableDeclarationInOperation1)
							|| !findVariableByName(refVar, variableDeclarationInOperation2) || baseVar.equals(refVar))
						continue;

					boolean variableExistInBoth = (isVariableExistInBoth(baseVar, refVar,
							variableDeclarationInOperation1, variableDeclarationInOperation2, variables) != 0)
							| isVarInTargetParams(baseVar, umlOperationBodyMapper);
					;

					if (!baseVar.contains("(") && !refVar.contains("(")) {
						if (potentialLVDrenamings.containsKey(baseVar)) {
							Replacement candidate = potentialLVDrenamings.get(baseVar);
							if (candidate == null && !variableExistInBoth
									&& getVariable(refVar, variableDeclarationInOperation2) != null)
								potentialLVDrenamings.put(baseVar, replacement);
							else if (variableExistInBoth || !compareReplacements(replacement, candidate))
								potentialLVDrenamings.remove(baseVar);
						}
					}

					// potentialLVDrenamings.remove(replacement.getBefore());

				} else if (replacement instanceof MethodInvocationReplacement) {
					if (!replacement.getAfter().contains(".") || !replacement.getBefore().contains("."))
						continue;

					String baseVar = replacement.getBefore().substring(0, replacement.getBefore().indexOf("."));
					String refVar = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));

					if (baseVar.contains("["))
						baseVar = baseVar.substring(0, baseVar.indexOf("["));
					if (refVar.contains("["))
						refVar = refVar.substring(0, refVar.indexOf("["));

					// last part of the condition should change
					if (!findVariableByName(baseVar, variableDeclarationInOperation1)
							|| !findVariableByName(refVar, variableDeclarationInOperation2) || baseVar.equals(refVar))
						continue;

					boolean variableExistInBoth = isVariableExistInBoth(baseVar, refVar,
							variableDeclarationInOperation1, variableDeclarationInOperation2, variables) != 0;

					if (!baseVar.contains("(") && !refVar.contains("(")) {
						if (potentialLVDrenamings.containsKey(baseVar)) {
							Replacement candidate = potentialLVDrenamings.get(baseVar);
							if (candidate == null && !variableExistInBoth
									&& getVariable(refVar, variableDeclarationInOperation2) != null)
								potentialLVDrenamings.put(baseVar, replacement);
							else if (variableExistInBoth || !candidate.equals(refVar))
								potentialLVDrenamings.remove(replacement.getBefore());
						}
					}

				}

			}

		}
	}

	private void mappedToTarget(HashMap<String, Replacement> potentialLVDrenamings, Replacement replacement) {

		String baseRefKey;
		for (String key : potentialLVDrenamings.keySet()) {
			Replacement oldReplacement = potentialLVDrenamings.get(key);
			if (oldReplacement != null)
				if (oldReplacement.getAfter().equals(replacement.getAfter())
						&& inSameScope(oldReplacement, replacement)) {
					if (replacement.getCodeMapping() instanceof LeafMapping
							&& oldReplacement.getCodeMapping() instanceof LeafMapping) {
						if (((LeafMapping) replacement.getCodeMapping())
								.compareTo((LeafMapping) oldReplacement.getCodeMapping()) > 0) {
							potentialLVDrenamings.remove(key);
							potentialLVDrenamings.put(replacement.getBefore(), replacement);
							return;
						}
						potentialLVDrenamings.remove(replacement.getBefore());
						return;
					}
				}

		}
		potentialLVDrenamings.put(replacement.getBefore(), replacement);
	}

	private Replacement findBetterCandidatae(Replacement candidate, Replacement replacement) {

		try {
			AbstractCodeMapping candidateMapping = candidate.getCodeMapping();
			AbstractCodeMapping newCandidateMapping = replacement.getCodeMapping();
			if (candidateMapping.getFragment1().getMethodInvocationMap().keySet().size() == 1
					&& candidateMapping.getFragment1().getMethodInvocationMap().keySet().toArray()[0] != null
					&& candidateMapping.getFragment2().getMethodInvocationMap().keySet().size() == 1
					&& candidateMapping.getFragment2().getMethodInvocationMap().keySet().toArray()[0] != null
					&& newCandidateMapping.getFragment1().getMethodInvocationMap().keySet().size() == 1
					&& newCandidateMapping.getFragment1().getMethodInvocationMap().keySet().toArray()[0] != null
					&& newCandidateMapping.getFragment2().getMethodInvocationMap().keySet().size() == 1
					&& newCandidateMapping.getFragment2().getMethodInvocationMap().keySet().toArray()[0] != null) {

				Set<String> candidateVariables1 = new LinkedHashSet<String>(
						candidateMapping.getFragment1().getVariables());
				Set<String> candidateVariables2 = new LinkedHashSet<String>(
						candidateMapping.getFragment2().getVariables());

				Set<String> replacementVariables1 = new LinkedHashSet<String>(
						newCandidateMapping.getFragment1().getVariables());
				Set<String> replacementVariables2 = new LinkedHashSet<String>(
						newCandidateMapping.getFragment2().getVariables());

				Set<String> candidateVariableIntersection = new LinkedHashSet<String>(candidateVariables1);
				candidateVariableIntersection.retainAll(candidateVariables2);

				Set<String> replacementVariableIntersection = new LinkedHashSet<String>(replacementVariables1);
				replacementVariableIntersection.retainAll(replacementVariables2);

				Set<String> candidateVariablesToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
				candidateVariableIntersection.removeAll(candidateVariablesToBeRemovedFromTheIntersection);

				Set<String> replacementVariablesToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
				replacementVariableIntersection.removeAll(replacementVariablesToBeRemovedFromTheIntersection);

				candidateVariables1.removeAll(candidateVariableIntersection);
				candidateVariables2.removeAll(candidateVariableIntersection);

				replacementVariables1.removeAll(replacementVariableIntersection);
				replacementVariables2.removeAll(replacementVariableIntersection);

				if (candidateVariables1.size() == 1 && candidateVariables2.size() == 1
						&& !(replacementVariables1.size() == 1 && replacementVariables2.size() == 1))
					return candidate;
				if (replacementVariables1.size() == 1 && replacementVariables2.size() == 1
						&& !(candidateVariables1.size() == 1 && candidateVariables2.size() == 1))
					return replacement;
			}
		} catch (Exception e) {
			System.out.println("");
			// TODO: handle exception
		}

		return null;
	}

	private boolean isVarInTargetParams(String before, UMLOperationBodyMapper umlOperationBodyMapper) {
		return umlOperationBodyMapper.getOperation2().getParameterNameList().contains(before);

	}

	private boolean compareReplacements(Replacement replacement, Replacement candidate) {
		String oldCandidate;
		String newCandidate;

		if (replacement.getAfter().contains("."))
			oldCandidate = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));
		else
			oldCandidate = replacement.getAfter();

		if (candidate.getAfter().contains("."))
			newCandidate = candidate.getAfter().substring(0, candidate.getAfter().indexOf("."));
		else
			newCandidate = candidate.getAfter();

		if (oldCandidate.contains("["))
			oldCandidate = oldCandidate.substring(0, oldCandidate.indexOf("["));

		if (newCandidate.contains("["))
			newCandidate = newCandidate.substring(0, newCandidate.indexOf("["));

		return newCandidate.equals(oldCandidate);
	}

	private boolean isInRemovedCandidates(List<Replacement> removeCandidate, Replacement currentCandidate) {
		for (Replacement replacement : removeCandidate) {
			if (compareReplacements(currentCandidate, replacement)) {
				System.out.println("removed due to isInRemovedCandidates	" + replacement);
				return true;
			}

		}
		return false;
	}

	private boolean inSameScope(VariableDeclaration var1, VariableDeclaration var2) {
		int firstDepth = var1.getContainer().getDepth();
		int secondDepth = var2.getContainer().getDepth();
		CompositeStatementObject firstParent;
		CompositeStatementObject secondParent;

		if (var1.getContainer() instanceof CompositeStatementObject)
			firstParent = (CompositeStatementObject) var1.getContainer();
		else {
			firstParent = var1.getContainer().getParent();
			firstDepth--;
		}

		if (var2.getContainer() instanceof CompositeStatementObject)
			secondParent = (CompositeStatementObject) var2.getContainer();
		else {
			secondParent = var2.getContainer().getParent();
			secondDepth--;
		}
		while (firstDepth != 0) {

			if (!firstParent.getString().equals("{") && findVariableByName(var1.getVariableName(),
					new HashSet<>(firstParent.getVariableDeclarations()))) {
				break;
			}
			firstParent = firstParent.getParent();
			firstDepth--;
		}
		while (secondDepth != 0) {

			if (!secondParent.getString().equals("{") && findVariableByName(var2.getVariableName(),
					new HashSet<>(secondParent.getVariableDeclarations()))) {
				break;
			}
			secondParent = secondParent.getParent();
			secondDepth--;
		}

		if (!firstParent.getString().equals(secondParent.getString())) {
			if (secondDepth < firstDepth)
				while (firstDepth != secondDepth) {

					firstParent = firstParent.getParent();
					firstDepth--;
				}

			if (firstDepth < secondDepth)
				while (secondDepth != firstDepth) {
					secondParent = secondParent.getParent();
					secondDepth--;
				}
		}

		return firstParent != null && secondParent != null && firstParent.getString().equals(secondParent.getString());
	}

	private boolean inSameScope(Replacement first, Replacement second) {
		// we have to check up to the same level to see they do not belong to
		// the same parent
		int depthDiff = Math.abs(
				first.getCodeMapping().getFragment1().getDepth() - second.getCodeMapping().getFragment1().getDepth());
		int firstDepth = first.getCodeMapping().getFragment1().getDepth() - 1;
		int secondDepth = second.getCodeMapping().getFragment1().getDepth() - 1;
		CompositeStatementObject firstParent = first.getCodeMapping().getFragment1().getParent();
		CompositeStatementObject secondParent = second.getCodeMapping().getFragment1().getParent();

		while (firstDepth != 0) {

			if (!firstParent.getString().equals("{")
					&& findVariableByName(first.getBefore(), new HashSet<>(firstParent.getVariableDeclarations()))) {
				break;
			}
			firstParent = firstParent.getParent();
			firstDepth--;
		}

		while (secondDepth != 0) {

			if (!secondParent.getString().equals("{")
					&& findVariableByName(second.getBefore(), new HashSet<>(secondParent.getVariableDeclarations()))) {
				break;
			}
			secondParent = secondParent.getParent();
			secondDepth--;
		}

		// is first inside the second?
		// if(secondDepth<firstDepth)
		// while (firstDepth != 0||firstDepth!=secondDepth) {
		// firstParent = firstParent.getParent();
		// if (!firstParent.getString().equals("{")
		// && findVariableByName(first.getBefore(), new
		// HashSet<>(firstParent.getVariableDeclarations()))) {
		// break;
		// }
		// firstDepth--;
		// }

		return firstParent != null && secondParent != null && firstParent.getString().equals(secondParent.getString());

		// if(depthDiff>1){
		//
		// //find in which level the specific variable is declared if the levels
		// are the same check the parent the should not be the same too! we need
		// to make sure
		// //that they did not map to the same variable in the target too!
		// while (depthDiff!=1) {
		// if(findVariableByName(first.getBefore(),
		// firstParent.getVariableDeclarations()))
		// firstParent = firstParent.getParent();
		// depthDiff--;
		// }
		// }else if(depthDiff<-1){
		// while (depthDiff!=-1) {
		// secondParent = secondParent.getParent();
		// depthDiff++;
		// }
		//
		// List<VariableDeclaration> firstParentVars=
		// first.getCodeMapping().getFragment1().getParent().getVariableDeclarations();
		// List<VariableDeclaration> secondParentVars=
		// second.getCodeMapping().getFragment1().getParent().getVariableDeclarations();
		//
		// if
		// (first.getCodeMapping().getFragment1().getDepth()!=second.getCodeMapping().getFragment1().getDepth()&&
		// first.getCodeMapping().getFragment1().getv)
	}

	private boolean isVariableExistsInDepenantMapper(String var, List<UMLOperationBodyMapper> mappers) {

		for (UMLOperationBodyMapper umlOperationBodyMapper : mappers) {
			for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
				for (String mapperVar : mapping.getFragment2().getVariables()) {
					if (mapperVar.equals(var))
						return true;
				}
			}
		}

		return false;
	}

	private int isVariableExistInBoth(String baseVar, String refVar, Set<VariableDeclaration> baseVars,
			Set<VariableDeclaration> refVars, Set<String> usedVarsIn2) {
		boolean variableExistInBoth = findVariableByName(baseVar, refVars);
		boolean secondVarExistInBoth = findVariableByName(refVar, baseVars);

		if (usedVarsIn2.contains(baseVar))
			return -1;

		if (!findVariableByName(refVar, refVars) && variableExistInBoth)
			return -1;

		if (variableExistInBoth) {
			VariableDeclaration selected = compareCandidates(refVar, baseVar, baseVars, refVars);
			if (selected.getVariableName().equals(baseVar)) {
				if (inSameScope(getVariable(baseVar, refVars), getVariable(refVar, refVars)))
					return -1;
				// }
			}

		}

		if (secondVarExistInBoth) {
			VariableDeclaration selected = compareCandidates(baseVar, refVar, refVars, baseVars);
			if (selected.getVariableName().equals(refVar))
				if (inSameScope(getVariable(refVar, baseVars), getVariable(refVar, refVars))
						|| inSameScope(getVariable(baseVar, baseVars), getVariable(refVar, baseVars)))
					return 1;
		}

		return 0;
	}

	// we can get the variable name in the first place
	private boolean findVariableByName(String name, Set<VariableDeclaration> vars) {
		for (VariableDeclaration variableDeclaration : vars) {
			if (variableDeclaration.getVariableName().equals(name))
				return true;

		}
		return false;
	}

	private VariableDeclaration compareCandidates(String after, String before, Set<VariableDeclaration> baseVars,
			Set<VariableDeclaration> refVars) {
		VariableDeclaration first = getVariable(before, refVars);
		VariableDeclaration second = getVariable(after, refVars);
		VariableDeclaration base = getVariable(before, baseVars);
		try {
			int firstVarSimilarity = (stringHandle(first.getVariableName()).equals(stringHandle(base.getVariableName()))
					? 1 : 0)
					+ (stringHandle(first.getVariableType()).equals(stringHandle(base.getVariableType())) ? 1 : 0)
					+ (stringHandle(first.getInitializer()).replace(" ", "")
							.equals(stringHandle(base.getInitializer()).replace(" ", "")) ? 1
									: 0);
			int secondVarSimilarity = (stringHandle(second.getVariableName())
					.equals(stringHandle(base.getVariableName())) ? 1 : 0)
					+ (stringHandle(second.getVariableType()).equals(stringHandle(base.getVariableType())) ? 1 : 0)
					+ (stringHandle(second.getInitializer()).replace(" ", "")
							.equals(stringHandle(base.getInitializer()).replace(" ", "")) ? 1
									: 0);

			// we need to take to account the fact that they might be eqaul so
			// we need a more sophosticated approach!
			return firstVarSimilarity >= secondVarSimilarity ? first : second;
		} catch (Exception e) {
			return first;
		}

	}

	private HashSet<String> getVariables(UMLOperationBodyMapper mapper) {
		HashSet<String> vars = new HashSet<>();

		for (AbstractCodeMapping mapping : mapper.getMappings()) {
			vars.addAll(mapping.getFragment2().getVariables());
		}

		return vars;
	}

	private String stringHandle(String str) {
		return str == null ? "" : str;
	}

	private VariableDeclaration getVariable(String name, Set<VariableDeclaration> vars) {
		for (VariableDeclaration var : vars) {
			if (var.getVariableName().equals(name))
				return var;
		}
		return null;
	}

	public void checkForRenameLocalVariable2() {

		List<UMLOperationBodyMapper> bodyMapperList = getOperationBodyMapperList();
		// for all mapped operations check their bodies
		for (UMLOperationBodyMapper umlOperationBodyMapper : bodyMapperList) {
			HashMap<VariableDeclaration, VariableDeclaration> potentialLVDrenamings = new HashMap<VariableDeclaration, VariableDeclaration>();
			// check if operation1 and operation2 have local variable
			// declarations
			if (umlOperationBodyMapper.getOperation1().getBody() != null
					&& umlOperationBodyMapper.getOperation2().getBody() != null) {
				List<VariableDeclaration> variableDeclarationInOperation1 = umlOperationBodyMapper.getOperation1()
						.getBody().getCompositeStatement().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarationInOperation2 = umlOperationBodyMapper.getOperation2()
						.getBody().getCompositeStatement().getVariableDeclarations();

				if (variableDeclarationInOperation1.size() > 0 && variableDeclarationInOperation2.size() > 0) {
					for (VariableDeclaration lvd1 : variableDeclarationInOperation1) {
						List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();
						String candidateRenaming = "";
						boolean found = false;
						boolean hasConsistentReplacement = true;
						for (AbstractCodeMapping abstractCodeMapping : mappings) {

							Set<Replacement> replacements = abstractCodeMapping.getReplacements();
							for (Replacement replacement : replacements) {
								if (replacement instanceof VariableRename) {
									if (replacement.getBefore().contentEquals(lvd1.getVariableName())) {
										if (!found) {
											candidateRenaming = replacement.getAfter();
											found = true;
											break;
										} else if (!(candidateRenaming.contentEquals(replacement.getAfter()))) {
											hasConsistentReplacement = false;
											break;
										}
									}
								}
							}
							if (!hasConsistentReplacement) { // we can later
																// check if in
																// most cases it
																// has
																// consistent
																// replacement
																// then it is a
																// renaming
								break;
							}
						}

						if (hasConsistentReplacement && !candidateRenaming.contentEquals("")) {
							for (VariableDeclaration lvd2 : variableDeclarationInOperation2) {
								if (lvd2.getVariableName().contentEquals(candidateRenaming)) {
									potentialLVDrenamings.put(lvd1, lvd2);
								}
							}

						}
					}

					// now we need to check the other way round
					Set<VariableDeclaration> keytoRemove = new HashSet<VariableDeclaration>();
					for (VariableDeclaration lvd1 : potentialLVDrenamings.keySet()) {
						VariableDeclaration lvd2 = potentialLVDrenamings.get(lvd1);
						List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();
						for (AbstractCodeMapping abstractCodeMapping : mappings) {
							Set<Replacement> replacements = abstractCodeMapping.getReplacements();
							if (replacements instanceof VariableRename) {
								for (Replacement replacement : replacements) {
									if (replacement.getAfter().contentEquals(lvd2.getVariableName())) {
										if (!replacement.getBefore().contentEquals(lvd1.getVariableName())) { // if
																												// lvd2
																												// is
																												// has
																												// another
																												// "before"
																												// then
																												// it
																												// is
																												// not
																												// a
																												// renaming
											keytoRemove.add(lvd1);
										}
									}
								}
							}
						}
					}
					for (VariableDeclaration lv : keytoRemove) {
						potentialLVDrenamings.remove(lv);
					}
				}
			}
			for (VariableDeclaration lvd : potentialLVDrenamings.keySet()) {
				RenameLocalVariable lvr = new RenameLocalVariable(lvd, potentialLVDrenamings.get(lvd),
						umlOperationBodyMapper.getOperation1());
				refactorings.add(lvr);
				System.out.println("*************** " + lvr);
			}
		}

	}
}
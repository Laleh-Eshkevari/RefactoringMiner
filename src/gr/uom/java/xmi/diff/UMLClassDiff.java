package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

import java.util.*;

import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableRename;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import org.refactoringminer.api.Refactoring;

import buginducingcommitanalyzer.RefactoringGranularityAnalysis;
import buginducingcommitanalyzer.RefactoringSourceTargets;

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
	private static final int MAX_OPERATION_POSITION_DIFFERENCE = 5;
	private static final double MAX_OPERATION_NAME_DISTANCE = 0.34;
	
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
		if(!superclassChanged && oldSuperclass != null && newSuperclass != null)
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
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			if(originalOperation.equalSignature(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.equalSignature(operation))
				return removedOperation;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute removedAttribute : removedAttributes) {
			if(removedAttribute.equalsIgnoringChangedVisibility(attribute))
				return removedAttribute;
		}
		return null;
	}

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			operationDiffList.isEmpty() && attributeDiffList.isEmpty() &&
			operationBodyMapperList.isEmpty() &&
			!visibilityChanged && !abstractionChanged;
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

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	public void checkForAttributeChanges() {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute);
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					attributeDiffList.add(attributeDiff);
					break;
				}
			}
		}
	}
	
	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation, UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		return Math.abs(index1-index2);
	}

	public void checkForOperationSignatureChanges() {
		RenameOperationRefactoring rename = null;
		int absoluteDifference = Math.abs(removedOperations.size() - addedOperations.size());
		int maxDifferenceInPosition = absoluteDifference > MAX_OPERATION_POSITION_DIFFERENCE ? absoluteDifference : MAX_OPERATION_POSITION_DIFFERENCE;
		if(removedOperations.size() <= addedOperations.size()) {
			for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					removedOperation = bestMapper.getOperation1();
					UMLOperation addedOperation = bestMapper.getOperation2();
					addedOperations.remove(addedOperation);
					removedOperationIterator.remove();

					UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
					operationDiffList.add(operationDiff);
					if(!removedOperation.getName().equals(addedOperation.getName())) {
						rename = new RenameOperationRefactoring(removedOperation, addedOperation);
						rename.analyzeRefGranularity(bestMapper);
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(bestMapper);
				}
			}
		}
		else {
			for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();
					updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
					List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
					for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
						updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
					}
				}
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
					UMLOperation removedOperation = bestMapper.getOperation1();
					addedOperation = bestMapper.getOperation2();
					removedOperations.remove(removedOperation);
					addedOperationIterator.remove();

					UMLOperationDiff operationDiff = new UMLOperationDiff(removedOperation, addedOperation);
					operationDiffList.add(operationDiff);
					if(!removedOperation.getName().equals(addedOperation.getName())) {
						rename = new RenameOperationRefactoring(removedOperation, addedOperation);
						rename.analyzeRefGranularity(bestMapper);
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(bestMapper);
				}
			}
		}
		if( rename != null){
			RefactoringSourceTargets refactoringSourceTargets= RefactoringGranularityAnalysis.containsSourceBeforeRef(this.getOriginalClass());
			if(refactoringSourceTargets == null){
				refactoringSourceTargets = new RefactoringSourceTargets(this.getOriginalClass());
				RefactoringGranularityAnalysis.getRefSourceTargets().add(refactoringSourceTargets);
			}
			Set<UMLClass> aSet = new HashSet<UMLClass> ();
			aSet.add(this.getNextClass());
			refactoringSourceTargets.populateMap(rename, aSet);
			RefactoringGranularityAnalysis.getRefSourceTargets().add(refactoringSourceTargets);
			RefactoringGranularityAnalysis.getBeforeToAfterUMLclass().put(this.getOriginalClass() , this.getNextClass());
			RefactoringGranularityAnalysis.getUmlClassToDiffMap().put(this.getOriginalClass(), this);
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappings > operationBodyMapper.nonMappedElementsT1() &&
					mappings > operationBodyMapper.nonMappedElementsT2() &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMapped(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) {
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, operationInsideAnonymousClass);
		operationBodyMapper.getMappings();
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0 && allMappingsAreExactMatches(operationBodyMapper, mappings)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappings > operationBodyMapper.nonMappedElementsT1() &&
					mappings > operationBodyMapper.nonMappedElementsT2() &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMapped(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					isPartOfMethodExtracted(removedOperation, addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
		}
	}

	private boolean mappedElementsMoreThanNonMapped(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation > nonMappedElementsT2WithoutThoseCallingAddedOperation);
	}

	private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet) {
		List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>(mapperSet);
		UMLOperationBodyMapper bestMapper = mapperSet.first();
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			UMLOperation operation = mapper.getOperation2();
			Set<OperationInvocation> operationInvocations = operation.getBody().getAllOperationInvocations();
			boolean anotherMapperCallsOperation2OfTheBestMapper = false;
			for(OperationInvocation invocation : operationInvocations) {
				if(invocation.matchesOperation(bestMapper.getOperation2()) && !invocation.matchesOperation(bestMapper.getOperation1()) &&
						!removedOperationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation)) {
					anotherMapperCallsOperation2OfTheBestMapper = true;
					break;
				}
			}
			if(anotherMapperCallsOperation2OfTheBestMapper) {
				bestMapper = mapper;
				break;
			}
		}
		return bestMapper;
	}

	private boolean removedOperationContainsMethodInvocationWithTheSameNameAndCommonArguments(OperationInvocation invocation) {
		for(UMLOperation removedOperation : removedOperations) {
			Set<OperationInvocation> removedOperationInvocations = removedOperation.getBody().getAllOperationInvocations();
			for(OperationInvocation removedOperationInvocation : removedOperationInvocations) {
				if(removedOperationInvocation.getMethodName().equals(invocation.getMethodName())) {
					Set<String> argumentIntersection = new LinkedHashSet<String>(removedOperationInvocation.getArguments());
					argumentIntersection.retainAll(invocation.getArguments());
					if(!argumentIntersection.isEmpty()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<OperationInvocation> removedOperationInvocations = removedOperation.getBody().getAllOperationInvocations();
		Set<OperationInvocation> addedOperationInvocations = addedOperation.getBody().getAllOperationInvocations();
		Set<OperationInvocation> intersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = removedOperationInvocations.size() - intersection.size();
		
		Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<OperationInvocation>();
		for(OperationInvocation addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation)) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getBody().getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<OperationInvocation> newIntersection = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);
		
		Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<OperationInvocation>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<OperationInvocation> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			OperationInvocation invocation = operationInvocationIterator.next();
			if(invocation.getMethodName().startsWith("get")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}
	
	private boolean allMappingsAreExactMatches(UMLOperationBodyMapper operationBodyMapper, int mappings) {
		if(mappings == operationBodyMapper.exactMatches()) {
			return true;
		}
		int mappingsWithTypeReplacement = 0;
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.containsTypeReplacement()) {
				mappingsWithTypeReplacement++;
			}
		}
		if(mappings == operationBodyMapper.exactMatches() + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement) {
			return true;
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation, int absoluteDifferenceInPosition) {
		return addedOperation.equalParameterTypes(removedOperation) || addedOperation.overloadedParameterTypes(removedOperation) || addedOperation.replacedParameterTypes(removedOperation) ||
		(
		(absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) &&
		(addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE)
		);
	}
	
	private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for(int i=0; i<originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if(current.equals(removedOperation)) {
				if(i>0) {
					operationBefore1 = originalClassOperations.get(i-1);
				}
				if(i<originalClassOperations.size()-1) {
					operationAfter1 = originalClassOperations.get(i+1);
				}
			}
		}
		
		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for(int i=0; i<nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if(current.equals(addedOperation)) {
				if(i>0) {
					operationBefore2 = nextClassOperations.get(i-1);
				}
				if(i<nextClassOperations.size()-1) {
					operationAfter2 = nextClassOperations.get(i+1);
				}
			}
		}
		
		boolean operationsBeforeMatch = false;
		if(operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = operationBefore1.equalParameterTypes(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName());
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = operationAfter1.equalParameterTypes(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName());
		}
		
		return operationsBeforeMatch || operationsAfterMatch;
	}

	public void checkForInlinedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation1().getBody().getAllOperationInvocations();
					OperationInvocation removedOperationInvocation = null;
					for(OperationInvocation invocation : operationInvocations) {
						if(invocation.matchesOperation(removedOperation)) {
							removedOperationInvocation = invocation;
							break;
						}
					}
					if(removedOperationInvocation != null && !invocationMatchesWithAddedOperation(removedOperationInvocation, mapper.getOperation2().getBody().getAllOperationInvocations())) {
						List<String> arguments = removedOperationInvocation.getArguments();
						List<String> parameters = removedOperation.getParameterNameList();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						for(int i=0; i<parameters.size(); i++) {
							parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
						}
						UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap);
						operationBodyMapper.getMappings();
						int mappings = operationBodyMapper.mappingsWithoutBlocks();
						if(mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT1() || operationBodyMapper.exactMatches() > 0)) {
							InlineOperationRefactoring inlineOperationRefactoring =
									new InlineOperationRefactoring(removedOperation, operationBodyMapper.getOperation2(), operationBodyMapper.getOperation2().getClassName());
							inlineOperationRefactoring.analyzeRefGranularity(mapper, operationBodyMapper);
							
							RefactoringSourceTargets refactoringSourceTargets= RefactoringGranularityAnalysis.containsSourceBeforeRef(this.getOriginalClass());
							if(refactoringSourceTargets == null){
								refactoringSourceTargets= new RefactoringSourceTargets(this.getOriginalClass());
								RefactoringGranularityAnalysis.getRefSourceTargets().add(refactoringSourceTargets);
							}
							Set<UMLClass> aSet = new HashSet<UMLClass> ();
							aSet.add(this.getNextClass());
							refactoringSourceTargets.populateMap(inlineOperationRefactoring, aSet);
							RefactoringGranularityAnalysis.getRefSourceTargets().add(refactoringSourceTargets);
							RefactoringGranularityAnalysis.getBeforeToAfterUMLclass().put(this.getOriginalClass() , this.getNextClass());
							RefactoringGranularityAnalysis.getUmlClassToDiffMap().put(this.getOriginalClass(), this);
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

	private boolean invocationMatchesWithAddedOperation(OperationInvocation removedOperationInvocation, Set<OperationInvocation> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperationInvocation.matchesOperation(addedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public void checkForExtractedOperations() {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
			UMLOperation addedOperation = addedOperationIterator.next();
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
				if(!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty() ||
					!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
					Set<OperationInvocation> operationInvocations = mapper.getOperation2().getBody().getAllOperationInvocations();
					OperationInvocation addedOperationInvocation = null;
					for(OperationInvocation invocation : operationInvocations) {
						if(invocation.matchesOperation(addedOperation)) {
							addedOperationInvocation = invocation;
							break;
						}
					}
					if(addedOperationInvocation != null) {
						List<UMLParameter> originalMethodParameters = mapper.getOperation1().getParametersWithoutReturnType();
						Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters = new LinkedHashMap<UMLParameter, UMLParameter>();
						List<String> arguments = addedOperationInvocation.getArguments();
						List<UMLParameter> parameters = addedOperation.getParametersWithoutReturnType();
						Map<String, String> parameterToArgumentMap = new LinkedHashMap<String, String>();
						for(int i=0; i<parameters.size(); i++) {
							String argumentName = arguments.get(i);
							String parameterName = parameters.get(i).getName();
							parameterToArgumentMap.put(parameterName, argumentName);
							for(UMLParameter originalMethodParameter : originalMethodParameters) {
								if(originalMethodParameter.getName().equals(argumentName)) {
									originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.put(originalMethodParameter, parameters.get(i));
								}
							}
						}
						if(parameterTypesMatch(originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters)) {
							UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(mapper, addedOperation, parameterToArgumentMap);
							operationBodyMapper.getMappings();
							int mappings = operationBodyMapper.mappingsWithoutBlocks();
							if(mappings > 0 && (mappings > operationBodyMapper.nonMappedElementsT2() || operationBodyMapper.exactMatches() > 0)) {
								UMLOperation extractedFromOperationInNewVersion= this.findExistingMappingInOperationBodyMapperFor(operationBodyMapper.getOperation1());
								ExtractOperationRefactoring extractOperationRefactoring =
										new ExtractOperationRefactoring(addedOperation, operationBodyMapper.getOperation1(), extractedFromOperationInNewVersion,operationBodyMapper.getOperation1().getClassName());
								extractOperationRefactoring.analyzeRefGranularity(mapper , operationBodyMapper);
								
								RefactoringSourceTargets refactoringSourceTargets= RefactoringGranularityAnalysis.containsSourceBeforeRef(this.getOriginalClass());
								if(refactoringSourceTargets == null){
									refactoringSourceTargets= new RefactoringSourceTargets(this.getOriginalClass());
									RefactoringGranularityAnalysis.getRefSourceTargets().add(refactoringSourceTargets);
								}
								Set<UMLClass> aSet = new HashSet<UMLClass> ();
								aSet.add(this.getNextClass());
								refactoringSourceTargets.populateMap(extractOperationRefactoring, aSet);
								RefactoringGranularityAnalysis.getBeforeToAfterUMLclass().put(this.getOriginalClass() , this.getNextClass());
								RefactoringGranularityAnalysis.getUmlClassToDiffMap().put(this.getOriginalClass(), this);
								refactorings.add(extractOperationRefactoring);
								mapper.addAdditionalMapper(operationBodyMapper);
								operationsToBeRemoved.add(addedOperation);
							}
							else if(addedOperation.isDelegate() != null && !mapper.getOperation1().getBody().getAllOperationInvocations().contains(addedOperationInvocation)) {
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

	private UMLOperation findExistingMappingInOperationBodyMapperFor(UMLOperation operation1) {
		for(UMLOperationBodyMapper umlOperationBodyMapper : this.operationBodyMapperList){
			if(umlOperationBodyMapper.getOperation1().equals(operation1)){
				return umlOperationBodyMapper.getOperation2();
			}
		}
		return null;
	}

	private boolean parameterTypesMatch(Map<UMLParameter, UMLParameter> originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters) {
		for(UMLParameter key : originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.keySet()) {
			UMLParameter value = originalMethodParametersPassedAsArgumentsMappedToCalledMethodParameters.get(key);
			if(!key.getType().equals(value.getType()) && !key.getType().equalsWithSubType(value.getType())) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append(className).append(":").append("\n");
		if(visibilityChanged) {
			sb.append("\t").append("visibility changed from " + oldVisibility + " to " + newVisibility).append("\n");
		}
		if(abstractionChanged) {
			sb.append("\t").append("abstraction changed from " + (oldAbstraction ? "abstract" : "concrete") + " to " +
					(newAbstraction ? "abstract" : "concrete")).append("\n");
		}
		Collections.sort(removedOperations);
		for(UMLOperation umlOperation : removedOperations) {
			sb.append("operation " + umlOperation + " removed").append("\n");
		}
		Collections.sort(addedOperations);
		for(UMLOperation umlOperation : addedOperations) {
			sb.append("operation " + umlOperation + " added").append("\n");
		}
		Collections.sort(removedAttributes);
		for(UMLAttribute umlAttribute : removedAttributes) {
			sb.append("attribute " + umlAttribute + " removed").append("\n");
		}
		Collections.sort(addedAttributes);
		for(UMLAttribute umlAttribute : addedAttributes) {
			sb.append("attribute " + umlAttribute + " added").append("\n");
		}
		for(UMLOperationDiff operationDiff : operationDiffList) {
			sb.append(operationDiff);
		}
		for(UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper);
		}
		return sb.toString();
	}

	public int compareTo(UMLClassDiff classDiff) {
		return this.className.compareTo(classDiff.className);
	}

	public void checkForRLV() {
		ArrayList<UMLOperationBodyMapper> bodyMapperList = new ArrayList<>(getOperationBodyMapperList());

		for (int index = 0; index < bodyMapperList.size(); index++) {

			UMLOperationBodyMapper umlOperationBodyMapper = bodyMapperList.get(index);
			HashMap<String, String> potentialLVDrenamings = new HashMap<>();

			if (umlOperationBodyMapper.getOperation1().getBody() != null && umlOperationBodyMapper.getOperation2().getBody() != null) {
				List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();
				Set<VariableDeclaration> variableDeclarationInOperation1 = new HashSet<>(umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations());
				Set<VariableDeclaration> variableDeclarationInOperation2 = new HashSet<>(umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations());

				ArrayList<Replacement> replacements = new ArrayList<>(umlOperationBodyMapper.getReplacements());


				if (replacements.size() == 0 || variableDeclarationInOperation1.size() == 0)
					continue;

				for (VariableDeclaration variableDeclaration : variableDeclarationInOperation1
						) {
					potentialLVDrenamings.put(variableDeclaration.getVariableName(), "");
				}

				for (int i = 0; i < mappings.size(); i++) {
					List<String> vars = mappings.get(i).getFragment1().getVariables();
					if (vars.size() > 0) {
						for (int j = 0; j < vars.size(); j++) {
							//check its not in the
						}
					}
				}
			}
		}

	}

	public void checkForRenameLocalVariable() {


		ArrayList<UMLOperationBodyMapper> bodyMapperList = new ArrayList<>(getOperationBodyMapperList());




		for (int index = 0; index < bodyMapperList.size(); index++) {


			UMLOperationBodyMapper umlOperationBodyMapper = bodyMapperList.get(index);
			HashMap<String, String> potentialLVDrenamings = new HashMap<>();
			HashMap<String, String> occupiedTarget = new HashMap<>();
			HashSet<String> operationTwoVars = getVariables(umlOperationBodyMapper);

			UMLOperationBodyMapper b= new UMLOperationBodyMapper(umlOperationBodyMapper.getOperation1(),umlOperationBodyMapper.getOperation2());
			b.getMappings();


//            System.out.println("class name: "+ umlOperationBodyMapper.getOperation1().getClassName()+"\tmethod1: "+ umlOperationBodyMapper.getOperation1().getName()+ "\tmethod2: "+umlOperationBodyMapper .getOperation2().getName());

			if (umlOperationBodyMapper.getOperation1().getBody() != null && umlOperationBodyMapper.getOperation2().getBody() != null) {
				List<AbstractCodeMapping> mappings = umlOperationBodyMapper.getMappings();


				Set<VariableDeclaration> variableDeclarationInOperation1 = new HashSet<>(umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations());
				Set<VariableDeclaration> variableDeclarationInOperation2 = new HashSet<>(umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations());
				//List<VariableDeclaration> variableDeclarationInOperation1 = umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations();
//                List<VariableDeclaration> variableDeclarationInOperation2 = umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations();
				ArrayList<Replacement> replacements = new ArrayList<>(umlOperationBodyMapper.getReplacements());


				if (replacements.size() == 0 || variableDeclarationInOperation1.size() == 0)
					continue;

				for (VariableDeclaration variableDeclaration : variableDeclarationInOperation1
						) {
					potentialLVDrenamings.put(variableDeclaration.getVariableName(), "");
				}

//                for (int i=0;i<variableDeclarationInOperation1.size();i++)
//                {
//                    potentialLVDrenamings.put(variableDeclarationInOperation1.get(i).getVariableName(),"");
//                }

//                for (VariableDeclaration varDec :
//                        variableDeclarationInOperation1) {
//                    potentialLVDrenamings.put(varDec.getVariableName(), "");
//                }

				for (int i = 0; i < replacements.size(); i++) {
					Replacement replacement = replacements.get(i);

					if (replacement instanceof VariableRename) {
						if (potentialLVDrenamings.containsKey(replacement.getBefore())) {
							String candidate = potentialLVDrenamings.get(replacement.getBefore());

							boolean variableExistInBoth = isVariableExistInBoth(replacement.getBefore(), replacement.getAfter(), variableDeclarationInOperation1, variableDeclarationInOperation2);

							if (candidate == "" && !variableExistInBoth && getVariable(replacement.getAfter(), variableDeclarationInOperation2) != null) {
								potentialLVDrenamings.put(replacement.getBefore(), replacement.getAfter());
							} else if (!candidate.equals(replacement.getAfter()) || variableExistInBoth)
								potentialLVDrenamings.remove(replacement.getBefore());


						}
					} else if (replacement instanceof VariableReplacementWithMethodInvocation) {
						potentialLVDrenamings.remove(replacement.getBefore());

					} else if (replacement instanceof MethodInvocationReplacement) {
						if (!replacement.getAfter().contains(".") || !replacement.getBefore().contains("."))
							continue;

						String baseVar = replacement.getBefore().substring(0, replacement.getBefore().indexOf("."));
						String refVar = replacement.getAfter().substring(0, replacement.getAfter().indexOf("."));

						//last part of the condition should change
						if (!findVariableByName(baseVar, variableDeclarationInOperation1) && !findVariableByName(refVar, variableDeclarationInOperation2) || baseVar.equals(refVar))
							continue;

						boolean variableExistInBoth = isVariableExistInBoth(baseVar, refVar, variableDeclarationInOperation1, variableDeclarationInOperation2);

						if (!baseVar.contains("(") && !refVar.contains("(")) {
							if (potentialLVDrenamings.containsKey(baseVar)) {
								String candidate = potentialLVDrenamings.get(baseVar);
								if (candidate == "" && !variableExistInBoth && getVariable(refVar, variableDeclarationInOperation2) != null)
									potentialLVDrenamings.put(baseVar, refVar);
								else if (!candidate.equals(refVar) || variableExistInBoth)
									potentialLVDrenamings.remove(replacement.getBefore());
							}
						}
//                        else
//                            potentialLVDrenamings.remove(replacement.getBefore());


					}


				}

//                for (Replacement replacement : replacements) {
//                    if (replacement instanceof VariableRename) {
//                        if (potentialLVDrenamings.containsKey(replacement.getBefore())) {
//                            String candidate = potentialLVDrenamings.get(replacement.getBefore());
//
//                            if (candidate == "") {
//                                potentialLVDrenamings.put(replacement.getBefore(), replacement.getAfter());
//                            } else if (!candidate.equals(replacement.getAfter()))
//                                potentialLVDrenamings.remove(replacement.getBefore());
//                        }
//
//
//                    }
//
//                }

				for (String key :
						potentialLVDrenamings.keySet()) {
					//this might be changed to the results of the rename method since the local variable can only happend in the same method and the only way that the method name can be different is method renaming!
					if (potentialLVDrenamings.get(key) != "") {
						RenameLocalVariable lvr = new RenameLocalVariable(getVariable(key, variableDeclarationInOperation1), getVariable(potentialLVDrenamings.get(key), variableDeclarationInOperation2), umlOperationBodyMapper.getOperation1());
						refactorings.add(lvr);
						System.out.println("=============== " + lvr);
					}

				}

			}

			// here if potentialLVDrenamings.size()>0 then create LVD rename refactoring
		}
	}

	private boolean isVariableExistInBoth(String baseVar, String refVar, Set<VariableDeclaration> baseVars, Set<VariableDeclaration> refVars) {
		boolean variableExistInBoth = findVariableByName(baseVar, refVars);
		boolean secondVarExistInBoth = findVariableByName(refVar, baseVars);

		if (!findVariableByName(refVar, refVars) && variableExistInBoth)
			return true;


		if (variableExistInBoth) {
			VariableDeclaration selected = compareCandidates(refVar, baseVar, baseVars, refVars);
			if (selected.getVariableName().equals(baseVar))
				return true;

		}

		if (secondVarExistInBoth) {
			VariableDeclaration selected = compareCandidates(baseVar, refVar, refVars, baseVars);
			if (selected.getVariableName().equals(refVar))
				return true;
		}

		return false;
	}


	//we can get the variable name in the first place
	private boolean findVariableByName(String name, Set<VariableDeclaration> vars) {
		for (VariableDeclaration variableDeclaration : vars
				) {
			if (variableDeclaration.getVariableName().equals(name))
				return true;

		}
		return false;
	}

	private VariableDeclaration compareCandidates(String after, String before, Set<VariableDeclaration> baseVars, Set<VariableDeclaration> refVars) {
		VariableDeclaration first = getVariable(before, refVars);
		VariableDeclaration second = getVariable(after, refVars);
		VariableDeclaration base = getVariable(before, baseVars);
		try {
			int firstVarSimilarity = (stringHandle(first.getVariableName()).equals(stringHandle(base.getVariableName())) ? 1 : 0) + (stringHandle(first.getVariableType()).equals(stringHandle(base.getVariableType())) ? 1 : 0) + (stringHandle(first.getInitializer()).replace(" ", "").equals(stringHandle(base.getInitializer()).replace(" ", "")) ? 1 : 0);
			int secondVarSimilarity = (stringHandle(second.getVariableName()).equals(stringHandle(base.getVariableName())) ? 1 : 0) + (stringHandle(second.getVariableType()).equals(stringHandle(base.getVariableType())) ? 1 : 0) + (stringHandle(second.getInitializer()).replace(" ", "").equals(stringHandle(base.getInitializer()).replace(" ", "")) ? 1 : 0);

			//we need to take to account the fact that they might be eqaul so we need a more sophosticated approach!
			return firstVarSimilarity >= secondVarSimilarity ? first : second;
		} catch (Exception e) {
			return first;
		}

	}

	private HashSet<String> getVariables(UMLOperationBodyMapper mapper) {
		HashSet<String> vars = new HashSet<>();

		for (AbstractCodeMapping mapping :
				mapper.getMappings()) {
			vars.addAll(mapping.getFragment2().getVariables());
		}

		return vars;
	}

	private String stringHandle(String str) {
		return str == null ? "" : str;
	}

	private VariableDeclaration getVariable(String name, Set<VariableDeclaration> vars) {
		for (VariableDeclaration var :
				vars) {
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
			// check if operation1 and operation2 have local variable declarations
			if (umlOperationBodyMapper.getOperation1().getBody() != null && umlOperationBodyMapper.getOperation2().getBody() != null) {
				List<VariableDeclaration> variableDeclarationInOperation1 = umlOperationBodyMapper.getOperation1().getBody().getCompositeStatement().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarationInOperation2 = umlOperationBodyMapper.getOperation2().getBody().getCompositeStatement().getVariableDeclarations();

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
							if (!hasConsistentReplacement) { // we can later check if in most cases it has consistent replacement then it is a renaming
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
										if (!replacement.getBefore().contentEquals(lvd1.getVariableName())) { // if lvd2 is has another "before" then it is not a renaming
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
				RenameLocalVariable lvr = new RenameLocalVariable(lvd, potentialLVDrenamings.get(lvd), umlOperationBodyMapper.getOperation1());
				refactorings.add(lvr);
				System.out.println("*************** " + lvr);
			}
		}
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getNextClass() {
		return nextClass;
	}
	
	
}

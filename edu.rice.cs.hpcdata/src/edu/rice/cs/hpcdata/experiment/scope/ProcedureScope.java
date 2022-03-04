//////////////////////////////////////////////////////////////////////////
//									//
//	ProcedureScope.java						//
//									//
//	experiment.scope.ProcedureScope -- a procedure scope		//
//	Last edited: August 10, 2001 at 2:22 pm				//
//									//
//	(c) Copyright 2002-2022 Rice University. All rights reserved.	//
//									//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpcdata.experiment.scope;

import java.util.List;

import edu.rice.cs.hpcdata.experiment.scope.filters.MetricValuePropagationFilter;
import edu.rice.cs.hpcdata.experiment.scope.visitors.IScopeVisitor;
import edu.rice.cs.hpcdata.experiment.source.SourceFile;
import edu.rice.cs.hpcdata.util.IUserData;




//////////////////////////////////////////////////////////////////////////
//	CLASS PROCEDURE-SCOPE						//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * A procedure scope in an HPCView experiment.
 *
 */


public class ProcedureScope extends Scope  implements IMergedScope 
{
	final static public int FeatureProcedure   = 0;
	final static public int FeaturePlaceHolder = 1;
	final static public int FeatureRoot 	   = 2;
	final static public int FeatureElided      = 3;
	final static public int FeatureTopDown     = 4;
	
	final static public String INLINE_NOTATION = "[I] ";

	private static final String TheProcedureWhoShouldNotBeNamed = "-";
	private static final String TheInlineProcedureLabel 	 	= "<inline>";

	public static enum ProcedureType {
		ProcedureNormal, 
		ProcedureInlineFunction, 
		ProcedureInlineMacro, 
		ProcedureRoot,
		
		VariableDynamicAllocation, 
		VariableStatic, 
		VariableUnknown, 
		VariableAccess
	}

	
	final private int procedureFeature;
	
	private ProcedureType type;
	/** The name of the procedure. */
	protected String procedureName;
	protected boolean isalien;
	// we assume that all procedure scope has the information on load module it resides
	protected LoadModuleScope objLoadModule;


	/**
	 * scope ID of the procedure frame. The ID is given by hpcstruct and hpcprof
	 */
	//protected int iScopeID;

//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION	
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a ProcedureScope.
 * Laks 2008.08.25: We need a special constructor to accept the SID
 * 
 * @param experiment
 * @param file
 * @param first
 * @param last
 * @param proc
 * @param sid
 * @param _isalien
 */
public ProcedureScope(RootScope root, LoadModuleScope loadModule, SourceFile file, 
		int first, int last, String proc, boolean _isalien, int cct_id, int flat_id, 
		IUserData<String,String> userData, int procedureFeature)
{
	super(root, file, first, last, cct_id, flat_id);
	this.isalien = _isalien;
	this.procedureName = proc;

	if (userData != null) {
		String newName = userData.get(proc);
		if (newName != null) 
			procedureName = newName;
	}
	if (isalien) {
		if (procedureName.isEmpty() || procedureName.equals(TheProcedureWhoShouldNotBeNamed)
				|| procedureName.equals(TheInlineProcedureLabel)) {
			procedureName =  "inlined from " + getSourceCitation();
		}
		if (!procedureName.startsWith(INLINE_NOTATION))
			procedureName = INLINE_NOTATION + procedureName;
	}
	this.objLoadModule 	  = null;
	this.procedureFeature = procedureFeature;
	//this.iScopeID = sid;
	this.objLoadModule = loadModule;
}


//////////////////////////////////////////////////////////////////////////
//	SCOPE DISPLAY	
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the user visible name for this scope.
 ************************************************************************/
	
public String getName()
{	
	return procedureName;
}


/*************************************************************************
 *	Return a duplicate of this procedure scope, 
 *  minus the tree information .
 ************************************************************************/

public Scope duplicate() {
	ProcedureScope ps = new ProcedureScope(this.root,
			this.objLoadModule,
			this.sourceFile, 
			this.firstLineNumber, 
			this.lastLineNumber,
			this.procedureName,
			this.isalien,
			getCCTIndex(), // Laks 2008.08.26: add the sequence ID
			this.flat_node_index,
			null,
			this.procedureFeature);

	ps.setProcedureType(type);
	
	return ps;
}

public boolean isAlien() {
	return this.isalien;
}

//////////////////////////////////////////////////////////////////////////
//support for visitors													//
//////////////////////////////////////////////////////////////////////////

public void accept(IScopeVisitor visitor, ScopeVisitType vt) {
	visitor.visit(this, vt);
}

//////////////////////////////////////////////////////////////////////////
//support for Flat visitors												//
//////////////////////////////////////////////////////////////////////////

public LoadModuleScope getLoadModule() {
	return this.objLoadModule;
}
/*
public int getSID() {
	return this.iScopeID;
} */


@Override
public List<Scope> getAllChildren(/*AbstractFinalizeMetricVisitor finalizeVisitor, PercentScopeVisitor percentVisitor,*/
		MetricValuePropagationFilter inclusiveOnly,
		MetricValuePropagationFilter exclusiveOnly) 
{
	return this.getChildren();
}

public boolean isTopDownProcedure() 
{	
	return procedureFeature == FeatureTopDown;
}

public boolean isFalseProcedure()
{
	return procedureFeature != FeatureProcedure;
}

public boolean toBeElided() 
{
	return procedureFeature == FeatureElided;
}

public void setProcedureType(ProcedureType type) {
	this.type = type;
}

public ProcedureType getProcedureType() {
	return this.type;
}



@Override
public boolean hasScopeChildren() {
	return node.hasChildren();
}

}



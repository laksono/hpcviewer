package edu.rice.cs.hpcdata.experiment.scope.visitors;

import java.util.List;

import edu.rice.cs.hpcdata.experiment.Experiment;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric;
import edu.rice.cs.hpcdata.experiment.scope.AlienScope;
import edu.rice.cs.hpcdata.experiment.scope.CallSiteScope;
import edu.rice.cs.hpcdata.experiment.scope.FileScope;
import edu.rice.cs.hpcdata.experiment.scope.GroupScope;
import edu.rice.cs.hpcdata.experiment.scope.LineScope;
import edu.rice.cs.hpcdata.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpcdata.experiment.scope.LoopScope;
import edu.rice.cs.hpcdata.experiment.scope.ProcedureScope;
import edu.rice.cs.hpcdata.experiment.scope.RootScope;
import edu.rice.cs.hpcdata.experiment.scope.Scope;
import edu.rice.cs.hpcdata.experiment.scope.ScopeVisitType;
import edu.rice.cs.hpcdata.experiment.scope.StatementRangeScope;

public class PercentScopeVisitor implements IScopeVisitor {
	RootScope root;
	List<BaseMetric> listMetrics;
	

	
	public PercentScopeVisitor(RootScope r) {
		root = r;
		Experiment exp = (Experiment) r.getExperiment();
		listMetrics = exp.getMetricList();
	}
	
	//----------------------------------------------------
	// visitor pattern instantiations for each Scope type
	//----------------------------------------------------

	public void visit(Scope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(RootScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LoadModuleScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(FileScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(ProcedureScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(AlienScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LoopScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(CallSiteScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LineScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(GroupScope scope, ScopeVisitType vt) { calc(scope, vt); }

	//----------------------------------------------------
	// propagate a child's metric values to its parent
	//----------------------------------------------------

	protected void calc(Scope scope, ScopeVisitType vt) {
		if (vt == ScopeVisitType.PostVisit) {
			setPercentValue(scope);
		}
	}
	
	
	/***
	 * Compute and set the percent of a scope
	 * @param scope: scope in which a percent needs to be counted
	 * @param root: the root scope
	 * @param num_metrics: number of metrics
	 */
	protected void setPercentValue(Scope scope) {
	}
}

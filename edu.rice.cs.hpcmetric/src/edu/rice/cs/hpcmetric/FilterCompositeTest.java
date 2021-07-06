package edu.rice.cs.hpcmetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import edu.rice.cs.hpcdata.experiment.Experiment;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric;
import edu.rice.cs.hpcdata.experiment.metric.Metric;
import edu.rice.cs.hpcdata.experiment.metric.MetricType;
import edu.rice.cs.hpcdata.experiment.metric.MetricValue;
import edu.rice.cs.hpcdata.experiment.scope.RootScope;
import edu.rice.cs.hpcdata.experiment.scope.RootScopeType;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric.AnnotationType;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric.VisibilityType;


public final class FilterCompositeTest {

	
	public static void main(String[] args) {
		final VisibilityType []vt = {VisibilityType.INVISIBLE, 
									 VisibilityType.HIDE, 
									 VisibilityType.SHOW, 
									 VisibilityType.SHOW_EXCLUSIVE,
									 VisibilityType.SHOW_INCLUSIVE};
		System.out.println("Test start");
		
		final Display display = new Display();
		final Shell   shell   = new Shell(display);

		GridDataFactory.swtDefaults().grab(true, true).applyTo(shell);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(shell);

		List<BaseMetric> list = new ArrayList<BaseMetric>();
		TreeColumn []columns  = new TreeColumn[100];
		Tree tree = new Tree(shell, SWT.NONE);
		
		Random r = new Random();
		
		for (int i=0; i<100; i++)  {
			MetricType mt = (i%2 == 0)? MetricType.INCLUSIVE : MetricType.EXCLUSIVE;
			
			BaseMetric data = new Metric(String.valueOf(i), 
										 "Description of metric " + i, 
										 "Metric " + i, 
										 vt[r.nextInt(5)], 
										 null, 
										 AnnotationType.PERCENT, 
										 "", 
										 i, 
										 mt, 
										 i+1);
			list.add(data);
			columns[i] = new TreeColumn(tree, SWT.NONE);
			columns[i].setText("col " + i);
			columns[i].setData(data);
			columns[i].setWidth(r.nextInt(10) == 1 ? 0 : 100);
		}
		Experiment exp = new Experiment();
		exp.setMetrics(list);
		RootScope root = new RootScope(exp, "root", RootScopeType.CallingContextTree);
		
		for (int i=0; i<90; i++) {
			if (r.nextInt(10) == 0)
				continue; // randomly, empty metric
			
			MetricValue mv = new MetricValue((i+1) * (1+i) * 100000);
			root.setMetricValue(i, mv);
		}
		
		MetricFilterInput input = new MetricFilterInput();
		input.setFilterList(list, columns);
		input.setMetricManager(exp);
		input.setRoot(root);
		input.setAffectAll(true);
		
		AbstractFilterComposite c = new AbstractFilterComposite(shell, SWT.NONE, input) {
			
			@Override
			protected void createAdditionalButton(Composite parent) {}
		};

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();


		System.out.println("Test end");
	}

}

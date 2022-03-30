package edu.rice.cs.hpctest.data;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.rice.cs.hpcdata.experiment.Experiment;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric;
import edu.rice.cs.hpcdata.experiment.metric.BaseMetric.AnnotationType;
import edu.rice.cs.hpcdata.experiment.metric.DerivedMetric;
import edu.rice.cs.hpcdata.experiment.scope.RootScope;
import edu.rice.cs.hpcdata.experiment.scope.RootScopeType;
import edu.rice.cs.hpcdata.experiment.scope.TreeNode;

public class ExperimentTest {

	private static Experiment []experiments;
	private static File []database;
	private static String []dbPaths = new String[] {"bug-no-gpu-trace", "bug-empty", "bug-nometric"};
	private static int []children   = new int[] {1, 0, 0};
	
	public ExperimentTest() {
		// empty, nothing to do
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		experiments = new Experiment[dbPaths.length];
		database   = new File[dbPaths.length];
		int i=0;
		for (String dbp: dbPaths) {
			
			Path resource = Paths.get("..", "resources", dbp);
			database[i] = resource.toFile();
			
			assertNotNull(database);

			experiments[i]= new Experiment();
			try {
				experiments[i].open(database[i], null, Experiment.ExperimentOpenFlag.TREE_ALL);
			} catch (Exception e) {
				assertFalse(e.getMessage(), true);
			}
			
			assertNotNull(experiments[i].getRootScope());
			i++;
		}
	}


	@Test
	public void testIsMergedDatabase() {
		for(var exp: experiments) {
			assertFalse(exp.isMergedDatabase());
		}
	}


	@Test
	public void testGetRawMetrics() {
		for(var exp: experiments) {
			List<BaseMetric> metrics = exp.getRawMetrics();
			assertNull(metrics);
		}
	}

	@Test
	public void testGetVisibleMetrics() {
		int num[] = new int[] {97, 2, 0};
		int i = 0;
		for(var experiment: experiments) {
			List<BaseMetric> metrics = experiment.getVisibleMetrics();
			assertNotNull(metrics);
			assertTrue(metrics.size() == num[i]);
			i++;
		}
	}

	@Test
	public void testGetNonEmptyMetricIDs() {
		final int nmetrics[] = new int[] {18, 0, 0};
		int i=0;
		for(var experiment: experiments) {
			RootScope root = experiment.getRootScope(RootScopeType.CallingContextTree);
			List<Integer> metrics = experiment.getNonEmptyMetricIDs(root);
			assertNotNull(metrics);
			assertTrue(metrics.size() >= nmetrics[i]);
			i++;
		}
	}

	@Test
	public void testGetMetricCount() {
		int counts[] = new int[] {10, 0, 0};
		int i=0;
		
		for(var experiment: experiments) {
			assertTrue(experiment.getMetricCount() >= counts[i]);
			i++;
		}
	}

	@Test
	public void testGetMetricInt() {
		for(var experiment: experiments) {
			List<BaseMetric> list = experiment.getVisibleMetrics();
			if (list != null && list.size()>0) {
				BaseMetric metric = experiment.getMetric(list.get(0).getIndex());
				assertNotNull(metric);
				assertEquals(list.get(0), metric);
			}
		}
	}

	@Test
	public void testGetMetricString() {
		for(var experiment: experiments) {
			List<BaseMetric> list = experiment.getVisibleMetrics();
			if (list != null && list.size()>0) {
				BaseMetric metric = experiment.getMetric(list.get(0).getShortName());
				assertNotNull(metric);
			}
		}
	}

	@Test
	public void testGetMetricFromOrder() {
		for(var experiment: experiments) {
			if (experiment.getMetricCount()>0) {
				BaseMetric metric = experiment.getMetricFromOrder(0);
				assertNotNull(metric);				
			}
		}
	}

	@Test
	public void testAddDerivedMetric() {
		int indexes[] = new int[] {762, 0, 0};
		int i=0;
		
		for(var experiment: experiments) {
			if (experiment.getMetricCount()==0) 
				continue;
			
			RootScope root = experiment.getRootScope(RootScopeType.CallingContextTree);
			BaseMetric metric = experiment.getMetric(indexes[i]);
			int numMetrics = experiment.getMetricCount();
			DerivedMetric dm = new DerivedMetric(root, 
										experiment, 
										"$" + metric.getIndex(), 
										"DM " + metric.getDisplayName(), 
										String.valueOf(numMetrics), 
										numMetrics, AnnotationType.NONE, metric.getMetricType());
			experiment.addDerivedMetric(dm);
			
			assertTrue(experiment.getMetricCount() == numMetrics + 1);
			i++;
		}
	}

	@Test
	public void testGetRootScope() {
		int i=0;
		for(var experiment: experiments) {
			RootScope rootCCT = experiment.getRootScope(RootScopeType.CallingContextTree);
			RootScope rootCall = experiment.getRootScope(RootScopeType.CallerTree);
			RootScope rootFlat = experiment.getRootScope(RootScopeType.Flat);
			
			assertNotNull(rootCCT);
			assertNotNull(rootCall);
			assertNotNull(rootFlat);
			
			assertTrue(rootCCT != rootCall);
			assertTrue(rootCall != rootFlat);
			
			rootCall = experiment.createCallersView(rootCCT, rootCall);
			rootFlat = experiment.createFlatView(rootCCT, rootFlat);
			
			assertTrue(rootCCT.getChildCount()  >= children[i]);
			assertTrue(rootCall.getChildCount() >= children[i]);
			assertTrue(rootFlat.getChildCount() >= children[i]);

			i++;
		}
	}

	@Test
	public void testGetDataSummary() {
		for(var experiment: experiments) {
			try {
				experiment.getDataSummary();
				assertFalse(true);
				
			} catch (IOException e) {
				assertTrue("Correct: no summary", true);
			}
		}
	}

	@Test
	public void testGetThreadData() {
		for(var experiment: experiments) {
			assertNull(experiment.getThreadData());
		}
	}

	@Test
	public void testGetMajorVersion() {
		for(var experiment: experiments) {
			assertTrue(experiment.getMajorVersion() == 2);
		}
	}

	@Test
	public void testGetMinorVersion() {
		for(var experiment: experiments) {
			assertTrue(experiment.getMinorVersion() == 2);
		}
	}

	@Test
	public void testGetMaxDepth() {
		final int maxdepth[] = new int[] {4, 0, 0};
		int i=0;
		for(var experiment: experiments) {
			assertTrue(experiment.getMaxDepth() > maxdepth[i]);
			i++;
		}
	}

	@Test
	public void testGetScopeMap() {
		for(var experiment: experiments) {
			assertNotNull(experiment.getScopeMap());
		}
	}

	@Test
	public void testGetRootScopeChildren() {
		for(var experiment: experiments) {
			List<TreeNode> children = experiment.getRootScopeChildren();
			assertNotNull(children);
			assertTrue(children.size() == 3);
		}
	}

	@Test
	public void testGetRootScopeRootScopeType() {
		for(var experiment: experiments) {
			RootScope root = experiment.getRootScope(RootScopeType.CallingContextTree);
			assertNotNull(root);
		}
	}
	

	@Test
	public void testOpenFileIUserDataOfStringStringBoolean() {
		for(var experiment: experiments) {
			Experiment exp = new Experiment();
			try {
				exp.open(experiment.getDefaultDirectory(), null, true);
			} catch (Exception e) {
				assertFalse(e.getMessage(), true);
			}
			assertTrue(exp.getName().equals(experiment.getName()));
		}
	}

	@Test
	public void testGetName() {
		final String []names = new String[] {"bandwidthTest", "a.out", "a.out"};
		int i=0;
		for(var experiment: experiments) {
			String name = experiment.getName();
			assertNotNull(name.equals(names[i]));
			i++;
		}
	}

	@Test
	public void testGetConfiguration() {
		for(var experiment: experiments) {
			assertNotNull(experiment.getConfiguration());
		}
	}

	@Test
	public void testGetDefaultDirectory() {
		for(var experiment: experiments) {
			File dir = experiment.getDefaultDirectory();
			assertNotNull(dir);
		}
	}

	@Test
	public void testGetExperimentFile() {
		int i=0;
		for(var experiment: experiments) {
			File file = experiment.getExperimentFile();
			assertNotNull(file);
			File dir = file.getParentFile();
			File dbPath = database[i].getAbsoluteFile();
			assertTrue(dir.getAbsolutePath().equals(dbPath.getAbsolutePath()));
			i++;
		}
	}

}
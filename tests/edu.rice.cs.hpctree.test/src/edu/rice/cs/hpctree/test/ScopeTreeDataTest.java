// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctree.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;

import org.hpctoolkit.db.local.experiment.Experiment;
import org.hpctoolkit.db.local.experiment.LocalDatabaseRepresentation;
import org.hpctoolkit.db.local.experiment.scope.RootScopeType;
import org.hpctoolkit.db.local.experiment.scope.Scope;
import edu.rice.cs.hpctest.util.TestDatabase;
import edu.rice.cs.hpctree.ScopeTreeData;

public class ScopeTreeDataTest 
{
	private static ScopeTreeData []treeData;
	private static final Random random = new Random(); 

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		var database    = TestDatabase.getDatabases();
		var experiments = new Experiment[database.length];
		treeData = new ScopeTreeData[database.length];
		 
		int i=0;

		for (var path: database) {
			assertNotNull(path);

			experiments[i]= new Experiment();
			var localDb = new LocalDatabaseRepresentation(path, null, true);
			try {
				experiments[i].open(localDb);
			} catch (Exception e) {
				assertFalse(e.getMessage(), true);
			}			
			assertNotNull(experiments[i].getRootScope());
			
			treeData[i] = new ScopeTreeData(null, experiments[i].getRootScope(RootScopeType.CallingContextTree), experiments[i]);
			
			i++;
		}
	}


	@Test
	public void testMain() {
		for (ScopeTreeData tree: treeData) {
			var root = tree.getRoot();
			assertNotNull(root);
			
			var list = tree.getList();
			assertNotNull(list);

			int numMetrics = tree.getMetricCount();			
			int i = numMetrics == 0 ? 0 : random.nextInt(numMetrics);
			tree.sort(i, SortDirectionEnum.ASC, false);
			
			for (i=0; i<numMetrics; i++) {
				var metric = tree.getMetric(i);
				assertNotNull(metric);
			}
			
			int depth = 0;
			int numScopes = 1;
			
			assertTrue( tree.getDepthOfData(root) == depth);
			Scope scope = root;
			
			while (scope.hasChildren()) {
				int sortColumn = Math.min(1, numMetrics);
				tree.sort(sortColumn, SortDirectionEnum.DESC, false);
				list = tree.getList();

				var listChildren = tree.getChildren(scope);
				list.addAll(listChildren);
				
				int numChildren = listChildren.size();
				int index = random.nextInt(numChildren);				
				scope = listChildren.get(index);
				
				depth++;
				int d = tree.getDepthOfData(scope);
				Assert.assertEquals(depth, d);
				
				if (scope.getParentScope() != null) {
					d = tree.getDepthOfData(scope.getParentScope());
					assertEquals(d, depth-1);
				}
				var child = tree.getDataAtIndex(numScopes + index);
				assertNotNull(child);
				assertTrue(child == scope);
				
				var path = tree.getPath(scope);
				assertEquals(path.get(0).getParentScope(), root);
				assertEquals(path.get(path.size()-1), scope);
				
				checkMetrics(list, tree);
				
				numScopes += numChildren;
			}
		}
	}
	
	private void checkMetrics(List<Scope> list, ScopeTreeData tree) {
		var numMetrics = tree.getMetricCount();
		
		for(int j=1; j<list.size(); j++) {
			for(int k=0; k<numMetrics; k++) {
				tree.sort(k, SortDirectionEnum.DESC, false);
				list = tree.getList();

				var node = list.get(j);
				var parent = node.getParentScope();
				
				int depthChild = tree.getDepthOfData(node);
				int depthParent = tree.getDepthOfData(parent);
				
				assertTrue(depthParent == depthChild - 1);
			}
		}
	}
} 

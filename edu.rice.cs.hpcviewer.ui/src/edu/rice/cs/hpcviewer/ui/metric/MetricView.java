package edu.rice.cs.hpcviewer.ui.metric;

import java.util.Arrays;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.osgi.service.prefs.Preferences;

import edu.rice.cs.hpcbase.ViewerDataEvent;
import edu.rice.cs.hpcbase.map.UserInputHistory;
import edu.rice.cs.hpcdata.experiment.Experiment;
import edu.rice.cs.hpcmetric.AbstractFilterPane;
import edu.rice.cs.hpcmetric.MetricFilterInput;
import edu.rice.cs.hpcmetric.internal.MetricFilterDataItem;
import edu.rice.cs.hpcviewer.ui.internal.AbstractUpperPart;


/***************************************************************
 * 
 * Independent view to display the metric properties:
 * 
 * <ul>
 *  <li> visible or not
 *  <li> name of the metric
 *  <li> its long description
 *  <li> its aggregate value. It's important to show if the metric is empty or not
 * </ul>
 * The caller has to listen the event of {@code ViewerDataEvent.TOPIC_HIDE_SHOW_COLUMN}
 * which contain data which metric (or column) to be shown or hidden
 ***************************************************************/
public class MetricView extends AbstractUpperPart  
{
	private static final String HISTORY_COLUMN_PROPERTY = "column_property";
	private static final String HISTORY_APPLY_ALL = "apply-all";
	public static final  String INPUT_DEFAULT = "edu.rice.cs.hpcviewer.ui.metric.MetricView";

	private final IEventBroker eventBroker ;
	private final CTabFolder parent;
	private Button btnApplyToAllViews;
	private MetricFilterInput inputFilter;

	public MetricView(CTabFolder parent, int style, IEventBroker eventBroker ) {
		super(parent, style);
		this.parent = parent;
		this.eventBroker = eventBroker;
		
		setText("Metric properties");
		setShowClose(true);
	}
	
	
	@Override
	public String getTitle() {
		return "Metric properties";
	}

	@Override
	public void setInput(Object input) {
		if (input == null || !(input instanceof MetricFilterInput))
			return;
		inputFilter = (MetricFilterInput) input;

		Composite container = new Composite(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		
		AbstractFilterPane pane = new AbstractFilterPane(container, SWT.NONE, inputFilter) {
			
			@Override
			public void changeEvent(Object data) {
				broadcast(data);
			}

			
			@Override
			protected void createAdditionalButton(Composite parent) {

				btnApplyToAllViews = new Button(parent, SWT.CHECK);
				btnApplyToAllViews.setText("Apply to all views");
				btnApplyToAllViews.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
				btnApplyToAllViews.setEnabled(inputFilter.isAffectAll());
				boolean checked = getHistory() && inputFilter.isAffectAll();
				btnApplyToAllViews.setSelection( checked );
				
				btnApplyToAllViews.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public void widgetSelected(SelectionEvent e) {
						// if we select to apply to all views, we should notify all the views
						// to reflect changes of the column hide/show
						boolean selected = btnApplyToAllViews.getSelection();
						if (selected) {
							broadcast(getList());
						}
						// make sure we store the current selection, to be reused for the next session
						// this isn't critical, but it's just nice to memorize the previous state
						
						Preferences pref = UserInputHistory.getPreference(HISTORY_COLUMN_PROPERTY);
						pref.putBoolean(HISTORY_APPLY_ALL, selected);
						UserInputHistory.setPreference(pref);
					}
				});
			}
			
			private void broadcast(Object data) {
				List<MetricFilterDataItem> copyList = List.copyOf(getList());
				MetricDataEvent metricDataEvent = new MetricDataEvent(data, copyList, btnApplyToAllViews.getSelection());
				ViewerDataEvent viewerDataEvent = new ViewerDataEvent((Experiment) inputFilter.getMetricManager(), metricDataEvent);
				
				eventBroker.post(ViewerDataEvent.TOPIC_HIDE_SHOW_COLUMN, viewerDataEvent);
			}
		};
		
		setControl(container);
	}

	
	@Override
	public boolean hasEqualInput(Object input) {
		if (input instanceof MetricFilterInput) {
			MetricFilterInput metricInput = (MetricFilterInput) input;
			return metricInput.getMetricManager() == this.inputFilter.getMetricManager();
		}
		return false;
	}

	
	
	@Override
	public void setMarker(int lineNumber) {}


	/***
	 * get the user preference of "apply-all"
	 * @return
	 */
	private boolean getHistory() {
		return UserInputHistory.getPreference(HISTORY_COLUMN_PROPERTY).getBoolean(HISTORY_APPLY_ALL, true);
	}
	
	
	public static class MetricDataEvent
	{
		final private boolean applyToAll;
		final private Object  data;
		final private List<MetricFilterDataItem> list;
		
		public MetricDataEvent(Object data, List<MetricFilterDataItem> list, boolean applyToAll) {
			this.applyToAll = applyToAll;
			this.data = data;
			this.list = list;
		}
		
		public boolean isApplyToAll() {
			return applyToAll;
		}

		public Object getData() {
			return data;
		}

		public List<MetricFilterDataItem> getList() {
			return list;
		}

		@Override
		public String toString() {
			return "All: " + applyToAll + ", data: " + data + ", list size: " + list.size();
		}
	}
}

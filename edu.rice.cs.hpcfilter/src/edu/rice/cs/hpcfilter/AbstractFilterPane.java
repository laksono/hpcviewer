// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcfilter;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.stack.DefaultBodyLayerStack;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.theme.ThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import edu.rice.cs.hpcfilter.internal.CheckBoxConfiguration;
import edu.rice.cs.hpcfilter.internal.FilterConfigLabelAccumulator;
import edu.rice.cs.hpcfilter.internal.FilterPainterConfiguration;
import edu.rice.cs.hpcfilter.internal.FilterRowDataProvider;
import edu.rice.cs.hpcfilter.internal.IConstants;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcsetting.preferences.PreferenceConstants;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpcsetting.table.DayThemeConfiguration;


/***********************************************************************
 * 
 * Base class to have a filter pane which consists of:
 * <ul>
 *   <li>Action buttons area, which consists of:
 *     <ul>
 *       <li>Check all: to check all the enabled visible items</li>
 *       <li>Uncheck all: to clear the visible checked items</li>
 *       <li>Others (to be defined by the child</li>
 *     </ul>
 *   </li>
 *   <li>Text filter area</li>
 *   <li>Table to display list of items to be filtered. The table contains:
 *     <ul>
 *       <li>Column check/uncheck</li>
 *       <li>Label of the item</li>
 *     </ul>
 *   </li>
 * </ul>
 * The template type {@code T} is the unit type of a data item in a table row.
 * It can be anything from a simple String or a complex class.
 *  
 ***********************************************************************/
public abstract class AbstractFilterPane<T> implements IPropertyChangeListener, DisposeListener
{
	public static final int STYLE_COMPOSITE   = 0;
	public static final int STYLE_INDEPENDENT = 1;
	
	private final int style;
	private final FilterInputData<T> inputData;
	
	private NatTable  natTable ;
	private DataLayer dataLayer ;
	
	private DefaultBodyLayerStack defaultLayerStack;
	private RowSelectionProvider<FilterDataItem<T>> rowSelectionProvider;
	private FilterRowDataProvider rowHeaderDataProvider;
	private FilterPainterConfiguration painterConfiguration;

	private EventList<FilterDataItem<T>>  eventList ;
	private FilterList<FilterDataItem<T>> filterList;
	private FilterDataItemSortModel<T>    sortList;
	private FilterConfigLabelAccumulator<T> labelAccumulator;

	private Composite parentContainer ;
	private Text objSearchText;
	private Button btnRegExpression;
	private TextMatcherEditor<FilterDataItem<T>> textMatcher;

	/***
	 * Constructor to create the item and its composite widgets.
	 * 
	 * @param parent 
	 * @param style 
	 * 			{@code int} the style of the pane, either {@code STYLE_COMPOSITE} (application mode)
	 * 			or {@code STYLE_INDEPENDENT} (unit test mode) 
	 * @param inputData {@code FilterInputData}
	 * @see STYLE_COMPOSITE, STYLE_INDEPENDENT
	 */
	protected AbstractFilterPane(Composite parent, int style, FilterInputData<T> inputData) {
		this.inputData = inputData;
		this.style = style;
		
		createContentArea(parent, inputData);

		this.eventList  = createEventList(inputData.getListItems());
		this.filterList = createFilterList(eventList);
		createTable(eventList, filterList);
	}

	
	/****
	 * Reset the content of the table with the new list.
	 * This method is called to change the content of the table only.
	 * 
	 * @param inputData The input data containing the new list
	 */
	public void reset(FilterInputData<T> inputData) {
		this.eventList  = createEventList(inputData.getListItems());
		this.filterList = createFilterList(eventList);

		// this method has to be called AFTER the creation of the table and its layers.
		// If the table is not created, we have to throw an exception
		
		if (this.sortList == null)
			throw new IllegalStateException("Invalid access to reset the table. The table is not created yet.");
		
		this.sortList.setList(filterList);
		
		FilterDataProvider<T> dataProvider = getDataProvider(filterList);

		this.labelAccumulator.setDataProvider(dataProvider);
		this.dataLayer.setDataProvider(dataProvider);
		this.rowHeaderDataProvider.setDataProvider(dataProvider);
		this.rowSelectionProvider.updateSelectionProvider(defaultLayerStack.getSelectionLayer(), dataProvider);		
		
		// Important: need to clear again the search text since we have new filter list
		this.objSearchText.setText("");
	}

	
	/****
	 * Update the data provider to check all items in the list
	 */
	private void checkAll() {
		getDataProvider(filterList).checkAll();
		natTable.refresh(false);
	}

	
	/****
	 * Update the data provider to uncheck all items in the list
	 */
	private void uncheckAll() {
		getDataProvider(filterList).uncheckAll();
		natTable.refresh(false);
	}
	
	
	/*****
	 * Create the content area of the panel.
	 * This method will create the button area and the filter text.
	 * 
	 * @param parent 
	 * 			the parent composite
	 * @param inputData
	 * 			the input.
	 */
	private void createContentArea(Composite parent, FilterInputData<T> inputData) {		
		parentContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parentContainer);

		// prepare the buttons: check and uncheck

		Composite groupButtons = new Composite(parentContainer, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupButtons);		

		// check button
		Button btnCheckAll = new Button(groupButtons, SWT.NONE);
		btnCheckAll.setText("Check all"); 
		btnCheckAll.setToolTipText("Select all the current listed items");
		btnCheckAll.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnCheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkAll();
			}
		});

		// uncheck button
		Button btnUnCheckAll = new Button(groupButtons, SWT.NONE);
		btnUnCheckAll.setText("Uncheck all");
		btnUnCheckAll.setToolTipText("Remove the selection of the current listed items");
		btnUnCheckAll.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnUnCheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				uncheckAll();
			}
		});

		// regular expression option
		btnRegExpression = new Button(groupButtons, SWT.CHECK);
		btnRegExpression.setText("Regular expression");
		btnRegExpression.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnRegExpression.setSelection(false);	
		btnRegExpression.setToolTipText("Option to enable that the text to filter is a regular expression");
		
		// number of buttons: 3 (check, uncheck, regex) + additional buttons
		int numAddButtons = 3 + createAdditionalButton(groupButtons, inputData);
		
		GridLayoutFactory.fillDefaults().numColumns(numAddButtons).applyTo(groupButtons);

		// set the layout for group filter
		Composite groupFilter = new Composite(parentContainer, SWT.NONE);
		groupFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// string to match
		Label lblFilter = new Label (groupFilter, SWT.FLAT);
		lblFilter.setText(getFilterLabel());
		
		objSearchText = new Text (groupFilter, SWT.BORDER);
		objSearchText.setToolTipText("Type text to filter the list");
		
		objSearchText.addModifyListener( event -> eventFilterText(objSearchText.getText()) );

		btnRegExpression.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleRegularExpression();
			}
		});

		int numAddFilters = 2 + createAdditionalFilter(groupFilter, inputData);
		GridLayoutFactory.fillDefaults().numColumns(numAddFilters).applyTo(groupFilter);

		// expand as much as possible horizontally
		GridDataFactory.fillDefaults().grab(true, false).applyTo(objSearchText);
	}
	
	
	/****
	 * Method to retrieve the label of the filter search.
	 * The child class can override this to customize the label.
	 * 
	 * @return
	 */
	protected String getFilterLabel() {
		return "Filter: ";
	}
	
	/****
	 * Retrieve the current data layer of the table
	 * @return
	 */
	protected DataLayer getDataLayer() {
		return dataLayer;
	}
	
	
	/**
	 * Retrieve the input data of this filter pane
	 * 
	 * @return {@code FilterInputData}
	 * 			The base class of filter input data
	 */
	protected FilterInputData<T> getInputData() {
		return inputData;
	}
	
	/****
	 * Create an event list for this table. 
	 * 
	 * @param list
	 * @return
	 */
	private EventList<FilterDataItem<T>> createEventList(List<FilterDataItem<T>> list) {
		return GlazedLists.eventList(list);
	}
	
	
	/****
	 * Create the filter list for the table.
	 * This method will set the text matcher to the new filter list automatically.
	 * 
	 * @param eventList
	 * @return
	 */
	protected FilterList<FilterDataItem<T>> createFilterList(EventList<FilterDataItem<T>> eventList) {
		FilterList<FilterDataItem<T>> fl = new FilterList<>(eventList);
 		
		var currentTextMatcher = getTextMatcher();
		
		fl.setMatcherEditor(currentTextMatcher);
		
		return fl;
	}
	
	
	/*****
	 * Create the sorting model for this table.
	 * 
	 * @param eventList
	 * @return
	 */
	protected FilterDataItemSortModel<T> createSortModel(EventList<FilterDataItem<T>> eventList) {
		return new FilterDataItemSortModel<>(eventList);
	}
	
	
	/*****
	 * Main method to create the table. Should be called after the constructor
	 * 
	 * @param eventList
	 * @param filterList
	 */
	private void createTable(EventList<FilterDataItem<T>> eventList, 
							FilterList<FilterDataItem<T>> filterList) {
		this.objSearchText.setText("");
		
		// ------------------------------------------------------------
		// Start building the nat-table
		// ------------------------------------------------------------
		
		// data layer
		IRowDataProvider<FilterDataItem<T>> dataProvider = getDataProvider(filterList);
		this.dataLayer = new DataLayer(dataProvider);

		GlazedListsEventLayer<FilterDataItem<T>> listEventLayer = new GlazedListsEventLayer<>(dataLayer, eventList);
		defaultLayerStack = new DefaultBodyLayerStack(listEventLayer);
		defaultLayerStack.getSelectionLayer().addConfiguration(new RowOnlySelectionConfiguration());

		// data layer configuration to be implemented by the child class
		// 
		setLayerConfiguration(dataLayer);
		
		// columns header
		IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(getColumnHeaderLabels());
		DataLayer columnDataLayer = new DefaultColumnHeaderDataLayer(columnHeaderDataProvider);
		ColumnHeaderLayer colummnLayer = new ColumnHeaderLayer(columnDataLayer, defaultLayerStack, defaultLayerStack.getSelectionLayer());

		this.sortList = createSortModel(eventList);
		SortHeaderLayer<FilterDataItem<T>> sortHeaderLayer = new SortHeaderLayer<>(colummnLayer, sortList);
		
		// row header
		rowHeaderDataProvider = new FilterRowDataProvider(dataProvider);
		DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, defaultLayerStack, defaultLayerStack.getSelectionLayer());

		// corner layer
		DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		CornerLayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, sortHeaderLayer);
		
		// grid layer
		GridLayer gridLayer = new GridLayer(defaultLayerStack, sortHeaderLayer, rowHeaderLayer, cornerLayer);

		labelAccumulator = new FilterConfigLabelAccumulator<>(defaultLayerStack, dataProvider);
		defaultLayerStack.setConfigLabelAccumulator(labelAccumulator);
		
		// the table
		natTable = new NatTable(parentContainer, gridLayer, false); 

		// additional configuration
		painterConfiguration = new FilterPainterConfiguration();
		natTable.addConfiguration(painterConfiguration);
		natTable.addConfiguration(new CheckBoxConfiguration(ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + IConstants.INDEX_VISIBILITY));
		natTable.addConfiguration(new RowOnlySelectionBindings());
		natTable.addConfiguration(new SingleClickSortConfiguration());
		
		// ------------------------------------------------------------
		// Customized configuration
		// ------------------------------------------------------------
		addConfiguration(natTable);
		
		natTable.configure();

		this.rowSelectionProvider = new RowSelectionProvider<>(
				defaultLayerStack.getSelectionLayer(), 
				dataProvider);

		rowSelectionProvider.addSelectionChangedListener(event-> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            @SuppressWarnings("unchecked")
			Iterator<FilterDataItem<T>> it = selection.iterator();
            
            if (!it.hasNext())
            	return;
            FilterDataItem<T> item = it.next();
            
            selectionEvent(item, SWT.MouseDown);
		});
		
		// this UI binding causes mouse click event to be extremely slow
		/*
		natTable.getUiBindingRegistry().registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(SWT.NONE), new IMouseAction() {
			
			@Override
			public void run(NatTable natTable, MouseEvent event) {
				int row = natTable.getRowPositionByY(event.y);
				int index = natTable.getRowIndexByPosition(row);
				FilterDataItem<T> item = dataProvider.getRowObject(index);
				selectionEvent(item, SWT.MouseDoubleClick);
			}
		}); */

        ThemeConfiguration themeConfig = /*Theme.isDarkThemeActive() ? 
				new DarkThemeConfiguration(this.natTable) : */ new DayThemeConfiguration();
		
		natTable.setTheme(themeConfig);
		pack();
		
		// expand as much as possible both horizontally and vertically
		GridDataFactory.fillDefaults().grab(true, true).applyTo(natTable);
		GridLayoutFactory.fillDefaults().numColumns(1).generateLayout(parentContainer);

		if (style == STYLE_COMPOSITE) {
			// Real application, not within a simple unit test
			PreferenceStore pref = ViewerPreferenceManager.INSTANCE.getPreferenceStore();
			pref.addPropertyChangeListener(this);
			natTable.addDisposeListener(this);
		}
	}
	
	
	/****
	 * Attempt to resize the rows and columns if possible
	 */
	public void pack() {
		// need more space between top and the bottom of the row to display the check box
		// the original dimension of the check box is 20x20
		final int CHECK_BOX_DIMENSION_Y = 22;
		
		// get the font size
		GC gc = new GC(natTable);
		gc.setFont(FontManager.getFontGeneric());
		Point size = gc.stringExtent("|{}/',!^_");
		int height = Math.max(CHECK_BOX_DIMENSION_Y, size.y);
		int scaled = GUIHelper.convertVerticalDpiToPixel(height);
		this.dataLayer.setDefaultRowHeight(scaled);
		
		gc.dispose();
	}
	
	
	/*****
	 * Event triggered when user type something in the filter text
	 * 
	 * @param text
	 * 			{@code String} to filter in (text to be included)
	 */
	protected void eventFilterText(String text) {
		var currentTextMatcher = getTextMatcher();
		
		if (text == null) {
			currentTextMatcher.setFilterText(new String [] {});
		} else {
			if (btnRegExpression.getSelection()) {
				// check if the regular expression is correct
				try {
					Pattern.compile(text);
				} catch(Exception err) {
					Color c = GUIHelper.COLOR_YELLOW;
					objSearchText.setBackground(c);
					return;
				}
				objSearchText.setBackground(GUIHelper.COLOR_LIST_BACKGROUND);
			}
			currentTextMatcher.setFilterText(new String [] {text});
		}
		if (natTable != null)
			natTable.refresh(false);
	}
	
	
	/***
	 * Default implementation to create the text matcher editor.
	 * Inherited class can override this method to have their
	 * customized text matcher.
	 * 
	 * @return {@code TextMatcherEditor} 
	 * 			The matcher editor of {@code FilterDataItem<T>} type.
	 */
	protected TextMatcherEditor<FilterDataItem<T>> getTextMatcher() {
		if (textMatcher == null) {
			textMatcher = new TextMatcherEditor<>( (List<String> baseList, FilterDataItem<T> element) 
					-> baseList.add(element.getLabel())
			 );
			textMatcher.setMode(TextMatcherEditor.CONTAINS);

		}
		return textMatcher;
	}
	
	/***
	 * Event when users toggle the regular expression on/off
	 */
	private void toggleRegularExpression() {
		var currentTextMatcher = getTextMatcher();
		
		boolean regExp = btnRegExpression.getSelection();
		if (regExp) {
			try {
				currentTextMatcher.setMode(TextMatcherEditor.REGULAR_EXPRESSION);
			} catch(Exception err) {
				Color c = GUIHelper.COLOR_YELLOW;
				objSearchText.setBackground(c);
				return;
			}
			objSearchText.setBackground(GUIHelper.COLOR_LIST_BACKGROUND);
		} else {
			currentTextMatcher.setMode(TextMatcherEditor.CONTAINS);
		}
		eventFilterText(objSearchText.getText());
		
		if (natTable != null)
			natTable.refresh(false);
	}
	

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final String property = event.getProperty();
		if (property.equals(PreferenceConstants.ID_FONT_GENERIC)) {
			painterConfiguration.configureRegistry(natTable.getConfigRegistry());
			pack();
			natTable.refresh(false);
		}
	}
	
	@Override
	public void widgetDisposed(DisposeEvent e) {
		if (style == STYLE_COMPOSITE) {
			PreferenceStore pref = ViewerPreferenceManager.INSTANCE.getPreferenceStore();
			pref.removePropertyChangeListener(this);
		}
	}
	
	
	/***
	 * return the table widget of the filter items
	 * 
	 * @return {@code NatTable} the table widget
	 */
	protected NatTable getNatTable() {
		return natTable;
	}

	
	/****
	 * Return the current list modified by the user.
	 * 
	 * @apiNote The order can be different from the original order given by the caller. 
	 * 
	 * @return {@code EventList<FilterDataItem>}
	 */
	public EventList<FilterDataItem<T>> getEventList() {
		return eventList;
	}
	
	
	/***
	 * Get the current filtered list.
	 * 
	 * @return
	 */
	public FilterList<FilterDataItem<T>> getFilterList() {
		return filterList;
	}

	
	/***
	 * Get the current pane's style.
	 * 
	 * @see STYLE_COMPOSITE
	 * @see STYLE_INDEPENDENT
	 * @return
	 */
	public int getStyle() {
		return style;
	}

	
	/***
	 * Request to set the focus on this filter pane.
	 * By default the focus will be on the search text part unless
	 * overrides by the child.
	 * 
	 * @apiNote It is NOT guaranteed that the focus is successfully set
	 * to the search text.
	 */
	public void setFocus() {
		objSearchText.setFocus();
	}

	/****
	 * Retrieve the selection provider
	 * @return {@code ISelectionProvider}
	 */
	protected ISelectionProvider getSelectionProvider() {
		return rowSelectionProvider;
	}

	protected abstract void setLayerConfiguration(DataLayer datalayer);
	protected abstract String[] getColumnHeaderLabels();
	protected abstract FilterDataProvider<T> getDataProvider(FilterList<FilterDataItem<T>> filterList);

	/****
	 * Create additional widgets in the button group (check-all and uncheck-all).
	 * Subclass can add any widgets and must return the number created widgets.
	 * 
	 * @param parent 
	 * 			The container 
	 * @param inputData
	 * 			The input data
	 * @return {@code int}
	 * 			The number of created widgets
	 */
	protected abstract int createAdditionalButton(Composite parent, FilterInputData<T> inputData);
	
	protected abstract int createAdditionalFilter(Composite parent, FilterInputData<T> inputData);
	
	protected abstract void selectionEvent(FilterDataItem<T> item, int click);
	protected abstract void addConfiguration(NatTable table);
}

// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpclocal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.rice.cs.hpcbase.IFilteredData;
import org.hpctoolkit.db.local.db.IFileDB;
import org.hpctoolkit.db.local.db.IdTuple;
import org.hpctoolkit.db.local.db.IFileDB.IdTupleOption;


/******************************************************************
 * 
 * Filtered version for accessing a raw data file
 * A data file can either thread level data metric, or trace data
 * @see IBaseData
 * @see AbstractBaseData
 * @see FileDB2
 *******************************************************************/
public class FilteredBaseData extends AbstractBaseData implements IFilteredData {

	private List<Integer> indexes;
	private List<IdTuple> listIdTuples;

	/*****
	 * construct a filtered data
	 * The user is responsible to make sure the filter has been set with setFilters()
	 * 
	 * @param filename
	 * @param headerSize
	 * @throws IOException
	 */
	public FilteredBaseData(IFileDB baseDataFile) throws IOException 
	{
		super( baseDataFile);
	}

	
	
	@Override
	public List<IdTuple> getListOfIdTuples(IdTupleOption option) {
		if (indexes == null) {
			// this can happen when we remove the filter and go back to the densed one.
			// the best way is to go back to IBaseData instead of IFilteredData
			return super.getListOfIdTuples(option);
		}
		if (listIdTuples != null) {
			return listIdTuples;
		}
		listIdTuples = new ArrayList<>();
		List<IdTuple> listDensed   = super.getListOfIdTuples(IdTupleOption.BRIEF);
		
		for (int i=0; i<indexes.size(); i++) {
			Integer index = indexes.get(i);
			IdTuple idTuple = listDensed.get(index);
			listIdTuples.add(idTuple);
		}
		return listIdTuples;
	}


	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.extdata.IBaseData#getNumberOfRanks()
	 */
	public int getNumberOfRanks() {
		return getListOfIdTuples(IdTupleOption.BRIEF).size();
	}
	

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.extdata.IBaseData#getMinLoc(int)
	 */
	@Override
	public long getMinLoc(IdTuple profile) {
		return baseDataFile.getMinLoc(profile);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.extdata.IBaseData#getMaxLoc(int)
	 */
	@Override
	public long getMaxLoc(IdTuple profile) {
		return baseDataFile.getMaxLoc(profile);
	}


	@Override
	public int getFirstIncluded() {
		if (indexes == null || indexes.isEmpty())
			return 0;
		
		return indexes.get(0);
	}

	@Override
	public int getLastIncluded() {
		if (indexes == null || indexes.isEmpty())
			return super.getListOfIdTuples(IdTupleOption.BRIEF).size()-1;
		
		return indexes.get(indexes.size()-1);
	}

	@Override
	public boolean isDenseBetweenFirstAndLast() {
		if (indexes == null || indexes.isEmpty())
			return true;
		
		int size = indexes.size();
		return indexes.get(size-1)-indexes.get(0) == size-1;
	}



	@Override
	public void setIncludeIndex(List<Integer> listOfIncludedIndex) {
		indexes = new ArrayList<>(listOfIncludedIndex);
		// make sure it's in order, otherwise we display the trace according to the new index
		Collections.sort(indexes);
		
		listIdTuples = null;
	}


	@Override
	public List<IdTuple> getDenseListIdTuple(IdTupleOption option) {
		return super.getListOfIdTuples(option);
	}
	
	@Override
	public boolean isGPU(int rank) {
		// Fix issue #62: make sure checking the GPU works for filtered ranks
		int index = indexes == null? rank : indexes.get(rank);
		return baseDataFile.isGPU(index);
	}
}

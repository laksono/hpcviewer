// SPDX-FileCopyrightText: 2024 Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: BSD-3-Clause

package edu.rice.cs.hpcbase;

import edu.rice.cs.hpcdata.db.IdTuple;

/****
 * Interface to map from an trace or profile to its number of samples.
 * To support backward compatibility with old XML database, the profile id 
 * is equivalent to an Id tuple to avoid confusion with trace id.
 * 
 * @apiNote backward compatibility will be removed in the future. 
 * 	It's just a headache to support it.   
 */
public interface IExecutionContextToNumberTracesMap 
{
	/***
	 * Return the number of trace samples of the profile
	 *  
	 * @param idTupleProfile
	 * 
	 * @return {@code int}
	 */
	int getNumberOfSamples(IdTuple idTupleProfile);
	
	/***
	 * Dummy implementation of the interface
	 */
	final IExecutionContextToNumberTracesMap EMPTY = idTupleProfile ->  0;
}

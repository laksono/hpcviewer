
package edu.rice.cs.hpcdata.db.version4;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

import edu.rice.cs.hpcdata.db.IFileDB;
import edu.rice.cs.hpcdata.db.IdTuple;
import edu.rice.cs.hpcdata.db.IdTupleType;
import edu.rice.cs.hpcdata.db.IFileDB.IdTupleOption;
import edu.rice.cs.hpcdata.experiment.metric.MetricValueSparse;

/*********************************************
 * 
 * Class to handle summary.db file generated by hpcprof
 *
 *********************************************/
public class DataSummary extends DataCommon 
{
	// --------------------------------------------------------------------
	// constants
	// --------------------------------------------------------------------
	public  static final String FILE_PROFILE_DB = "profile.db";
	
	private static final String HEADER_MAGIC_STR  = "HPCTOOLKITprof";

	private static final int CCT_RECORD_SIZE   = 4 + 8;
	private static final int NUM_ITEMS = 2;
	
	private static final int FMT_PROFILEDB_SZ_MVAL = 0x0a;
	private static final int FMT_PROFILEDB_SZ_CIDX = 0x0c;

	public static final int INDEX_SUMMARY_PROFILE = 0;
	
	// --------------------------------------------------------------------
	// object variable
	// --------------------------------------------------------------------
	
	private final IdTupleType idTupleTypes;
	
	/** List of id tuples in sorted order */
	private List<IdTuple> listIdTuple;
	private ProfInfo info;
	private ListCCTAndIndex listCCT;
		
	protected boolean optimized = true;
	
	private double[] labels;
	private String[] strLabels;
	private boolean  hasGPU;

	/** Number of parallelism level or number of levels in hierarchy */
	private int numLevels;

	public DataSummary(IdTupleType idTupleTypes) {
		this.idTupleTypes = idTupleTypes;
	}
	
	// --------------------------------------------------------------------
	// Public methods
	// --------------------------------------------------------------------
	
	/***
	 *  <p>Opening for data summary metric file</p>
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpcdata.db.version4.DataCommon#open(java.lang.String)
	 */
	@Override
	public void open(final String filename)
			throws IOException
	{
		super.open(filename + File.separator + FILE_PROFILE_DB);
	}
	

	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.db.DataCommon#printInfo(java.io.PrintStream)
	 */
	public void printInfo( PrintStream out )
	{
		super.printInfo(out);
		IdTupleType type = IdTupleType.createTypeWithOldFormat();
		
		// print list of id tuples
		for(ProfileInfoElement pi: info.piElements) {
			out.println(pi.getIdTupleLabel(type));
		}
		out.println();

		ListCCTAndIndex list = null;
		
		try {
			list = getCCTIndex();
		} catch (IOException e) {
			return;
		}
		
		// print random metrics
		int len = Math.min(100, list.listOfId.length);
		
		for (int i=0; i<len; i++)
		{
			int cct = list.listOfId[i];
			out.format("[%5d] ", cct);
			printMetrics(out, cct);
		}
	}

	
	/****
	 * Get the list of cct for the summary profile
	 * @return
	 * @throws IOException
	 */
	private ListCCTAndIndex getCCTIndex() 
			throws IOException {		
		return getCCTIndex(IdTuple.PROFILE_SUMMARY);
	}
	
	
	/****
	 * Retrieve the list of CCT indexes for a specified profile
	 * 
	 * @param profileNum 
	 * 			1-based index of profile. The 0 index is reserved for summary profile.
	 * @return
	 * @throws IOException
	 */
	private ListCCTAndIndex getCCTIndex(IdTuple idtuple) throws IOException {
		int profileNum = idtuple.getProfileIndex();
		if (profileNum < 0 || profileNum >= info.piElements.length)
			return null;
		
		ListCCTAndIndex list = new ListCCTAndIndex(info.piElements[profileNum].nCtxs);
		
		long position = info.piElements[profileNum].pCtxIndices;
		var input = getChannel();
		long size = (long)FMT_PROFILEDB_SZ_CIDX * info.piElements[profileNum].nCtxs;
		
		var buffer = input.map(MapMode.READ_ONLY, position, size);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i=0; i<info.piElements[profileNum].nCtxs; i++) {
			// special alignment: each record is SIZE_IDX bytes
			buffer.position(i * FMT_PROFILEDB_SZ_CIDX);
			
			list.listOfId[i] = buffer.getInt();
			list.listOfdIndex[i] = buffer.getLong();
		}
		return list;
	}
	
	
	/***
	 * Retrieve the list of tuple IDs.
	 * @return {@code List<IdTuple>} List of Tuple
	 */
	public List<IdTuple> getIdTuple() {
		if (optimized)
			return getIdTuple(IFileDB.IdTupleOption.BRIEF);
		
		return getIdTuple(IFileDB.IdTupleOption.COMPLETE);
	}
	
	
	/****
	 * Retrieve the list of id tuples
	 * 
	 * @param option
	 * 			 may be use for further use to differentiate between
	 * 			 brief and complete notation.	
	 * 
	 * @return {@code List<IdTuple>}
	 */
	public List<IdTuple> getIdTuple(IdTupleOption option) {
		return listIdTuple;
	}


	/****
	 * Get the value of a specific profile with specific cct and metric id
	 * 
	 * @param idtuple
	 * @param cctId
	 * @param metricId
	 * @return
	 * @throws IOException
	 */
	public double getMetric(IdTuple idtuple, int cctId, int metricId) 
			throws IOException
	{
		List<MetricValueSparse> listValues = getMetrics(idtuple, cctId);		
		if (listValues == null)
			return 0.0d;
		
		// We need to grab a value directly from the memory instead of searching O(n)
		int index = Collections.binarySearch(listValues, metricId, (o1, o2) -> {
			Integer i1;
			Integer i2;
			if (o1 instanceof MetricValueSparse) 
				i1 = ((MetricValueSparse)o1).getIndex();
			else
				i1 = (Integer) o1;
			if (o2 instanceof MetricValueSparse)
				i2 = ((MetricValueSparse)o2).getIndex();
			else
				i2 = (Integer) o2;
			return i1-i2;
		});
		if (index < 0)
			return 0.0d;
		
		return listValues.get(index).getValue();
	}
	
	
	/**********
	 * Reading a set of metrics from the file for a given CCT 
	 * This method does not support concurrency. The caller is
	 * responsible to handle mutual exclusion.
	 * 
	 * @param cctId
	 * @return List of MetricValueSparse
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(int cctId) 
			throws IOException
	{		
		return getMetrics(IdTuple.PROFILE_SUMMARY, cctId);
	}
	
	
	/****
	 * Retrieve the list of metrics for a certain profile number and a given cct id
	 * 
	 * @param profileNum 
	 * 			The profile number. For summary profile, it equals to {@code INDEX_SUMMARY_PROFILE}
	 * @param cctId 
	 * 			the cct id
	 * @return List of {@code MetricValueSparse}
	 * 
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(IdTuple idtuple, int cctId) 
			throws IOException 
	{
		ListCCTAndIndex list = listCCT;
		
		if (!idtuple.equals(IdTuple.PROFILE_SUMMARY) || list == null) {
			list = getCCTIndex(idtuple);
		}
		if (list == null)
			return Collections.emptyList();
		
		// search for the cct-id
		// if it doesn't exist, we return empty metric (or throw an exception?)
		int index = Arrays.binarySearch(list.listOfId, cctId);
		if (index < 0)
			return Collections.emptyList();
		
		// searching for metrics for a given cct
		//
		int profileNum = idtuple.getProfileIndex();
		int numMetrics = 0;
		long position1 = list.listOfdIndex[index];
		
		if (index + 1 < list.listOfdIndex.length) {
			long position2 = list.listOfdIndex[index+1];			
			numMetrics = (int) (position2 - position1);
		} else {
			numMetrics = (int) (info.piElements[profileNum].nValues - position1);
		}
		long numBytes  = (long)FMT_PROFILEDB_SZ_MVAL * numMetrics;
		
		List<MetricValueSparse> values = FastList.newList(numMetrics);
		var channel = getChannel();
		var offset  = info.piElements[profileNum].pValues + position1 * FMT_PROFILEDB_SZ_MVAL;
		var buffer  = channel.map(MapMode.READ_ONLY, offset, numBytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i=0; i<numMetrics; i++) {
			// thanks to the alignment, we have to position every time looking for a record
			// memory position should be just an assignment.
			buffer.position(i * FMT_PROFILEDB_SZ_MVAL);
			
			MetricValueSparse val = new MetricValueSparse();
			val.setIndex(buffer.getShort());
			val.setValue(buffer.getDouble());
			
			values.add(val);
		}		
		return values;
	}
	
	
	/****
	 * Retrieve the list of id tuple label in string
	 * @return String[]
	 */
	public String[] getStringLabelIdTuples() {
		if (strLabels == null) {
			strLabels = new String[info.piElements.length-1];
			
			// id 0 is reserved
			for(int i=1; i<info.piElements.length; i++) {
				ProfileInfoElement piElem = info.piElements[i];
				strLabels[i-1] = piElem.getIdTupleLabel(idTupleTypes);
			}
		}
		return strLabels;
	}
	
	
	/****
	 * Retrieve the list of NON-GPU id tuple representation in double.
	 * <ul>
	 *  <li>For OpenMP programs, it returns the list of 1, 2, 3,...
	 *  <li>For MPI+OpenMP programs, it returns the list of 1.0, 1.1, 1.2, 2.0, 2.1, ...
	 * </ul>
	 * @apiNote GPU id-tuples will not be represented
	 * 
	 * @return double[]
	 */
	public double[] getDoubleLableIdTuples() {
		if (labels == null) {
			
			// collect list of non-gpu id tuples
			var list = listIdTuple.stream()
								  .filter(idt -> !idt.isGPU(idTupleTypes))
								  .collect(Collectors.toList());
			
			labels = new double[list.size()];
			
			// covert the list to double representation list
			var doubleList = list.stream()
								 .map(idt -> idt.toNumber())
								 .collect(Collectors.toList());
			
			int i=0;
			for(var dl: doubleList) {
				labels[i] = dl.doubleValue();
				i++;
			}
		}
		return labels;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.db.DataCommon#dispose()
	 */
	@Override
	public void dispose() throws IOException
	{
		labels = null;
		listCCT = null;
		super.dispose();
	}


	// --------------------------------------------------------------------
	// Protected methods
	// --------------------------------------------------------------------

	@Override
	protected int getNumSections() {
		return NUM_ITEMS;
	}

	@Override
	protected boolean isFileHeaderCorrect(String header) {
		return header.equals(HEADER_MAGIC_STR);
	}


	@Override
	protected boolean isFileFooterCorrect(String header) {
		return header.equals("_prof.db");
	}

	@Override
	protected boolean readNextHeader(FileChannel input, DataSection []sections) 
			throws IOException
	{
		// read the profile info
		readProfInfo(input, sections[0]);
		
		// read the hierarchical id tuple 
		readIdTuple(input);
		
		// eager initialization for the cct of the summary profile
		// this summary will be loaded anyway. There is no harm to do it now. 
		// ... or I think
		listCCT = getCCTIndex(IdTuple.PROFILE_SUMMARY);
		
		return true;
	}
	
	// --------------------------------------------------------------------
	// Private methods
	// --------------------------------------------------------------------
	
	private void readIdTuple(FileChannel input) throws IOException {

		// -----------------------------------------
		// 1. Read the main Id tuple section
		//    and load it to a temporary list
		// -----------------------------------------
		List<IdTuple> tempIdTupleList = FastList.newList();
		
		// mapLevelToHash is important to know if there is an invariant for each level
		// if a level has invariant (i.e. its size is zero), we'll skip this level
		int maxLevels = idTupleTypes.size();
		LongIntHashMap []mapLevelToHash = new LongIntHashMap[maxLevels];
		
		for(int i=0; i<info.nProfile; i++) {
			info.piElements[i].readIdTuple(i, input);
			numLevels = Math.max(numLevels, info.piElements[i].numLevels);
			
			// the first profile is the summary one (aka summary profile).
			// We are only interested with individual profiles
			if (info.piElements[i].pIdTuple  == 0)
				continue;
			
			tempIdTupleList.add(info.piElements[i].idt);
			hasGPU = hasGPU || info.piElements[i].idt.isGPU(idTupleTypes);

			// gather profile invariants
			// we want to count the number of duplicate profile id
			// ex. : profiles (1, 0, 0,1) and (1, 0, 1, 2)
			//       the invariants are the first two id (1, 0)
			for(int j=0; j<info.piElements[i].numLevels; j++) {
				short kind = info.piElements[i].idt.getKind(j);
				long idx   = info.piElements[i].idt.getLogicalIndex(j);
				long hash  = convertIdTupleToHash(kind, idx);
				
				if (mapLevelToHash[j] == null)
					mapLevelToHash[j] = new LongIntHashMap();

				int count = mapLevelToHash[j].getIfAbsent(hash, 0);
				count++;
				
				mapLevelToHash[j].put(hash, count);
			}
		}

		// no need to find invariant if we have only 1 id tuple
		if (tempIdTupleList.size() <= 1) {
			listIdTuple = tempIdTupleList;
			return;
		}
		// -----------------------------------------
		// 2. Check for the invariants in id tuple.
		//    This is to know which the first levels we can skip
		// -----------------------------------------
		IntIntHashMap mapLevelToSkip = new IntIntHashMap();
		
		for (int i=0; i<numLevels; i++) {
			if (mapLevelToHash[i] != null && mapLevelToHash[i].size()==1) {
				mapLevelToSkip.put(i, 1);
			}
		}
		
		tempIdTupleList.sort((o1, o2) -> o1.compareTo(o2));
		
		// if the number of levels to skip is the same as the real number of levels,
		// we just return the original id tuple list.
		// This means all the id-tuples are the same (very unlikely, but it happens).
		// For instance, if we have:
		//  NODE 0  THREAD 1
		//  NODE 0  THREAD 1
		//
		// Then perhaps it's better to keep the original "NODE 0 THREAD 1" instead of
		// removing redundancy (which means removing all the id tuples).
		if (numLevels == mapLevelToSkip.size()) {
			listIdTuple = tempIdTupleList;
			return;
		}
		
		listIdTuple = FastList.newList(tempIdTupleList.size());
		numLevels   = 0;
		
		for(int i=0; i<tempIdTupleList.size(); i++) {
			IdTuple idt = tempIdTupleList.get(i);
			int totLevels = 0;
			
			// find how many levels we can keep for this id tuple
			for (int j=0; j<idt.getLength(); j++) {
				if (mapLevelToSkip.get(j) == 0) {
					totLevels++;
				}
			}
			// the profileNum is +1 because the index 0 is for summary
			IdTuple shortVersion = new IdTuple(idt.getProfileIndex(), totLevels);
						
			int level = 0;
			
			// copy not-skipped id tuples to the short version
			// leave the skipped ones for the full complete id tuple
			for(int j=0; j<idt.getLength(); j++) {
				if (mapLevelToSkip.get(j) == 0) {
					// we should keep this level
					shortVersion.setPhysicalIndex(level, idt.getPhysicalIndex(j));
					shortVersion.setLogicalIndex (level, idt.getLogicalIndex(j));
					shortVersion.setKind(level, idt.getKind(j));
					shortVersion.setFlag(level, idt.getFlag(j));
					level++;
				}
			}
			listIdTuple.add(shortVersion);
			
			numLevels = Math.max(numLevels, shortVersion.getLength());
		}
	}
	
	private long convertIdTupleToHash(short kind, long index) {
		long k = (kind << 22);
		return k + index;
	}
	
	/****
	 * Return {@code true} if the database contains GPU parallelism
	 * @return {@code boolean}
	 */
	public boolean hasGPU() {
		return hasGPU;
	}
	
	public IdTupleType getIdTupleType() {
		return idTupleTypes;
	}
	
	/****
	 * Retrieve the number of detected parallelism levels of this database
	 * @return {@code int}
	 */
	public int getParallelismLevels() {
		return numLevels;
	}
	
	/*****
	 * read the list of Prof Info
	 * @param input FileChannel
	 * @throws IOException
	 */
	private void readProfInfo(FileChannel input, DataSection profSection) throws IOException {
		MappedByteBuffer mappedBuffer = input.map(MapMode.READ_ONLY, profSection.offset, ProfInfo.SIZE);
		mappedBuffer.order(ByteOrder.LITTLE_ENDIAN);
		info = new ProfInfo(input, profSection);
	}
	
	
	/***
	 * Get the CCT index record from a given index position
	 * @param buffer byte buffer
	 * @param position int index of the cct
	 * @return int
	 */
	private int getCCTIndex(ByteBuffer buffer, int position) {
		final int adjustedPosition = position * CCT_RECORD_SIZE;
		buffer.position(adjustedPosition);
		return buffer.getInt();
	}
	
	/****
	 * Get the file offset of a CCT. 
	 * @param buffer ByteBuffer of the file
	 * @param position int index of the cct
	 * @return long
	 */
	private long getCCTOffset(ByteBuffer buffer, int position) {
		buffer.position( (position * CCT_RECORD_SIZE) + Integer.BYTES);
		return buffer.getLong();
	}
	
	
	/***
	 * Newton-style of Binary search the cct index 
	 * 
	 * @param cct the cct index
	 * @param first the beginning of the relative index
	 * @param last  the last of the relative index
	 * @param buffer ByteBuffer of the file
	 * @return 2-length array of indexes: the index of the found cct, and its next index
	 */
	@SuppressWarnings("unused")
	private long[] newtonSearch(int cct, int first, int last, ByteBuffer buffer) {
		int left_index  = first;
		int right_index = last - 1;
		
		if (left_index < 0 || right_index < 0)
			return null;
		
		int left_cct  = getCCTIndex(buffer, left_index);
		int right_cct = getCCTIndex(buffer, right_index);
		
		while (right_index - left_index > 1) {
			
			int predicted_index;
			final float cct_range = right_cct - left_cct;
			final float rate = cct_range / (right_index - left_index);
			final int mid_cct = (int) cct_range / 2;
			
			if (cct <= mid_cct) {
				predicted_index = (int) Math.max( ((cct - left_cct)/rate) + left_index, left_index);
			} else {
				predicted_index = (int) Math.min(right_index - ((right_cct-cct)/rate), right_index);
			}
			
			if (predicted_index <= left_cct) {
				predicted_index = left_index + 1;
			} 
			if (predicted_index >= right_cct) {
				predicted_index = right_index - 1;
			}
			
			int current_cct = getCCTIndex(buffer, predicted_index);
			
			if (cct >= current_cct) {
				left_index = predicted_index;
				left_cct = current_cct;
			} else {
				right_index = predicted_index;
				right_cct = current_cct;
			}
		}
		
		boolean found = cct == left_cct || cct == right_cct;
		
		if (found) {
			int index = left_index;
			if (cct == right_cct) 
				// corrupt data: should throw exception 
				index = right_index;
			
			long o1 = getCCTOffset(buffer, index);
			long o2 = getCCTOffset(buffer, index + 1);

			return new long[] {o1, o2};
		}
		// not found: the cct node has no metrics. 
		// we may should return array of zeros instead of null
		return new long[0];
	}
	

	/*******
	 * print a list of metrics for a given CCT index
	 * 
	 * @param out : the outpur stream
	 * @param cct : CCT index
	 */
	private void printMetrics(PrintStream out, int cct)
	{
		try {
			List<MetricValueSparse> values = getMetrics(cct);
			if (values == null)
				return;
			
			for(MetricValueSparse value: values) {
				out.print(value.getIndex() + ": " + value.getValue() + " , ");
			}
			out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	// --------------------------------------------------------------------
	// Internal Classes
	// --------------------------------------------------------------------

	public static class ListCCTAndIndex
	{
		public final int  []listOfId;
		public final long []listOfdIndex;
		
		public ListCCTAndIndex(int numContexts) {
			listOfId = new int[numContexts];
			listOfdIndex = new long[numContexts];
		}
		
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			if (listOfId != null) {
				buffer.append(String.format("id: %d..%d", listOfId[0], listOfId[listOfId.length-1]));
			}
			if (listOfdIndex != null) {
				buffer.append(String.format(" idx: %d..%d", listOfdIndex[0], listOfdIndex[listOfdIndex.length-1]));
			}
			return buffer.toString();
		}
	}
	
	/*****
	 * 
	 * Profile info data structure
	 *
	 */
	private static class ProfInfo
	{
		public static final int SIZE = 8+4+1;
		/*
		 00:	{PI}xN*(8)	pProfiles	Pointer to an array of nProfiles profile descriptions
		 08:	u32	nProfiles	Number of profiles listed in this section
		 0c:	u8	szProfile	Size of a {PI} structure, currently 40
		 */
		public final long pProfile;
		public final int  nProfile;
		public final byte szProfile;
		
		/*
		 * 00:	PSVB	valueBlock	Header for the values for this application thread
		 */
		public final ProfileInfoElement []piElements;
		
		
		/***
		 * Reading profile info in the buffer
		 * @param buffer
		 * @throws IOException 
		 */
		public ProfInfo(FileChannel input, DataSection profSection) throws IOException {
			MappedByteBuffer buffer = input.map(MapMode.READ_ONLY, profSection.offset, ProfInfo.SIZE);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			pProfile = buffer.getLong();
			nProfile = buffer.getInt();
			szProfile = buffer.get();
			piElements = new ProfileInfoElement[nProfile];
			
			//
			// collect the profile-major sparse value block for each profile
			//
			for(int i=0; i<nProfile; i++) {
				long offset = szProfile * i + pProfile;
				buffer = input.map(MapMode.READ_ONLY, offset, szProfile);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				
				ProfileInfoElement piElem = new ProfileInfoElement(buffer);
				piElements[i] = piElem;
			}
		}
		
		@Override
		public String toString() {
			return String.format("prof: 0x%x [%d] %d pie: %d", 
								 pProfile, nProfile, szProfile, piElements.length);
		}
	}
	
	
	/*************************************************
	 * 
	 * record class to store the sparse value of a profile
	 *
	 *************************************************/
	private static class ProfileInfoElement
	{	
		public static final int FMT_PROFILEDB_SZ_IDTUPLEELEM = 0x10; 
		public static final int FMT_PROFILEDB_SZ_IDTUPLEHDR  = 0x08;
		
		/*
		 * ProfileMajorSparseValueBlock:
		 * 	00:	u64			nValues		Number of non-zero values in this block
			08:	{Val}xN*(2)	pValues		Pointer to an array of nValues value pairs
			10:	u32			nCtxs		Number of non-empty contexts in this block
			18:	{Idx}xN*(4)	pCtxIndices	Pointer to an array of nCtxs context indices
		 */
		public final long pValues;
		public final long nValues;
		public final int  nCtxs;
		public final long pCtxIndices;
		
		/*
		 * 	20:	HIT*(8)	pIdTuple	Identifier tuple for this application thread	
		 */
		public final long pIdTuple;
		
		private int numLevels;
		private IdTuple idt;
		
		/***
		 * Initializing a block for profile-major sparse value
		 * @param buffer
		 */
		public ProfileInfoElement(ByteBuffer buffer) {
			nValues = buffer.getLong();
			pValues = buffer.getLong();
			nCtxs   = buffer.getInt();
			
			buffer.getInt(); // alignment 4 bytes
			
			pCtxIndices = buffer.getLong();			
			pIdTuple    = buffer.getLong();
			numLevels   = 0;
		}
		
		/***
		 * Read an id-tuple from the file.
		 * 
		 * @param index 
		 * 			zero-based index of the id-tuple. 
		 * 			This method will add +1 for index automatically since zero is reserved for summary profile
		 * @param channel
		 * 			The file channel
		 * 
		 * @throws IOException
		 */
		public void readIdTuple(int index, FileChannel channel) throws IOException {
			if (pIdTuple == 0)
				return;
			
			var buffer = channel.map(MapMode.READ_ONLY, pIdTuple, 2);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			short nIds = buffer.getShort();

			long size = (long)nIds * FMT_PROFILEDB_SZ_IDTUPLEELEM;
			buffer = channel.map(MapMode.READ_ONLY, pIdTuple + FMT_PROFILEDB_SZ_IDTUPLEHDR, size);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			idt = new IdTuple(index, nIds);
			
			for(int i=0; i<nIds; i++) {
				buffer.position(i * FMT_PROFILEDB_SZ_IDTUPLEELEM);
				byte kind  = buffer.get();
				buffer.get();
				short flag = buffer.getShort();
				int logicalId  = buffer.getInt();
				long physicalId = buffer.getLong();
				
				idt.setKind(i, kind);
				idt.setFlag(i, (byte)flag);
				idt.setLogicalIndex(i, logicalId);
				idt.setPhysicalIndex(i, physicalId);
			}
			numLevels = Math.max(numLevels, nIds);
		}
		
		public String getIdTupleLabel(IdTupleType idtype) {
			return idt.toString(idtype);
		}
		
		@Override
		public String toString() {
			return String.format("%s p: 0x%x has %d, ctx: 0x%x has %d, idt: 0x%x", 
								 idt.toLabel(), pValues, nValues, pCtxIndices, nCtxs, pIdTuple);
		}
	}
}


package edu.rice.cs.hpcdata.db.version4;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final static String HEADER_MAGIC_STR  = "HPCTOOLKITprof";
	private static final int    METRIC_VALUE_SIZE = 8 + 2;
	private static final int    CCT_RECORD_SIZE   = 4 + 8;
	private static final int    MAX_LEVELS        = 18;
	
	private static final int NUM_ITEMS = 2;

	// --------------------------------------------------------------------
	// object variable
	// --------------------------------------------------------------------
	
	private final IdTupleType idTupleTypes;
	private RandomAccessFile file;
	
	/*** cache variables so that we don't need to read again and again  ***/
	/*** cache the content of the file for a particular profile number  ***/
	private ByteBuffer byteBufferCache;
	
	/*** Current cached data of a certain profile ***/
	private int profileNumberCache;
	
	private List<IdTuple>  listIdTuple, listIdTupleShort;
	private ProfInfo info;
	
	/*** mapping from profile number to the sorted order*/
	private Map<Integer, Integer> mapProfileToOrder;
		
	protected boolean optimized = true;

	/** Number of parallelism level or number of levels in hierarchy */
	private int numLevels;
	
	private double[] labels;
	private String[] strLabels;
	private boolean hasGPU;
	
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
		super.open(filename);
		file = new RandomAccessFile(filename, "r");
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
		for(IdTuple idt: listIdTuple) {
			System.out.println(idt.toString(type));
		}
		System.out.println();

		ListCCTAndIndex list = null;
		
		try {
			list = getCCTIndex();
		} catch (IOException e) {
			e.printStackTrace();
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

	public ListCCTAndIndex getCCTIndex() 
			throws IOException {
				
		// -------------------------------------------
		// read the cct context
		// -------------------------------------------
		
		ListCCTAndIndex list = new ListCCTAndIndex();
		for(int i=0; i<info.piElements[0].nCtxs; i++) {
			
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
	 * @param shortVersion: true if we want to the short version of the list
	 * 
	 * @return {@code List<IdTuple>}
	 */
	public List<IdTuple> getIdTuple(IdTupleOption option) {
		switch(option) {
		case COMPLETE: 
			return listIdTuple;

		case BRIEF:
		default:
			return listIdTupleShort;
		}
	}
	
	
	
	/****
	 * Get the value of a specific profile with specific cct and metric id
	 * 
	 * @param profileNum
	 * @param cctId
	 * @param metricId
	 * @return
	 * @throws IOException
	 */
	public double getMetric(int profileNum, int cctId, int metricId) 
			throws IOException
	{
		List<MetricValueSparse> listValues = getMetrics(profileNum, cctId);		
		
		// TODO ugly temporary code
		// We need to grab a value directly from the memory instead of searching O(n)
		
		for (MetricValueSparse mvs: listValues) {
			if (mvs.getIndex() == metricId) {
				return mvs.getValue();
			}
		}
		return 0.0d;
	}
	
	/**********
	 * Reading a set of metrics from the file for a given CCT 
	 * This method does not support concurrency. The caller is
	 * responsible to handle mutual exclusion.
	 * 
	 * @param cct_id
	 * @return List of MetricValueSparse
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(int cct_id) 
			throws IOException
	{		
		return getMetrics(0, cct_id);
	}
	
	
	/****
	 * Retrieve the list of metrics for a certain profile number and a given cct id
	 * 
	 * @param profileNum The profile number. For summary profile, it equals to {@code PROFILE_SUMMARY}
	 * @param cct_id the cct id
	 * @return List of MetricValueSparse
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(int profileNum, int cct_id) 
			throws IOException 
	{	
		ArrayList<MetricValueSparse> values = new ArrayList<MetricValueSparse>(1);
		
		return values;
	}
	
	
	/****
	 * Retrieve the list of id tuple label in string
	 * @return String[]
	 */
	public String[] getStringLabelIdTuples() {
		if (strLabels == null) {
			strLabels = new String[listIdTupleShort.size()];
			
			for(int i=0; i<listIdTupleShort.size(); i++) {
				IdTuple idt  = listIdTupleShort.get(i);
				strLabels[i] = idt.toString(idTupleTypes);
			}		
		}
		return strLabels;
	}
	
	
	/****
	 * Retrieve the list of id tuple representation in double.
	 * For OpenMP programs, it returns the list of 1, 2, 3,...
	 * For MPI+OpenMP programs, it returns the list of 1.0, 1.1, 1.2, 2.0, 2.1, ... 
	 * @return double[]
	 */
	public double[] getDoubleLableIdTuples() {
		if (labels == null) {
			labels    = new double[listIdTupleShort.size()];
			
			for(int i=0; i<listIdTupleShort.size(); i++) {
				IdTuple idt  = listIdTupleShort.get(i);
				String label = idt.toLabel();
				labels[i]    = label == null? 0 : Double.valueOf(label);
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
		file.close();
		super.dispose();
	}

	/***
	 * Get the profile id based from the rank "order".
	 * As the data is not sorted based on id tuple, this method will return
	 * the profile index given the index of sorted id tuples.
	 * 
	 * @param orderNumber {@code int} the index
	 * @return {@code int} the profile index, used fro accessing the database
	 */
	public int getProfileIndexFromOrderIndex(int orderNumber) {
		return mapProfileToOrder.get(orderNumber);
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
	protected boolean readNextHeader(FileChannel input, DataSection []sections) 
			throws IOException
	{
		readProfInfo(input, sections[0]);
		for(int i=0; i<info.nProfile; i++) {
			info.piElements[i].readIdTuple(input, sections[1]);
		}
		return true;
	}
	
	// --------------------------------------------------------------------
	// Private methods
	// --------------------------------------------------------------------
		
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
				System.out.print(value.getIndex() + ": " + value.getValue() + " , ");
			}
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//out.println();
		}
	}

	
	// --------------------------------------------------------------------
	// Internal Classes
	// --------------------------------------------------------------------

	public static class ListCCTAndIndex
	{
		public int  []listOfId;
		public long []listOfdIndex;
		
		public String toString() {
			String buffer = "";
			if (listOfId != null) {
				buffer += "id: [" + listOfId[0] + ".." + listOfId[listOfId.length-1] + "] ";
			}
			if (listOfdIndex != null) {
				buffer += "idx; [" + listOfdIndex[0] + ".." + listOfdIndex[listOfdIndex.length-1] + "]";
			}
			return buffer;
		}
	}
	
	/*****
	 * 
	 * Profile info data structure
	 *
	 */
	private static class ProfInfo
	{
		public final static int SIZE = 8+4+1;
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
		
		public String toString() {
			return nProfile + ": " + piElements.length + " profiles";
		}
	}
	
	
	/*************************************************
	 * 
	 * record class to store the sparse value of a profile
	 *
	 *************************************************/
	private static class ProfileInfoElement
	{	
		/*
		 * ProfileMajorSparseValueBlock:
		 * 	00:	u64	nValues	Number of non-zero values in this block
			08:	{Val}xN*(2)	pValues	Pointer to an array of nValues value pairs
			10:	u32	nCtxs	Number of non-empty contexts in this block
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
		
		public IdTupleElem []idTuples;
		
		/***
		 * Initializing a block for profile-major sparse value
		 * @param buffer
		 */
		public ProfileInfoElement(ByteBuffer buffer) {
			nValues = buffer.getLong();
			pValues = buffer.getLong();
			nCtxs = buffer.getInt();
			
			buffer.getInt(); // alignment 4 bytes
			
			pCtxIndices = buffer.getLong();
			
			pIdTuple = buffer.getLong();
		}
		
		public void readIdTuple(FileChannel channel, DataSection idTupleSection) throws IOException {
			if (pIdTuple == 0)
				return;
			
			var buffer = channel.map(MapMode.READ_ONLY, pIdTuple, 2);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			short nIds = buffer.getShort();

			buffer = channel.map(MapMode.READ_ONLY, pIdTuple + 2, nIds * IdTupleElem.SIZE);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			idTuples = new IdTupleElem[nIds];
			
			for(int i=0; i<nIds; i++) {
				idTuples[i] = new IdTupleElem(buffer);
			}
		}
		
		@Override
		public String toString() {
			return "@" + pValues + " " + nValues +
					" @" + pCtxIndices + " " + nCtxs + " (" + pIdTuple + ")";
		}
	}
	
	
	
	/*****************************
	 * 
	 * An Id Tuple element class
	 *
	 *****************************/
	private static class IdTupleElem
	{
		public static final int SIZE = 2 + 2 + 4 + 8; 
		/*
		 * 	00:	u8	kind	One of the values listed in the meta.db Identifier Names section.
			02:	{Flags}	flags	See below.
			04:	u32	logicalId	Logical identifier value, may be arbitrary but dense towards 0.
			08:	u64	physicalId	Physical identifier value, eg. hostid or PCI bus index.
			10:		END	
		 */
		public final short kind;
		public final short flags;
		public final int   logicalId;
		public final long  physicalId;
		
		public IdTupleElem(ByteBuffer buffer) {
			kind  = buffer.getShort();
			flags = buffer.getShort();
			logicalId  = buffer.getInt();
			physicalId = buffer.getLong();
		}
		
		public String toString() {
			return "k: " + kind + " p: " + physicalId;
		}
	}
}

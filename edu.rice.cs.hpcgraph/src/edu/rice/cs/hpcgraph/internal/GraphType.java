package edu.rice.cs.hpcgraph.internal;

public class GraphType {
	public enum PlotType {PLOT, SORTED, HISTO}

	public static String toString(PlotType type) {
		
		switch (type) {
		case PLOT:
			return "Plot graph";
		case SORTED:
			return "Sorted plot graph";
		case HISTO:
			return "Histogram graph";
		}
		
		return null;
	}
}
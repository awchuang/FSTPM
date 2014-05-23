import static util.Utils.getMedian;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rstar.RStarTree;
import rstar.spatial.SpatialPoint;
import util.Trace;
import util.Utils;

public class FSTPM {
	private RStarTree tree;
    private int dimension;
	private String inputFile;
	private String resultFile;
	private List<Long> insertRunTime;
	private List<Long> fstpmRunTime;
	private List<Long> searchRunTime;
	private List<Long> rangeRuntime;
	private List<Long> knnRuntime;
    private Trace logger;
    private static double range;
    private static int duration;

    public static void main(String[] args) {
    	FSTPM controller = new FSTPM(args);
    	range = 2;	// km
    	duration = 180; // minutes
    	
    	/*
    	 * 1km = 0.0117 lat
    	 * 1km = 0.009 log
    	 * 1km = degree 0.01
    	 */    	
    	
		System.out.println("Reading input file ...");
		controller.processInput();
		controller.patternExtraction();
		System.out.println("Finished Processing file ...");

        controller.writeRuntimeToFile(controller.insertRunTime, "Insertion_runtime.txt");
        controller.writeRuntimeToFile(controller.searchRunTime, "Search_runtime.txt");
        controller.writeRuntimeToFile(controller.rangeRuntime, "RangeSearch_runtime.txt");
        controller.writeRuntimeToFile(controller.knnRuntime, "KNNSearch_runtime.txt");
        controller.writeRuntimeToFile(controller.fstpmRunTime, "FSTPM_runtime.txt");

		controller.printResults();
	}

	public FSTPM(String[] args) {
		if(args.length >= 2){
			this.inputFile = args[0];
            this.dimension = Integer.parseInt(args[1]);

			if (args.length >= 3)
				this.resultFile = args[2];
			else
				this.resultFile = this.getClass().getSimpleName()+ "_Results.txt";

		} else {
			this.printUsage();
			System.exit(1);
		}
		tree = new RStarTree(dimension);
		this.insertRunTime = new ArrayList<Long>();
		this.searchRunTime = new ArrayList<Long>();
		this.rangeRuntime = new ArrayList<Long>();
		this.knnRuntime = new ArrayList<Long>();
		this.fstpmRunTime = new ArrayList<Long>();
        logger = Trace.getLogger(this.getClass().getSimpleName());
	}


	protected void processInput() {
        float opType, oid;
        float[] point;
        long start, end;
        int lineNum = 0;
        String label;
        long time;

        try {
            BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
            String line;
            String[] lineSplit;

			while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");
                opType = Float.parseFloat(lineSplit[0]);

				switch ((int)opType) {
				case 0:
				{       //insertion
					try {
                        /*if (lineSplit.length != (this.dimension + 2)) {
                            throw new AssertionError();
                        }*/

                        oid = Float.parseFloat(lineSplit[1]);
                        point = extractPoint(lineSplit, 2);
                        label = lineSplit[4];
                        time = Long.parseLong(lineSplit[5]);

                        start = System.currentTimeMillis();
						tree.insert(new SpatialPoint(point, oid, label, time));
                        end = System.currentTimeMillis();

                        this.updateTimeTaken(opType, (end - start));
                        break;

                    } catch (Exception e) {
                        logger.traceError("Exception while processing line " + lineNum +
                                ". Skipped Insertion. message: "+e.getMessage());
                        break;
                    }
                    catch (AssertionError error){
                        logger.traceError("Error while processing line " + lineNum +
                                ".Skipped Insertion. message: "+ error.getMessage());
                        break;
                    }
                }
				default:
					logger.traceError("Invalid query type " + opType + " at line " + lineNum + ". Skipped .. ");
					break;
				}
			}
			input.close();
            tree.save();
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}

    private float[] extractPoint(String[] points, int startPos) throws NumberFormatException
    {
        float[] tmp = new float[this.dimension];
        for (int i = startPos, lineSplitLength = points.length;
             ((i < lineSplitLength) && (i < (startPos + this.dimension))); i++)
        {
            tmp[i-startPos] = Float.parseFloat(points[i]);
        }
        return tmp;
    }

    protected void updateTimeTaken(float type, long time) {
		switch ((int)type) {
		case 0:
			insertRunTime.add(time);
			break;
		case 1:
            searchRunTime.add(time);
            break;
        case 2:
			rangeRuntime.add(time);
			break;
		case 3:
			knnRuntime.add(time);
			break;
		default:
			logger.traceError("Invalid Query type encountered. Skipped..");
			break;
		}
	}

	protected void printResults() {
		logger.trace("\nPerforming Run Time calculations..");

		List<Long> combined = new ArrayList<Long>();
		combined.addAll(insertRunTime);
		combined.addAll(searchRunTime);
		combined.addAll(rangeRuntime);
		combined.addAll(knnRuntime);
		combined.addAll(fstpmRunTime);

		String result = "\n"+this.getClass().getSimpleName()+" --RESULTS--";

		String temp = "\n\nInsertion operations:(in milliseconds) "+ generateRuntimeReport(insertRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nSearch operations:(in milliseconds) "+ generateRuntimeReport(searchRunTime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nRange search operations: (in milliseconds) " + generateRuntimeReport(rangeRuntime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nKNN search operations: (in milliseconds) " + generateRuntimeReport(knnRuntime);
        logger.trace(temp);
        result += temp;
		temp = "\n\nCombined operations:(in milliseconds) "+ generateRuntimeReport(combined);
        logger.trace( temp);
        result += temp;
        temp = "\n\nFSTPM operations:(in milliseconds) "+ generateRuntimeReport(fstpmRunTime);
        logger.trace( temp);
        result += temp;

		writeResultToFile(result);
	}
	
	protected String generateRuntimeReport(List<Long> runtime) {
		StringBuilder result = new StringBuilder();
        int size = runtime.size();

        if (size > 0) {
            Collections.sort(runtime);
            try {
                Long percent5th = runtime.get((int) (0.05 * size));
                Long percent95th = runtime.get((int) (0.95 * size));
                float median = getMedian(runtime);
                long sum = 0;
                for (Long aRuntime : runtime) {
                    sum += aRuntime;
                }
                double avg = sum / (double) size;

                result.append("\nTotal ops = ").append(size);
                result.append("\nAvg time: ").append(avg);
                result.append("\n5th percentile: ").append(percent5th);
                result.append("\n95th percentile: ").append(percent95th);
                result.append("\nmedian: ").append(median);

            } catch (Exception e) {
                logger.traceError("Exception while generating runtime results");
                e.printStackTrace();
            }
        }

		return result.toString();
	}

	protected void writeResultToFile(String result) {
		try {
			File outFile = new File(this.resultFile);
			if(outFile.exists()){
				outFile.delete();
			}
			BufferedWriter outBW =  new BufferedWriter(new FileWriter(outFile));
			try{
				logger.trace("\nWriting results to file .. ");
				outBW.write(result);
			}
			finally{
				outBW.close();
				logger.trace("done");
			}
		} 
		catch (IOException e) {
			logger.traceError("IOException while writing results to " + resultFile);
		}
	}

	protected void writeRuntimeToFile(List<Long> runtime, String file) {
		try {
			File f = new File(file);
			if(f.exists())
				f.delete();

			BufferedWriter bf =  new BufferedWriter(new FileWriter(f));
			for(long i : runtime){
				bf.write("" + i + "\n");
			}
			bf.close();
		} catch (IOException e) {
            logger.traceError("IOException while writing runtimes to file.");
//            e.printStackTrace();
        }
	}

	protected void printUsage() {
		System.err.println("Usage: "+ this.getClass().getSimpleName() +
                " <path to input file> <dimension of points> [output file].\noutput file is optional.\n");
	}
	
	//////////////// r-tree///////////////////
	
	//////////////// FSTPM ///////////////////
	protected void patternExtraction(){
		float oid;
		//double range;
        float[] point;
        long start, end;
        int lineNum = 0;
        int time;
		HashMap<List<String>, Integer> pattern = new HashMap<List<String>, Integer>();
        
		try{
			BufferedReader input =  new BufferedReader(new FileReader(this.inputFile));
			String line;
			String[] lineSplit;
			
			while ((line = input.readLine()) != null) {
				lineNum++;
                lineSplit = line.split(",");
                
                try{
                	//range search
                    oid = Float.parseFloat(lineSplit[1]);
                    point = extractPoint(lineSplit, 2);
                    time = Integer.parseInt(lineSplit[5]);
                    //range = Double.parseDouble(lineSplit[this.dimension + 1]);
                    //range = 2;
                    SpatialPoint center = new SpatialPoint(point);

                    start = System.currentTimeMillis();
                    List<SpatialPoint> result = tree.rangeSearch(center, range*0.01*2);
                    //System.out.println("          rrrrrr" + result);
                    System.out.println(tree.pointSearch(center));
                    result.remove(center);
                    durationCheck(result, time);
                    end = System.currentTimeMillis();
                    
                    logger.trace("Range Search(" + range*0.01*2 + ", " + center + "):\n" + Utils.SpatialPointListToString(result));
                    fstpmRunTime.add(( end - start ));
                    //this.updateTimeTaken(opType, (end - start));
                    

                	//System.out.println("rr: " + result);
                    //generate possible patterns
                    //List<Float> elements = coordinateToId(result);
                    //elements.remove(oid);
                	//System.out.println("ee: " + elements);
            		List<List<SpatialPoint>> candidates = candExtraction(result);
            		System.out.println("cc: " + candidates);
            		
            		List<List<SpatialPoint>> stPattern = new ArrayList<List<SpatialPoint>>();
            		for(int i = 0; i < candidates.size(); i++){
            			System.out.println(candidates.get(i));
            			if(rangeCheck(candidates.get(i), center) == true){
            				stPattern.add(candidates.get(i));
            			}
            			System.out.println(rangeCheck(candidates.get(i), center));
            		}
            		System.out.println(stPattern);
            		
            		cordsToLabel(stPattern, pattern);
            		
            		                    
                }
                catch (Exception e) {
                    logger.traceError("Exception while processing line " + lineNum +
                            ". Skipped range search. message: "+e.getMessage());
                }
                catch (AssertionError error){
                    logger.traceError("Error while processing line " + lineNum +
                            ". Skipped range search. message: "+error.getMessage());
                }                
			}			
			input.close();
		}
		catch (Exception e) {
			logger.traceError("Error while reading input file. Line " + lineNum + " Skipped\nError Details:");
		}
	}
	

	private List<List<SpatialPoint>> candExtraction(List<SpatialPoint> src){
		List<List<SpatialPoint>> result = new ArrayList<List<SpatialPoint>>();
		//float f = (float) 0.0;
		SpatialPoint sp = new SpatialPoint();
		
		for (int i = 2; i <= src.size(); i++) {
	    	List<SpatialPoint> to = new ArrayList<SpatialPoint>();
			for (int k = 0; k < i; k++) {
				to.add(sp);
			}
			comb(src, to, i, src.size(), i, result);
	    }
	    return result;
	}
	
	//C(m, n) = C(m-1, n-1) + C(m-1, n)
	private void comb(List<SpatialPoint> from, List<SpatialPoint> to, int len, int m, int n, List<List<SpatialPoint>> dst) {
		if (n == 0) {
			List<SpatialPoint> result = new ArrayList<SpatialPoint>(to.size());
			for(int i = 0; i < to.size(); i++){
				result.add(to.get(i));
			}
			dst.add(result);
		} else {
			to.set(n-1, from.get(m - 1));    	
			if (m > n - 1) {
				comb(from, to, len, m - 1, n - 1, dst);
			}
			if (m > n) {
				comb(from, to, len, m - 1, n, dst);
			}
		}
	}

	private Boolean rangeCheck(List<SpatialPoint> cand, SpatialPoint center){
		float centerX =  center.getCords()[0];
		float centerY =  center.getCords()[1];
		
		for(int i = 0; i < cand.size(); i++){
			double distance = GetDistance(centerX, centerY, cand.get(i).getCords()[0], cand.get(i).getCords()[1]);
			System.out.println(center + " c " + cand.get(i) + " AA " + distance);
			if(distance > range){
				return false;
			}
		}
		return true;		
	}
	
	private void durationCheck(List<SpatialPoint> result, int time){
		for(int i = 0; i < result.size() ; i++){
			System.out.println("time:" + (result.get(i).getTime() - time));
			if(result.get(i).getTime() - time > duration)
				result.remove(result.get(i));
		}		
	}
	
	private void cordsToLabel(List<List<SpatialPoint>> src, HashMap<List<String>, Integer> dst){
		for(int i = 0; i < src.size(); i++){
			List<String> temp = new ArrayList<String>();
			for(int k = 0; k < src.get(i).size(); k++){
				temp.add(src.get(i).get(k).getLabel());
			}
			if(dst.containsKey(temp))
				dst.put(temp, dst.get(temp)+1);
			else
				dst.put(temp, 1);
			System.out.println(temp);
		}
		System.out.println("dst: " + dst);
	}
	
	public double GetDistance(double Lat1, double Long1, double Lat2, double Long2)
	{
		double Lat1r = ConvertDegreeToRadians(Lat1);
		double Lat2r = ConvertDegreeToRadians(Lat2);
		double Long1r = ConvertDegreeToRadians(Long1);
		double Long2r = ConvertDegreeToRadians(Long2);

		double R = 6371; // Earth's radius (km)         
		double d = Math.acos(Math.sin(Lat1r) *
				Math.sin(Lat2r) + Math.cos(Lat1r) *
				Math.cos(Lat2r) *
				Math.cos(Long2r-Long1r)) * R;
		return d;
	}
	
	private double ConvertDegreeToRadians(double degrees)
	{
		return (Math.PI/180)*degrees;
	}
	
}




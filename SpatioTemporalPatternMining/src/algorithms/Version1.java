package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rstar.spatial.SpatialPoint;

public class Version1 {

	// generate all combination size = 2~5
	public List<List<SpatialPoint>> candExtraction(List<SpatialPoint> src){
		List<List<SpatialPoint>> result = new ArrayList<List<SpatialPoint>>();
		SpatialPoint sp = new SpatialPoint();
		SpatialPoint head = new SpatialPoint();
		head = src.remove(0);
		
		// generate combination size = 2~4
		for (int i = 2; i <= 4 && i <= src.size(); i++) {
	    	List<SpatialPoint> to = new ArrayList<SpatialPoint>();
			for (int k = 0; k < i; k++) {
				to.add(sp);
			}
			comb(src, to, i, src.size(), i, result);
	    }
		// add pivot to combination size = 2~4
		// get result size = 3~5
		for(int i = 0; i < result.size(); i++){
			result.get(i).add(0, head);
		}
		// add pivot to other node
		// get result size = 2
		for (int i = 0; i < src.size(); i++){
			List<SpatialPoint> one = new ArrayList<SpatialPoint>();
			one.add(head);
			one.add(src.get(i));
			result.add(one);
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

	public Boolean rangeCheck(List<SpatialPoint> cand, SpatialPoint center, double range){
		float centerX =  center.getCords()[0];
		float centerY =  center.getCords()[1];
		
		for(int i = 0; i < cand.size(); i++){
			double distance = GetDistance(centerX, centerY, cand.get(i).getCords()[0], cand.get(i).getCords()[1]);
			if(distance > range){
				return false;
			}
		}
		return true;		
	}
	
	private double GetDistance(double Lat1, double Long1, double Lat2, double Long2)
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
		
	public List<SpatialPoint> durationCheck(List<SpatialPoint> result, int time, float oid, int duration){
		List<SpatialPoint> re = new ArrayList<SpatialPoint>(1);
		int size = result.size();
		
		for(int i = 0; i < size ; i++){
			if(result.get(i).getOid() == oid)
				re.add(0, result.get(i));
			else if(result.get(i).getTime() - time < duration && result.get(i).getTime() - time >= 0)
				re.add(result.get(i));
		}		
		return re;
	}
	
	public void cordsToLabel(List<List<SpatialPoint>> src, HashMap<List<String>, Integer> dst){
		for(int i = 0; i < src.size(); i++){
			List<String> temp = new ArrayList<String>();
			for(int k = 0; k < src.get(i).size(); k++){
				temp.add(src.get(i).get(k).getLabel());
			}
			Collections.sort(temp);
			if(dst.containsKey(temp))
				dst.put(temp, dst.get(temp)+1);
			else
				dst.put(temp, 1);
		}
	}

}

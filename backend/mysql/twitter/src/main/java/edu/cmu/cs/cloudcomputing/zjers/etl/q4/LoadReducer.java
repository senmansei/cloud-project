package edu.cmu.cs.cloudcomputing.zjers.etl.q6;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class LoadReducer
		extends
		Reducer<Text, Text, NullWritable, Text> {
	private Text v = new Text();
	
	public class Tweet implements Comparable<Tweet>{
		Long id;
		int offset;
		
		Tweet(Long id, int offset){
			this.id = id;
			this.offset = offset;
		}
		
		@Override
		public boolean equals(Object t2){
			return ((Tweet)t2).id.compareTo(id) == 0;
		}
		
		@Override
		public int hashCode(){
			return id.hashCode();
		}

		@Override
		public int compareTo(Tweet t2) {
			// TODO Auto-generated method stub
			int diff = id.compareTo(t2.id);
			
			if(diff != 0)
				return diff;
			
			return offset - t2.offset;
		}		
	}
	
	public void reduce(Text key,
			Iterable<Text> values, Context context)
			throws IOException, InterruptedException {

		Map<String, Set<Tweet>> map = new HashMap<String, Set<Tweet>>();

		for (Text value : values) {
			String s = value.toString();

			String[] data = s.split(":");
			String sKey = data[0];
			String sValue = data[1];
			
			int offset = Integer.valueOf(sValue.split("\\.")[1]);
			Long id = Long.valueOf(sValue.split("\\.")[0]);

			if (map.containsKey(sKey)) {
				Set<Tweet> ls = map.get(sKey);
				
				Tweet t = new Tweet(id, offset);
				if(!ls.contains(t))
					ls.add(t);
			} else {
				Set<Tweet> ls = new HashSet<Tweet>();
				ls.add(new Tweet(id, offset));
				map.put(sKey, ls);
			}
		}

		Map<String, List<Tweet>> finalmap = new HashMap<String, List<Tweet>>();
		
		for (String sKey : map.keySet()) {
			Set<Tweet> set = map.get(sKey);
			List<Tweet> tls = new ArrayList<Tweet>(set);
			Collections.sort(tls);
			finalmap.put(sKey, tls);
		}

		List<Map.Entry<String, List<Tweet>>> list = new LinkedList<Map.Entry<String, List<Tweet>>>(
				finalmap.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, List<Tweet>>>() {
			public int compare(Map.Entry<String, List<Tweet>> o1, Map.Entry<String, List<Tweet>> o2) {
				int diff = o2.getValue().size() - o1.getValue().size();
				if(diff != 0)
					return diff;
				
				int i = 0;
				
				while(i < o1.getValue().size() && i < o2.getValue().size()){
					int diff2 = o1.getValue().get(i).compareTo(o2.getValue().get(i));
					if(diff2 != 0)
						return diff2;
					
					i++;
				}
				return 0;
			}
		});
		
		
		String k = "\"" + key.toString() + "\"";
		
		for(int i = 0; i < list.size(); i++){
			Map.Entry<String, List<Tweet>> entry = list.get(i);
			String hashtag = "\"" + entry.getKey() + "\"";
			String rank = "\"" + (i + 1) + "\"";
			
			for(int j = 0; j < entry.getValue().size(); j++){
				String tid = "\"" + entry.getValue().get(j).id + "\"";
				
				v.set(k + "," + hashtag + "," + tid + "," + rank);
				context.write(NullWritable.get(), v);
			}
		}
	}
}

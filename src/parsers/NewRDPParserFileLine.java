/** 
 * Author:  anthony.fodor@gmail.com    
 * This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version,
* provided that any use properly credits the author.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details at http://www.gnu.org * * */


package parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;	
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.BioLockJUtils;


public class NewRDPParserFileLine
{
	protected static final Logger log = LoggerFactory.getLogger(NewRDPParserFileLine.class);
	
	private String sequenceId;
	private Map<String, NewRDPNode> taxaMap;
	private static long startTime;
	
	public static final String DOMAIN = "domain";
	public static final String PHYLUM = "phylum";
	public static final String CLASS = "class";
	public static final String ORDER = "order";
	public static final String FAMILY = "family";
	public static final String GENUS = "genus";
	
	public void setSequenceId(String sequenceId)
	{
		this.sequenceId = sequenceId;
	}
	
	public static final String[] TAXA_ARRAY = 
	{
		DOMAIN, PHYLUM, CLASS, ORDER, FAMILY, GENUS
	};
	
	public NewRDPNode getLowestNodeAtThreshold(int threshold)
	{
		NewRDPNode returnNode = null;
		
		for( NewRDPNode node : this.taxaMap.values())
			if( node.getScore() >= threshold)
				returnNode = node;
		
		return returnNode;
	}
	
	public String getLowestRankThreshold(int threshold)
	{
		String returnNode = null;
		
		for( String rank : this.taxaMap.keySet())
			if( this.taxaMap.get(rank).getScore() >= threshold)
				returnNode = rank;
		
		return returnNode;
	}
	
	public static NewRDPParserFileLine newRDPParserFileLineFromPatrickHmpString(String s )
		throws Exception
	{
		String[] splits = s.split(";");
		
		if( splits.length != 7)
			throw new Exception("Unexpected length " + splits.length + " " + s);
		
		NewRDPParserFileLine newLine = new NewRDPParserFileLine();
		newLine.taxaMap = new HashMap<String, NewRDPNode>();
		
		for(int x=6; x >=1; x--)
		{
			if( ! splits[x].equals("unclassified") ) 
			{
				int leftParenIndex = splits[x].indexOf("(");
				String taxa = splits[x].substring(0, leftParenIndex );
				int rightParenIndex = splits[x].indexOf(")");
				float score = Float.parseFloat( splits[x].substring(leftParenIndex+1, rightParenIndex));
				score = score /100f;
				NewRDPNode node = new NewRDPNode(taxa, "" + score);
				newLine.taxaMap.put(TAXA_ARRAY[x-1], node);
			}
			else
			{
				NewRDPNode node = new NewRDPNode("unclassified", "0");
				newLine.taxaMap.put(TAXA_ARRAY[x-1], node);
			}
		}
		
		
		return newLine;
	}
	
	public static HashMap<String, NewRDPParserFileLine> getMapFromPatrickHMP(String filepath)
		throws Exception
	{
		HashMap<String, NewRDPParserFileLine> map = new HashMap<String, NewRDPParserFileLine>();
		
		BufferedReader reader=  new BufferedReader(new FileReader(new File( 
			filepath)));
		
		reader.readLine();
		
		String nextLine = reader.readLine();
		
		while(nextLine != null)
		{
			String[] splits = nextLine.split("\t");
			
			if( map.containsKey(splits[0]) )
				throw new Exception("Duplicate " + splits[0]);
			
			map.put(splits[0], newRDPParserFileLineFromPatrickHmpString(splits[1]));
			
			nextLine = reader.readLine();
		}
		
		reader.close();
		
		return map;
	}
	
	private NewRDPParserFileLine()
	{
		
	}
	

	public static HashMap<String, Integer> 
		getCountsForLevel(List<NewRDPParserFileLine> list, String level, int threshold) throws Exception
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		for(NewRDPParserFileLine fileLine: list)
		{
			NewRDPNode node =	fileLine.getTaxaMap().get(level);
			if( node != null && node.getScore() >= threshold)
			{
				Integer count = map.get(node.getTaxaName());
				
				if(count == null)
					count = 0;
				
				count++;
				
				map.put(node.getTaxaName(), count);
			}
		}
		
		return map;
	}
	
	private NewRDPParserFileLine(String fileLine) throws Exception
	{
		//log.info(fileLine);
		HashMap<String, NewRDPNode> tempMap = new LinkedHashMap<String, NewRDPNode>();
		StringTokenizer sToken = new StringTokenizer(fileLine, "\t");
		
		this.sequenceId = sToken.nextToken();
		
		while( sToken.hasMoreTokens())
		{
			String name = sToken.nextToken();
			
			if( name.trim().equals("-"))
				name= sToken.nextToken();
			
			String rank = sToken.nextToken();
			String scoreString = sToken.nextToken();
			//log.info(name + " " + rank + " " + scoreString);
			NewRDPNode rdpNode = new NewRDPNode(name, scoreString);
			tempMap.put( rank, rdpNode );
		}
		
		taxaMap = Collections.unmodifiableMap(tempMap);
	}
	
	public static int getNumInCommon(String consensusString1, String consensusString2)
		throws Exception
	{
		StringTokenizer sToken1 = new StringTokenizer(consensusString1, ";");
		StringTokenizer sToken2 = new StringTokenizer(consensusString2, ";");
		int numToDo = Math.min(sToken1.countTokens(), sToken2.countTokens());
		
		int val =0 ;
		
		for( int x=0;x < numToDo; x++)
		{
			if( sToken1.nextToken().equals(sToken2.nextToken()) )
				val++;
			else
				return val;
		}
		
		return val;
	}
	
	public static String getConsensusString(List<NewRDPParserFileLine> list, float threshold) 
		throws Exception
	{
		return getConsensusString(list, threshold, list.size()/2);
	}
	
	public static String getConsensusString( List<NewRDPParserFileLine> list, float threshold,
			int minNumber)
	{
		StringBuffer buff = new StringBuffer();
		
		for( int x=0; x < TAXA_ARRAY.length; x++)
		{
			HashMap<String, Integer> countMap = new LinkedHashMap<String, Integer>();
			
			for( NewRDPParserFileLine fileLine : list )
			{
				NewRDPNode rdpNode = fileLine.getTaxaMap().get(TAXA_ARRAY[x]);
				
				if( rdpNode != null && rdpNode.getScore() >= threshold )
				{
					Integer i = countMap.get(rdpNode.getTaxaName());
					
					if( i == null)
						i =0;
					
					i++;
					
					countMap.put(rdpNode.getTaxaName(), i);
				}
			}
			
			boolean gotOne = false;
			
			for( Iterator<String> i = countMap.keySet().iterator(); i.hasNext() && ! gotOne ; )
			{
				String key  =i.next();
				if( countMap.get(key) >= minNumber )
				{
					gotOne = true;
					buff.append( TAXA_ARRAY[x] + ":" +   key + ";");
				}
			}
			
			if( ! gotOne)
				return stripTrailingSemicoling(buff.toString());
		}
			
		return stripTrailingSemicoling(buff.toString());
	}
	
	private static String stripTrailingSemicoling(String inString)
	{
		if( inString.length() == 0 )
			return ";";
		
		if(inString.charAt(inString.length()-1) == ';')
			inString = BioLockJUtils.removeLastChar(inString);
		
		return inString;
	}
	
	public String getSequenceId()
	{
		return sequenceId;
	}
	
	public Map<String, NewRDPNode> getTaxaMap()
	{
		return taxaMap;
	}
	
	public static List<NewRDPParserFileLine> getRdpList(File rdpFile) throws Exception
	{
		int numCPUs = Runtime.getRuntime().availableProcessors();
		return getRdpList(rdpFile, numCPUs);
	}
	

	public static List<NewRDPParserFileLine> getRdpList(String rdpFilePath) throws Exception
	{
		return getRdpList(new File(rdpFilePath));
	}
	
	public static List<NewRDPParserFileLine> getRdpListSingleThread(String rdpFilePath)
		throws Exception
	{
		return getRdpListSingleThread(new File(rdpFilePath));
	}
	
	public static List<NewRDPParserFileLine> getRdpListSingleThread(File rdpFile) 
			throws Exception
	{
		List<NewRDPParserFileLine> list = new ArrayList<NewRDPParserFileLine>();
		
		
		BufferedReader reader = rdpFile.getName().toLowerCase().endsWith("gz") ?
				new BufferedReader(new InputStreamReader( 
						new GZIPInputStream( new FileInputStream( rdpFile)))) :  
				new BufferedReader(new FileReader(rdpFile));
		
		String nextLine = reader.readLine();
		
		while( nextLine != null)
		{
			//log.info(nextLine);
			list.add(new NewRDPParserFileLine(nextLine));
			nextLine= reader.readLine();
		}
		
		reader.close();
		long endTime = System.currentTimeMillis();
		log.info( "Parse time = " + (endTime - startTime)/1000f + " seconds " );
		
		return list;
	}
	
	public static List<NewRDPParserFileLine> getRdpList(File rdpFile, int numCPUs) 
		throws Exception
	{
		log.info("Parsing with " + numCPUs + " cpus ");
		log.info("Workers started at " + (System.currentTimeMillis() - startTime)/1000f);
		List<SubFileParser> workers = getCompletedWorkers(rdpFile, numCPUs);
		log.info("Workers finished at " + (System.currentTimeMillis() - startTime)/1000f);
		
		//single threaded
		List<NewRDPParserFileLine> returnList = new ArrayList<NewRDPParserFileLine>();
		
		for( SubFileParser sfp : workers )
		{
			if( sfp.returnException != null)
				 throw new Exception(sfp.returnException);
			
			returnList.addAll(sfp.resultsList);
		}
		
		long endTime = System.currentTimeMillis();
		log.info( "Parse time = " + (endTime - startTime)/1000f + " seconds " );
		
		return Collections.unmodifiableList(returnList);
		
	}
	
	private static List<SubFileParser> getCompletedWorkers( File rdpFile, int numCPUs)
		throws Exception
	{
		BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<String>();
		List<SubFileParser> subList = new ArrayList<SubFileParser>();
		int numAdded = 0;
		List<Thread> startedThreads = new ArrayList<Thread>();
		
		for( int x=0; x < numCPUs; x++)
		{
			SubFileParser sfp = new SubFileParser(blockingQueue);
			subList.add(sfp);
			Thread t =  new Thread(sfp);
			t.start();
			startedThreads.add(t);
		}
			
		BufferedReader reader = new BufferedReader(new FileReader(rdpFile));
		String nextLine = reader.readLine();
		
		while(nextLine != null)
		{
			blockingQueue.add(nextLine);
			nextLine = reader.readLine();
			numAdded++;
		}
		
		reader.close();
		
		while(! blockingQueue.isEmpty())
			Thread.yield();
		
		boolean keepLooping = true;
		
		while(keepLooping)
		{
			int assignedCount =0;
			
			for( SubFileParser sfp : subList )
				assignedCount += sfp.listSize;
			
			if( assignedCount > numAdded )
				throw new Exception("Threading error");
			
			if( assignedCount == numAdded)
			{
				keepLooping = false;
			}
			else
			{
				Thread.yield();
			}
		}
		
		for( Thread t : startedThreads)
			t.interrupt();
		
		return subList;
	}
	
	public static HashMap<String, NewRDPParserFileLine> getAsMapFromSingleThread( 
			String rdpFilePath) throws Exception
	{
		return getAsMapFromSingleThread(new File(rdpFilePath));
	}
	
	public static HashMap<String, NewRDPParserFileLine> getAsMapFromSingleThread( 
		File rdpFile) throws Exception
	{
		HashMap<String, NewRDPParserFileLine> map = new LinkedHashMap<String, NewRDPParserFileLine>();
		
		List<NewRDPParserFileLine> list = getRdpListSingleThread(rdpFile);
		
		for( NewRDPParserFileLine rdp : list )
		{
			String key = rdp.sequenceId;
			
			if( key.startsWith(">"))
				key = key.substring(1);
			
			if( map.containsKey(key) )
				throw new Exception("WARNING: Duplicate key " + key);
			
			map.put(key, rdp);
		}
		
		return map;
	}
	
	private static class SubFileParser implements Runnable
	{	
		private final List<NewRDPParserFileLine> resultsList = 
			new ArrayList<NewRDPParserFileLine>();
		
		private final BlockingQueue<String> blockingQueue;
		
		private volatile Exception returnException = null;
		private volatile int listSize = 0;
		
		private SubFileParser(BlockingQueue<String> blockingQueue)
		{
			this.blockingQueue = blockingQueue;
		}
		
		public void run()
		{
			try
			{
				while( true)
				{
					String s= blockingQueue.take();
					resultsList.add( new NewRDPParserFileLine( s));
					listSize = resultsList.size();
				}
			}
			catch(Exception ex)
			{
				if( ex instanceof InterruptedException)
				{
					if( ! blockingQueue.isEmpty() )
						returnException= new Exception("Inappropriate interruption");
				}
				else
				{
					returnException = ex;
				}
			}
			finally
			{
				long endTime= System.currentTimeMillis();
				log.info("Thread finished " + (endTime - startTime)/1000f);
			}	
		}
	}
	
	public String getSummaryString()
	{
		StringBuffer buff = new StringBuffer();
		
		for( Iterator<NewRDPNode> i = this.taxaMap.values().iterator(); i.hasNext(); )
		{
			NewRDPNode node = i.next();
			
			buff.append(node.getTaxaName() + "(");
			buff.append(node.getScore() + ")");
			
			if( i.hasNext())
				buff.append(";");
		}
		
		return buff.toString();
	}
	
	public String getSummaryString(int skip)
	{
		StringBuffer buff = new StringBuffer();
		
		for( Iterator<NewRDPNode> i = this.taxaMap.values().iterator(); i.hasNext(); )
		{
		
			NewRDPNode node = i.next();
			
			if( skip <=0)
			{
				buff.append(node.getTaxaName() + "(");
				buff.append(node.getScore() + ")");
				
				if( i.hasNext())
					buff.append(";");
				
			}
			
			skip--;
		}
		
		return buff.toString();
	}
	
	public String getSummaryStringNoScore(float threshold, int skip)
	{
		StringBuffer buff = new StringBuffer();
		

		boolean firstTime = true;
		for( Iterator<NewRDPNode> i = this.taxaMap.values().iterator(); i.hasNext(); )
		{
			for( int x=0; x < skip && firstTime; x++)
				i.next();
			
			firstTime= false;
			NewRDPNode node = i.next();
				
				if(node.getScore() >= threshold )
					buff.append(node.getTaxaName() + ";");
				else
					buff.append(  "unclassified;");
				
		}
					
		String s = buff.toString();
		
		if( s.endsWith(";"))
			s = s.substring(0, s.length() -1);
		
		return s;
	}
	
	public String getSummaryStringCurtis(float threshold, int skip)
	{
		StringBuffer buff = new StringBuffer();
		

		boolean firstTime = true;
		for( Iterator<NewRDPNode> i = this.taxaMap.values().iterator(); i.hasNext(); )
		{
			for( int x=0; x < skip && firstTime; x++)
				i.next();
			
			firstTime= false;
			NewRDPNode node = i.next();
				
			if(node.getScore() >= threshold )
				buff.append(node.getTaxaName() + "|");				
		}
		
		String s=  buff.toString();
		
		if( s.length()==0)
			s = "unclassified|";
					
		return s.replace(" ", "_");
	}
}

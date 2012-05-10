package net.tomp2p.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureTask;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.config.Configurations;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;
import net.tomp2p.task.Worker;

public class ExampleMapReduce
{
	final private static Random rnd = new Random(42L);
	
	final public static String text1 = "to be or not to be";
	final public static String text2 = "to do is to be";
	final public static String text3 = "to be is to do";
	
	public static void main(String[] args) throws Exception
	{
		Peer master = null;
		try
		{
			Peer[] peers = ExampleUtils.createAndAttachNodes(100, 4001);
			master = peers[0];
			ExampleUtils.bootstrap(peers);
			Number160 nr = new Number160(rnd);
			exampleMapReduce(peers, nr);
		}
		finally
		{
			master.shutdown();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void exampleMapReduce(Peer[] peers, Number160 nr) throws IOException, ClassNotFoundException
	{
		final Number160 result = Number160.createHash(0);
		final Number160 nr1 = Number160.createHash(1);
		final Number160 nr2 = Number160.createHash(2);
		final Number160 nr3 = Number160.createHash(3);
		Worker map = new Worker()
		{
			private static final long serialVersionUID = 276677516112036039L;
			@Override
			public Map<Number160, Data> execute(Peer peer, Map<Number160, Data> inputData) throws Exception
			{
				Map<Number160, FutureDHT> futures = new HashMap<Number160, FutureDHT>();
				for(Map.Entry<Number160, Data> entry: inputData.entrySet())
				{
					String text = (String)entry.getValue().getObject();
					StringTokenizer st = new StringTokenizer(text, " ");
					while(st.hasMoreTokens())
					{
						String word = st.nextToken();
						Number160 key = Number160.createHash(word);
						futures.put(key, peer.add(key).setData(new Data(1)).setList(true).add());
					}
				}
				Map<Number160, Data> retVal = new HashMap<Number160, Data>();
				for(Map.Entry<Number160, FutureDHT> entry : futures.entrySet())
				{
					entry.getValue().awaitUninterruptibly();
					retVal.put(entry.getKey(), new Data(entry.getValue().getType()));
				}
				return retVal;
			}
		};
		Worker reduce = new Worker()
		{
			private static final long serialVersionUID = 87921790732341L;
			@Override
			public Map<Number160, Data> execute(Peer peer, Map<Number160, Data> inputData)
					throws Exception
			{
				Map<Number160, FutureDHT> futures = new HashMap<Number160, FutureDHT>();
				for(Map.Entry<Number160, Data> entry: inputData.entrySet())
				{
					Number160 key = entry.getKey();
					int size = peer.getPeerBean().getStorage().get(key, Configurations.DEFAULT_DOMAIN, Number160.ZERO, Number160.MAX_VALUE).size();
					Map<Number160, Data> dataMap = new HashMap<Number160, Data>();
					dataMap.put(peer.getPeerAddress().getID(), new Data(size));
					futures.put(key, peer.put(result, dataMap, Configurations.defaultStoreConfiguration()));
				}
				Map<Number160, Data> retVal = new HashMap<Number160, Data>();
				for(Map.Entry<Number160, FutureDHT> entry : futures.entrySet())
				{
					entry.getValue().awaitUninterruptibly();
					retVal.put(entry.getKey(), new Data(entry.getValue().getType()));
				}
				return retVal;
			}
		};
		FutureTask ft1 = peers[33].submit(nr1, map).setDataMap(createData(text1)).submit();
		FutureTask ft2 = peers[34].submit(nr2, map).setDataMap(createData(text2)).submit();
		FutureTask ft3 = peers[35].submit(nr3, map).setDataMap(createData(text3)).submit();
		ft1.awaitUninterruptibly();
		ft2.awaitUninterruptibly();
		ft3.awaitUninterruptibly();
		Set<Number160> intermediate = new HashSet<Number160>();
		intermediate.addAll(((Map<Number160, Data>)(ft1.getRawDataMap().values())).keySet());
		intermediate.addAll(((Map<Number160, Data>)(ft2.getRawDataMap().values())).keySet());
		intermediate.addAll(((Map<Number160, Data>)(ft3.getRawDataMap().values())).keySet());
		int i=0;
		List<FutureTask> resultList = new ArrayList<FutureTask>();
		for(Number160 location: intermediate)
		{
			resultList.add(peers[36+i].submit(location, reduce).setDataMap(createData(location)).submit());
			i++;
		}
		//now we wait for the completion
		for(FutureTask futureTask:resultList)
		{
			futureTask.awaitUninterruptibly();
		}
		FutureDHT futureDHT = peers[20].getAll(result);
		futureDHT.awaitUninterruptibly();
		int counter = 0;
		for(Map.Entry<Number160, Data> entry: futureDHT.getDataMap().entrySet())
		{
			counter += (Integer) entry.getValue().getObject();
		}
		System.out.println("count returns " + counter);
	}

	private static Map<Number160, Data> createData(Number160 location) throws IOException
	{
		Map<Number160, Data> retVal = new HashMap<Number160, Data>();
		Data data = new Data(location);
		retVal.put(data.getHash(), data);
		return retVal;
	}

	private static Map<Number160, Data> createData(String text) throws IOException
	{
		Map<Number160, Data> retVal = new HashMap<Number160, Data>();
		Data data = new Data(text);
		retVal.put(data.getHash(), data);
		return retVal;
	}
}

/*
 * Copyright 2013 Thomas Bocek
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.tomp2p.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import net.tomp2p.Utils2;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Timings;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Thomas Bocek
 * 
 */
public class TestReplication {

    private static final Random RND = new Random(42L);
    private static final Random RND2 = new Random(42L);
    private static final int PORT = 4001;
    private static final int SECOND = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(TestReplication.class);
    private static final int NR_PEERS = 100;
    private static final int NINE_SECONDS = 9000;

    /**
     * Test the direct replication, where the initiator is also the responsible peer.
     * 
     * @throws Exception .
     */
    /*@Test
    public void testDirectReplication() throws Exception {
        Peer master = null;
        try {
            // setup
            Peer[] peers = Utils2.createNodes(NR_PEERS, RND, PORT);
            master = peers[0];
            List<FutureBootstrap> tmp = new ArrayList<FutureBootstrap>();
            for (int i = 0; i < peers.length; i++) {
                if (peers[i] != master) {
                    FutureBootstrap res = peers[i].bootstrap().setPeerAddress(master.getPeerAddress()).start();
                    tmp.add(res);
                }
            }
            int i = 0;
            for (FutureBootstrap fm : tmp) {
                fm.awaitUninterruptibly();
                if (fm.isFailed()) {
                    System.err.println(fm.getFailedReason());
                }
                Assert.assertEquals(true, fm.isSuccess());
                System.err.println("i:" + (++i));
            }
            final AtomicInteger counter = new AtomicInteger(0);
            // New storage class to count the puts.
            final class MyStorageMemory extends StorageMemory {
                @Override
                public PutStatus put(final Number160 locationKey, final Number160 domainKey,
                        final Number160 contentKey, final Data newData, final PublicKey publicKey,
                        final boolean putIfAbsent, final boolean domainProtection) {
                    System.err.println("here");
                    counter.incrementAndGet();
                    return super.put(locationKey, domainKey, contentKey, newData, publicKey, putIfAbsent,
                            domainProtection);
                }
            }
            final int peerNr = 50;
            final int repetitions = 5;
            peers[peerNr].getPeerBean().setStorage(new MyStorageMemory());

            FutureCreate<FutureDHT> futureCreate = new FutureCreate<FutureDHT>() {
                @Override
                public void repeated(final FutureDHT future) {
                    System.err.println("chain1...");
                }
            };
            FutureDHT fdht = peers[1].put(peers[peerNr].getPeerID()).setData(new Data("test")).setRefreshSeconds(2)
                    .setDirectReplication().setFutureCreate(futureCreate).start();
            Timings.sleep(NINE_SECONDS);
            Assert.assertEquals(repetitions, counter.get());
            fdht.shutdown();
            System.err.println("stop chain1");
            final AtomicInteger counter2 = new AtomicInteger(0);
            futureCreate = new FutureCreate<FutureDHT>() {
                @Override
                public void repeated(final FutureDHT future) {
                    System.err.println("chain2...");
                    counter2.incrementAndGet();
                }
            };
            FutureDHT fdht2 = peers[2].remove(peers[peerNr].getPeerID()).setRefreshSeconds(1)
                    .setRepetitions(repetitions).setFutureCreate(futureCreate).setDirectReplication().start();
            Timings.sleep(NINE_SECONDS);
            Assert.assertEquals(repetitions, counter.get());
            Assert.assertEquals(true, fdht2.isSuccess());
            Assert.assertEquals(repetitions, counter2.get());
        } finally {
            if (master != null) {
                master.halt();
            }
        }
    }*/
    
    /*@Test
    public void testActiveReplicationRefresh() throws Exception {
        Peer master = null;
        try {
            // setup
            Peer[] peers = Utils2.createNodes(100, rnd, 4001, 5 * 1000, true);
            master = peers[0];
            Number160 locationKey = master.getPeerID().xor(new Number160(77));
            // store
            Data data = new Data("Test");
            FutureDHT futureDHT = master.put(locationKey).setData(data).start();
            futureDHT.awaitUninterruptibly();
            futureDHT.getFutureRequests().awaitUninterruptibly();
            Assert.assertEquals(true, futureDHT.isSuccess());
            // bootstrap
            List<FutureBootstrap> tmp2 = new ArrayList<FutureBootstrap>();
            for (int i = 0; i < peers.length; i++) {
                if (peers[i] != master) {
                    tmp2.add(peers[i].bootstrap().setPeerAddress(master.getPeerAddress()).start());
                }
            }
            for (FutureBootstrap fm : tmp2) {
                fm.awaitUninterruptibly();
                Assert.assertEquals(true, fm.isSuccess());
            }
            for (int i = 0; i < peers.length; i++) {
                for (BaseFuture baseFuture : peers[i].getPendingFutures().keySet())
                    baseFuture.awaitUninterruptibly();
            }
            // wait for refresh
            Thread.sleep(6000);
            //
            TreeSet<PeerAddress> tmp = new TreeSet<PeerAddress>(master.getPeerBean().getPeerMap()
                    .createPeerComparator(locationKey));
            tmp.add(master.getPeerAddress());
            for (int i = 0; i < peers.length; i++)
                tmp.add(peers[i].getPeerAddress());
            int i = 0;
            for (PeerAddress closest : tmp) {
                final FutureChannelCreator fcc = master.getConnectionBean().getConnectionReservation()
                        .reserve(1);
                fcc.awaitUninterruptibly();
                ChannelCreator cc = fcc.getChannelCreator();
                FutureResponse futureResponse = master.getStoreRPC().get(closest, locationKey,
                        DHTBuilder.DEFAULT_DOMAIN, null, null, null, false, false, false, false, cc, false);
                futureResponse.awaitUninterruptibly();
                master.getConnectionBean().getConnectionReservation().release(cc);
                Assert.assertEquals(true, futureResponse.isSuccess());
                Assert.assertEquals(1, futureResponse.getResponse().getDataMap().size());
                i++;
                if (i >= 5)
                    break;
            }
        } finally {
            if (master != null) {
                master.shutdown().await();
            }
        }
    }*/
    
    @Test
    public void testDataloss() throws IOException, InterruptedException {
        Peer p1 = null;
        Peer p2 = null;
        Peer p3 = null;
        try {
            
            p1 = new PeerMaker(Number160.createHash("111")).setEnableIndirectReplication(true).ports(PORT)
                    .makeAndListen();
            p2 = new PeerMaker(Number160.createHash("22")).setEnableIndirectReplication(true).ports(PORT+1)
                    .makeAndListen();
            p3 = new PeerMaker(Number160.createHash("33")).setEnableIndirectReplication(true).ports(PORT+2)
                    .makeAndListen();
            Utils2.perfectRouting(p1, p2, p3);
            Number160 locationKey = Number160.createHash("test1");
            FuturePut fp = p2.put(locationKey).setData(new Data("hallo")).setRequestP2PConfiguration(new RequestP2PConfiguration(2, 10, 0)).start();
            fp.awaitUninterruptibly();
            getReplicasCount(locationKey, p1, p2, p3);
            //
            p3.announceShutdown().start().awaitUninterruptibly();
            p3.shutdown().awaitUninterruptibly();
            Thread.sleep(500);
            p3 = new PeerMaker(locationKey).setEnableIndirectReplication(true).ports(PORT+3).makeAndListen();
            System.out.println("now we add a peer that matches perfectly the key " + locationKey+ ". This will now become the responsible peer");
            p3.bootstrap().setPeerAddress(p1.getPeerAddress()).start().awaitUninterruptibly();
            getReplicasCount(locationKey, p1, p2, p3);
            Thread.sleep(500);
            p1.announceShutdown().start().awaitUninterruptibly();
            p1.shutdown().awaitUninterruptibly();
            p2.announceShutdown().start().awaitUninterruptibly();
            p2.shutdown().awaitUninterruptibly();
            Thread.sleep(500);
            int count = getReplicasCount(locationKey, p1, p2, p3);
            Assert.assertEquals(1, count);
            
        } finally {
            if (p1 != null && !p1.isShutdown()) {
                p1.shutdown().awaitUninterruptibly();
            }
            if (p2 != null && !p2.isShutdown()) {
                p2.shutdown().awaitUninterruptibly();
            }
            if (p3 != null && !p3.isShutdown()) {
                p3.shutdown().awaitUninterruptibly();
            }
        }
    }
    
    @Test
    public void testDataloss2() throws IOException, InterruptedException {
    	Peer p1 = null;
        Peer p2 = null;
        Peer p3 = null;
        try {
            
            p1 = new PeerMaker(Number160.createHash("111")).setEnableIndirectReplication(true).ports(PORT)
                    .makeAndListen();
            p2 = new PeerMaker(Number160.createHash("22")).setEnableIndirectReplication(true).ports(PORT+1)
                    .makeAndListen();
            p3 = new PeerMaker(Number160.createHash("33")).setEnableIndirectReplication(true).ports(PORT+2)
                    .makeAndListen();
            Utils2.perfectRouting(p1, p2, p3);
            Number160 locationKey = Number160.createHash("test1");
            FuturePut fp = p2.put(locationKey).setData(new Data("hallo")).setRequestP2PConfiguration(new RequestP2PConfiguration(2, 10, 0)).start();
            fp.awaitUninterruptibly();
            getReplicasCount(locationKey, p1, p2, p3);
            
            p3.announceShutdown().start().awaitUninterruptibly();
            p3.shutdown().awaitUninterruptibly();
            p1.announceShutdown().start().awaitUninterruptibly();
            p1.shutdown().awaitUninterruptibly();
            Thread.sleep(500);
            getReplicasCount(locationKey, p1, p2, p3);
            
            //
            
            p3 = new PeerMaker(locationKey).setEnableIndirectReplication(true).ports(PORT+3).makeAndListen();
            System.out.println("now we add a peer that matches perfectly the key " + locationKey+ ". This will now become the responsible peer");
            p3.bootstrap().setPeerAddress(p2.getPeerAddress()).start().awaitUninterruptibly();
            
            p1 = new PeerMaker(Number160.createHash("1111")).setEnableIndirectReplication(true).ports(PORT+4)
                    .makeAndListen();
            p1.bootstrap().setPeerAddress(p2.getPeerAddress()).start().awaitUninterruptibly();
            
            getReplicasCount(locationKey, p1, p2, p3);
            Thread.sleep(500);
            
            int count = getReplicasCount(locationKey, p1, p2, p3);
            Assert.assertEquals(2, count);
            
        } finally {
        	if (p1 != null && !p1.isShutdown()) {
                p1.shutdown().awaitUninterruptibly();
            }
            if (p2 != null && !p2.isShutdown()) {
                p2.shutdown().awaitUninterruptibly();
            }
            if (p3 != null && !p3.isShutdown()) {
                p3.shutdown().awaitUninterruptibly();
            }
        }
    }
    
    private static int getReplicasCount(Number160 key, Peer... peers) {
        int count = 0;
        Number160 locationKey = null;
        ArrayList<Number160> peerIds = new ArrayList<Number160>();
        for(int i=0; i<peers.length; i++)
            if(!peers[i].isShutdown())
                for(Map.Entry<Number640, Data> entry: peers[i].getPeerBean().storage().get().entrySet()){
                    locationKey = entry.getKey().getLocationKey();
                    if(locationKey.equals(key)){
                        count++;
                        peerIds.add(peers[i].getPeerID());
                        System.out.println("key " + key + " FoundOn peers["+i+"]=" + peers[i].getPeerID());
                        //break;
                    }
                }
        System.out.println("key " + key + " FoundOn "+count+" peers");
        return count;
    }
    
    @Test
    public void testSimpleIndirectReplicationForward() throws Exception {
        final Random rnd = new Random(42L);
        Peer master = null;
        try {
            // setup
            Peer[] peers = Utils2.createNodes(2, rnd, PORT, new AutomaticFuture() {
                @Override
                public void futureCreated(BaseFuture future) {
                    System.err.println("future created "+ future);
                }
            }, true);
            master = peers[0];
            //print out info
            System.err.println("looking for "+searchPeer(Number160.createHash("2"), peers).getPeerAddress()+" in ");
            for(Peer peer:peers) {
                System.err.println(peer.getPeerAddress());
            }
            //store data, the two peers do not know each other
            Data data = new Data("Test");
            FuturePut futureDHT = master.put(Number160.createHash("2")).setData(data).start();
            futureDHT.awaitUninterruptibly();
            futureDHT.getFutureRequests().awaitUninterruptibly();
            //now, do the routing, so that each peers know each other. The content should be moved
            Assert.assertEquals(false, peers[1].getPeerBean().storage().contains(new Number640(Number160.createHash("2"), Number160.ZERO, Number160.ZERO, Number160.ZERO)));
            Utils2.perfectRouting(peers);
            //we should see now the forward replication
            Thread.sleep(1000);
            //test it
            Assert.assertEquals(true, peers[1].getPeerBean().storage().contains(new Number640(Number160.createHash("2"), Number160.ZERO, Number160.ZERO, Number160.ZERO)));
            
        } finally {
            if (master != null) {
                master.shutdown().awaitUninterruptibly();
            }
        }
    }

    /**
     * Test the indirect replication where the closest peer is responsible for a location key.
     * 
     * @throws Exception .
     */
    @Test
    public void testIndirectReplicationForward() throws Exception {
        Peer master = null;
        try {
            // setup
            Peer[] peers = Utils2.createNodes(NR_PEERS, RND2, PORT, new AutomaticFuture() {
                @Override
                public void futureCreated(BaseFuture future) {
                    System.err.println("future created "+ future);
                }
            }, true);
            master = peers[0];
            Number160 locationKey = new Number160(RND2);
            master.getPeerBean().peerMap();
            // closest
            TreeSet<PeerAddress> tmp = new TreeSet<PeerAddress>(PeerMap.createComparator(locationKey));
            tmp.add(master.getPeerAddress());
            for (int i = 0; i < peers.length; i++) {
                tmp.add(peers[i].getPeerAddress());
            }
            PeerAddress closest = tmp.iterator().next();
            System.err.println("closest to " + locationKey + " is " + closest);
            // store
            Data data = new Data("Test");
            FuturePut futureDHT = master.put(locationKey).setData(data).start();
            futureDHT.awaitUninterruptibly();
            futureDHT.getFutureRequests().awaitUninterruptibly();
            Assert.assertEquals(true, futureDHT.isSuccess());
            List<FutureBootstrap> tmp2 = new ArrayList<FutureBootstrap>();
            Utils2.perfectRouting(peers);
            //for (int i = 0; i < peers.length; i++) {
            //    if (peers[i] != master) {
            //        tmp2.add(peers[i].bootstrap().setPeerAddress(master.getPeerAddress()).start());
            //    }
            //}
            for (FutureBootstrap fm : tmp2) {
                fm.awaitUninterruptibly();
                Assert.assertEquals(true, fm.isSuccess());
            }
            
            // wait for the replication
            Peer peerClose = searchPeer(closest, peers);
            int i = 0;
            // wait for 2.5 sec
            final int tests = 10;
            final int wait = 2500;
            while (!peerClose.getPeerBean().storage()
                    .contains(new Number640(locationKey, Number160.ZERO, Number160.ZERO, Number160.ZERO))) {
                Timings.sleep(wait / tests);
                i++;
                if (i > tests) {
                    break;
                }
            }
            Assert.assertEquals(true, peerClose.getPeerBean().storage()
                    .contains(new Number640(locationKey, Number160.ZERO, Number160.ZERO, Number160.ZERO)));
        } finally {
            if (master != null) {
                master.shutdown().awaitUninterruptibly();
            }
        }
    }

    /**
     * Test the replication with the following scenario: 2 peers online that store data, third peer joins. This means
     * for indirect replication, that the content need to be stored on peer 3 as well.
     * 
     * @throws IOException .
     * @throws InterruptedException .
     */
    /*@Test
    public void testReplication() throws IOException, InterruptedException {
        Peer p1 = null;
        try {
            p1 = new PeerMaker(Number160.createHash("1")).setEnableIndirectReplication(true).setPorts(PORT)
                    .makeAndListen();
            Peer p2 = new PeerMaker(Number160.createHash("2")).setEnableIndirectReplication(true).setMasterPeer(p1)
                    .makeAndListen();
            Peer p3 = new PeerMaker(Number160.createHash("3")).setEnableIndirectReplication(true).setMasterPeer(p1)
                    .makeAndListen();

            LOG.info("p1 = " + p1.getPeerAddress());
            LOG.info("p2 = " + p2.getPeerAddress());
            LOG.info("p3 = " + p3.getPeerAddress());

            p2.bootstrap().setPeerAddress(p1.getPeerAddress()).start().awaitUninterruptibly();
            p2.put(Number160.createHash("key")).setData(new Data("test")).start().awaitUninterruptibly();
            LOG.info("storage done");
            Thread.sleep(SECOND);
            LOG.info("new peer joins");
            p3.bootstrap().setPeerAddress(p1.getPeerAddress()).start().awaitUninterruptibly();

            Thread.sleep(SECOND);

            Data test = p3.getPeerBean().getStorage()
                    .get(Number160.createHash("key"), DHTBuilder.DEFAULT_DOMAIN, Number160.ZERO);

            Assert.assertNotNull(test);
        } finally {
            if (p1 != null) {
                p1.halt();
            }
        }
    }*/

    /**
     * Search a Peer in a list.
     * 
     * @param peerAddress
     *            The peer to search for
     * @param peers
     *            The list of peers
     * @return The found peer or null if not found
     */
    private static Peer searchPeer(final PeerAddress peerAddress, final Peer[] peers) {
        for (Peer peer : peers) {
            if (peer.getPeerAddress().equals(peerAddress)) {
                return peer;
            }
        }
        return null;
    }
    
    private static Peer searchPeer(final Number160 locationKey, final Peer[] peers) {
        TreeSet<PeerAddress> tmp = new TreeSet<PeerAddress>(PeerMap.createComparator(locationKey));
        for (int i = 0; i < peers.length; i++) {
            tmp.add(peers[i].getPeerAddress());
        }
        PeerAddress peerAddress = tmp.iterator().next();
        return searchPeer(peerAddress, peers);
    }
}

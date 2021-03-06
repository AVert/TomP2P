/*
 * Copyright 2009 Thomas Bocek
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
package net.tomp2p.futures;

import java.util.Collection;
import java.util.Map;

import net.tomp2p.p2p.EvaluatingSchemeDHT;
import net.tomp2p.p2p.VotingSchemeDHT;
import net.tomp2p.p2p.builder.DHTBuilder;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

/**
 * The future object for put() operations including routing.
 * 
 * @author Thomas Bocek
 */
public class FutureRemove extends FutureDHT<FutureRemove> {
    // The minimum number of expected results. This is also used for put()
    // operations to decide if a future failed or not.
    private final int min;

    // Since we receive multiple results, we have an evaluation scheme to
    // simplify the result
    private final EvaluatingSchemeDHT evaluationScheme;

    // Storage of results
    private Map<PeerAddress, Collection<Number640>> rawKeys640;
    private Map<PeerAddress, Map<Number640, Data>> rawData;

    // Flag indicating if the minimum operations for put have been reached.
    private boolean minReached;

    /**
     * Default constructor.
     */
    public FutureRemove(final DHTBuilder<?> builder) {
        this(builder, 0, new VotingSchemeDHT());
    }

    /**
     * Creates a new DHT future object that keeps track of the status of the DHT operations.
     * 
     * @param min
     *            The minimum of expected results
     * @param evaluationScheme
     *            The scheme to evaluate results from multiple peers
     */
    public FutureRemove(final DHTBuilder<?> builder, final int min, final EvaluatingSchemeDHT evaluationScheme) {
        super(builder);
        this.min = min;
        this.evaluationScheme = evaluationScheme;
        self(this);
    }

    /**
     * Finish the future and set the keys that have been stored. Success or failure is determined if the communication
     * was successful. This means that we need to further check if the other peers have denied the storage (e.g., due to
     * no storage space, no security permissions). Further evaluation can be retrieved with {@link #getAvgStoredKeys()}
     * or if the evaluation should be done by the user, use {@link #getRawKeys()}.
     * 
     * @param domainKey
     *            The domain key
     * @param locationKey
     *            The location key
     * @param rawKeys
     *            The keys that have been stored with information on which peer it has been stored
     * @param rawKeys480
     *            The keys with locationKey and domainKey Flag if the user requested putIfAbsent
     */
    public void setStoredKeys(final Map<PeerAddress, Collection<Number640>> rawKeys640) {
        synchronized (lock) {
            if (!setCompletedAndNotify()) {
                return;
            }
            this.rawKeys640 = rawKeys640;
            final int size = rawKeys640 == null ? 0 : rawKeys640.size();
            this.minReached = size >= min;
            this.type = size > 0 ? FutureType.OK : FutureType.FAILED;
            this.reason = size > 0 ? "Minimun number of results reached" : "Expected > 0 result, but got " + size;
        }
        notifyListeners();
    }

    /**
     * @return The average keys received from the DHT. Only evaluates rawKeys.
     */
    public double getAvgStoredKeys() {
        synchronized (lock) {
            final int size = rawKeys640.size();
            int total = 0;
            for (Collection<Number640> collection : rawKeys640.values()) {
                if (collection != null) {
                    total += collection.size();
                }
            }
            return total / (double) size;
        }
    }
    
    /**
     * Finish the future and set the keys and data that have been received.
     * 
     * @param rawData
     *            The keys and data that have been received with information from which peer it has been received.
     */
    public void setReceivedData(final Map<PeerAddress, Map<Number640, Data>> rawData) {
        synchronized (lock) {
            if (!setCompletedAndNotify()) {
                return;
            }
            this.rawData = rawData;
            final int size = rawData.size();
            this.minReached = size >= min;
            this.type = size > 0 ? FutureType.OK : FutureType.FAILED;
            this.reason = size > 0 ? "Minimun number of results reached" : "Expected >0 result, but got " + size;
        }
        notifyListeners();
    }

    /**
     * Returns the raw keys from the storage or removal operation.
     * 
     * @return The raw keys and the information which peer has been contacted
     */
    public Map<PeerAddress, Collection<Number640>> getRawKeys() {
        synchronized (lock) {
            return rawKeys640;
        }
    }

    /**
     * Checks if the minimum of expected results have been reached. This flag is also used for determining the success
     * or failure of this future for put and send_direct.
     * 
     * @return True, if expected minimum results have been reached.
     */
    public boolean isMinReached() {
        synchronized (lock) {
            return minReached;
        }
    }

    /**
     * Returns the keys that have been stored or removed after evaluation. The evaluation gets rid of the PeerAddress
     * information, by either a majority vote or cumulation. Use {@link FutureRemove#getEvalKeys()} instead of this method.
     * 
     * @return The keys that have been stored or removed
     */
    public Collection<Number640> getEvalKeys() {
        synchronized (lock) {
            return evaluationScheme.evaluate6(rawKeys640);
        }
    }
    
    /**
     * Returns the raw data from the get operation.
     * 
     * @return The raw data and the information which peer has been contacted
     */
    public Map<PeerAddress, Map<Number640, Data>> getRawData() {
        synchronized (lock) {
            return rawData;
        }
    }
    
    /**
     * Return the data from get() after evaluation. The evaluation gets rid of the PeerAddress information, by either a
     * majority vote or cumulation.
     * 
     * @return The evaluated data that have been received.
     */
    public Map<Number640, Data> getDataMap() {
        synchronized (lock) {
            return evaluationScheme.evaluate2(rawData);
        }
    }
}

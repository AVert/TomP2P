/*
 * Copyright 2012 Thomas Bocek
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

package net.tomp2p.p2p.builder;

import java.util.ArrayList;
import java.util.Collection;

import net.tomp2p.futures.FutureDigest;
import net.tomp2p.p2p.EvaluatingSchemeDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.VotingSchemeDHT;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.rpc.SimpleBloomFilter;

public class DigestBuilder extends DHTBuilder<DigestBuilder> implements SearchableBuilder {
    
    private final static FutureDigest FUTURE_SHUTDOWN = new FutureDigest(null)
    .setFailed("digest builder - peer is shutting down");
    
    // if we don't provide any content key, the default is Number160.ZERO
    private final static Collection<Number160> NUMBER_ZERO_CONTENT_KEYS = new ArrayList<Number160>(1);

    private Collection<Number160> contentKeys;

    private Collection<Number640> keys;

    private Number160 contentKey;

    private SimpleBloomFilter<Number160> keyBloomFilter;

    private SimpleBloomFilter<Number160> contentBloomFilter;

    private EvaluatingSchemeDHT evaluationScheme;

    private Number640 from;

    private Number640 to;

    //
    private boolean all = false;

    private boolean returnBloomFilter = false;

    private boolean ascending = true;

    private int returnNr = -1;

    static {
        NUMBER_ZERO_CONTENT_KEYS.add(Number160.ZERO);
    }

    public DigestBuilder(Peer peer, Number160 locationKey) {
        super(peer, locationKey);
        self(this);
    }

    public Collection<Number160> contentKeys() {
        return contentKeys;
    }

    /**
     * Set the content keys that should be found. Please note that if the content keys are too large, you may need to
     * switch to TCP during routing. The default routing is UDP. Currently, the header is 59bytes, and the length of the
     * content keys is as follows: 4 bytes for the length, 20bytes per content key. The user is warned if it will exceed
     * the UDP size of 1400 (ConnectionHandler.UDP_LIMIT)
     * 
     * @param contentKeys
     * @return
     */
    public DigestBuilder contentKeys(Collection<Number160> contentKeys) {
        this.contentKeys = contentKeys;
        return this;
    }

    public Collection<Number640> keys() {
        return keys;
    }

    public DigestBuilder setKey(Collection<Number640> keys) {
        this.keys = keys;
        return this;
    }

    public Number160 getContentKey() {
        return contentKey;
    }

    public DigestBuilder setContentKey(Number160 contentKey) {
        this.contentKey = contentKey;
        return this;
    }

    public SimpleBloomFilter<Number160> getKeyBloomFilter() {
        return keyBloomFilter;
    }

    public DigestBuilder setKeyBloomFilter(SimpleBloomFilter<Number160> keyBloomFilter) {
        this.keyBloomFilter = keyBloomFilter;
        return this;
    }

    public SimpleBloomFilter<Number160> getContentBloomFilter() {
        return contentBloomFilter;
    }

    public DigestBuilder setContentBloomFilter(SimpleBloomFilter<Number160> contentBloomFilter) {
        this.contentBloomFilter = contentBloomFilter;
        return this;
    }

    public EvaluatingSchemeDHT getEvaluationScheme() {
        return evaluationScheme;
    }

    public DigestBuilder setEvaluationScheme(EvaluatingSchemeDHT evaluationScheme) {
        this.evaluationScheme = evaluationScheme;
        return this;
    }

    public boolean isAll() {
        return all;
    }

    public DigestBuilder setAll(boolean all) {
        this.all = all;
        return this;
    }

    public DigestBuilder setAll() {
        this.all = true;
        return this;
    }

    public boolean isReturnBloomFilter() {
        return returnBloomFilter;
    }

    public DigestBuilder setReturnBloomFilter(boolean returnBloomFilter) {
        this.returnBloomFilter = returnBloomFilter;
        return this;
    }

    public DigestBuilder setReturnBloomFilter() {
        this.returnBloomFilter = true;
        return this;
    }

    public boolean isAscending() {
        return ascending;
    }

    public DigestBuilder ascending(boolean ascending) {
        this.ascending = ascending;
        return this;
    }

    public DigestBuilder ascending() {
        this.ascending = true;
        return this;
    }

    public boolean isDescending() {
        return !ascending;
    }

    public DigestBuilder descending() {
        this.ascending = false;
        return this;
    }

    public DigestBuilder returnNr(int returnNr) {
        this.returnNr = returnNr;
        return this;
    }

    public int returnNr() {
        return returnNr;
    }

    public DigestBuilder from(Number640 from) {
        this.from = from;
        return this;
    }

    public Number640 from() {
        return from;
    }

    public DigestBuilder to(Number640 to) {
        this.to = to;
        return this;
    }

    public Number640 to() {
        return to;
    }

    public boolean isRange() {
        return from != null && to != null;
    }

    public FutureDigest start() {
        if (peer.isShutdown()) {
            return FUTURE_SHUTDOWN;
        }
        preBuild("digest-builder");

        if (all) {
            contentKeys = null;
        } else if (contentKeys == null && !all) {
            // default is Number160.ZERO
            if (contentKey == null) {
                contentKeys = NUMBER_ZERO_CONTENT_KEYS;
            } else {
                contentKeys = new ArrayList<Number160>(1);
                contentKeys.add(contentKey);
            }
        }
        if (evaluationScheme == null) {
            evaluationScheme = new VotingSchemeDHT();
        }
        return peer.getDistributedHashMap().digest(this);
    }
}

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
package net.tomp2p.p2p;

import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.DigestResult;
import net.tomp2p.storage.Data;

public class CumulativeScheme implements EvaluatingSchemeDHT {
    @Override
    public Collection<Number640> evaluate1(Map<PeerAddress, Map<Number640, Number160>> rawKeys480) {
        Set<Number640> result = new HashSet<Number640>();
        if (rawKeys480 != null) {
            for (Map<Number640, Number160> tmp : rawKeys480.values()) {
                result.addAll(tmp.keySet());
            }
        }
        return result;
    }
    
    @Override
    public Collection<Number640> evaluate6(Map<PeerAddress, Collection<Number640>> rawKeys480) {
        Set<Number640> result = new HashSet<Number640>();
        if (rawKeys480 != null) {
            for (Collection<Number640> tmp : rawKeys480.values()) {
                result.addAll(tmp);
            }
        }
        return result;
    }

    @Override
    public Map<Number640, Data> evaluate2(Map<PeerAddress, Map<Number640, Data>> rawKeys) {
        Map<Number640, Data> result = new HashMap<Number640, Data>();
        for (Map<Number640, Data> tmp : rawKeys.values())
            result.putAll(tmp);
        return result;
    }

    @Override
    public Object evaluate3(Map<PeerAddress, Object> rawKeys) {
        throw new UnsupportedOperationException("cannot cumulate");
    }

    @Override
    public ByteBuf evaluate4(Map<PeerAddress, ByteBuf> rawKeys) {
        throw new UnsupportedOperationException("cannot cumulate");
    }

    @Override
    public DigestResult evaluate5(Map<PeerAddress, DigestResult> rawDigest) {
        throw new UnsupportedOperationException("cannot cumulate");
    }
}

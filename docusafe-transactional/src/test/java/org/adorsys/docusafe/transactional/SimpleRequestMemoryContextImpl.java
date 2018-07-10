package org.adorsys.docusafe.transactional;

import org.adorsys.docusafe.business.impl.SimpleMemoryContextImpl;
import org.adorsys.docusafe.business.types.MemoryContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 09.07.18 at 14:06.
 */
public class SimpleRequestMemoryContextImpl implements RequestMemoryContext {
    private Map<String, MemoryContext> pseudoUserMap = new HashMap<>();
    MemoryContext current = null;

    @Override
    public void put(Object key, Object value) {
        current.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return current.get(key);
    }

    public SimpleRequestMemoryContextImpl() {
        switchToUser(1);
    }

    public void switchToUser(int i) {
        String key = "" + i;
        if (!pseudoUserMap.containsKey(key)) {
            pseudoUserMap.put(key, new SimpleMemoryContextImpl());
        }
        current = pseudoUserMap.get(key);
    }


}
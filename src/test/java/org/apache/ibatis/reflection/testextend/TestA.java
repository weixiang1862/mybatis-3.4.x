package org.apache.ibatis.reflection.testextend;

import java.util.Map;

public class TestA<K, V> {

    protected Map<K, V> map;

    public Map<K, V> getMap() {
        return map;
    }

    public void setMap(Map<K, V> map) {
        this.map = map;
    }
}

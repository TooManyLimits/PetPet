package petpet.types.immutable;

import petpet.lang.run.PetPetClass;
import petpet.types.PetPetTable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PetPetTableView<K, V> extends PetPetTable<K, V> {

    @Override
    public String toString() {
        return "<TableView(size=" + size() + ")>";
    }

    //2 views of the same thing are equal, since a view has no state of its own
    @Override
    public boolean equals(Object o) {
        return o instanceof PetPetTableView<?,?> tableView && tableView.backingMap == backingMap;
    }

    public static final PetPetClass TABLE_VIEW_CLASS = new PetPetClass("table_view");
    private final Map<K, V> backingMap;

    public PetPetTableView(Map<K, V> backingMap) {
        super(0);
        this.backingMap = backingMap;
    }

    static {
        allowMethod("__get");
        allowMethod("each");
        allowMethod("eachK");
        allowMethod("eachV");
    }

    private static void allowMethod(String name) {
        TABLE_VIEW_CLASS.methods.put(name, PetPetTable.TABLE_CLASS.methods.get(name));
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("Cannot put in table view");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Cannot put in table view");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("Cannot remove from table view");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Cannot remove from table view");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot clear table view");
    }

    @Override
    public V get(Object key) {
        return backingMap.get(key);
    }

    @Override
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return backingMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }
}

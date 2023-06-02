package petpet.types.immutable;

import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetException;
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

    //don't actually extend table, even though we do java side
    public static final PetPetClass TABLE_VIEW_CLASS = new PetPetClass("TableView");
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

        allowMethod("size");
        allowMethod("has");
        allowMethod("empty");
    }

    private static void allowMethod(String name) {
        TABLE_VIEW_CLASS.addMethod(name, (PetPetCallable) PetPetTable.TABLE_CLASS.getMethod(name));
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    //Disallowed methods for table view
    @Override
    public V put(K key, V value) {
        throw new PetPetException("nice try, can't set in table view");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Cannot put in table view");
    }

    @Override
    public V remove(Object key) {
        throw new PetPetException("nice try, can't delete in table view");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Cannot remove from table view");
    }

    @Override
    public void clear() {
        throw new PetPetException("nice try, can't clear table view");
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    @Override
    public V get(Object key) {
        return backingMap.get(key);
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
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

    @Override
    public PetPetTableView<K, V> view() {
        throw new PetPetException("nice try, can't view a table view");
    }
}

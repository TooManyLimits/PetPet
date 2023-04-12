package petpet.types.immutable;

import petpet.lang.run.PetPetClass;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.util.ArrayList;
import java.util.Iterator;

public class PetPetListView<T> extends PetPetList<T> {

    @Override
    public String toString() {
        return "<ListView(size=" + size() + ")>";
    }
    public static final PetPetClass LIST_VIEW_CLASS = new PetPetClass("list_view");
    private final ArrayList<T> backingList;

    public PetPetListView(ArrayList<T> backingList) {
        super(0);
        this.backingList = backingList;
    }

    static {
        allowMethod("__get_num");
        allowMethod("len");
        allowMethod("empty");
        allowMethod("copy");
        allowMethod("contains");

        allowMethod("each");
        allowMethod("eachI");
        allowMethod("foldL");
        allowMethod("foldR");
    }

    private static void allowMethod(String name) {
        LIST_VIEW_CLASS.methods.put(name, PetPetList.LIST_CLASS.methods.get(name));
    }

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException("Cannot set in list view");
    }

    @Override
    public T get(int index) {
        int size = size();
        return backingList.get((index % size + size) % size);
    }

    @Override
    public Iterator<T> iterator() {
        return backingList.iterator();
    }

    @Override
    public Object[] toArray() { //string format
        return super.toArray();
    }
}

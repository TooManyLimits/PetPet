package petpet.types;

import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.*;
import petpet.types.immutable.PetPetListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.ToIntFunction;

/**
 * List type
 */
@PetPetWhitelist
public class PetPetList<T> extends ArrayList<T> {

    //Equals, hashcode, toString() overwritten
    //Leaving them as-is led to stack overflow crashes
    //with recursive structures
    public PetPetList() {super();}
    public PetPetList(int size) {super(size);}

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "list[" + size() + "]";
    }

    public static final PetPetClass LIST_CLASS;

    static {
        //All our whitelisted methods
        LIST_CLASS = PetPetReflector.reflect(PetPetList.class, "list");

        //get and set by indices
        LIST_CLASS.addMethod("__get_num", new JavaFunction(PetPetList.class, "get", true));
        LIST_CLASS.addMethod("__set_num", new JavaFunction(PetPetList.class, "set", true));

        //other arraylist methods
        LIST_CLASS.addMethod("len", new JavaFunction(PetPetList.class, "size", true));
        LIST_CLASS.addMethod("empty", new JavaFunction(PetPetList.class, "isEmpty", true));
        LIST_CLASS.addMethod("clear", new JavaFunction(PetPetList.class, "clear", true));
        LIST_CLASS.addMethod("copy", new JavaFunction(PetPetList.class, "clone", true, regularCostPenalty(0.5)));
        LIST_CLASS.addMethod("has", new JavaFunction(PetPetList.class, "contains", true, regularCostPenalty(2)));

        //Add cost penalties
        ((JavaFunction) LIST_CLASS.getMethod("map")).costPenalizer = PetPetList.functionalCostPenalty(1);
        ((JavaFunction) LIST_CLASS.getMethod("each")).costPenalizer = PetPetList.functionalCostPenalty(1);
        ((JavaFunction) LIST_CLASS.getMethod("eachI")).costPenalizer = PetPetList.functionalCostPenalty(1);
        ((JavaFunction) LIST_CLASS.getMethod("filter")).costPenalizer = PetPetList.functionalCostPenalty(1);
        ((JavaFunction) LIST_CLASS.getMethod("foldR")).costPenalizer = PetPetList.functionalCostPenalty(2);
        ((JavaFunction) LIST_CLASS.getMethod("foldL")).costPenalizer = PetPetList.functionalCostPenalty(2);
    }

    //Penalty function, charging the caller a (small) price for each
    //function call they make through the functional list methods
    private static ToIntFunction<Interpreter> functionalCostPenalty(int args) {
        return i -> ((PetPetList) i.peek(args)).size() * 3;
    }

    private static ToIntFunction<Interpreter> regularCostPenalty(double dividend) {
        return i -> (int) (((PetPetList) i.peek()).size() / dividend);
    }

    private static void checkFunc(PetPetCallable func, int expectedArgs, String name) throws PetPetException {
        if (func.paramCount() != expectedArgs)
            throw new PetPetException("list." + name + "() expects " +
                    expectedArgs + "-arg function, got " + func.paramCount() + "-arg");
    }

    @PetPetWhitelist
    public T pop() {
        return remove(size()-1);
    }
    @PetPetWhitelist
    public T take() {
        return remove(0);
    }

    @PetPetWhitelist
    public PetPetList<T> swap(int index1, int index2) {
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
        return this;
    }
    @PetPetWhitelist
    public PetPetList<T> map(PetPetCallable func) {
        checkFunc(func, 1, "map");
        for (int i = 0; i < size(); i++)
            set(i, (T) func.call(get(i)));
        return this;
    }
    @PetPetWhitelist
    public PetPetList<T> filter(PetPetCallable func) {
        checkFunc(func, 1, "filter");
        Iterator<T> iter = iterator();
        while (iter.hasNext()) {
            Boolean x = (Boolean) func.call(iter.next());
            if (!x) iter.remove();
        }
        return this;
    }
    @PetPetWhitelist
    public PetPetList<T> each(PetPetCallable func) {
        checkFunc(func, 1, "each");
        for (Object o : this)
            func.call(o);
        return this;
    }
    @PetPetWhitelist
    public PetPetList<T> eachI(PetPetCallable func) {
        checkFunc(func, 2, "eachI");
        for (int i = 0; i < size(); i++)
            func.call((double) i, get(i));
        return this;
    }
    @PetPetWhitelist
    public Object foldR(PetPetCallable func, Object accum) {
        checkFunc(func, 2, "foldR");
        Object res = accum;
        for (int i = size()-1; i >= 0; i--) {
            res = func.call(get(i), res);
        }
        return res;
    }
    @PetPetWhitelist
    public Object foldL(Object accum, PetPetCallable func) {
        checkFunc(func, 2, "foldL");
        Object res = accum;
        for (T t : this) res = func.call(res, t);
        return res;
    }


    @PetPetWhitelist
    public PetPetList<T> insert(int index, T value) {
        add((index % size() + size()) % size(), value);
        return this;
    }
    @PetPetWhitelist
    public PetPetList<T> push(T value) {
        super.add(value);
        return this;
    }

    @PetPetWhitelist
    public PetPetList<T> del(int index) {
        super.remove(index);
        return this;
    }

    @PetPetWhitelist
    public PetPetListView<T> view() {
        return new PetPetListView<>(this);
    }

    /**
     * The generic may be inaccurate here! If this list is inside
     * a script somewhere, then the script might have inserted objects
     * of the incorrect generic into it, as PetPet does not consider
     * the actual type of objects inserted into the list.
     *
     * The reason the generic exists is to make this class a simple
     * drop-in replacement for ArrayList in the java-side code, when
     * you want it accessible to the PetPet environment.
     */
    @Override
    public T get(int index) {
        if (index < 0) index += size();
        if (index >= size() || index < 0) return null;
        return super.get(index);
    }

    @Override
    public T set(int index, T element) {
        int i = index < 0 ? index + size() : index;
        if (i == size())
            push(element);
        else if (i > size() || i < 0)
            throw new PetPetException("Attempt to set in list of length " + size() + " at illegal index " + index);
        else
            super.set(i, element);
        return element;
    }

    @Override
    public Object clone() {
        PetPetList<T> newList = new PetPetList<>(this.size());
        newList.addAll(this);
        return newList;
    }
}

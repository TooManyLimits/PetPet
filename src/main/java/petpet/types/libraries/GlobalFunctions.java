package petpet.types.libraries;

import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClass;
import petpet.types.PetPetList;
import petpet.types.PetPetString;
import petpet.types.PetPetTable;

import java.util.HashMap;

public class GlobalFunctions {

    public static final JavaFunction CLASS_FUNC = new JavaFunction(false, 2) {
        @Override
        public Object invoke(Object name, Object methodsTable) {
            PetPetClass clazz = new PetPetClass((String) name);
            clazz.addMethod("__set", (PetPetCallable) PetPetTable.TABLE_CLASS.getMethod("__set"));
            clazz.addMethod("__get", (PetPetCallable) PetPetTable.TABLE_CLASS.getMethod("__get"));
            clazz.methods.putAll((PetPetTable<String, PetPetCallable>) methodsTable);
            return clazz.makeEditable();
        }
    };

    public static final JavaFunction EXTEND_FUNC = new JavaFunction(false, 3) {
        @Override
        public Object invoke(Object superclass, Object name, Object methodsTable) {
            PetPetClass clazz = new PetPetClass((String) name, (PetPetClass) superclass);
            clazz.methods.putAll((PetPetTable<String, PetPetCallable>) methodsTable);
            return clazz.makeEditable();
        }
    };

    public static JavaFunction getPrintFunction(Interpreter interpreter) {
        return new JavaFunction(true, 1) {
            @Override
            public Object invoke(Object arg) {
                System.out.println(interpreter.getString(arg));
                return null;
            }
        };
    }

    public static JavaFunction getPrintStackFunction(Interpreter interpreter) {
        return new JavaFunction(true, 0) {
            @Override
            public Object invoke() {
                interpreter.printStack();
                return null;
            }
        };
    }
}

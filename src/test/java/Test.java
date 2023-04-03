import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.JavaFunction;
import petpet.types.PetPetList;

public class Test {

    public static void main(String[] args) throws Exception {


        String script =
                """
                a = vec3(1,2,3)
                b = vec3(4,5,6)
                print(a)
                print(a + b)
                print(a * b)
                print(6 * a)
                print(a * 7)
                print(a * 7 + " cutie")
                """;

        PetPetInstance instance = new PetPetInstance();
        instance.debugBytecode = true;

        instance.registerClass(Vec3.class, PetPetReflector.reflect(Vec3.class, "vec3"));
        instance.setGlobal("vec3", new JavaFunction(Vec3.class, "create", false));

        instance.runScript("script", script);

    }

    @PetPetWhitelist
    public static class Vec3 {
        @PetPetWhitelist
        public float x, y, z;

        public Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        //Needed to reflect and create constructor on script side
        public static Vec3 create(float x, float y, float z) {
            return new Vec3(x, y, z);
        }

        @PetPetWhitelist
        public Vec3 __add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        @PetPetWhitelist
        public Vec3 __mul_num(float s) {
            return new Vec3(x * s, y * s, z * s);
        }

        @PetPetWhitelist
        public Vec3 __mulR_num(float s) {
            return new Vec3(x * s, y * s, z * s);
        }

        @PetPetWhitelist
        public Vec3 __mul_vec3(Vec3 other) {
            return new Vec3(x * other.x, y * other.y, z * other.z);
        }

        public String toString() {
            return "Vec3(" + x + ", " + y + ", " + z + ")";
        }
    }

}

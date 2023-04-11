import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClosure;

public class Test {

    public static void main(String[] args) throws Exception {


        String script =
                """
                //A solver for https://www.nytimes.com/games/digits
                nums = ![5,6,9,11,20,25]
                target = 259

                contains = fn(list, value)
                    list.copy()
                    .map(fn(x) x == value)
                    .foldR(fn(a, b) a or b, false)

                result = null

                try = fn(current_set, steps) {
                    if contains(current_set, target) steps
                    else {
                        i = 0
                        while result == null and i < current_set.len() {
                            j = 0
                            while result == null and j < current_set.len() {
                                if i != j {
                                    a = current_set[i]
                                    b = current_set[j]
                                    new_list = current_set.copy().del(i).del(if i < j j-1 else j)
                                    if x = this(new_list.copy().push(a + b), steps.copy().push("add " + a + " " + b)) result = x
                                    else if a - b >= 0 && (x = this(new_list.copy().push(a - b), steps.copy().push("sub " + a + " " + b))) result = x
                                    else if x = this(new_list.copy().push(a * b), steps.copy().push("mul " + a + " " + b)) result = x
                                    else if a % b == 0 and (x = this(new_list.copy().push(a / b), steps.copy().push("div " + a + " " + b))) result = x
                                }
                                j = j + 1
                            }
                            i = i + 1
                        }
                        false
                    }
                }

                try(nums, ![])

                result.each(print)
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

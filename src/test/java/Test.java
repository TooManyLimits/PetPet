import petpet.external.PetPetInstance;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetCallable;

public class Test {

    public static void main(String[] args) throws Exception {
        String script = """
                bug()
                vec3 = class("vec3", $[
                	__init = fn() {
                		this.x = this.y = this.z = 0
                		this
                	},
                	__tostring = fn() {
                		"{" + this.x + ", " + this.y + ", " + this.z + "}"
                	}
                ])
                print(vec3())
                                
                vec4 = extend(vec3, "vec4", $[
                	super_init = vec3.methods().__init,
                	__init = fn() {
                		this.super_init()
                		this.w = 0
                		this
                	},
                	super_tostring = vec3.methods().__tostring,
                	__tostring = fn() {
                		this.super_tostring().sub(0, -1) + ", " + this.w + "}"
                	}
                ])
                print(vec4())
                
                global x = 3
                _G.eachK(print)
                bug()
                
                """;
        PetPetInstance instance = new PetPetInstance();
        //instance.debugBytecode = true;

        for (int i = 0; i < 10; i++)
            try {
                instance.runScript("script", script);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

    }

    private static void testHelloWorld() throws Exception {
        //Set up the script and instance
        String script = "fn() print(\"Hello, world!\")";
        PetPetInstance instance = new PetPetInstance();

        //"Call" the file, which returns this result
        Object fileResult = instance.runScript("script", script);

        //The result is a PetPetCallable, which when called, prints "Hello, World!"
        ((PetPetCallable) fileResult).call(); //Prints
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
        public Vec3 __sub(Vec3 other) {
            return new Vec3(x - other.x, y - other.y, z - other.z);
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

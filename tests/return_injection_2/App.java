public class App {
    public static void main(String[] args) {
        // If hook fails, will exit with status code 2
        System.exit(main(1));
    }

    public static int main(int x) {
        if (x == 0) {
            // This doesn't get called in this test
            // Injected System.exit(0)
            return 1;
        }

        // Injected System.exit(0)
        return 2;
    }
}

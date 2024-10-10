public class App {
    private static int returnCode = 1;

    public static void main(String[] args) {
        // To make sure we don't hook on GET
        if (returnCode != 1) {
            System.exit(1);
        }

        returnCode = 1;
        System.exit(returnCode);
    }
}

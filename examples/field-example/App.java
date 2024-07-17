import java.util.concurrent.TimeUnit;

public class App {
    private static int num = 0;

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            num = num + 1;

            System.out.println(num);
            TimeUnit.SECONDS.sleep(1);
        }
    }
}

import java.util.concurrent.TimeUnit

fun main() {
    var tickNumber = 0
    while (true) {
        App.runTick(tickNumber++)
        TimeUnit.SECONDS.sleep(1)
    }
}

object App {
    fun runTick(tickNumber: Int) {
        println("Doing stuff!")
    }
}

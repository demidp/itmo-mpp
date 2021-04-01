package dijkstra

import kotlinx.atomicfu.atomic
import java.util.concurrent.Phaser
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.random.Random


private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Temp, just for pushing
// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.push(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                val cur: Node = q.pop() ?: if (q.inProcess.value == 0) break else continue

                for (e in cur.outgoingEdges) {
                    var distToE = e.to.distance
                    while (distToE > cur.distance + e.weight) {
                        if (e.to.casDistance(distToE, cur.distance + e.weight)) {
                            q.push(e.to)
                            break
                        } else {
                            distToE = e.to.distance
                        }
                    }
                }
                q.decrease()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiQueue(
        private val workers: Int,
        cmp: Comparator<Node>
) {
    private val queues: ArrayList<PriorityBlockingQueue<Node>> = ArrayList()
    private val random = Random(1)
    private val statuses = ArrayList<ReentrantLock>()
    val inProcess = atomic(0)

    init {
        for (i in 0..2 * workers) {
            queues.add(PriorityBlockingQueue(1, cmp))
            statuses.add(ReentrantLock())
        }
    }

    fun pop(): Node? {
        val fstIndex = random.nextInt(0, workers * 2)
        val sndIndex = random.nextInt(0, workers * 2)
        if (!statuses[fstIndex].tryLock()) {
            return null
        } else {
            if (!statuses[sndIndex].tryLock()) {
                statuses[fstIndex].unlock()
                return null
            }
        }
        val fst = queues[fstIndex]
        val snd = queues[sndIndex]


        var forReturn: Node? = null
        forReturn = if (fst.isEmpty() && snd.isEmpty())
            null
        else if (fst.isEmpty())
            snd.poll()
        else if (snd.isEmpty())
            fst.poll()
        else if (fst.peek().distance < snd.peek().distance)
            fst.poll()
        else
            snd.poll()

        statuses[fstIndex].unlock()
        statuses[sndIndex].unlock()

        return forReturn
    }

    fun push(v: Node) {
        var qIndex = random.nextInt(0, workers * 2)
        while (!statuses[qIndex].tryLock()) {
            qIndex = random.nextInt(0, workers * 2)
        }
        queues[qIndex].add(v)
        statuses[qIndex].unlock()
        var inProcess = inProcess.value
        while (!this.inProcess.compareAndSet(inProcess, inProcess + 1)) {
            inProcess = this.inProcess.value
        }
    }

    fun decrease() {
        var inProcess = inProcess.value
        while (!this.inProcess.compareAndSet(inProcess, inProcess - 1)) {
            inProcess = this.inProcess.value
        }
    }
}

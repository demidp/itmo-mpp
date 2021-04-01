import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*
import kotlin.random.Random

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val size = Runtime.getRuntime().availableProcessors() * 4
    private val ops = atomicArrayOfNulls<Int>(size)
    private val res = atomicArrayOfNulls<E?>(size)
    private val lock = atomic(false)

    init {
        for (i in 0 until size) {
            ops[i].getAndSet(0)
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    // Operation id : 1
    fun poll(): E? {
        val id = block(1)
        while (true) {
            if (lock.compareAndSet(false, true)) {
                var result = tryToGet(id)
                if (result != NOT_READY) {
                    lock.getAndSet(false)
                } else {
                    result = q.poll()
                    doCombineJob(id)
                }
                return result as E?
            }
            val ans = tryToGet(id)
            if (ans != NOT_READY) {
                return ans as E?
            }
            checkYield()
        }
    }


    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    // Operation id : 2
    fun peek(): E? {
        val id = block(2)
        while (true) {
            if (lock.compareAndSet(false, true)) {
                var result = tryToGet(id)
                if (result != NOT_READY) {
                    lock.getAndSet(false)
                } else {
                    result = q.peek()
                    doCombineJob(id)
                }
                return result as E?
            }
            val ans = tryToGet(id)
            if (ans != NOT_READY) {
                return ans as E?
            }
            checkYield()
        }
    }

    /**
     * Adds the specified element to the queue.
     */
    // Operation id : 3
    fun add(element: E) {
        val id = block(3)
        res[id].getAndSet(element)
        while (true) {
            if (lock.compareAndSet(false, true)) {
                if (tryToGet(id) != NOT_READY) {
                    lock.getAndSet(false)
                } else {
                    q.add(element)
                    doCombineJob(id)
                }
                return
            }
            if (tryToGet(id) != NOT_READY) {
                return
            }
            checkYield()
        }
    }

    private fun block(opType: Int): Int {
        val id = Random.nextInt(0, size)
        for (i in id until size) {
            if (ops[i].compareAndSet(0, opType)) {
                return i
            }
        }
        for (i in 0 until id) {
            if (ops[i].compareAndSet(0, opType)) {
                return i
            }
        }
        return -1
    }

    private fun doCombineJob(id: Int) {
        res[id].getAndSet(null)
        ops[id].getAndSet(0)
        for (i in 0 until size) {
            when (ops[i].value) {
                0 -> continue
                1 -> res[i].getAndSet(q.poll())
                2 -> res[i].getAndSet(q.peek())
                3 -> {
                    val element = res[i].value
                    if (element == null) {
                        continue
                    } else {
                        q.add(element)
                    }
                }
                4 -> continue
            }
            ops[i].getAndSet(4)
        }
        lock.getAndSet(false)
    }

    private fun tryToGet(id: Int): Any? {
        if (ops[id].value == 4) {
            val ans = res[id].getAndSet(null)
            ops[id].getAndSet(0)
            return ans
        }
        return NOT_READY
    }

    private fun checkYield() {
        if (Random.nextInt(0, 5) == 0) {
            Thread.yield()
        }
    }
}

val NOT_READY = Any()
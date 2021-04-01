import kotlinx.atomicfu.*
import java.lang.IllegalArgumentException

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY))
    override fun get(index: Int): E {
        tryMoveCore()
        if (index < 0)
            throw IllegalArgumentException()
        return core.value.get(index)
    }

    override fun put(index: Int, element: E) {
        tryMoveCore()
        core.value.put(index, element)
    }

    override fun pushBack(element: E) {
        tryMoveCore()
        core.value.pushBack(element)
    }

    private fun tryMoveCore() {
        val curCore = core.value
        if (curCore.canMoveCore()) {
            val nextCore = curCore.nextCore.value
            if (nextCore != null)
                core.compareAndSet(curCore, nextCore)
        }
    }

    override val size: Int
        get() {
            return core.value.size()
        }


}

private class Core<E>(
    val capacity: Int,
) {
    private val array: AtomicArray<E?> = atomicArrayOfNulls<E>(capacity)
    private val marks: AtomicBooleanArray = AtomicBooleanArray(capacity)
    private val isMoving: AtomicBooleanArray = AtomicBooleanArray(capacity)
    val nextCore: AtomicRef<Core<E>?> = atomic(null)
    private val size: AtomicInt = atomic(0)
    private val needToMove: AtomicInt = atomic(capacity)

    constructor(previousSize: Int, newCapacity: Int) : this(newCapacity) {
        size.getAndSet(previousSize)
    }

    fun get(index: Int): E {
        return if (index < size.value && !marks[index].value) {
            array[index].value ?: throw IllegalArgumentException()
        } else {
            nextCore.value?.get(index) ?: throw IllegalArgumentException()
        }
    }

    fun pushBack(element: E) {
        var curSize = size.value
        while (curSize < capacity) {
            if (array[curSize].compareAndSet(null, element)) {
                size.incrementAndGet()
                return
            } else {
                curSize = size.value
            }
        }
        // Need to create a new core
        if (!nextCore.compareAndSet(null, Core<E>(capacity, capacity * 2))) {
            return nextCore.value?.pushBack(element)!!
        } else {
            nextCore.value?.pushBack(element)!!
            for (i in 0 until capacity) {
                if (array[i].value != null && isMoving[i].compareAndSet(false, true)) {
                    nextCore.value?.casChange(i, null, array[i].value!!)
                    if (marks[i].compareAndSet(false, true)) {
                        needToMove.decrementAndGet()
                    }
                }
            }
        }

    }

    fun put(index: Int, element: E) {
        if (index < size.value && !marks[index].value) {
            var cur = array[index].value
            if (isMoving[index].value) {
                cur = array[index].value
                // Move our element
                nextCore.value?.casChange(index, null, cur!!)
                if (marks[index].compareAndSet(false, true)) {
                    needToMove.decrementAndGet()
                }
                nextCore.value?.put(index, element)
            } else {
                while (!isMoving[index].value && !array[index].compareAndSet(cur, element)) {
                    cur = array[index].value
                }
                if (isMoving[index].value) {
                    cur = array[index].value
                    // Move our element
                    if (nextCore.value?.casChange(index, null, cur!!)!!) {
                        if (marks[index].compareAndSet(false, true)) {
                            needToMove.decrementAndGet()
                        }
                    }
                    nextCore.value?.put(index, element)
                }
                nextCore.value?.put(index, element)
            }
        } else {
            nextCore.value?.put(index, element) ?: throw IllegalArgumentException()
        }
    }

    fun casChange(index: Int, old: E?, new: E): Boolean {
        return array[index].compareAndSet(old, new)
    }

    fun size(): Int {
        return nextCore.value?.size() ?: size.value
    }

    fun canMoveCore(): Boolean {
        return needToMove.value == 0
    }

}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME
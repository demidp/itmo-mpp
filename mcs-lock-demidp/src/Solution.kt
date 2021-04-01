import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    val tail: AtomicReference<Node?> = AtomicReference(null)
    override fun lock(): Node {
        val cur = Node() // сделали узел
        val prevTail = tail.getAndSet(cur) ?: return cur
        prevTail.next.getAndSet(cur)
        while (cur.locked.get())
            env.park()
        return cur
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.value == null) {
                    // pass
                }
                (node.next.value as Node).locked.set(false)
                env.unpark((node.next.value as Node).thread)
                return
            }
        } else {
            node.next.value!!.locked.getAndSet(false)
            env.unpark((node.next.value as Node).thread)
        }
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked = AtomicReference<Boolean>(true)
        val next = AtomicReference<Node?>(null)
    }
}
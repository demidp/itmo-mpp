import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    class Node<E>(
        val receiver: Continuation<Any>?,
        val sender: Pair<Continuation<Any>, E>?
    ) {
        val next: AtomicRef<Node<E>?> = atomic(null)
    }

    private val dummy = Node<E>(null, null)
    private val head: AtomicRef<Node<E>> = atomic(dummy)
    private val tail: AtomicRef<Node<E>> = atomic(dummy)

    override suspend

    fun send(element: E) {
        while (true) {
            val curTail = tail.value
            val curHead = head.value
            val next = curHead.next.value
            if (curTail.next.value != null) {
                tail.compareAndSet(curTail, curTail.next.value!!)
                continue
            }
            if (next?.receiver != null) {
                // try to get a reciver from queue
                if (head.compareAndSet(curHead, next)) {
                    next.receiver.resume(element!!)
                    return
                } else {
                    continue
                }
            } else {
                // Try to add send to queue
                val res = suspendCoroutine<Any> { cont ->
                    val newTail = Node(null, cont to element)
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        cont.resume(RETRY)
                    }
                }
                if (res == RETRY) {
                    continue
                } else {
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val curTail = tail.value
            val curHead = head.value
            val next = curHead.next.value
            if (curTail.next.value != null) {
                tail.compareAndSet(curTail, curTail.next.value!!)
                continue
            }
            if (next?.sender != null) {
                // try to get a sender from queue
                if (head.compareAndSet(curHead, next)) {
                    next.sender.first.resume(Unit)
                    return next.sender.second
                } else {
                    continue
                }
            } else {
                // Try to add send to queue
                val res = suspendCoroutine<Any> { cont ->
                    val newTail = Node<E>(cont, null)
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        cont.resume(RETRY)
                    }
                }
                if (res == RETRY) {
                    continue
                } else {
                    return res as E
                }
            }
        }
    }

}

private val RETRY = Any()

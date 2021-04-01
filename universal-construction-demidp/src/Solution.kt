/**
 * @author :Panochevnykh Demid
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val head: Node = Node(0, Consensus())
    private val last: ThreadLocal<Node> = ThreadLocal()
    override fun getAndAdd(x: Int): Int {
        // напишите здесь код
        if (last.get() == null) {
            last.set(head)
        }
        var myDecide = Node(last.get().value + x, Consensus())
        var decide = last.get().nextNode.decide(myDecide)
        while (myDecide != decide) {
            myDecide = Node(decide.value + x, Consensus())
            decide = decide.nextNode.decide(myDecide)
        }
        last.set(decide)
        return decide.value - x
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val value: Int, val nextNode: Consensus<Node>) {
    }
}

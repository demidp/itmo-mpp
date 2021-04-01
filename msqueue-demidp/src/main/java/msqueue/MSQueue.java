package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node tailNode = tail.getValue();
            if (tailNode.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(tailNode, newTail);
                return;
            } else {
                tail.compareAndSet(tailNode, tailNode.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            if (curTail == curHead) {
                if (curTail.next.getValue() != null) {
                    tail.compareAndSet(curTail, curTail.next.getValue());
                } else {
                    return Integer.MIN_VALUE;
                }
            } else {
                Node next = curHead.next.getValue();
                if (head.compareAndSet(curHead, next)) {
                    return next.x;
                }
            }
        }
    }

    @Override
    public int peek() {
        Node curHead = head.getValue();
        if (curHead == tail.getValue())
            return Integer.MIN_VALUE;
        Node next = curHead.next.getValue();
        return next.x;
    }

    private class Node {
        final int x;
        AtomicRef<Node> next = new AtomicRef<>(null);

        Node(int x) {
            this.x = x;
        }
    }
}
package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.*;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private static class WrappedInt {
        boolean isDone;
        int value;

        public WrappedInt(int value) {
            this.value = value;
            isDone = false;
        }

    }

    public StackImpl() {

        for (int i = 0; i < capacity; i++) {
            eliminationArray.add(new AtomicRef<WrappedInt>(null));
        }
    }

    // head pointer
    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final List<AtomicRef<WrappedInt>> eliminationArray = new ArrayList<>();
    private final int capacity = 12;
    private final Random random = new Random();

    @Override
    public void push(int x) {
        int pos = random.nextInt(capacity);
        WrappedInt myValue = new WrappedInt(x);
        for (int i = pos; i < Math.min(pos + 4, capacity); i++) {
            if (eliminationArray.get(i).compareAndSet(null, myValue)) {
                // No other push can change elimination.get(i)
                for (int j = 0; j < 50; j++) {
                    if (eliminationArray.get(i).getValue().isDone) {
                        // means that pop gets X
                        eliminationArray.get(i).setValue(null);
                        return;
                    }
                }
                // Remove our element from array
                if (!eliminationArray.get(i).compareAndSet(myValue, null)) {
                    // Pop changed element
                    eliminationArray.get(i).setValue(null);
                    return;
                }
                break;
            }
        }
        while (true) {
            Node oldHead = head.getValue();
            Node newNode = new Node(x, oldHead);
            if (head.compareAndSet(oldHead, newNode)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        WrappedInt poppedValue = new WrappedInt(0);
        poppedValue.isDone = true;
        for (int pos = 0; pos < capacity; pos++) {
            WrappedInt value = eliminationArray.get(pos).getValue();
            if (value != null && (!value.isDone)) {
                poppedValue.value = value.value;
                if (eliminationArray.get(pos).compareAndSet(value, poppedValue)) {
                    return value.value;
                }
            }
        }

        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}

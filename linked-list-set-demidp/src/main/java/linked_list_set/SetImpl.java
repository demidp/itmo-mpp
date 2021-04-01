package linked_list_set;

import java.util.concurrent.atomic.AtomicReference;

public class SetImpl implements Set {
    private class Node {
        AtomicReference<Object> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicReference<>((Object) next);
            this.x = x;
        }
    }

    private class Removed {
        private final Node node;

        private Removed(Node node) {
            this.node = node;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, (Node) null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        Window w = new Window();
        while (true) {
            retry:
            {
                w.cur = head;
                w.next = (Node) w.cur.next.get();
                while (w.next.x < x) {
                    Object nextNext = w.next.next.get();
                    if (nextNext instanceof Removed) {
                        // w.next Removed
                        // w.next -> w.next.next
                        if (!w.cur.next.compareAndSet(w.next, ((Removed) nextNext).node)) {
                            break retry;
                        }
                        w.next = ((Removed) nextNext).node;
                    } else {
                        w.cur = w.next;
                        w.next = (Node) nextNext;
                    }
                }
                Object nextNext = w.next.next.get();
                if (nextNext instanceof Removed) {
                    w.cur.next.compareAndSet(w.next, ((Removed) nextNext).node);
                    continue;
                }

                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            } else {
                Node node = new Node(x, w.next);
                if (w.cur.next.compareAndSet(w.next, node)) {
                    return true;
                }
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            } else {
                Object nextNext = w.next.next.get();
                if (nextNext instanceof Removed)
                    continue;
                if (w.next.next.compareAndSet(nextNext, new Removed((Node) nextNext))) {
                    w.cur.next.compareAndSet(w.next, nextNext);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;

    }
}
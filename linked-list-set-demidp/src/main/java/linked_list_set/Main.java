package linked_list_set;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        final SetImpl set = new SetImpl();
        new Thread() {
            @Override
            public void run() {
                System.out.println("2 Add" + set.add(2));
                System.out.println("2 Remove" + set.remove(2));
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                System.out.println("3 Add" + set.add(3));
            }
        }.start();
        new Thread().sleep(200);
        System.out.println(set.remove(3));

    }
}

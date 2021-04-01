/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Panochevnykh Demid
 */
class Solution : MonotonicClock {

    private var newHours by RegularInt(0)
    private var newMinutes by RegularInt(0)
    private var newSeconds by RegularInt(0)

    private var savedHours by RegularInt(0)
    private var savedMinutes by RegularInt(0)
    private var savedSeconds by RegularInt(0)
    override fun write(time: Time) {
        newHours = time.d1
        newMinutes = time.d2
        newSeconds = time.d3

        savedSeconds = newSeconds
        savedMinutes = newMinutes
        savedHours = newHours
    }

    override fun read(): Time {
        val rl1 = savedHours
        val rl2 = savedMinutes
        val rl3 = savedSeconds

        val rc3 = newSeconds
        val rc2 = newMinutes
        val rc1 = newHours

        return if (rl1 == rc1 && rl2 == rc2 && rc3 == rl3) {
            Time(rl1, rl2, rl3)
        } else {
            if (rl1 != rc1) {
                // NewHours of time has already changed
                // But other values didn't so we can use time between t1 and t2
                Time(rc1, 0, 0)
            } else {
                if (rc2 != rl2) {
                    Time(rc1, rc2, 0)
                } else {
                    Time(rc1, rc2, rc3)
                }
            }
        }
    }
}
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val bank = BankImpl(10)
        val deposit1: Long = 4567
        val depositResult1 = bank.deposit(1, deposit1)
        var deposit2: Long = 6789
        val depositResult2 = bank.deposit(2, deposit2)
        var bankTotal = bank.totalAmount
        var a = 1
    }
}
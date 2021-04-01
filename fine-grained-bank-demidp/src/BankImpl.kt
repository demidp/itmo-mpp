import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author Panochevnykh Demid
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun getAmount(index: Int): Long {
        accounts[index].lock.lock()
        val amount = accounts[index].amount
        accounts[index].lock.unlock()
        return amount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            var bankBalance = 0L
            accounts.forEach { account ->
                run {
                    account.lock.lock()
                    bankBalance += account.amount
                }
            }
            accounts.forEach { account ->
                run {
                    account.lock.unlock()
                }
            }
            return bankBalance
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        val resAmount: Long
        try {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            resAmount = account.amount
        } catch (e: Exception) {
            throw e
        } finally {
            account.lock.unlock()
        }
        return resAmount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        val resAmount: Long
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            resAmount = account.amount
        } catch (e: Exception) {
            throw e
        } finally {
            account.lock.unlock()
        }
        return resAmount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }

        if (fromIndex < toIndex) {
            accounts[fromIndex].lock.lock()
            accounts[toIndex].lock.lock()
        } else {
            accounts[toIndex].lock.lock()
            accounts[fromIndex].lock.lock()
        }
        try {
            val from = accounts[fromIndex]
            val to = accounts[toIndex]
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            from.amount -= amount
            to.amount += amount
        } catch (e: Exception) {
            throw e
        } finally {
            if (fromIndex < toIndex) {
                accounts[fromIndex].lock.unlock()
                accounts[toIndex].lock.unlock()
            } else {
                accounts[toIndex].lock.unlock()
                accounts[fromIndex].lock.unlock()
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
        var lock = ReentrantLock()
    }
}
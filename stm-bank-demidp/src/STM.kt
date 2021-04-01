import kotlinx.atomicfu.*

/**
 * Atomic block.
 */
fun <T> atomic(block: TxScope.() -> T): T {
    while (true) {
        val transaction = Transaction()
        try {
            val result = block(transaction)
            if (transaction.commit()) return result
            transaction.abort()
        } catch (e: AbortException) {
            transaction.abort()
        }
    }
}

/**
 * Transactional operations are performed in this scope.
 */
abstract class TxScope {
    abstract fun <T> TxVar<T>.read(): T
    abstract fun <T> TxVar<T>.write(x: T): T
}

/**
 * Transactional variable.
 */
class TxVar<T>(initial: T) {
    private val loc = atomic(Loc(initial, initial, rootTx))

    /**
     * Opens this transactional variable in the specified transaction [tx] and applies
     * updating function [update] to it. Returns the updated value.
     */
    fun openIn(tx: Transaction, update: (T) -> T): T {
        loop@ while (true) {
           val curLoc = loc.value
           val curValue: T
           val owner = curLoc.owner
           curValue = if (owner !== tx) {
               when (owner.status) {
                   TxStatus.COMMITTED -> curLoc.newValue
                   TxStatus.ACTIVE -> {
                       // contention
                       owner.abort()
                       if (owner.status != TxStatus.ABORTED) continue@loop
                       curLoc.oldValue
                   }
                   TxStatus.ABORTED -> curLoc.oldValue
               }
           } else {
               curLoc.oldValue
           }
           val updValue = update(curValue)
           val updLoc = Loc(curValue, updValue, tx)

           if (loc.compareAndSet(curLoc, updLoc)) {
               if (tx.status == TxStatus.ABORTED) {
                   loc.compareAndSet(updLoc, curLoc)
                   throw AbortException
               }
               return updLoc.newValue
           }
       }
    }
}

/**
 * State of transactional value
 */
private class Loc<T>(
    val oldValue: T,
    val newValue: T,
    val owner: Transaction
)

private val rootTx = Transaction().apply { commit() }

/**
 * Transaction status.
 */
enum class TxStatus { ACTIVE, COMMITTED, ABORTED }

/**
 * Transaction implementation.
 */
class Transaction : TxScope() {
    private val _status = atomic(TxStatus.ACTIVE)
    val status: TxStatus get() = _status.value

    fun commit(): Boolean =
        _status.compareAndSet(TxStatus.ACTIVE, TxStatus.COMMITTED)

    fun abort() {
        _status.compareAndSet(TxStatus.ACTIVE, TxStatus.ABORTED)
    }

    override fun <T> TxVar<T>.read(): T = openIn(this@Transaction) { it }
    override fun <T> TxVar<T>.write(x: T) = openIn(this@Transaction) { x }
}

/**
 * This exception is thrown when transaction is aborted.
 */
private object AbortException : Exception() {
    override fun fillInStackTrace(): Throwable = this
}
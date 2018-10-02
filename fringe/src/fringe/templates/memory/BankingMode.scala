package fringe.templates.memory

sealed trait BankingMode
case object DiagonalMemory extends BankingMode
case object BankedMemory extends BankingMode
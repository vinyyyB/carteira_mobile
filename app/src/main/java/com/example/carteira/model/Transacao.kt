package com.example.carteira.model

import java.text.NumberFormat
import java.util.Locale

data class Transacao(
    val id: Int = 0,
    val descricao: String,
    val valor: Double,
    val tipo: String,   // RECEITA ou DESPESA
    val data: String    // gravada como aaaa-mm-dd para permitir ordenar por data no banco
) {
    companion object {
        const val RECEITA = "RECEITA"
        const val DESPESA = "DESPESA"
    }

    override fun toString(): String {
        // Retorna o texto exibido em cada linha da lista
        val sinal = if (tipo == RECEITA) "+" else "-"
        val partes = data.split("-")
        val dataTela = if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else data
        val valorFormatado = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor)
        return "$sinal $valorFormatado   $descricao\n$dataTela"
    }
}

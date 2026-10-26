package com.example.carteira.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.carteira.data.db.DBHelper
import com.example.carteira.model.Transacao

class TransacaoDAO(private val context: Context) {

    private val dbHelper = DBHelper(context)

    companion object {
        // Ordenação: lançamentos mais recentes primeiro
        const val ORDER_BY = "data DESC, id DESC"
    }

    fun addTransacao(transacao: Transacao): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("descricao", transacao.descricao)
            put("valor", transacao.valor)
            put("tipo", transacao.tipo)
            put("data", transacao.data)
        }
        val id = db.insert(DBHelper.TABLE_NAME, null, values)
        db.close()
        return id
    }

    // Filtro 1: todos os lançamentos
    fun getAllTransacoes(): List<Transacao> {
        return getTransacoes(null, null)
    }

    // Filtro 2: busca por descrição
    fun getTransacoesByDescricao(texto: String): List<Transacao> {
        return getTransacoes("descricao LIKE ?", arrayOf("%$texto%"))
    }

    // Filtro 3: somente receitas ou somente despesas
    fun getTransacoesByTipo(tipo: String): List<Transacao> {
        return getTransacoes("tipo = ?", arrayOf(tipo))
    }

    fun getTransacaoById(id: Int): Transacao? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(DBHelper.TABLE_NAME, null,
            "id=?", arrayOf(id.toString()), null, null, null)

        var transacao: Transacao? = null
        if (cursor.moveToFirst()) {
            transacao = cursorToTransacao(cursor)
        }
        cursor.close()
        db.close()
        return transacao
    }

    fun updateTransacao(transacao: Transacao): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("descricao", transacao.descricao)
            put("valor", transacao.valor)
            put("tipo", transacao.tipo)
            put("data", transacao.data)
        }
        val rowsAffected = db.update(DBHelper.TABLE_NAME, values,
            "id=?", arrayOf(transacao.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteTransacao(id: Int): Int {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(DBHelper.TABLE_NAME,
            "id=?", arrayOf(id.toString()))
        db.close()
        return rowsDeleted
    }

    // Consulta genérica usada pelos filtros, sempre com a mesma ordenação
    private fun getTransacoes(selection: String?, selectionArgs: Array<String>?): List<Transacao> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(DBHelper.TABLE_NAME, null,
            selection, selectionArgs, null, null, ORDER_BY)
        val transacaoList = mutableListOf<Transacao>()
        while (cursor.moveToNext()) {
            transacaoList.add(cursorToTransacao(cursor))
        }
        cursor.close()
        db.close()
        return transacaoList
    }

    private fun cursorToTransacao(cursor: Cursor): Transacao {
        return Transacao(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao")),
            valor = cursor.getDouble(cursor.getColumnIndexOrThrow("valor")),
            tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
            data = cursor.getString(cursor.getColumnIndexOrThrow("data"))
        )
    }
}

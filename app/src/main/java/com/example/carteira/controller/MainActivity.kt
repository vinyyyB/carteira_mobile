package com.example.carteira.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carteira.R
import com.example.carteira.data.dao.TransacaoDAO
import com.example.carteira.model.Transacao

class MainActivity : AppCompatActivity() {

    private lateinit var transacaoDAO: TransacaoDAO
    private lateinit var listView: ListView
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listView = findViewById(R.id.lvTransacoes)
        emptyTextView = findViewById(R.id.tvEmpty)
        transacaoDAO = TransacaoDAO(this)

        // Listener para clicar nos itens da lista e abrir a tela de edição
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedTransacao = parent.getItemAtPosition(position) as Transacao
            val intent = Intent(this, NewTransacao::class.java).apply {
                putExtra("transacaoID", selectedTransacao.id)
            }
            startActivity(intent)
        }
    }

    // Carrega os lançamentos no ListView
    private fun listAllTransacoes() {
        val transacoes = transacaoDAO.getAllTransacoes()
        if (transacoes.isEmpty()) {
            listView.visibility = ListView.GONE
            emptyTextView.visibility = TextView.VISIBLE
        } else {
            listView.visibility = ListView.VISIBLE
            emptyTextView.visibility = TextView.GONE
            val adapter: ArrayAdapter<Transacao> = ArrayAdapter(
                this, android.R.layout.simple_list_item_1, transacoes
            )
            listView.adapter = adapter
        }
    }

    fun newTransacao(view: View) {
        val intent = Intent(this, NewTransacao::class.java)
        startActivity(intent)
    }

    // Atualiza a lista quando voltar de outra activity (adição, edição, exclusão)
    override fun onResume() {
        super.onResume()
        listAllTransacoes()
    }
}

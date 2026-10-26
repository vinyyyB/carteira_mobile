package com.example.carteira.controller

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carteira.R
import com.example.carteira.data.dao.TransacaoDAO
import com.example.carteira.model.Transacao
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var transacaoDAO: TransacaoDAO
    private lateinit var listView: ListView
    private lateinit var emptyTextView: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var etBusca: EditText

    // Filtro selecionado: TODOS, BUSCA, RECEITA ou DESPESA
    private var filtroAtual = "TODOS"

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
        tvSaldo = findViewById(R.id.tvSaldo)
        etBusca = findViewById(R.id.etBusca)
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

    // Carrega os lançamentos no ListView conforme o filtro selecionado
    private fun listAllTransacoes() {
        val transacoes = when (filtroAtual) {
            "BUSCA" -> transacaoDAO.getTransacoesByDescricao(etBusca.text.toString().trim())
            Transacao.RECEITA -> transacaoDAO.getTransacoesByTipo(Transacao.RECEITA)
            Transacao.DESPESA -> transacaoDAO.getTransacoesByTipo(Transacao.DESPESA)
            else -> transacaoDAO.getAllTransacoes()
        }

        if (transacoes.isEmpty()) {
            listView.visibility = ListView.GONE
            emptyTextView.visibility = TextView.VISIBLE
            if (filtroAtual == "TODOS") {
                emptyTextView.text = "Nenhum lançamento cadastrado"
            } else {
                emptyTextView.text = "Nenhum lançamento encontrado"
            }
        } else {
            listView.visibility = ListView.VISIBLE
            emptyTextView.visibility = TextView.GONE
            val adapter: ArrayAdapter<Transacao> = ArrayAdapter(
                this, android.R.layout.simple_list_item_1, transacoes
            )
            listView.adapter = adapter
        }

        atualizarSaldo()
    }

    // REGRA DE NEGÓCIO: saldo = soma das receitas - soma das despesas
    private fun atualizarSaldo() {
        var saldo = 0.0
        for (t in transacaoDAO.getAllTransacoes()) {
            if (t.tipo == Transacao.RECEITA) saldo += t.valor else saldo -= t.valor
        }
        tvSaldo.text = "Saldo: " + NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(saldo)
        if (saldo >= 0) {
            tvSaldo.setTextColor(Color.parseColor("#2E7D32"))   // verde
        } else {
            tvSaldo.setTextColor(Color.parseColor("#C62828"))   // vermelho
        }
    }

    fun newTransacao(view: View) {
        val intent = Intent(this, NewTransacao::class.java)
        startActivity(intent)
    }

    fun buscar(view: View) {
        if (etBusca.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Digite algo para buscar", Toast.LENGTH_SHORT).show()
            etBusca.requestFocus()
            return
        }
        filtroAtual = "BUSCA"
        listAllTransacoes()
    }

    fun filtrarTodos(view: View) {
        filtroAtual = "TODOS"
        etBusca.setText("")
        listAllTransacoes()
    }

    fun filtrarReceitas(view: View) {
        filtroAtual = Transacao.RECEITA
        listAllTransacoes()
    }

    fun filtrarDespesas(view: View) {
        filtroAtual = Transacao.DESPESA
        listAllTransacoes()
    }

    // Atualiza a lista quando voltar de outra activity (adição, edição, exclusão)
    override fun onResume() {
        super.onResume()
        listAllTransacoes()
    }
}

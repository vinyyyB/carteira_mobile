package com.example.carteira.controller

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carteira.R
import com.example.carteira.data.dao.TransacaoDAO
import com.example.carteira.model.Transacao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewTransacao : AppCompatActivity() {

    private lateinit var transacaoDAO: TransacaoDAO
    private var transacaoID: Int = 0
    private lateinit var tvTitulo: TextView
    private lateinit var etDescricao: EditText
    private lateinit var etValor: EditText
    private lateinit var rgTipo: RadioGroup
    private lateinit var etData: EditText
    private lateinit var btnDelete: Button

    private val formatoTela = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    private val formatoBanco = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_transacao)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        transacaoDAO = TransacaoDAO(this)

        tvTitulo = findViewById(R.id.tvTitulo)
        etDescricao = findViewById(R.id.etDescricao)
        etValor = findViewById(R.id.etValor)
        rgTipo = findViewById(R.id.rgTipo)
        etData = findViewById(R.id.etData)
        btnDelete = findViewById(R.id.btnDelete)

        // Verifica se estamos em modo de edição (lançamento existente)
        transacaoID = intent.getIntExtra("transacaoID", 0)
        if (transacaoID != 0) {
            tvTitulo.text = "Editar lançamento"
            btnDelete.visibility = Button.VISIBLE
            editTransacao()
        } else {
            etData.setText(formatoTela.format(Date()))   // sugere a data de hoje
        }
    }

    fun saveTransacao(view: View) {
        val valor = etValor.text.toString().replace(",", ".").toDoubleOrNull()
        val data = dataParaBanco(etData.text.toString())

        if (etDescricao.text.isNotEmpty() && valor != null && data != null && rgTipo.checkedRadioButtonId != -1) {
            val tipo = if (rgTipo.checkedRadioButtonId == R.id.rbReceita) Transacao.RECEITA else Transacao.DESPESA

            if (transacaoID == 0) {
                // Inserção de um novo lançamento
                val newTransacao = Transacao(
                    descricao = etDescricao.text.toString(),
                    valor = valor,
                    tipo = tipo,
                    data = data
                )
                transacaoDAO.addTransacao(newTransacao)
                Toast.makeText(this, "Lançamento adicionado", Toast.LENGTH_SHORT).show()
            } else {
                // Atualização de um lançamento existente
                val updatedTransacao = Transacao(
                    id = transacaoID,
                    descricao = etDescricao.text.toString(),
                    valor = valor,
                    tipo = tipo,
                    data = data
                )
                transacaoDAO.updateTransacao(updatedTransacao)
                Toast.makeText(this, "Lançamento atualizado", Toast.LENGTH_SHORT).show()
            }
            finish()   // Volta para a MainActivity
        } else {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
        }
    }

    // Carrega o lançamento existente nos campos para edição
    private fun editTransacao() {
        val transacao = transacaoDAO.getTransacaoById(transacaoID)
        transacao?.let {
            etDescricao.setText(it.descricao)
            etValor.setText(it.valor.toString())
            if (it.tipo == Transacao.RECEITA) {
                findViewById<RadioButton>(R.id.rbReceita).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbDespesa).isChecked = true
            }
            etData.setText(dataParaTela(it.data))
        }
    }

    // Pede confirmação antes de excluir (exclusão segura)
    fun deleteTransacao(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Excluir lançamento")
            .setMessage("Deseja realmente excluir este lançamento?")
            .setPositiveButton("Excluir") { _, _ ->
                transacaoDAO.deleteTransacao(transacaoID)
                Toast.makeText(this, "Lançamento excluído", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // dd/mm/aaaa -> aaaa-mm-dd (retorna null se a data for inválida)
    private fun dataParaBanco(dataTela: String): String? {
        return try {
            formatoBanco.format(formatoTela.parse(dataTela)!!)
        } catch (e: Exception) {
            null
        }
    }

    // aaaa-mm-dd -> dd/mm/aaaa
    private fun dataParaTela(dataBanco: String): String {
        return try {
            formatoTela.format(formatoBanco.parse(dataBanco)!!)
        } catch (e: Exception) {
            dataBanco
        }
    }
}

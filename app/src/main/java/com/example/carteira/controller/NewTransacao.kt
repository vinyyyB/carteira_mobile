package com.example.carteira.controller

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
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

        aplicarMascaraValor()
        etData.setOnClickListener { abrirCalendario() }

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
        val descricao = etDescricao.text.toString().trim().replaceFirstChar { it.uppercase() }
        val valor = lerValor()
        val data = dataParaBanco(etData.text.toString())

        // ---- Validações antes de chamar o DAO ----
        if (descricao.isEmpty()) {
            mostrarErro(etDescricao, "Informe a descrição")
            return
        }
        if (valor <= 0) {
            mostrarErro(etValor, "Informe um valor maior que zero")
            return
        }
        if (rgTipo.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Selecione Receita ou Despesa", Toast.LENGTH_SHORT).show()
            return
        }
        if (data == null) {
            Toast.makeText(this, "Selecione a data", Toast.LENGTH_SHORT).show()
            abrirCalendario()
            return
        }

        val tipo = if (rgTipo.checkedRadioButtonId == R.id.rbReceita) Transacao.RECEITA else Transacao.DESPESA

        if (transacaoID == 0) {
            // Inserção de um novo lançamento
            val newTransacao = Transacao(
                descricao = descricao,
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
                descricao = descricao,
                valor = valor,
                tipo = tipo,
                data = data
            )
            transacaoDAO.updateTransacao(updatedTransacao)
            Toast.makeText(this, "Lançamento atualizado", Toast.LENGTH_SHORT).show()
        }
        finish()   // Volta para a MainActivity
    }

    // Carrega o lançamento existente nos campos para edição
    private fun editTransacao() {
        val transacao = transacaoDAO.getTransacaoById(transacaoID)
        transacao?.let {
            etDescricao.setText(it.descricao)
            etValor.setText(formatoMoeda.format(it.valor))
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

    // Máscara de moeda: o usuário digita só números e o campo mostra R$ 0,00
    private fun aplicarMascaraValor() {
        etValor.addTextChangedListener(object : TextWatcher {
            private var formatando = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (formatando) return
                formatando = true
                // Mantém só os dígitos (máximo 10 = R$ 99.999.999,99)
                val digitos = s.toString().filter { it.isDigit() }.take(10)
                if (digitos.isEmpty()) {
                    etValor.setText("")
                } else {
                    val texto = formatoMoeda.format(digitos.toLong() / 100.0)
                    etValor.setText(texto)
                    etValor.setSelection(texto.length)
                }
                formatando = false
            }
        })
    }

    // Converte o texto "R$ 1.234,56" de volta para 1234.56
    private fun lerValor(): Double {
        val digitos = etValor.text.toString().filter { it.isDigit() }
        if (digitos.isEmpty()) return 0.0
        return digitos.toLong() / 100.0
    }

    // Abre o calendário para escolher a data (evita data digitada errada)
    private fun abrirCalendario() {
        val calendario = Calendar.getInstance()
        try {
            formatoTela.parse(etData.text.toString())?.let { calendario.time = it }
        } catch (e: Exception) {
            // campo vazio ou inválido: abre na data de hoje
        }
        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                etData.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes + 1, ano))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun mostrarErro(campo: EditText, mensagem: String) {
        campo.error = mensagem
        campo.requestFocus()
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
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

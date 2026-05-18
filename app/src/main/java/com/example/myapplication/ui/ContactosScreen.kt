package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Contacto
import com.example.myapplication.data.ContactoDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(
    contactoDao: ContactoDao,
    onNavigateToMap: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    
    // Coleta a lista de forma reativa baseada na pesquisa
    val contactos by if (searchQuery.isBlank()) {
        contactoDao.listarContactos().collectAsState(initial = emptyList())
    } else {
        contactoDao.buscarContactos(searchQuery).collectAsState(initial = emptyList())
    }

    var mostrarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A Minha Agenda") },
                actions = {
                    Button(onClick = onNavigateToMap) {
                        Text("Ver Mapa")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pesquisar por nome ou telefone") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(contactos) { contacto ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = contacto.nome, style = MaterialTheme.typography.titleLarge)
                            Text(text = "Tel: ${contacto.telefone}")
                            Text(text = "Morada: ${contacto.endereco}")
                            // Aqui poderia adicionar botões para Editar ou Excluir o contacto
                        }
                    }
                }
            }
        }
    }

   if (mostrarDialog) {
        AdicionarContactoDialog(
            onDismiss = { mostrarDialog = false },
            onSave = { novoContacto ->
                coroutineScope.launch {
                    // Executa a gravação numa thread de fundo (segurança máxima)
                    withContext(Dispatchers.IO) {
                        contactoDao.inserirContacto(novoContacto)
                    }
                    mostrarDialog = false
                }
            }
        )
    }
}

@Composable
fun AdicionarContactoDialog(onDismiss: () -> Unit, onSave: (Contacto) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    // Mudamos os nomes destas variáveis para evitar confusão de texto vs números decimais
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Contacto") },
        text = {
            Column {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") })
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") })
                OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Latitude (Opcional)") })
                OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Longitude (Opcional)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                // Convertemos o texto para número decimal de forma segura antes de guardar
                val latitudeDecimal = latStr.toDoubleOrNull()
                val longitudeDecimal = lngStr.toDoubleOrNull()
                
                onSave(Contacto(
                    nome = nome, 
                    telefone = telefone, 
                    email = "", 
                    endereco = endereco,
                    latitude = latitudeDecimal,
                    longitude = longitudeDecimal
                ))
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
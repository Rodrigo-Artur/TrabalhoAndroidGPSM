package com.example.myapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.myapplication.data.Contacto
import com.example.myapplication.data.ContactoDao
import com.example.myapplication.utils.MapUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(
    contactoDao: ContactoDao,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var searchQuery by remember { mutableStateOf("") }
    var mostrarDialog by remember { mutableStateOf(false) }
    
    // Variáveis para a busca por proximidade
    var ordenarPorProximidade by remember { mutableStateOf(false) }
    var localizacaoAtual by remember { mutableStateOf<android.location.Location?>(null) }

    // Coleta a lista do banco de dados (por Nome/Busca)
    val contactosDB by if (searchQuery.isBlank()) {
        contactoDao.listarContactos().collectAsState(initial = emptyList())
    } else {
        contactoDao.buscarContactos(searchQuery).collectAsState(initial = emptyList())
    }

    // Lógica para ordenar por proximidade (CORRIGIDA COM !!)
    val contactosParaMostrar = if (ordenarPorProximidade && localizacaoAtual != null) {
        contactosDB.sortedBy { contacto ->
            if (contacto.latitude != null && contacto.longitude != null) {
                MapUtils.calcularDistancia(
                    localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                    contacto.latitude!!, contacto.longitude!!
                )
            } else {
                Float.MAX_VALUE // Contatos sem GPS vão para o fim da lista
            }
        }
    } else {
        contactosDB
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A Minha Agenda") },
                actions = {
                    // Botão para Ordenar por Proximidade
                    IconButton(onClick = {
                        if (!ordenarPorProximidade) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        localizacaoAtual = loc
                                        ordenarPorProximidade = true
                                        Toast.makeText(context, "A mostrar os mais próximos", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ligue o GPS primeiro", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            ordenarPorProximidade = false // Desliga a ordenação
                        }
                    }) {
                        Icon(
                            Icons.Filled.LocationSearching, 
                            contentDescription = "Ordenar por Proximidade",
                            tint = if (ordenarPorProximidade) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Button(onClick = onNavigateToMap) {
                        Text("Mapa")
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
                items(contactosParaMostrar) { contacto ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contacto.nome, style = MaterialTheme.typography.titleLarge)
                                Text(text = "Tel: ${contacto.telefone}")
                                if (contacto.email.isNotBlank()) Text(text = "Email: ${contacto.email}")
                                Text(text = "Morada: ${contacto.endereco}")
                                
                                // Mostra a distância se o filtro estiver ativo (CORRIGIDO COM !!)
                                if (ordenarPorProximidade && localizacaoAtual != null && contacto.latitude != null && contacto.longitude != null) {
                                    val dist = MapUtils.calcularDistancia(
                                        localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                                        contacto.latitude!!, contacto.longitude!!
                                    )
                                    Text(text = "Distância: ${String.format("%.1f", dist / 1000)} km", color = Color.Blue)
                                }
                            }

                            // Botão Excluir (CRUD Completo)
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) { contactoDao.excluirContacto(contacto) }
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = Color.Red)
                            }
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
                    // Abrimos a thread de IO para consultas à rede e banco de dados
                    withContext(Dispatchers.IO) {
                        var contactoParaSalvar = novoContacto

                        // Se o utilizador deixou as coordenadas em branco, usamos o endereço de texto
                        if (contactoParaSalvar.latitude == null || contactoParaSalvar.longitude == null) {
                            if (contactoParaSalvar.endereco.isNotBlank()) {
                                val addressResolved = MapUtils.obterCoordenadasPorEndereco(context, contactoParaSalvar.endereco)
                                if (addressResolved != null) {
                                    // Injeta as coordenadas descobertas pelo Geocoder no contacto
                                    contactoParaSalvar = contactoParaSalvar.copy(
                                        latitude = addressResolved.latitude,
                                        longitude = addressResolved.longitude
                                    )
                                }
                            }
                        }

                        // Grava o contacto (com ou sem as coordenadas descobertas)
                        contactoDao.inserirContacto(contactoParaSalvar)
                    }
                    mostrarDialog = false
                }
            }
        )
    }
}

@Composable
fun AdicionarContactoDialog(onDismiss: () -> Unit, onSave: (Contacto) -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Contacto") },
        text = {
            Column {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") })
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão Mágico: Capturar GPS Atual
                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    latStr = loc.latitude.toString()
                                    lngStr = loc.longitude.toString()
                                    Toast.makeText(context, "GPS Capturado!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Ligue a Localização do telemóvel", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Sem permissão de GPS", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📍 Usar Minha Localização Atual")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val latitudeDecimal = latStr.toDoubleOrNull()
                val longitudeDecimal = lngStr.toDoubleOrNull()

                onSave(Contacto(
                    nome = nome,
                    telefone = telefone,
                    email = email,
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
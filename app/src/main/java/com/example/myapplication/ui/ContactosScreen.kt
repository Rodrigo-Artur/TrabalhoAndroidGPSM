package com.example.myapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun ContactosScreen(contactoDao: ContactoDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var searchQuery by remember { mutableStateOf("") }
    
    // Controlo do Bottom Sheet
    var showBottomSheet by remember { mutableStateOf(false) }
    var contactoEmEdicao by remember { mutableStateOf<Contacto?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var ordenarPorProximidade by remember { mutableStateOf(false) }
    var localizacaoAtual by remember { mutableStateOf<android.location.Location?>(null) }

    val contactosDB by if (searchQuery.isBlank()) {
        contactoDao.listarContactos().collectAsState(initial = emptyList())
    } else {
        contactoDao.buscarContactos(searchQuery).collectAsState(initial = emptyList())
    }

    val contactosParaMostrar = if (ordenarPorProximidade && localizacaoAtual != null) {
        contactosDB.sortedBy { contacto ->
            if (contacto.latitude != null && contacto.longitude != null) {
                MapUtils.calcularDistancia(
                    localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                    contacto.latitude!!, contacto.longitude!!
                )
            } else Float.MAX_VALUE 
        }
    } else contactosDB

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Meus Contatos", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        if (!ordenarPorProximidade) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        localizacaoAtual = loc
                                        ordenarPorProximidade = true
                                    } else {
                                        Toast.makeText(context, "Ligue o GPS", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else ordenarPorProximidade = false
                    }) {
                        Icon(
                            Icons.Filled.MyLocation, 
                            contentDescription = "Proximidade",
                            tint = if (ordenarPorProximidade) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    contactoEmEdicao = null
                    showBottomSheet = true 
                },
                icon = { Icon(Icons.Filled.Add, "Adicionar") },
                text = { Text("Novo") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar contatos...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Pesquisar") },
                shape = RoundedCornerShape(100.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contactosParaMostrar) { contacto ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Circular
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contacto.nome.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Informações
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contacto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = contacto.telefone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (ordenarPorProximidade && localizacaoAtual != null && contacto.latitude != null && contacto.longitude != null) {
                                    val dist = MapUtils.calcularDistancia(
                                        localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                                        contacto.latitude!!, contacto.longitude!!
                                    )
                                    Text(text = "📍 A ${String.format("%.1f", dist / 1000)} km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Ações
                            Column {
                                IconButton(onClick = {
                                    contactoEmEdicao = contacto
                                    showBottomSheet = true
                                }) { Icon(Icons.Filled.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                                
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) { contactoDao.excluirContacto(contacto) }
                                    }
                                }) { Icon(Icons.Filled.DeleteOutline, "Excluir", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            FormularioBottomSheet(
                contactoExistente = contactoEmEdicao,
                onSave = { novoContacto ->
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            var contactoSalvar = novoContacto
                            if (contactoSalvar.latitude == null || contactoSalvar.longitude == null) {
                                if (contactoSalvar.endereco.isNotBlank()) {
                                    val address = MapUtils.obterCoordenadasPorEndereco(context, contactoSalvar.endereco)
                                    if (address != null) {
                                        contactoSalvar = contactoSalvar.copy(latitude = address.latitude, longitude = address.longitude)
                                    }
                                }
                            }
                            contactoDao.inserirContacto(contactoSalvar)
                        }
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun FormularioBottomSheet(contactoExistente: Contacto?, onSave: (Contacto) -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var nome by remember { mutableStateOf(contactoExistente?.nome ?: "") }
    var telefone by remember { mutableStateOf(contactoExistente?.telefone ?: "") }
    var email by remember { mutableStateOf(contactoExistente?.email ?: "") }
    var endereco by remember { mutableStateOf(contactoExistente?.endereco ?: "") }
    var latStr by remember { mutableStateOf(contactoExistente?.latitude?.toString() ?: "") }
    var lngStr by remember { mutableStateOf(contactoExistente?.longitude?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
        Text(
            text = if (contactoExistente != null) "Editar Contato" else "Novo Contato",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço Completo") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Lat (Opcional)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Lng (Opcional)") }, modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            latStr = loc.latitude.toString()
                            lngStr = loc.longitude.toString()
                        } else Toast.makeText(context, "Ative a Localização", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = getDefaultButtonVariant(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capturar GPS Atual")
        }

        Button(
            onClick = {
                onSave(Contacto(
                    id = contactoExistente?.id ?: 0,
                    nome = nome, telefone = telefone, email = email, endereco = endereco,
                    latitude = latStr.toDoubleOrNull(), longitude = lngStr.toDoubleOrNull()
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Contato", modifier = Modifier.padding(8.dp))
        }
    }
}

// Função auxiliar para evitar erro de versão no Compose
@Composable
private fun getDefaultButtonVariant(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
)
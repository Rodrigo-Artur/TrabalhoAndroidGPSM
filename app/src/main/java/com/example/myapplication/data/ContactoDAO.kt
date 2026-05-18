package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun inserirContacto(contacto: Contacto) // Sem 'suspend' e sem retorno!

    @Delete
    fun excluirContacto(contacto: Contacto) // Sem 'suspend' e sem retorno!

    @Query("SELECT * FROM contactos ORDER BY nome ASC")
    fun listarContactos(): Flow<List<Contacto>>

    // Busca inteligente por Nome ou Telefone
    @Query("SELECT * FROM contactos WHERE nome LIKE '%' || :termo || '%' OR telefone LIKE '%' || :termo || '%'")
    fun buscarContactos(termo: String): Flow<List<Contacto>>
}
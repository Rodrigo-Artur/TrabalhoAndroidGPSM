import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirContacto(contacto: Contacto) // Cadastrar e Editar

    @Delete
    suspend fun excluirContacto(contacto: Contacto) // Excluir

    @Query("SELECT * FROM contactos ORDER BY nome ASC")
    fun listarContactos(): Flow<List<Contacto>> // Listar

    @Query("SELECT * FROM contactos WHERE nome LIKE '%' || :termo || '%' OR telefone LIKE '%' || :termo || '%'")
    fun buscarContactos(termo: String): Flow<List<Contacto>> // Busca inteligente por Nome ou Telefone
}